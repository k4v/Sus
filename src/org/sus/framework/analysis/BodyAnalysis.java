package org.sus.framework.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sus.framework.util.SusHelper;
import org.sus.framework.util.VariableAccess;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeStmt;
import soot.jimple.NullConstant;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;

public class BodyAnalysis extends BodyTransformer
{
	private static BodyAnalysis bodyAnalysis = null;
	
	// Loop-depth of current statement
	private int loopLevel = 0;
	
	// Maps each thread and runnable variable to it's concrete type.
	// Allows us to find which run() function will be called.
	private Map<Value, Type> innerRunnable = new HashMap<Value, Type>();
	
	// Runnable types on which .run() is actually called.
	// Also stores the statement and whether run() was called within a loop
	Set<ThreadProperties> startedRunnables = new HashSet<ThreadProperties>();
	Map<String, Set<VariableAccess>> variableAccesses = new HashMap<String, Set<VariableAccess>>();
	
	private List<Value> lockStack = new ArrayList<Value>();
	boolean expSecondExitMonitor = false;
	
	protected static BodyAnalysis initAnalysis()
	{
		if(bodyAnalysis == null)
		{
			bodyAnalysis = new BodyAnalysis();
			PackManager.v().getPack("jtp").add(new Transform("jtp.TestSoot", bodyAnalysis));
		}
		
		return bodyAnalysis;
	}
	
	@Override
	protected void internalTransform(Body methodBody, String phaseName, Map<String, String> options)
	{
		// Find loop statements in this method body
		Set<Stmt> loopEntryStmts = new HashSet<Stmt>();
		Set<Stmt> loopExitStmts = new HashSet<Stmt>();
		LoopNestTree loopNestTree = new LoopNestTree(methodBody);
		for (Loop loop : loopNestTree)
        {
            loopEntryStmts.add(loop.getHead());
        	loopExitStmts.addAll(loop.targetsOfLoopExit((Stmt)(loop.getLoopExits().toArray()[0])));
        }
        
		Iterator<Unit> unitIterator = methodBody.getUnits().snapshotIterator();
		while(unitIterator.hasNext())
	    {
	    	// Read each program statement as Jimple
	    	Stmt sootStmt = (Stmt)unitIterator.next();
	    	
	    	// If the current statement exits a loop, mark it
	    	if((loopLevel > 0) && loopExitStmts.contains(sootStmt))
	    	{
	    		loopLevel--;
	    	}
	    	// If the current statement enters a loop, mark it
	    	if(loopEntryStmts.contains(sootStmt))
	    	{
	    		loopLevel++;
	    	}
	    	
	    	checkAccessStmt(sootStmt, methodBody.getMethod());
	    	checkSynchronizationStmt(sootStmt);
	    	
	    	// If a new thread variable has been created, save the variable and wait for it's .start() method
	    	checkThreadCreationStmt(sootStmt);
	    	// Check if thread.start() was called. If so, save the type 
	    	Value startedRunnable = null;
	    	if ((startedRunnable = checkThreadStartStmt(sootStmt)) != null)
	    	{
	    		// .start() was called on this Runnable. Save it.
	    		Type runnableType = innerRunnable.get(startedRunnable);
	    		if(runnableType != null)
	    		{
	    			startedRunnables.add(new ThreadProperties(startedRunnable, runnableType, sootStmt, (loopLevel > 0)));
	    			//System.out.println("Saving runnable value "+startedRunnable+" at "+sootStmt);
	    		}
	    	}
	    }
	}
	
