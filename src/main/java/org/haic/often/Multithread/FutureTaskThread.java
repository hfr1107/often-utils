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

	public FutureTaskThread(T A, OneFutureTask.Callable<T, V> callable) {
		this.callable = new OneFutureTask<>(A, callable);
	}

	public FutureTaskThread(T A, T B, TwoFutureTask.Callable<T, T, V> callable) {
		this.callable = new TwoFutureTask<>(A, B, callable);
	}

	public FutureTaskThread(T A, T B, T C, ThreeFutureTask.Callable<T, T, T, V> callable) {
		this.callable = new ThreeFutureTask<>(A, B, C, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, FourFutureTask.Callable<T, T, T, T, V> callable) {
		this.callable = new FourFutureTask<>(A, B, C, D, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, FiveFutureTask.Callable<T, T, T, T, T, V> callable) {
		this.callable = new FiveFutureTask<>(A, B, C, D, E, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, SixFutureTask.Callable<T, T, T, T, T, T, V> callable) {
		this.callable = new SixFutureTask<>(A, B, C, D, E, F, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, SevenFutureTask.Callable<T, T, T, T, T, T, T, V> callable) {
		this.callable = new SevenFutureTask<>(A, B, C, D, E, F, G, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, EightFutureTask.Callable<T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new EightFutureTask<>(A, B, C, D, E, F, G, H, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, NineFutureTask.Callable<T, T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new NineFutureTask<>(A, B, C, D, E, F, G, H, I, callable);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J, TenFutureTask.Callable<T, T, T, T, T, T, T, T, T, T, V> callable) {
		this.callable = new TenFutureTask<>(A, B, C, D, E, F, G, H, I, J, callable);
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