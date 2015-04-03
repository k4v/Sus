package org.sus.framework.util;

/**
 * 
 * @author karthik
 *
 * Pair simply represents a 2-tuple of type S and T.
 */
public class Pair<S, T>
{
	private S firstElement;
	private T secondElement;
	
	public Pair(S firstElement, T secondElement)
	{
		this.firstElement = firstElement;
		this.secondElement = secondElement;
	}
	
	public S getFirst()
	{
		return firstElement;
	}
	
	public T getSecond()
	{
		return secondElement;
	}
}
