package com.cosmicdan.turboshell.models.data;

import java.util.Collection;
import java.util.Stack;

/**
 * Thanks to "Calvin" at StackOverflow for this simple solution
 * https://stackoverflow.com/a/16206356/1767892
 */
public final class SizedStack<T> extends Stack<T> {
	private final int mMaxSize;

	public SizedStack(final int maxSize) {
		mMaxSize = maxSize;
	}

	@Override
	public T push(final T item) {
		//If the stack is too big, remove elements until it's the right size.
		while (size() >= mMaxSize) {
			remove(0);
		}
		return super.push(item);
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized SizedStack<T> clone() {
		final SizedStack<T> clone = new SizedStack<>(mMaxSize);
		clone.addAll((Collection<? extends T>) super.clone());
		return clone;
	}
}
