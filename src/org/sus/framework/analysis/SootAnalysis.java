package org.sus.framework.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sus.framework.util.Access;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.Unit;
import soot.jimple.Stmt;
import soot.options.Options;

public class SootAnalysis extends BodyTransformer
{
	List<Stage> raceDetectionStages = new ArrayList<Stage>();
	
	public SootAnalysis()
	{
		raceDetectionStages.add(new OriginalPairs());
	}
	
	public static void main(String[] args)
	{
		String mainclass = "org.sus.framework.test.HelloThread";

		// Set Soot classpath
	    String bootPath = System.getProperty("sun.boot.class.path");
	    String javaPath = System.getProperty("java.class.path");
	    String path = bootPath + File.pathSeparator +javaPath;
	    Scene.v().setSootClassPath(path);

        // Add an intra-procedural analysis phase to Soot
	    SootAnalysis sootAnalysis = new SootAnalysis();
	    PackManager.v().getPack("jtp").add(new Transform("jtp.TestSoot", sootAnalysis));

        // Load and set Main class
	    Options.v().set_app(true);
	    SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
	    Scene.v().setMainClass(appclass);
	    Scene.v().loadNecessaryClasses();
	    
	    // Start working
	    PackManager.v().runPacks();
	}

	@Override
	protected void internalTransform(Body b, String phaseName,
		Map<String, String> options)
	{
		OriginalPairs originalPairsStage = ((OriginalPairs)raceDetectionStages.get(Stage.STAGES.ORIGINAL_PAIRS.ordinal()));
	    
		Iterator<Unit> unitIterator = b.getUnits().snapshotIterator();
		while(unitIterator.hasNext())
	    {
	    	// Read each program statement as Jimple
	    	Stmt sootStmt = (Stmt)unitIterator.next();
	    	// System.out.println(sootStmt);
	    	
	    	if(sootStmt.containsFieldRef())
	    	{
	    		Access fieldAccess = new Access(sootStmt);
	    		originalPairsStage.addAccess(fieldAccess);
	    	}
	    }
		
		originalPairsStage.computeAccessPairs();
	}
}
