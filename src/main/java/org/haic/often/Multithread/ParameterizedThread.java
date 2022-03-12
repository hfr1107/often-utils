package org.haic.often.Multithread;

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

	public ParameterizedThread(T A, OneParameterizedThread.ParameterizedThreadStart<T> parameterStart) {
		runnable = new OneParameterizedThread<>(A, parameterStart);
	}

	public ParameterizedThread(T A, T B, TwoParameterizedThread.ParameterizedThreadStart<T, T> parameterStart) {
		runnable = new TwoParameterizedThread<>(A, B, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, ThreeParameterizedThread.ParameterizedThreadStart<T, T, T> parameterStart) {
		runnable = new ThreeParameterizedThread<>(A, B, C, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, FourParameterizedThread.ParameterizedThreadStart<T, T, T, T> parameterStart) {
		runnable = new FourParameterizedThread<>(A, B, C, D, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, FiveParameterizedThread.ParameterizedThreadStart<T, T, T, T, T> parameterStart) {
		runnable = new FiveParameterizedThread<>(A, B, C, D, E, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, SixParameterizedThread.ParameterizedThreadStart<T, T, T, T, T, T> parameterStart) {
		runnable = new SixParameterizedThread<>(A, B, C, D, E, F, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, SevenParameterizedThread.ParameterizedThreadStart<T, T, T, T, T, T, T> parameterStart) {
		runnable = new SevenParameterizedThread<>(A, B, C, D, E, F, G, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H,
			EightParameterizedThread.ParameterizedThreadStart<T, T, T, T, T, T, T, T> parameterStart) {
		runnable = new EightParameterizedThread<>(A, B, C, D, E, F, G, H, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I,
			NineParameterizedThread.ParameterizedThreadStart<T, T, T, T, T, T, T, T, T> parameterStart) {
		runnable = new NineParameterizedThread<>(A, B, C, D, E, F, G, H, I, parameterStart);
	}

	public ParameterizedThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J,
			TenParameterizedThread.ParameterizedThreadStart<T, T, T, T, T, T, T, T, T, T> parameterStart) {
		runnable = new TenParameterizedThread<>(A, B, C, D, E, F, G, H, I, J, parameterStart);
	}

	/**
	 * a method with parameter
	 */
	@Override public void run() {
		runnable.run();
	}

	protected record OneParameterizedThread<T>(T A, ParameterizedThreadStart<T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A> {
			/**
			 * a method with parameter
			 */
			void run(A A);
		}
	}

	protected record TwoParameterizedThread<T>(T A, T B, ParameterizedThreadStart<T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B);
		}
	}

	protected record ThreeParameterizedThread<T>(T A, T B, T C, ParameterizedThreadStart<T, T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C);
		}
	}

	protected record FourParameterizedThread<T>(T A, T B, T C, T D, ParameterizedThreadStart<T, T, T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D);
		}
	}

	protected record FiveParameterizedThread<T>(T A, T B, T C, T D, T E, ParameterizedThreadStart<T, T, T, T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D, E);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D, E> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D, E E);
		}
	}

	protected record SixParameterizedThread<T>(T A, T B, T C, T D, T E, T F, ParameterizedThreadStart<T, T, T, T, T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D, E, F);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D, E, F> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D, E E, F F);
		}
	}

	protected record SevenParameterizedThread<T>(T A, T B, T C, T D, T E, T F, T G, ParameterizedThreadStart<T, T, T, T, T, T, T> parameterStart)
			implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D, E, F, G);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D, E, F, G> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D, E E, F F, G G);
		}
	}

	protected record EightParameterizedThread<T>(T A, T B, T C, T D, T E, T F, T G, T H, ParameterizedThreadStart<T, T, T, T, T, T, T, T> parameterStart)
			implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D, E, F, G, H);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D, E, F, G, H> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D, E E, F F, G G, H H);
		}
	}

	protected record NineParameterizedThread<T>(T A, T B, T C, T D, T E, T F, T G, T H, T I, ParameterizedThreadStart<T, T, T, T, T, T, T, T, T> parameterStart)
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

	protected record TenParameterizedThread<T>(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J,
											   ParameterizedThreadStart<T, T, T, T, T, T, T, T, T, T> parameterStart) implements Runnable {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public void run() {
			parameterStart.run(A, B, C, D, E, F, G, H, I, J);
		}

		/**
		 * ParameterizedThreadStart defines the start method for starting a thread.
		 */
		public interface ParameterizedThreadStart<A, B, C, D, E, F, G, H, I, J> {
			/**
			 * a method with parameter
			 */
			void run(A A, B B, C C, D D, E E, F F, G G, H H, I I, J J);
		}
	}

}