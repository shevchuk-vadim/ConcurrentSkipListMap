package ua.shevchuk.concurrent;

import java.util.Set;

public abstract class AbstractConcurrentSet<E> extends AbstractConcurrentCollection<E> implements Set<E>{

	@Override
	public int hashCode() {
		int code = 0;
		for (E item : this) {
			code += item.hashCode();
		}
		return code;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof Set)) {
			return false;
		}
		Set<?> collection = (Set<?>) object;
		try {
			return (containsAll(collection) && collection.containsAll(this));
		} catch (ClassCastException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
	}

}
