package CSU_Yunlu_2019.util;

import CSU_Yunlu_2019.util.basic.Stack;

public class CircleStack<E> implements Stack<E> {

	private int top;
	private Object[] elementData;
	private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
	private int popCount = 0;
	private int pushCount = 0;

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity is negative
	 */
	public CircleStack(int initialCapacity) {
		if (initialCapacity > 0) {
			this.elementData = new Object[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = EMPTY_ELEMENTDATA;
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
		top = -1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E peek() {
		return (E) elementData[top];
	}

	@SuppressWarnings("unchecked")
	public E peekSecend() {
		return (E) elementData[(top - 1 + elementData.length) % elementData.length];
	}

	@SuppressWarnings("unchecked")
	public E peekThird() {
		return (E) elementData[(top - 2 + elementData.length) % elementData.length];
	}

	@SuppressWarnings("unchecked")
	@Override
	public E pop() {
		Object object = elementData[top];
		if (object != null) {
			elementData[top] = null;
			top = (--top + elementData.length) % elementData.length;
			popCount++;
			pushCount = 0;
		}
		return (E) object;
	}

	@Override
	public E push(E e) {
		top = (++top) % elementData.length;
		elementData[top] = e;
		pushCount++;
		popCount = 0;
		return e;
	}

	@Override
	public int search(E e) {
		if (e != null)
			for (int i = 0; i < elementData.length; i++)
				if (e.equals(elementData[i]))
					return i;
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return top < 0 || elementData[top] == null;
	}

	public int getPushCount() {
		return pushCount;
	}

	public int getPopCount() {
		return popCount;
	}

	@Override
	public void clear() {
		top = -1;
		popCount = 0;
		pushCount = 0;
		for (int i = 0; i < elementData.length; i++) {
			elementData[i] = null;
		}
	}

}
