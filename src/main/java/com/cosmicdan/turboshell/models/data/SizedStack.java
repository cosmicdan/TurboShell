package com.cosmicdan.turboshell.models.data;

import java.util.Stack;

/**
 * Thanks to "Calvin" at StackOverflow for this simple solution
 * https://stackoverflow.com/a/16206356/1767892
 */
public class SizedStack<T> extends Stack<T> {
	private final int mMaxSize;

	public SizedStack(int maxSize) {
		mMaxSize = maxSize;
	}

	@Override
	public T push(T object) {
		//If the stack is too big, remove elements until it's the right size.
		while (size() >= mMaxSize) {
			remove(0);
		}
		return super.push(object);
	}
}
