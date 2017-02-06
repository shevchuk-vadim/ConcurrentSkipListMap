package ua.shevchuk.concurrent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class ConcurrentSkipListSet<E> extends AbstractConcurrenNavigableSet<E> implements Cloneable, Serializable {

	private static final long serialVersionUID = -6852381495890067210L;
	private static final int MAXIMUM_HEIGHT = 32;
	
	private final Node<E> headNode;
	private final AtomicInteger size; 
	private final AtomicInteger startLevel;
	private final AtomicInteger nodeHeight;
	
	private final E leastItem;
	private final boolean leastInclusive;
	private final E greatestItem;
	private final boolean greatestInclusive;
	private final boolean descending;
	
	public ConcurrentSkipListSet() {
		this((Comparator<? super E>) null);
	}
	
	public ConcurrentSkipListSet(Comparator<? super E> comparator) {
		super(comparator);
		headNode = new Node<>(null, MAXIMUM_HEIGHT);
		size = new AtomicInteger();
		startLevel = new AtomicInteger();
		nodeHeight = new AtomicInteger(1);

		leastItem = null;
		leastInclusive = true;
		greatestItem = null;
		greatestInclusive = false;
		descending = false;
	}

	public ConcurrentSkipListSet(Collection<? extends E> collection) {
		this((Comparator<? super E>) null);
		addAll(collection);
	}

	public ConcurrentSkipListSet(SortedSet<E> set) {
		this(set.comparator(), set);
	}
	
	protected ConcurrentSkipListSet(Comparator<? super E> comparator, Set<? extends E> set) {
		this(comparator);
		
		@SuppressWarnings("unchecked")
		Node<E>[] leftNodes = new Node[MAXIMUM_HEIGHT];
		Arrays.fill(leftNodes, headNode);
		for (E item : set) {
			int height = nodeHeight.get();
			Node<E> node = new Node<>(item, height);
			for (int level = 0; level < height; level++) {
				node.setNext(level, headNode);
				leftNodes[level].setNext(level, node);
				leftNodes[level] = node;
			}
			changeSize(true);
		}
	}

	private ConcurrentSkipListSet(ConcurrentSkipListSet<E> set, E leastItem, boolean leastInclusive
			, E greatestItem, boolean greatestInclusive, boolean descending) {
		super((descending == set.descending) ? set.comparator() : Collections.reverseOrder(set.comparator()));
		headNode = set.headNode;
		size = set.size;
		startLevel = set.startLevel;
		nodeHeight = set.nodeHeight;

		this.leastItem = leastItem;
		this.leastInclusive = leastInclusive;
		this.greatestItem = greatestItem;
		this.greatestInclusive = greatestInclusive;
		this.descending = descending;
	}

	protected E addOrGet(E item) {
		if (!inSubSet(item)) {
			throw new IllegalArgumentException();
		}
		Finder finder = new Finder(item, true, false, false, false);
		do {
			item = finder.find();
		} while (!finder.insert());
		return item;
	}

	@Override
	public boolean add(E item) {
		return (addOrGet(item) == null);
	}

	protected E removeAndGet(E item) {
		if (!inSubSet(item)) {
			return null;  
		}
		Finder finder = new Finder(item, true, false, false, false);
		item = finder.find();
		return finder.remove() ? item : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object item) {
		return (removeAndGet((E) item) != null);
	}

	protected E get(E item) {
		if (!inSubSet(item)) {
			return null;  
		}
		return new Finder(item, true, false, false, false).find();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object item) {
		return (get((E) item) != null);
	}

	@Override
	public E ceiling(E item) {
		if (item == null) {
			throw new NullPointerException();
		}
		return new Finder(item, true, !descending, descending, false).find();
	}

	@Override
	public E floor(E item) {
		if (item == null) {
			throw new NullPointerException();
		}
		return new Finder(item, true, descending, !descending, false).find();
	}

	@Override
	public E higher(E item) {
		if (item == null) {
			throw new NullPointerException();
		}
		return new Finder(item, false, !descending, descending, false).find();
	}

	@Override
	public E lower(E item) {
		if (item == null) {
			throw new NullPointerException();
		}
		return new Finder(item, false, descending, !descending, false).find();
	}

	protected E lowest() {
		return new Finder(null, false, !descending, descending, false).find();
	}

	@Override
	public E first() {
		E item = lowest();
		if (item == null) {
			throw new NoSuchElementException();
		}
		return item;
	}

	protected E highest() {
		return new Finder(null, false, descending, !descending, false).find();
	}

	@Override
	public E last() {
		E item = highest();
		if (item == null) {
			throw new NoSuchElementException();
		}
		return item;
	}

	@Override
	public E pollFirst() {
		Finder finder = new Finder(null, false, !descending, descending, false);
		E item;
		do {
			item = finder.find();
		} while (!finder.remove());
		return item;
	}

	@Override
	public E pollLast() {
		Finder finder = new Finder(null, false, descending, !descending, false);
		E item;
		do {
			item = finder.find();
		} while (!finder.remove());
		return item;
	}

	@Override
	public boolean isEmpty() {
		return (new Finder(null, false, !descending, descending, false).find() == null);
	}

	@Override
	public int size() {
		if ((leastItem == null) && (greatestItem == null)) {
			return size.get();
		}
		int size = 0;
		ConcurrentIterator<E> iterator = descending ? descendingIterator() : iterator();
		while (iterator.hasNext()) {
			iterator.next();
			size++;
		}
		return size;
	}

	@Override
	public ConcurrentIterator<E> iterator() {
		return new SetIterator();
	}

	@Override
	public NavigableSet<E> descendingSet() {
		return new ConcurrentSkipListSet<>(this, leastItem, leastInclusive, greatestItem, greatestInclusive, !descending);
	}

	@Override
	public NavigableSet<E> headSet(E toItem, boolean toInclusive) {
		if (toItem == null) {
			throw new NullPointerException();
		}
		return descending ? doSubSet(toItem, toInclusive, null, false) 
			: doSubSet(null, false, toItem, toInclusive);
	}

	@Override
	public NavigableSet<E> subSet(E fromItem, boolean fromInclusive, E toItem, boolean toInclusive) {
		if ((fromItem == null) || (toItem == null)) {
			throw new NullPointerException();
		}
		return descending ? doSubSet(toItem, toInclusive, fromItem, fromInclusive) 
			: doSubSet(fromItem, fromInclusive, toItem, toInclusive);
	}

	@Override
	public NavigableSet<E> tailSet(E fromItem, boolean fromInclusive) {
		if (fromItem == null) {
			throw new NullPointerException();
		}
		return descending ? doSubSet(null, false, fromItem, fromInclusive) 
			: doSubSet(fromItem, fromInclusive, null, false);
	}

	@Override
	public ConcurrentSkipListSet<E> clone() {
		return new ConcurrentSkipListSet<>(this);
	}

	private NavigableSet<E> doSubSet(E leastItem, boolean leastInclusive, E greatestItem, boolean greatestInclusive) {
		boolean ok;
		if (leastItem == null) {
			leastItem = this.leastItem;
			leastInclusive = this.leastInclusive;
			ok = (greaterThenLeast(greatestItem, false) && lessThenGreatest(greatestItem, greatestInclusive));
		} else if (greatestItem == null) {
			greatestItem = this.greatestItem;
			greatestInclusive = this.greatestInclusive;
			ok = (greaterThenLeast(leastItem, leastInclusive) && lessThenGreatest(leastItem, false));
		} else {
			ok = ((compare(leastItem, greatestItem) <= 0) 
					&& greaterThenLeast(leastItem, leastInclusive) && lessThenGreatest(greatestItem, greatestInclusive));
		}
		if (!ok) {
			throw new IllegalArgumentException();
		}
		return new ConcurrentSkipListSet<>(this, leastItem, leastInclusive, greatestItem, greatestInclusive, descending);
	}
	
	private boolean inSubSet(E item) {
		if (item == null) {
			throw new NullPointerException();
		}
		return (greaterThenLeast(item, true) && lessThenGreatest(item, true));
	}
		
	private boolean greaterThenLeast(E item, boolean inclusive) {
		if (leastItem == null) {
			return true;
		}
		int compare = compare(item, leastItem);
		return ((compare > 0) || (compare == 0) && (leastInclusive || !inclusive));
	}

	private boolean lessThenGreatest(E item, boolean inclusive) {
		if (greatestItem == null) {
			return true;
		}
		int compare = compare(item, greatestItem);
		return ((compare < 0) || (compare == 0) && (greatestInclusive || !inclusive));
	}

	@SuppressWarnings("unchecked")
	private int compare(E item1, E item2) {
		int result = (comparator() == null) ? ((Comparable<? super E>) item1).compareTo(item2)
			: comparator().compare(item1, item2);
		return descending ? -result : result;
	}

	private void changeSize(boolean increase) {
		int sise = increase ? this.size.incrementAndGet() : this.size.decrementAndGet();
		int n = 1;
		int startLevel = -1;
		int nodeHeight = 1;
		do {
			startLevel++;
			n &= sise;
			nodeHeight += n;
		} while ((sise >>= 1) != 0);
		this.startLevel.set(startLevel);
		this.nodeHeight.set(nodeHeight);
	}

    private class SetIterator implements ConcurrentIterator<E> {

    	private boolean canMoveNext = true;
    	private boolean canRemove;
    	private E item;
    	private Finder finder;

    	public SetIterator () {
   			finder = descending ? new Finder(greatestItem, greatestInclusive, false, true, true) 
   					: new Finder(leastItem, leastInclusive, true, false, true);
    	}

		@Override
		public boolean hasNext() {
			if (canMoveNext) {
				canMoveNext = false;
				item = finder.find();
			}
			return (item != null);
		}

		@Override
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			canMoveNext = true;
			canRemove = true;
			return item;
		}

		@Override
		public boolean removeElement() {
			if (!canRemove) {
				throw new IllegalStateException();
			}
			canRemove = false;
			return finder.remove();
		}

    }

    private class Finder {
		
		private E item;
		private boolean equal;
		private boolean greater;
		private boolean less;
		private boolean iteration;
		
		private Node<E>[] leftNodes;
		private Node<E>[] rightNodes;
		private int fromLevel;
		private int toLevel;
		private Node<E> foundNode;
		private Node<E> insertedNode;
				
		@SuppressWarnings("unchecked")
		public Finder(E item, boolean equal, boolean greater, boolean less, boolean iteration) {
			this.item = item;
			this.equal = equal;
			this.greater = greater;
			this.less = less;
			this.iteration = iteration;

			leftNodes = new Node[MAXIMUM_HEIGHT + 1];
			Arrays.fill(leftNodes, headNode);
			rightNodes = new Node[MAXIMUM_HEIGHT];
		}
		
		public E find() {
			if (foundNode == null) {
				if (greater) {
					if ((item == null) || !greaterThenLeast(item, equal)) {
						item = leastItem;
						equal = leastInclusive;
					}
					if (item == null) {
						return (foundNode = nextNode(0, headNode)).item;
					}
				} else if (less) {
					if ((item == null) || !lessThenGreatest(item, equal)) { 
						item = greatestItem;
						equal = greatestInclusive;
					}	
				}
				fromLevel = startLevel.get();
			} else {
				if (greater) {
					if (iteration) {
						return (foundNode = nextNode(0, foundNode)).item;
					}
				} else {
					if (iteration) {
						item = foundNode.item;
						equal = false;
						for (; (leftNodes[fromLevel] == foundNode); fromLevel++);
					}					
					for (; leftNodes[fromLevel].isMarked(fromLevel); fromLevel++);
					if (fromLevel > toLevel) {
						leftNodes[fromLevel - 1] = leftNodes[fromLevel--];
					}
				}
			}

			Node<E> leftNode = leftNodes[fromLevel];
			Node<E> rightNode = null;
			while (fromLevel >= toLevel) {
				rightNode = nextNode(fromLevel, leftNode);
				leftNodes[fromLevel] = leftNode;
				rightNodes[fromLevel] = rightNode; 
				int compare = (rightNode == headNode) ? 1 : (item == null) ? -1 : compare(rightNode.item, item);
				if (compare < 0) {
					leftNode = rightNode;
				} else if (compare > 0) {
					fromLevel--;
				} else if (equal) {
					return (foundNode = rightNode).item;
				} else if (greater) {
					leftNode = rightNode;
				} else {
					fromLevel--;
				}
			}

			fromLevel = toLevel;
			if (greater) {
				if ((rightNode.item != null) && lessThenGreatest(rightNode.item, true)) { 
					return (foundNode = rightNode).item;
				}
			} else if (less) {
				if ((leftNode.item != null) && greaterThenLeast(leftNode.item, true)) { 
					return (foundNode = leftNode).item;
				}
			}
			return (foundNode = headNode).item;
		}
		
		public boolean insert() {
			if (foundNode != headNode) {
				return true;
			}
			int height = nodeHeight.get();
			insertedNode = new Node<>(item, height);
			insertedNode.setNext(0, rightNodes[0]);
			if (!leftNodes[0].setNext(0, rightNodes[0], insertedNode)) {
				return false;
			}
			changeSize(true);
			fromLevel = toLevel = 1;
			
			while ((toLevel < height) && (find() == null)) {
				insertedNode.setNext(toLevel, rightNodes[toLevel]);
				if (leftNodes[toLevel].setNext(toLevel, rightNodes[toLevel], insertedNode)) {
					fromLevel = ++toLevel;
				}
			}
			return true;
		}

		public boolean remove() {
			if (foundNode == headNode) {
				return true;
			}
			boolean ok = false;
			for (int level = foundNode.height() - 1; level >= 0; level--) {
				ok = foundNode.mark(level); 
			}
			if (ok) {
				changeSize(false);
			}
			return ok;
		}
		
		private Node<E> nextNode(int level, Node<E> node) {
			Node<E> expectNode = node.getNext(level);
			Node<E> nextNode = expectNode;
			while (nextNode.isMarked(level)) {
				nextNode = nextNode.getNext(level);
			}
			if (nextNode != expectNode) {
				node.setNext(level, expectNode, nextNode);
			}
			return nextNode;
		}
	
    }

	private static class Node<E> {

    	private final E item;
    	private final AtomicMarkableReference<Node<E>>[] next;

		@SuppressWarnings("unchecked")
		public Node(E item, int height) {
        	this.item = item;
        	next = new AtomicMarkableReference[height];
        	for (int level = 0; level < height; level++) {
        		next[level] = new AtomicMarkableReference<>(this, false);
        	}
        }
        
		public int height() {
        	return next.length;
        }
   
		public void setNext(int level, Node<E> updateNode) {
			next[level].set(updateNode, false);
		}

		public boolean setNext(int level, Node<E> expectNode, Node<E> updateNode) {
			return next[level].compareAndSet(expectNode, updateNode, false, false);
		}

		public Node <E> getNext(int level) {
        	return next[level].getReference();
        }

		public boolean mark(int level) {
			do {
				Node<E> reference = next[level].getReference();
				if (next[level].compareAndSet(reference, reference, false, true)) {
					return true;
				}
			} while (!next[level].isMarked());
			return false;
		}

		boolean isMarked(int level) {
        	return next[level].isMarked();
        }

    }

}
