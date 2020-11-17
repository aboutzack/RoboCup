package CSU_Yunlu_2020.util.test;

import CSU_Yunlu_2020.util.CircleStack;

public class TestCircleStack {
	public static void main(String[] args) {
		CircleStack<Integer> stack = new CircleStack<>(10);
		for (int i = 0; i < 50; i++)
			stack.push(i);
		while (!stack.isEmpty()) {
			System.out.println(stack.pop());
		}
		System.out.println(stack.peek());
	}
}
