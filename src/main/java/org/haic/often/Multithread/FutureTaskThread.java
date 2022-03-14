package org.haic.often.Multithread;

import org.haic.often.Multithread.FutureTask.*;

import java.util.concurrent.Callable;

/**
 * FutureTaskThread defines a thread with a generic parameter
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/12 13:49
 */
public class FutureTaskThread<T, V> implements Callable<V> {
	protected Callable<V> callable;

	public FutureTaskThread(Callable<V> callable) {
		this.callable = callable;
	}

	public FutureTaskThread(T A, OneFutureTaskThread.Callable<T, V> callable) {
		this.callable = new OneFutureTaskThread<>(A, callable);
	}

	public FutureTaskThread(T A, T B, TwoFutureTaskThread.Callable<T, T, V> callable) {
		this.callable = new TwoFutureTaskThread<>(A, B, callable);
	}

	public FutureTaskThread(T A, T B, T C, ThreeFutureTaskThread.Callable<T, T, T, V> callable) {
		this.callable = new ThreeFutureTaskThread<>(A, B, C, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, FourFutureTaskThread.Callable<T, T, T, T, V> callable) {
		this.callable = new FourFutureTaskThread<>(A, B, C, D, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, FiveFutureTaskThread.Callable<T, T, T, T, T, V> callable) {
		this.callable = new FiveFutureTaskThread<>(A, B, C, D, E, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, SixFutureTaskThread.Callable<T, T, T, T, T, T, V> callable) {
		this.callable = new SixFutureTaskThread<>(A, B, C, D, E, F, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, SevenFutureTaskThread.Callable<T, T, T, T, T, T, T, V> callable) {
		this.callable = new SevenFutureTaskThread<>(A, B, C, D, E, F, G, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, EightFutureTaskThread.Callable<T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new EightFutureTaskThread<>(A, B, C, D, E, F, G, H, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, NineFutureTaskThread.Callable<T, T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new NineFutureTaskThread<>(A, B, C, D, E, F, G, H, I, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J, TenFutureTaskThread.Callable<T, T, T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new TenFutureTaskThread<>(A, B, C, D, E, F, G, H, I, J, callable);
	}

	/**
	 * a method with parameter
	 *
	 * @return 泛型返回
	 * @throws Exception 抛出异常
	 */
	@Override public V call() throws Exception {
		return callable.call();
	}

}