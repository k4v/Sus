package org.sus.framework.util;

import java.util.Iterator;
import java.util.List;

import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class VariableAccess
{
	private SootField accessedField;
	private Stmt accessStmt;
	private SootMethod accessMethod;
	private boolean isWrite;
	private boolean isStatic;
	private List<Value> lockObjects;
	
	public VariableAccess(SootField accessedField, Stmt sootStmt, SootMethod accessMethod, boolean isWrite, boolean isStatic, List<Value> lockObjects)
	{
		this.accessedField = accessedField;
		this.accessStmt = sootStmt;
		this.accessMethod = accessMethod;
		this.isWrite = isWrite;
		this.isStatic = isStatic;
		this.lockObjects = lockObjects;
	}
	
	public SootField getField()
	{
		return this.accessedField;
	}
	
	public Stmt getAccessStmt()
	{
		return this.accessStmt;
	}
	
	public SootMethod getAccessMethod()
	{
		return this.accessMethod;
	}

	public boolean isWriteAccess()
	{
		return this.isWrite;
	}
	
	public boolean isStaticField()
	{
		return this.isStatic;
	}
	
	protected Iterator<Value> getLockIterator()
	{
		return lockObjects.iterator();
	}
	
	public boolean hasCommonLock(VariableAccess otherAccess)
	{
		Iterator<Value> otherIt = otherAccess.getLockIterator();
		while(otherIt.hasNext())
		{
			Value otherLock = otherIt.next();
			for(Value thisLock : lockObjects)
			{
				if(otherLock.equivTo(thisLock))
				{
					return true;
				}
				// Placeholder till we figure out how to save and compare lock objects
				return true;
			}
		}
		
		return false;
	}
}
