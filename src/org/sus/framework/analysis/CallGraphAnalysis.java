package org.sus.framework.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sus.framework.util.SusHelper;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PhaseOptions;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.Targets;
import soot.jimple.toolkits.callgraph.TransitiveTargets;
import soot.options.Options;

public class CallGraphAnalysis extends SceneTransformer
{
	private static CallGraphAnalysis callGraphAnalysis = null;
	
	private CallGraph callGraph = null;

	protected static CallGraphAnalysis initAnalysis()
	{
		// Add an intra-procedural analysis phase to Soot
	    if(callGraphAnalysis == null)
	    {
	    	CallGraphAnalysis callGraphAnalysis = new CallGraphAnalysis();
	    	
		    PackManager.v().getPack("wjtp").add(new Transform("wjtp.TestSootCallGraph", callGraphAnalysis));
	
		    //Perform analysis for the whole program
		    Options.v().set_whole_program(true);
		    
		    excludeJDKLibrary();
	
	        //Enable the Spark (PTA) call graph
		    enableSparkCallGraph();
	    }
	    
	    return callGraphAnalysis;
	}
	
	private static void enableSparkCallGraph()
	{
		//Enable Spark
		HashMap<String,String> opt = new HashMap<String,String>();
		opt.put("on-fly-cg","true");
	    SparkTransformer.v().transform("",opt);
	    PhaseOptions.v().setPhaseOption("cg.spark", "enabled:true");
	}
	
	private static void excludeJDKLibrary()
	{
		List<String> excludeList = SusHelper.getJdkPackageList();
		Options.v().set_exclude(excludeList);
		// This option must be disabled for a sound call graph
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
	}

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options)
	{
		System.out.println("Proceeding with call graph generation");
	}
	
	protected void getIntersectingMethods(Set<ThreadProperties> startedRunnables)
	{
		if(this.callGraph == null)
		{
			this.callGraph = Scene.v().getCallGraph();
		}
		
		for(ThreadProperties threadProperties : startedRunnables)
		{
			Type classType = threadProperties.getRunnableType();
			SootClass runnableClass = Scene.v().loadClassAndSupport(classType.toString());
			
			for(SootMethod sootMethod : runnableClass.getMethods())
			{
				// Only consider run() methods
				if(!sootMethod.getDeclaration().equals("public void run()"))
				{
					continue;
				}
				
				// Get directly reachable and transitive targets from this function
				ReachableMethods directTargets = new ReachableMethods(this.callGraph, new Targets(callGraph.edgesOutOf(sootMethod)));
				TransitiveTargets transitiveTargets = new TransitiveTargets(this.callGraph);
				
				Iterator<MethodOrMethodContext> transitiveIterator = transitiveTargets.iterator(sootMethod);
				while (transitiveIterator.hasNext())
				{
					SootMethod transitiveTarget = (SootMethod) transitiveIterator.next();
					// Show all target methods not in JDK
					if(!SusHelper.isPackageInJdk(SusHelper.getClassFromSignature(transitiveTarget.getSignature())))
					{
						// This method is directly reachable from this run()
						if(directTargets.contains(transitiveTarget))
						{
							threadProperties.addDirectReachableMethod(transitiveTarget);
							System.out.println(sootMethod + " may call " + transitiveTarget);
						}
						// This method is transitively reachable from this run()
						else
						{
							threadProperties.addTransitiveReachableMethod(transitiveTarget);
							System.out.println(sootMethod + " may reach " + transitiveTarget);
						}
					}
				}
			}
		}
	}
}
