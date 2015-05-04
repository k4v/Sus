package org.sus.framework.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.Set;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class AnalysisLoader
{
	static LinkedList<String> excludeList;
	
	public void main(String mainClass)
	{
		// Set Soot classpath
	    String bootPath = System.getProperty("sun.boot.class.path");
	    String javaPath = System.getProperty("java.class.path");
	    String path = bootPath + File.pathSeparator +javaPath;
	    Scene.v().setSootClassPath(path);

	    // Add intra-procedural analysis phases to Soot
	    BodyAnalysis susBodyAnalysis = BodyAnalysis.initAnalysis();
	    CallGraphAnalysis susCallGraphAnalysis = CallGraphAnalysis.initAnalysis();
	    
	    // Load and set main class
	    Options.v().set_app(true);
	    Options.v().set_keep_line_number(true);
	    SootClass appClass = Scene.v().loadClassAndSupport(mainClass);
	    Scene.v().setMainClass(appClass);
	    Scene.v().loadNecessaryClasses();

	    // Start working
	    PackManager.v().runPacks();
	    
	    // Now we have all Thread/Runnable classes for which .start() is called.
	    // Now we run call graph analysis on them
	    Set<ThreadProperties> startedRunnables = susBodyAnalysis.startedRunnables;
	    susCallGraphAnalysis.addMainClassToRunnables(mainClass, startedRunnables);
	    susCallGraphAnalysis.getReachableMethodsFromThreads(startedRunnables);
	    
	    // Now we have all methods reachable from known Runnable.run() methods.
	    // Now do thread escape and alias analysis. These are inter-procedural. 
	    AliasAnalysis susAliasAnalysis = AliasAnalysis.initAnalysis();
	    susAliasAnalysis.getVariableAnalysis(startedRunnables, susBodyAnalysis.variableAccesses);
	    
	    System.out.println("It's all over!");
	}
}
