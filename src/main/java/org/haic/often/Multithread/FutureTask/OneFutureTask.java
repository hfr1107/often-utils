package org.haic.often.Multithread.FutureTask;

import java.util.concurrent.Callable;

/**
 * record class one parameter's  parameterized FutureTask Thread
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/14 17:23
 */
public record OneFutureTask<T, V>(T A, Callable<T, V> callable) implements Callable<V> {
	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public V call() throws Exception {
		return callable.call(A);
	}

	/**
	 * FutureTaskThread defines the start method for starting a thread.
	 */
	public interface Callable<A, V> {
		V call(A A) throws Exception;
	}
}