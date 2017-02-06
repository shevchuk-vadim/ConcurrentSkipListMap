package ua.shevchuk.concurrent;

import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractConcurrentCollection<E> implements Collection<E> {

	@Override
	public boolean add(E item) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		boolean modified = false;
		for (E item : collection) {
			if (add(item)) {
				modified = true;
			}
		}
		return modified;
	}
	
	@Override
	public boolean containsAll(Collection<?> collection) {
		for (Object item : collection) {
			if (!contains(item)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean modified = false;
		ConcurrentIterator<E> iterator = iterator();
		while (iterator.hasNext()) {
			if (collection.contains(iterator.next()) && iterator.removeElement()) {
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		boolean modified = false;
		ConcurrentIterator<E> iterator = iterator();
		while (iterator.hasNext()) {
			if (!collection.contains(iterator.next()) && iterator.removeElement()) {
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		ConcurrentIterator<E> iterator = iterator();
		while (iterator.hasNext()) {
			iterator.next();
			iterator.removeElement();
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		for (E item : this) {
			builder.append(", ").append((item == this) ? "(this Collection)" : item);
		}
		return builder.delete(1, 3).append("]").toString();
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[0]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] array) {
		T[] result = array;
		int count = 0;
		for (E item : this) {
			if (count == result.length) {
				result = Arrays.copyOf(result, count * 3 / 2 + 1);
			}
			result[count++] = (T) item;
		}
		if (count < result.length) {
			if (result == array) {
				result[count] = null;
			} else {
				result = Arrays.copyOf(result, count);
			}
		}
		return result;
	}

	@Override
	abstract public ConcurrentIterator<E> iterator();
	
}
