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

	public ParameterizedThread(T A, OneParameterized.Runnable<T> runnable) {
		this.runnable = new OneParameterized<>(A, runnable);
	}

	public ParameterizedThread(T A, T B, TwoParameterized.Runnable<T, T> runnable) {
		this.runnable = new TwoParameterized<>(A, B, runnable);
	}

	public ParameterizedThread(T A, T B, T C, ThreeParameterized.Runnable<T, T, T> runnable) {
		this.runnable = new ThreeParameterized<>(A, B, C, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, FourParameterized.Runnable<T, T, T, T> runnable) {
		this.runnable = new FourParameterized<>(A, B, C, D, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, FiveParameterized.Runnable<T, T, T, T, T> runnable) {
		this.runnable = new FiveParameterized<>(A, B, C, D, E, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, SixParameterized.Runnable<T, T, T, T, T, T> runnable) {
		this.runnable = new SixParameterized<>(A, B, C, D, E, F, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, SevenParameterized.Runnable<T, T, T, T, T, T, T> runnable) {
		this.runnable = new SevenParameterized<>(A, B, C, D, E, F, G, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, EightParameterized.Runnable<T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new EightParameterized<>(A, B, C, D, E, F, G, H, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, NineParameterized.Runnable<T, T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new NineParameterized<>(A, B, C, D, E, F, G, H, I, runnable);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J, TenParameterized.Runnable<T, T, T, T, T, T, T, T, T, T> runnable) {
		this.runnable = new TenParameterized<>(A, B, C, D, E, F, G, H, I, J, runnable);
	}

	/**
	 * a method with parameter
	 */
	@Override public void run() {
		runnable.run();
	}

}