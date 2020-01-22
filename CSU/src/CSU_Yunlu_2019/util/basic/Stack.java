package CSU_Yunlu_2019.util.basic;

public interface Stack<E> {
	public boolean isEmpty();

	public E peek();

	public E pop();

	public E push(E e);

	public int search(E e);

	public void clear();
}
