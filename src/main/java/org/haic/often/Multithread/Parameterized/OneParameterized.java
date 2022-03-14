package org.haic.often.Multithread.Parameterized;

/**
 * record class one parameter's  parameterized Thread
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/14 17:23
 */
public record OneParameterized<T>(T A, Runnable<T> runnable) implements Runnable {
	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public void run() {
		runnable.run(A);
	}

	/**
	 * Runnable defines the start method for starting a thread.
	 */
	public interface Runnable<A> {
		/**
		 * a method with parameter
		 */
		void run(A A);
	}
}