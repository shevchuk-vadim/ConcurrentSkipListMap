package ua.shevchuk.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class ConcurrentSkipListMap<K, V> implements ConcurrentNavigableMap<K, V>, Cloneable, Serializable {

	private static final long serialVersionUID = -1977164380894719245L;
	
	private final Comparator<? super K> comparator;
	private final ConcurrentSkipListSet<Map.Entry<K, V>> set;

	public ConcurrentSkipListMap() {
		comparator = null;
		set = new ConcurrentSkipListSet<>();
	}

	public ConcurrentSkipListMap(Comparator<? super K> comparator) {
		this.comparator = comparator;
		set = new ConcurrentSkipListSet<>(new EntryComparator<K, V>(comparator));
	}

	@SuppressWarnings("unchecked")
	public ConcurrentSkipListMap(Map<? extends K, ? extends V> map) {
		comparator = null;
		set = new ConcurrentSkipListSet<>((Collection<? extends Map.Entry<K, V>>) map.entrySet());
	}

	@SuppressWarnings("unchecked")
	public ConcurrentSkipListMap(SortedMap<K, ? extends V> map) {
		comparator = map.comparator();
		set = new ConcurrentSkipListSet<>((comparator == null) ? null
				: new EntryComparator<>(comparator), (Set<? extends Map.Entry<K, V>>) map.entrySet());
	}

	private ConcurrentSkipListMap(Comparator<? super K> comparator, NavigableSet<Map.Entry<K, V>> set) {
		this.comparator = comparator;
		this.set = (ConcurrentSkipListSet<Map.Entry<K, V>>) set;
	}

	@Override
	public Comparator<? super K> comparator() {
		return comparator;
	}

	@Override
	public V put(K key, V value) {
		while (true) {
			Map.Entry<K, V> entry = set.addOrGet(Entry.newInstance(key, value));
			if (entry == null) {
				return null;
			}
			V oldValue = Entry.setValueOf(entry, value);
			if (oldValue != null) {
				return oldValue;
			}
			Thread.yield();
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return Entry.getValueOf(set.addOrGet(Entry.newInstance(key, value)));
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V replace(K key, V value) {
		return Entry.setValueOf(set.get(Entry.newInstance(key, value)), value);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		if (newValue == null) {
			throw new NullPointerException();
		}
		return Entry.updateValueOf(set.get(Entry.newInstance(key, oldValue)), oldValue, newValue);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		return Entry.getValueOf(set.removeAndGet(Entry.newInstance((K) key)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object key, Object value) {
		Map.Entry<K, V> entry = Entry.newInstance((K) key, (V) value);
		return new ValueCollection<>(set.subSet(entry, true, entry, true)).remove(value);
	}

	@Override
	public void clear() {
		set.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		return Entry.getValueOf(set.get(Entry.newInstance((K) key)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		return set.contains(Entry.newInstance((K) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return new ValueCollection<>(set).contains(value);
	}

	@Override
	public Map.Entry<K, V> ceilingEntry(K key) {
		return set.ceiling(Entry.newInstance(key));
	}

	@Override
	public K ceilingKey(K key) {
		return Entry.getKeyOf(set.ceiling(Entry.newInstance(key)));
	}

	@Override
	public Map.Entry<K, V> floorEntry(K key) {
		return set.floor(Entry.newInstance(key));
	}

	@Override
	public K floorKey(K key) {
		return Entry.getKeyOf(set.floor(Entry.newInstance(key)));
	}

	@Override
	public Map.Entry<K, V> higherEntry(K key) {
		return set.higher(Entry.newInstance(key));
	}

	@Override
	public K higherKey(K key) {
		return Entry.getKeyOf(set.higher(Entry.newInstance(key)));
	}

	@Override
	public Map.Entry<K, V> lowerEntry(K key) {
		return set.lower(Entry.newInstance(key));
	}

	@Override
	public K lowerKey(K key) {
		return Entry.getKeyOf(set.lower(Entry.newInstance(key)));
	}

	@Override
	public Map.Entry<K, V> firstEntry() {
		return set.lowest();
	}

	@Override
	public K firstKey() {
		return Entry.getKeyOf(set.lowest());
	}

	@Override
	public Map.Entry<K, V> lastEntry() {
		return set.highest();
	}

	@Override
	public K lastKey() {
		return Entry.getKeyOf(set.highest());
	}

	@Override
	public Map.Entry<K, V> pollFirstEntry() {
		return set.pollFirst();
	}

	@Override
	public Map.Entry<K, V> pollLastEntry() {
		return set.pollLast();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public ConcurrentNavigableMap<K, V> descendingMap() {
		return new ConcurrentSkipListMap<>(Collections.reverseOrder(comparator), set.descendingSet());
	}	

	@Override
	public ConcurrentNavigableMap<K, V> headMap(K toKey) {
		return new ConcurrentSkipListMap<>(comparator, set.headSet(Entry.newInstance(toKey)));
	}

	@Override
	public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean toInclusive) {
		return new ConcurrentSkipListMap<>(comparator, set.headSet(Entry.newInstance(toKey), toInclusive));
	}

	@Override
	public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
		return new ConcurrentSkipListMap<>(comparator, set.subSet(Entry.newInstance(fromKey), Entry.newInstance(toKey)));
	}

	@Override
	public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return new ConcurrentSkipListMap<>(comparator, set.subSet(Entry.newInstance(fromKey), fromInclusive
				, Entry.newInstance(toKey), toInclusive));
	}

	@Override
	public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
		return new ConcurrentSkipListMap<>(comparator, set.tailSet(Entry.newInstance(fromKey)));
	}

	@Override
	public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean fromInclusive) {
		return new ConcurrentSkipListMap<>(comparator, set.tailSet(Entry.newInstance(fromKey), fromInclusive));
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return new EntrySet<>(set);
	}

	@Override
	public NavigableSet<K> keySet() {
		return new KeySet<>(comparator, set);
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return keySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return new KeySet<>(Collections.reverseOrder(comparator), set.descendingSet());
	}

	@Override
	public Collection<V> values() {
		return new ValueCollection<>(set);
	}

	@Override
	public ConcurrentNavigableMap<K, V> clone() {
		return new ConcurrentSkipListMap<>(this);
	}

	@Override
	public int hashCode() {
		return entrySet().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return (object == this) ? true
			: (object instanceof Map) ? entrySet().equals(((Map<?, ?>) object).entrySet())
			: false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		for (Map.Entry<K, V> entry : entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			builder.append(", ").append((key == this) ? "(this Map)" : key);
			builder.append("=").append((value == this) ? "(this Map)" : value);
		}
		return builder.delete(1, 3).append("}").toString();
	}

    private static class EntrySet<K, V> extends AbstractConcurrentSet<Map.Entry<K, V>> {

    	private ConcurrentSkipListSet<Map.Entry<K, V>> set;

    	public EntrySet(NavigableSet<Map.Entry<K, V>> set) {
    		this.set = (ConcurrentSkipListSet<Map.Entry<K, V>>) set;
    	}

		@Override
		public boolean remove(Object item) {
			if (item instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<K, V> entry = (Map.Entry<K, V>) item;
				if (entry.getKey() != null) {
					return (new ValueCollection<>(set.subSet(entry, true, entry, true)).remove(entry.getValue()));
				}
			}
			return false;
		}

		@Override
		public boolean contains(Object item) {
			if (item instanceof Map.Entry) {
				@SuppressWarnings("unchecked")
				Map.Entry<K, V> entry = (Map.Entry<K, V>) item;
				if (entry.getKey() != null) {
					V value = entry.getValue();
					if (value != null) {
						entry = set.get(entry);
						if (entry != null) {
							return entry.getValue().equals(value);
						}
					}
				}
			}
			return false;
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		public int size() {
			return set.size();
		}

		@Override
		public ConcurrentIterator<Map.Entry<K, V>> iterator() {
			return set.iterator();
		}

    }

    private static class KeySet<K, V> extends AbstractConcurrenNavigableSet<K> {

    	private ConcurrentSkipListSet<Map.Entry<K, V>> set;

    	public KeySet(Comparator<? super K> comparator, NavigableSet<Map.Entry<K, V>> set) {
    		super(comparator);
    		this.set = (ConcurrentSkipListSet<Map.Entry<K, V>>) set;
    	}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object item) {
			return set.remove(Entry.newInstance((K) item));
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object item) {
			return set.contains(Entry.newInstance((K) item));
		}

		@Override
		public K ceiling(K item) {
			return Entry.getKeyOf(set.ceiling(Entry.newInstance(item)));
		}

		@Override
		public K floor(K item) {
			return Entry.getKeyOf(set.floor(Entry.newInstance(item)));
		}

		@Override
		public K higher(K item) {
			return Entry.getKeyOf(set.higher(Entry.newInstance(item)));
		}

		@Override
		public K lower(K item) {
			return Entry.getKeyOf(set.lower(Entry.newInstance(item)));
		}

		@Override
		public K first() {
			return set.first().getKey();
		}

		@Override
		public K last() {
			return set.last().getKey();
		}

		@Override
		public K pollFirst() {
			return Entry.getKeyOf(set.pollFirst());
		}

		@Override
		public K pollLast() {
			return Entry.getKeyOf(set.pollLast());
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		public int size() {
			return set.size();
		}

		@Override
		public ConcurrentIterator<K> iterator() {
			return new KeyIterator(set.iterator());
		}

		@Override
		public NavigableSet<K> descendingSet() {
			return new KeySet<K, V>(Collections.reverseOrder(comparator()), set.descendingSet());
		}

		@Override
		public NavigableSet<K> headSet(K toItem, boolean toInclusive) {
			return new KeySet<K, V>(comparator(), set.headSet(Entry.newInstance(toItem), toInclusive));
		}

		@Override
		public NavigableSet<K> subSet(K fromItem, boolean fromInclusive, K toItem, boolean toInclusive) {
			return new KeySet<K, V>(comparator(), set.subSet(Entry.newInstance(fromItem)
							, fromInclusive, Entry.newInstance(toItem), toInclusive));
		}

		@Override
		public NavigableSet<K> tailSet(K fromItem, boolean fromInclusive) {
			return new KeySet<K, V>(comparator(), set.tailSet(Entry.newInstance(fromItem), fromInclusive));
		}

		private class KeyIterator implements ConcurrentIterator<K> {

	    	private ConcurrentIterator<Map.Entry<K, V>> iterator;

	    	public KeyIterator(ConcurrentIterator<Map.Entry<K, V>> iterator) {
    			this.iterator = iterator;
	    	}
	    	
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public K next() {
				return iterator.next().getKey();
			}

	    	@Override
	    	public boolean removeElement() {
	    		return iterator.removeElement();
			}

	    }

    }
    
	private static class ValueCollection<K, V> extends AbstractConcurrentCollection<V> {
	
    	private ConcurrentSkipListSet<Map.Entry<K, V>> set;

    	public ValueCollection(NavigableSet<Map.Entry<K, V>> set) {
    		this.set = (ConcurrentSkipListSet<Map.Entry<K, V>>) set;
    	}

		@Override
		public boolean remove(Object value) {
			if (value == null) {
				return false;
			}
			ConcurrentIterator<Map.Entry<K, V>> iterator = set.iterator();
			while (iterator.hasNext()) {
				if (Entry.markValueOf(iterator.next(), value) && iterator.removeElement()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean contains(Object value) {
			if (value == null) {
				throw new NullPointerException();
			}
			ConcurrentIterator<Map.Entry<K, V>> iterator = set.iterator();
			while (iterator.hasNext()) {
				if (iterator.next().getValue().equals(value)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		public int size() {
			return set.size();
		}

		@Override
		public ConcurrentIterator<V> iterator() {
			return new ValueIterator(set.iterator());
		}

	    private class ValueIterator implements ConcurrentIterator<V> {

	    	private ConcurrentIterator<Map.Entry<K, V>> iterator;

	    	public ValueIterator(ConcurrentIterator<Map.Entry<K, V>> iterator) {
    			this.iterator = iterator;
	    	}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public V next() {
				return iterator.next().getValue();
			}

	    	@Override
	    	public boolean removeElement() {
	    		return iterator.removeElement();
			}

	    }
   
	}

	private static class EntryComparator<K, V> implements Comparator<Map.Entry<K, V>>, Serializable {

		private static final long serialVersionUID = 57730319261322974L;

		private final Comparator<? super K> comparator;
		
		private EntryComparator(Comparator<? super K> comparator) {
			this.comparator = comparator;
		}
		
		@Override
		public int compare(Map.Entry<K, V> entry1, Map.Entry<K, V> entry2) {
			return comparator.compare(entry1.getKey(), entry2.getKey());
		}
		
	}
	
	private static class Entry<K, V> implements Map.Entry<K, V>, Comparable<Map.Entry<K, V>> {

		private final K key;
		private final AtomicMarkableReference<V> value;
		
		public Entry(K key, V value) {
			this.key = key;
			this.value = new AtomicMarkableReference<>(value, false);
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value.getReference();
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Map.Entry<K, V> entry) {
			return ((Comparable<K>) key).compareTo(entry.getKey());
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof Map.Entry)) {
				return false;
			}
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
			return getKey().equals(entry.getKey()) && getValue().equals(entry.getValue()); 
		}

		@Override
		public int hashCode() {
			return getKey().hashCode() ^ getValue().hashCode();
		}
		
		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}

	    private static <K, V> Entry<K, V> newInstance(K key, V value) {
			if ((key == null) || (value == null)){
				throw new NullPointerException();
			}
			return new Entry<K, V>(key, value);
		}

		private static <K, V> Entry<K, V> newInstance(K key) {
			if (key == null) {
				throw new NullPointerException();
			}
			return new Entry<K, V>(key, null);
		}
		
		private static <K, V> K getKeyOf(Map.Entry<K, V> entry) {
			return (entry == null) ? null : entry.getKey();
		}

		private static <K, V> V getValueOf(Map.Entry<K, V> entry) {
			return (entry == null) ? null : entry.getValue();
		}

		private static <K, V> V setValueOf(Map.Entry<K, V> entry, V newValue) {
			if (entry == null) {
				return null;
			}
			AtomicMarkableReference<V> valueReference = ((Entry<K, V>) entry).value;
			while (true) {
				V expectedValue = valueReference.getReference();
				if (valueReference.compareAndSet(expectedValue, newValue, false, false) || (expectedValue == newValue)) {
					return expectedValue;		
				}
				if (valueReference.isMarked()) {
					return null;
				}
			} 
		}

		private static <K, V> boolean updateValueOf(Map.Entry<K, V> entry, V oldValue, V newValue) {
			if (entry == null) {
				return false;
			}
			AtomicMarkableReference<V> valueReference = ((Entry<K, V>) entry).value;
			while (true) {
				V expectedValue = valueReference.getReference();
				if (!expectedValue.equals(oldValue)) {
					return false;
				}
				if (valueReference.compareAndSet(expectedValue, newValue, false, false) || (expectedValue == newValue)) {
					return true;		
				}
				if (valueReference.isMarked()) {
					return false;
				}
			} 
		}

		private static <K, V> boolean markValueOf(Map.Entry<K, V> entry, Object oldValue) {
			if (entry == null) {
				return false;
			}
			AtomicMarkableReference<V> valueReference = ((Entry<K, V>) entry).value;
			while (true) {
				V expectedValue = valueReference.getReference();
				if (!expectedValue.equals(oldValue)) {
					return false;
				}
				if (valueReference.compareAndSet(expectedValue, expectedValue, false, true) || valueReference.isMarked()) {
					return true;		
				}
			}
		}

	}

}
