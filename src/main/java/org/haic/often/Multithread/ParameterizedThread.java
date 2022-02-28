package org.haic.often.Multithread;

/**
 * ParameterizedThread defines a thread with a generic parameter
 *
 * @param <T> 泛型
 * @author haicdust
 */
public record ParameterizedThread<T>(T T, ParameterizedThreadStart<T> parameterStart) implements Runnable {

	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public void run() {
		parameterStart.run(T);
	}
}