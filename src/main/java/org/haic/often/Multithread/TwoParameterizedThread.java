package org.haic.often.Multithread;

/**
 * ParameterizedThread defines a thread with a generic parameter
 *
 * @param <T> 泛型
 * @author haicdust
 * @version 1.0
 * @since 2022/3/11 21:58
 */
public record TwoParameterizedThread<T>(T A, T B, ParameterizedThreadStart<T, T> parameterStart) implements Runnable {
	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public void run() {
		parameterStart.run(A, B);
	}

	/**
	 * ParameterizedThreadStart defines the start method for starting a thread.
	 */
	public interface ParameterizedThreadStart<A, B> {
		/**
		 * a method with parameter
		 */
		void run(A A, B B);
	}
}