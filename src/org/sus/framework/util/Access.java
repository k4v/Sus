package org.sus.framework.util;

import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NullConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;

/**
 * @author karthik
 *
 * The Access class is used to represent a memory access.
 * When Soot encounters a FieldRef statement, the memory
 * access is stored is saved in this object.
 */

public class Access
{
	private Stmt    sootStmt;
	private boolean isStatic;
	private boolean isWrite;
	private Object  accessObject;
	private Value   accessValue;
	
	public Access(Stmt sootStmt)
	{
		// If this does not involve a variable access,
		// this is not a statement we're interested in.
		if(!sootStmt.containsFieldRef())
		{
			throw new IllegalArgumentException("Access statement does not contain field reference");
		}
		
		this.sootStmt = sootStmt;
		processStatement();
	}
	
	private void processStatement()
	{
		this.isWrite = (((DefinitionStmt)sootStmt).getLeftOp() instanceof FieldRef);
		this.isStatic = (sootStmt.getFieldRef() instanceof StaticFieldRef);
		this.accessObject = isStatic ? NullConstant.v() : ((InstanceFieldRef)sootStmt.getFieldRef()).getBase();
		this.accessValue = isWrite ? ((DefinitionStmt)sootStmt).getRightOp() : ((DefinitionStmt)sootStmt).getLeftOp();
	}
	
	public boolean getIsWrite()
	{
		return isWrite;
	}
	
	public boolean getIsStatic()
	{
		return isStatic;
	}
	
	public Stmt getStatement()
	{
		return this.sootStmt;
	}
}
