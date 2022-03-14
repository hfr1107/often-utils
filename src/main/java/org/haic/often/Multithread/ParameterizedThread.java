package org.haic.often.Multithread;

import org.haic.often.Multithread.Parameterized.*;

/**
 * ParameterizedThread defines a thread with a generic parameter
 *
 * @param <T> 泛型
 * @author haicdust
 * @version 1.0
 * @since 2022/3/11 21:58
 */
public class ParameterizedThread<T> implements Runnable {
	protected Runnable runnable;

	public ParameterizedThread(Runnable runnable) {
		this.runnable = runnable;
	}

	public ParameterizedThread(T A, OneParameterizedThread.Runnable<T> runnable) {
		this.runnable = new OneParameterizedThread<>(A, runnable);
	}

	public ParameterizedThread(T A, T B, TwoParameterizedThread.Runnable<T, T> runnable) {
		this.runnable = new TwoParameterizedThread<>(A, B, runnable);
	}

	public ParameterizedThread(T A, T B, T C, ThreeParameterizedThread.Runnable<T, T, T> runnable) {
		this.runnable = new ThreeParameterizedThread<>(A, B, C, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, FourParameterizedThread.Runnable<T, T, T, T> runnable) {
		this.runnable = new FourParameterizedThread<>(A, B, C, D, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, FiveParameterizedThread.Runnable<T, T, T, T, T> runnable) {
		this.runnable = new FiveParameterizedThread<>(A, B, C, D, E, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, SixParameterizedThread.Runnable<T, T, T, T, T, T> runnable) {
		this.runnable = new SixParameterizedThread<>(A, B, C, D, E, F, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, SevenParameterizedThread.Runnable<T, T, T, T, T, T, T> runnable) {
		this.runnable = new SevenParameterizedThread<>(A, B, C, D, E, F, G, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, EightParameterizedThread.Runnable<T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new EightParameterizedThread<>(A, B, C, D, E, F, G, H, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, NineParameterizedThread.Runnable<T, T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new NineParameterizedThread<>(A, B, C, D, E, F, G, H, I, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J, TenParameterizedThread.Runnable<T, T, T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new TenParameterizedThread<>(A, B, C, D, E, F, G, H, I, J, runnable);
	}

	/**
	 * a method with parameter
	 */
	@Override public void run() {
		runnable.run();
	}

}