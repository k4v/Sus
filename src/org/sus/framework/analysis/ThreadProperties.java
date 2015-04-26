package org.sus.framework.analysis;

import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;

public class ThreadProperties
{
	private Type runnableType;
	private Stmt threadStartStmt;
	private boolean isMultiplyExecuted;
	
	private Set<SootMethod> directReachableMethods;
	private Set<SootMethod> transitiveReachableMethods;
	
	public ThreadProperties(Type runnableType, Stmt threadStartStmt, boolean isMultiplyExecuted)
	{
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
	
	public void addTransitiveReachableMethod(SootMethod newMethod)
	{
		this.transitiveReachableMethods.add(newMethod);
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
}
