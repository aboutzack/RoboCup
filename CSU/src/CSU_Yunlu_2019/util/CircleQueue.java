package CSU_Yunlu_2019.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public class CircleQueue<E> implements Queue<E> {
	private int head;
	private int end;
	private Object[] elementData;
	private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
	private int enqueueCount = 0;
	// private int dequeueCount = 0;

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity is negative
	 */
	public CircleQueue(int initialCapacity) {
		if (initialCapacity > 0) {
			this.elementData = new Object[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = EMPTY_ELEMENTDATA;
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
		head = -1;
		end = -1;
	}

	@Override
	public int size() {
		int size = (head + elementData.length - end) % elementData.length;
		return size != 0 ? size : (enqueueCount > 0 ? elementData.length : 0);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		if (o != null) {
			int size = size();
			for (int i = 0; i < size; i++) {
				if (o.equals(elementData[(end + i) % elementData.length]))
					return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int pointer = 0;
			E last;

			@Override
			public boolean hasNext() {
				return pointer < size();
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				E e;
				if (hasNext()) {
					e = (E) elementData[(end + pointer) % elementData.length];
					last = e;
				} else
					e = last;
				return e;
			}
		};
	}

	@Override
	public Object[] toArray() {
		Object[] other = new Object[size()];
		for (int i = 0; i < other.length; i++) {
			other[i] = elementData[(end + i) % elementData.length];
		}
		return other;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < size())
			a = (T[]) new Object[size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = (T) elementData[(end + i) % elementData.length];
		}
		return a;
	}

	@Override
	public boolean remove(Object o) {
		int size = size();
		for (int i = 0; i < size; i++) {
			if (o != null && o.equals(elementData[(end + i) % elementData.length])) {
				for (int j = i + 1; j < size; j++) {
					elementData[(end + j) % elementData.length] = elementData[(end + j - 1) % elementData.length];
				}
				head = (--head) % elementData.length;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean add(E e) {
		if (size() == elementData.length)
			throw new IllegalStateException();
		if (e == null)
			throw new NullPointerException();
		elementData[(++head) % elementData.length] = e;
		return true;
	}

	@Override
	public boolean offer(E e) {
		if (size() == elementData.length)
			return false;
		elementData[(++head) % elementData.length] = e;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E remove() {
		if (size() > 0)
			return (E) elementData[(++end) % elementData.length];
		throw new NoSuchElementException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public E poll() {
		if (size() > 0)
			return (E) elementData[(++end) % elementData.length];
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E element() {
		if (size() > 0)
			return (E) elementData[(1 + end) % elementData.length];
		return null;
		// throw new NoSuchElementException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		if (size() > 0)
			return (E) elementData[(1 + end) % elementData.length];
		else
			return null;
		// throw new NoSuchElementException();
	}

}
