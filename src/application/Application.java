package application;

import java.util.NavigableMap;
import java.util.NavigableSet;
//import java.util.concurrent.ConcurrentSkipListMap;
import ua.shevchuk.concurrent.ConcurrentSkipListSet;
import ua.shevchuk.concurrent.ConcurrentSkipListMap;

public class Application<K, V> {

	public static void main(String[] args) {
		NavigableSet<Integer> set1 = new ConcurrentSkipListSet<>();
		set1.add(1);
		set1.add(5);
		set1.add(3);
		set1.add(2);
		set1.add(4);
		ConcurrentSkipListMap<Integer, Integer> map1 = new ConcurrentSkipListMap<>();
		NavigableSet<Integer> set2 = map1.keySet();
		//NavigableSet<Integer> set2 = set1.tailSet(8, true).descendingSet();
		//NavigableSet<Integer> set3 = ((ConcurrentSkipListSet<Integer>) set2).clone();
		//System.out.println(set1.size());
		//System.out.println(set2.size());
		//System.out.println(set2.isEmpty());

		
		
/*
		ConcurrentSkipListSet<Integer> set1 = new ConcurrentSkipListSet<>();
		set1.add(1);
		set1.add(2);
		set1.add(3);
		ConcurrentSkipListSet<Integer> set2 = ((ConcurrentSkipListSet<Integer>) set1.subSet(2, false, 3, false));
		ConcurrentSkipListSet<Integer> set3 = ((ConcurrentSkipListSet<Integer>) set2.headSet(2, true));
		System.out.println(set3);
*/
/*
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(set2);
			byte[] ba = bos.toByteArray();
			System.out.println(bos.size());
			ByteArrayInputStream bis = new ByteArrayInputStream(ba);
			ObjectInputStream ois = new ObjectInputStream(bis);
			try {
				@SuppressWarnings("unchecked")
				ConcurrentSkipListSet<Integer> set3 = (ConcurrentSkipListSet<Integer>) ois.readObject();
				set1.remove(2);
				//System.out.println(set3.add(4));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ConcurrentSkipListMap<Integer, Integer> map1 = new ConcurrentSkipListMap<>();
		map1.put(1, 1);
		map1.put(2, 2);
		map1.put(3, 3);
		//map1.entrySet().clone();
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(map1.entrySet());
			byte[] ba = bos.toByteArray();
			System.out.println(bos.size());
			ByteArrayInputStream bis = new ByteArrayInputStream(ba);
			ObjectInputStream ois = new ObjectInputStream(bis);
			try {
				@SuppressWarnings("unchecked")
				ConcurrentSkipListSet<Integer> set3 = (ConcurrentSkipListSet<Integer>) ois.readObject();
				set1.remove(2);
				System.out.println(set3.add(4));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
	}
}
