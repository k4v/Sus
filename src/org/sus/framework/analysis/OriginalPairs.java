package org.sus.framework.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sus.framework.util.Access;
import org.sus.framework.util.Pair;

/**
 * @author karthik
 *
 * This class computes the OriginalPairs set
 * of memory accesses, given the list of all
 * instance, static and array accesses in an
 * aapplication.
 */
public class OriginalPairs extends Stage
{
	List<Access> allFieldAccesses = new ArrayList<Access>();
	List<Access> writeFieldAccesses = new ArrayList<Access>();
	
	List<Access> allStaticAccesses = new ArrayList<Access>();
	List<Access> writeStaticAccesses = new ArrayList<Access>();
	
	List<Access> allArrayAccesses = new ArrayList<Access>();
	List<Access> writeArrayAccesses = new ArrayList<Access>();
	
	Set<Pair<Access, Access>> resultSet = null;
	
	public OriginalPairs()
	{
		super(Stage.STAGES.ORIGINAL_PAIRS.ordinal());
	}
	
	public void addAccess(Access fieldAccess)
	{
		System.out.println("Saving access to "+fieldAccess.getStatement().getFieldRef().toString());
		// Save static field access
		if(fieldAccess.getIsStatic())
		{
			allStaticAccesses.add(fieldAccess);
			
			if(fieldAccess.getIsWrite())
				writeStaticAccesses.add(fieldAccess);
		}
		// Save instance field access
		else
		{
			allFieldAccesses.add(fieldAccess);
			
			if(fieldAccess.getIsWrite())
				writeFieldAccesses.add(fieldAccess);
		}
		
		resultSet = null;
	}
	
	public void computeAccessPairs()
	{
		Set<Pair<Access, Access>> fieldAccessResults = new HashSet<Pair<Access, Access>>();
		Set<Pair<Access, Access>> staticAccessResults = new HashSet<Pair<Access, Access>>();
		
		for(Access writeAccess : writeFieldAccesses)
			for(Access fieldAccess : allFieldAccesses)
				if(!writeAccess.equals(fieldAccess))
					fieldAccessResults.add(new Pair<Access, Access>(writeAccess, fieldAccess));
		
		for(Access writeAccess : writeStaticAccesses)
			for(Access staticAccess : allStaticAccesses)
				if(!writeAccess.equals(staticAccess))
					staticAccessResults.add(new Pair<Access, Access>(writeAccess, staticAccess));
		
		resultSet = new HashSet<>();
		resultSet.addAll(fieldAccessResults);
		resultSet.addAll(staticAccessResults);
		
		System.out.println("Final result:");
		System.out.println(resultSet);
	}
	
	public Set<Pair<Access, Access>> getResult()
	{
		if(this.resultSet == null)
		{
			computeAccessPairs();
		}
		
		return this.resultSet;
	}
}
