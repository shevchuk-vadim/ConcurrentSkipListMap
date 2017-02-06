package ua.shevchuk.concurrent;

import java.util.Iterator;

public interface ConcurrentIterator<E> extends Iterator<E> {

	boolean removeElement();
	
	default void remove () {
		removeElement();
	}
	
}
