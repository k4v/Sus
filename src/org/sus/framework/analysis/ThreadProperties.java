package org.sus.framework.analysis;

import java.util.HashSet;
import java.util.Set;

import org.sus.framework.util.Pair;
import org.sus.framework.util.VariableAccess;

import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.Stmt;

public class ThreadProperties
{
	private Value createdRunnable;
	private Type runnableType;
	private Stmt threadStartStmt;
	private boolean isMultiplyExecuted;

	// The run() method that starts this thread
	private SootMethod runMethod;
	
	private Set<SootMethod> directReachableMethods;
	private Set<SootMethod> transitiveReachableMethods;
	
	private Set<VariableAccess> variableAccesses;
	
	public ThreadProperties(Value createdRunnable, Type runnableType, Stmt threadStartStmt, boolean isMultiplyExecuted)
	{
		this.createdRunnable = createdRunnable;
		this.runnableType = runnableType;
		this.threadStartStmt = threadStartStmt;
		this.isMultiplyExecuted = isMultiplyExecuted;
		
		this.directReachableMethods = new HashSet<SootMethod>();
		this.transitiveReachableMethods = new HashSet<SootMethod>();
	}
	
	public void addDirectReachableMethod(SootMethod newMethod)
	{
		this.directReachableMethods.add(newMethod);
	}
	
	public Set<SootMethod> getDirectReachableMethods()
	{
		return this.directReachableMethods;
	}
	
	public void addTransitiveReachableMethod(SootMethod newMethod)
	{
		this.transitiveReachableMethods.add(newMethod);
	}
	
	public Set<SootMethod> getTransitiveReachableMethods()
	{
		return this.transitiveReachableMethods;
	}
	
	public void setRunMethod(SootMethod runMethod)
	{
		this.runMethod = runMethod;
	}
	
	public SootMethod getRunMethod()
	{
		return this.runMethod;
	}

	public Value getCreatedRunnable()
	{
		return this.createdRunnable;
	}
	
	public Type getRunnableType()
	{
		return this.runnableType;
	}
	
	public Stmt getThreadStartStmt()
	{
		return this.threadStartStmt;
	}
	
	public boolean isThreadMultiplyStarted()
	{
		return this.isMultiplyExecuted;
	}
	
	public void setVariableAccessses(Set<VariableAccess> variableAccesses)
	{
		this.variableAccesses = variableAccesses;
	}
	
	public Set<VariableAccess> getVariableAccessses()
	{
		return this.variableAccesses;
	}
	
	public Set<Pair<VariableAccess, VariableAccess>> detectRaceAccesses(ThreadProperties anotherThread)
	{
		Set<Pair<VariableAccess, VariableAccess>> raceAccesses = new HashSet<Pair<VariableAccess, VariableAccess>>();
		Set<VariableAccess> otherAccesses = anotherThread.getVariableAccessses();
		
		for(VariableAccess thisAccess : this.variableAccesses)
		{
			for(VariableAccess otherAccess : otherAccesses)
			{
				System.out.println("Comparing access in line "+thisAccess.getAccessStmt().getJavaSourceStartLineNumber()+" and "+otherAccess.getAccessStmt().getJavaSourceStartLineNumber());
				if(thisAccess.getField().equals(otherAccess.getField()))
				{
					boolean writeOrFunction = (thisAccess.getField().getType() instanceof PrimType ?
							(thisAccess.isWriteAccess() || otherAccess.isWriteAccess()) :
							(thisAccess.getAccessStmt().containsInvokeExpr() || otherAccess.getAccessStmt().containsInvokeExpr()));
					boolean monitorLock = thisAccess.hasCommonLock(otherAccess);
					
					if(writeOrFunction && !monitorLock)
					{
						System.out.println("Race");
						raceAccesses.add(new Pair<VariableAccess, VariableAccess>(thisAccess, otherAccess));
					}
				}
			}
		}
		return raceAccesses;
	}
}
