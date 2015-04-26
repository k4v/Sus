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
	
	public static void main(String[] args)
	{
		String mainClass = "org.sus.framework.test.HelloThread";

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
	    SootClass appclass = Scene.v().loadClassAndSupport(mainClass);
	    Scene.v().setMainClass(appclass);
	    Scene.v().loadNecessaryClasses();

	    // Start working
	    PackManager.v().runPacks();
	    
	    // Now we have all Thread/Runnable classes for which .start() is called.
	    // Now we run call graph analysis on them
	    Set<ThreadProperties> startedRunnableTypes = susBodyAnalysis.startedRunnables;
	    susCallGraphAnalysis.getIntersectingMethods(startedRunnableTypes);
	    
	    System.out.println("It's all over!");
	}
}
