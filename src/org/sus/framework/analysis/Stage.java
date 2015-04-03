package org.sus.framework.analysis;

import java.util.Set;

import org.sus.framework.util.Access;
import org.sus.framework.util.Pair;

public abstract class Stage
{
	// Dictates the priority of stages that would 
	// be cut out for performance reasons.
	private int stagePriority;
	
	public Stage(int stagePriority)
	{
		this.stagePriority = stagePriority;
	}
	
	public int getStagePriority()
	{
		return this.stagePriority;
	}
	
	public abstract void computeAccessPairs();
	public abstract Set<Pair<Access, Access>> getResult();
	
	// List of stages compute static data races
	public static enum STAGES
	{
		ORIGINAL_PAIRS,
		REACHABLE_PAIRS,
		ALIASING_PAIRS,
		ESCAPING_PAIRS,
		UNLOCKED_PAIRS
	}
}
