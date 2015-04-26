package org.sus.framework.util;

import java.util.ArrayList;
import java.util.List;

public class SusHelper
{
	private static List<String> jdkPackageList = null;
	
	static
	{
		jdkPackageList = new ArrayList<String> ();

		jdkPackageList.add("java.");
	    jdkPackageList.add("javax.");
	    jdkPackageList.add("sun.");
	    jdkPackageList.add("sunw.");
	    jdkPackageList.add("com.sun.");
	    jdkPackageList.add("com.ibm.");
	    jdkPackageList.add("com.apple.");
	    jdkPackageList.add("apple.awt.");
	}
	
	public static String getClassFromSignature(String signature)
	{
		return signature.substring(1, signature.indexOf(":"));
	}
	
	public static List<String> getJdkPackageList()
	{
		return new ArrayList<String>(jdkPackageList);
	}
	
	public static boolean isPackageInJdk(String resourceName)
	{
		for(String jdkPackage : jdkPackageList)
		{
			if(resourceName.indexOf(jdkPackage) == 0)
				return true;
		}
		
		return false;
	}
}
