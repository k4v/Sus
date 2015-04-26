package org.sus.framework.analysis;

import java.util.Set;

public class AliasAnalysis
{
	private static AliasAnalysis aliasAnalysis = null;
	
	protected static AliasAnalysis initAnalysis()
	{
		if(aliasAnalysis == null)
		{
			aliasAnalysis = new AliasAnalysis();
		}
		
		return aliasAnalysis;
	}
	
	protected void getVariableAnalysis(Set<ThreadProperties> startedRunnables)
	{
		
	}
}
