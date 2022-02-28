package org.haic.often.Multithread;

/**
 * ParameterizedThreadStart defines the start method for starting a thread.
 *
 * @author haicdust
 * @param <T>
 *            泛型
 */
public interface ParameterizedThreadStart<T> {
	/**
	 * a method with parameter
	 *
	 * @param T
	 *            泛型
	 */
	void run(final T T);
}