	// Check if the current statement is a thread creation statement
	private boolean checkThreadCreationStmt(Stmt sootStmt)
	{
		// Check 1: If the thread is created in a new Thread() constructor 
    	if(sootStmt.containsInvokeExpr())
    	{
    		// Check if the current statement is a constructor
    		if(sootStmt.getInvokeExpr() instanceof SpecialInvokeExpr)
    		{
    			// It's a constructor.
    			SpecialInvokeExpr constructorExpr = (SpecialInvokeExpr)sootStmt.getInvokeExpr();
    			// The actual class being constructed
    			String className = SusHelper.getClassFromSignature(constructorExpr.getMethod().getSignature());
    			try
    			{
    				// We're constructing a new Thread() or new Thread(Runnable)
    				if(Thread.class.isAssignableFrom(
    						Class.forName(className, false, this.getClass().getClassLoader())))
    				{
    					String argumentClass = (constructorExpr.getArgCount() == 0) ? null :
    						constructorExpr.getArg(0).getType().toString();
    					
    					// We have something like new Thread(? extends Runnable)
    					boolean runnableInvocation =
    							(argumentClass != null) &&
    							(className.equals("java.lang.Thread")) &&
    							(java.lang.Runnable.class.isAssignableFrom(
    									Class.forName(argumentClass, false, this.getClass().getClassLoader())));
    					
    					// Thread constructed as new Thread(Runnable)
    					if(runnableInvocation)
    					{
    						Type runnableType = innerRunnable.get(constructorExpr.getArg(0));
    						if(runnableType != null)
    						{
    							innerRunnable.put(constructorExpr.getBase(), runnableType);
    							//System.out.println("Setting "+constructorExpr.getBase()+" as type: "+runnableType);
    							return true;
    						}
    					}
    					else if(!className.equals("java.lang.Thread"))
    					{
    						// It's a constructor statement. new <? extends Thread>. Now get the assignee
    						innerRunnable.put(constructorExpr.getBase(), constructorExpr.getBase().getType());
    						//System.out.println("Setting "+constructorExpr.getBase()+" as type: "+constructorExpr.getBase().getType());
    						return true;
    					}
    				}
    				// We're doing a new Runnable() now. Not thread creation yet.
    				else if(java.lang.Runnable.class.isAssignableFrom(
							Class.forName(className, false, this.getClass().getClassLoader())))
    				{
    					innerRunnable.put(constructorExpr.getBase(), constructorExpr.getBase().getType());
    				}
    			} catch (Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    	}
    	// Check 2: Newly constructed runnables are only assigned to a Runnable variable
    	// after the constructor ends. Hence this is required.
    	// Same for thread constructions
    	else if (sootStmt instanceof DefinitionStmt)
    	{
    		// This section makes sure all variables that are assigned Runnable values are marked with the right type
    		DefinitionStmt defStmt = (DefinitionStmt)sootStmt;
    		if(innerRunnable.containsKey(defStmt.getRightOp()))
    		{
    			innerRunnable.put(defStmt.getLeftOp(), innerRunnable.get(defStmt.getRightOp()));
    			//System.out.println("Setting "+defStmt.getLeftOp()+" as type: "+innerRunnable.get(defStmt.getRightOp()));
    		}
    	}
    	
    	return false;
	}
	
	private Value checkThreadStartStmt(Stmt sootStmt)
	{
		// Check for function calls 
    	if(sootStmt instanceof InvokeStmt)
    	{
    		InvokeStmt invokeStmt = (InvokeStmt)sootStmt;
    		// Check if the current statement is a constructor
    		if(invokeStmt.getInvokeExpr() instanceof VirtualInvokeExpr)
    		{
    			// It's a constructor. Check if it's a Thread allocation site
    			VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr)invokeStmt.getInvokeExpr();
    			String functionSignature = invokeExpr.getMethod().getSignature();
    			String className = SusHelper.getClassFromSignature(functionSignature);
    			String functionName = invokeExpr.getMethod().getName();
    			
    			try
    			{
    				if((Thread.class.isAssignableFrom(
    						Class.forName(className, false, this.getClass().getClassLoader()))) &&
	    					(functionName.equals("start")) &&
	    					(invokeExpr.getMethod().getParameterCount() == 0))
    				{
    					//System.out.println("Invoke method "+invokeExpr.getMethod()+" on "+invokeExpr.getBase());
    					//System.out.println("run() called on "+innerRunnable.get(invokeExpr.getBase()));
    					return invokeExpr.getBase();
    				}
    			} catch (ClassNotFoundException e)
    			{
    				System.err.println("Unable to find class "+className);
    			}
    		}
    	}
    	return null;
	}
	
	private void checkAccessStmt(Stmt sootStmt, SootMethod currentMethod)
	{
		if(sootStmt.containsFieldRef())
		{
			SootField accessField = sootStmt.getFieldRef().getField();
    		boolean isWrite = (((DefinitionStmt)sootStmt).getLeftOp() instanceof FieldRef);
    		boolean isStatic = (sootStmt.getFieldRef() instanceof StaticFieldRef);
    		Value object = isStatic ? NullConstant.v() : ((InstanceFieldRef)sootStmt.getFieldRef()).getBase();
    		
    		String methodSig = currentMethod.getSignature();
    		
    		List<Value> currentLocks = new ArrayList<Value>(lockStack);
    		
    		System.out.println(isWrite+" access on "+isStatic+" field "+accessField+" of "+object+" in "+methodSig+" with "+currentLocks.size()+" locks");
    		
    		if(!variableAccesses.containsKey(methodSig))
    		{
    			variableAccesses.put(methodSig, new HashSet<VariableAccess>());
    		}
    		variableAccesses.get(currentMethod.getSignature()).add(
    				new VariableAccess(accessField, sootStmt, currentMethod, isWrite, isStatic, currentLocks));
		}
	}
	
	private void checkSynchronizationStmt(Stmt sootStmt)
	{
		if(sootStmt instanceof EnterMonitorStmt)
		{
			EnterMonitorStmt enterMonitorStmt = (EnterMonitorStmt)sootStmt;
			lockStack.add(0, enterMonitorStmt.getOp());
			expSecondExitMonitor = false;
		}
		else if(sootStmt instanceof ExitMonitorStmt)
		{
			if(!expSecondExitMonitor)
			{
				lockStack.remove(0);
				expSecondExitMonitor = true;
			} else
			{
				expSecondExitMonitor = false;
			}
		}
	}
}
