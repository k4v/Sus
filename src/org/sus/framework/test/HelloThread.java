package org.sus.framework.test;

public class HelloThread
{
	static int x=1;
	public static void main(String[] args)
	{
		TestClass c = new TestClass(x);
		TestThread t = new TestThread();
		t.start();
		// Race condition here, may throw DivideByZeroException
		int z = t.y+1/x;
		System.out.println(z);
	}
	
	static class TestThread extends Thread
	{
		int y;

		public void run()
		{
			x=0;
			y++;
		}
	}
}