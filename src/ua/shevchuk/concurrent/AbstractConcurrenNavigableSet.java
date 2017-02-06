package ua.shevchuk.concurrent;

import java.util.Comparator;
import java.util.NavigableSet;

public abstract class AbstractConcurrenNavigableSet<E> extends AbstractConcurrentSet<E> implements NavigableSet<E> {

	private final Comparator<? super E> comparator;
	
	public AbstractConcurrenNavigableSet(Comparator<? super E> comparator) {
		this.comparator = comparator;
	}
	
	@Override
	public Comparator<? super E> comparator() {
		return comparator;
	}

	@Override
	public ConcurrentIterator<E> descendingIterator() {
		return (ConcurrentIterator<E>) descendingSet().iterator();
	}

	@Override
	public NavigableSet<E> headSet(E toItem) {
		return headSet(toItem, false);
	}

	@Override
	public NavigableSet<E> subSet(E fromItem, E toItem) {
		return subSet(fromItem, true, toItem, false);
	}

	@Override
	public NavigableSet<E> tailSet(E fromItem) {
		return tailSet(fromItem, true);
	}

}
