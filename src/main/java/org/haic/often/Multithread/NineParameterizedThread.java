package org.haic.often.Multithread;

/**
 * ParameterizedThread defines a thread with a generic parameter
 *
 * @param <T> 泛型
 * @author haicdust
 * @version 1.0
 * @since 2022/3/11 21:58
 */
public record NineParameterizedThread<T>(T A, T B, T C, T D, T E, T F, T G, T H, T I, ParameterizedThreadStart<T, T, T, T, T, T, T, T, T> parameterStart)
		implements Runnable {
	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public void run() {
		parameterStart.run(A, B, C, D, E, F, G, H, I);
	}

	/**
	 * ParameterizedThreadStart defines the start method for starting a thread.
	 */
	public interface ParameterizedThreadStart<A, B, C, D, E, F, G, H, I> {
		/**
		 * a method with parameter
		 */
		void run(A A, B B, C C, D D, E E, F F, G G, H H, I I);
	}
}