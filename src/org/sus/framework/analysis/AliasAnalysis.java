package org.sus.framework.analysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sus.framework.util.Pair;
import org.sus.framework.util.VariableAccess;

import soot.SootMethod;

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
	
	protected void getVariableAnalysis(Set<ThreadProperties> startedRunnables, Map<String, Set<VariableAccess>> variableAccesses)
	{
		Set<Pair<VariableAccess, VariableAccess>> raceAccesses = new HashSet<Pair<VariableAccess, VariableAccess>>();
		
		for (ThreadProperties threadProperties : startedRunnables)
		{
			if(variableAccesses.get(threadProperties.getRunMethod().getSignature()) != null)
				threadProperties.setVariableAccessses(variableAccesses.get(threadProperties.getRunMethod().getSignature()));
			
			for(SootMethod reachableMethod : threadProperties.getDirectReachableMethods())
			{
				if(variableAccesses.get(reachableMethod.getSignature()) != null)
					threadProperties.setVariableAccessses(variableAccesses.get(reachableMethod.getSignature()));
			}
			
			// For non-main threads
			if(threadProperties.getCreatedRunnable() != null)
			{
				for(SootMethod transitiveTarget : threadProperties.getTransitiveReachableMethods())
				{
					if(variableAccesses.get(transitiveTarget.getSignature()) != null)
						threadProperties.setVariableAccessses(variableAccesses.get(transitiveTarget.getSignature()));
				}
			}
		}
		
		for(ThreadProperties firstThread : startedRunnables)
		{
			for(ThreadProperties secondThread : startedRunnables)
			{
				//if(!firstThread.equals(secondThread))
					raceAccesses.addAll(firstThread.detectRaceAccesses(secondThread));
			}
		}
		
		System.out.println(raceAccesses.size()+" race accesses found");
		for(Pair<VariableAccess, VariableAccess> raceAccess : raceAccesses)
		{
			System.out.println(raceAccess.getFirst().getAccessStmt()+" and "+raceAccess.getSecond().getAccessStmt());
		}
	}
}
