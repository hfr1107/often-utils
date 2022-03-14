package org.haic.often.Multithread.FutureTask;

import java.util.concurrent.Callable;

/**
 * record class two parameter's  parameterized FutureTask Thread
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/14 17:46
 */
public record TwoFutureTaskThread<T, V>(T A, T B, Callable<T, T, V> callable) implements Callable<V> {
	/**
	 * run method to be called in that separately executing thread.
	 */
	@Override public V call() throws Exception {
		return callable.call(A, B);
	}

	/**
	 * FutureTaskThread defines the start method for starting a thread.
	 */
	public interface Callable<A, B, V> {
		V call(A A, B B) throws Exception;
	}
}