package org.haic.often.Multithread;

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

	public FutureTaskThread(T A, OneFutureTaskThread.FutureTaskThreadStart<T, V> futureTaskStart) {
		callable = new OneFutureTaskThread<>(A, futureTaskStart);
	}

	public FutureTaskThread(T A, T B, TwoFutureTaskThread.FutureTaskThreadStart<T, T, V> futureTaskStart) {
		callable = new TwoFutureTaskThread<>(A, B, futureTaskStart);
	}

	public FutureTaskThread(T A, T B, T C, ThreeFutureTaskThread.FutureTaskThreadStart<T, T, T, V> parameterStart) {
		callable = new ThreeFutureTaskThread<>(A, B, C, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, FourFutureTaskThread.FutureTaskThreadStart<T, T, T, T, V> parameterStart) {
		callable = new FourFutureTaskThread<>(A, B, C, D, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, FiveFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, V> parameterStart) {
		callable = new FiveFutureTaskThread<>(A, B, C, D, E, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, SixFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, T, V> parameterStart) {
		callable = new SixFutureTaskThread<>(A, B, C, D, E, F, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, SevenFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, T, T, V> parameterStart) {
		callable = new SevenFutureTaskThread<>(A, B, C, D, E, F, G, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, EightFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, T, T, T, V> parameterStart) {
		callable = new EightFutureTaskThread<>(A, B, C, D, E, F, G, H, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I,
			NineFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, T, T, T, T, V> parameterStart) {
		callable = new NineFutureTaskThread<>(A, B, C, D, E, F, G, H, I, parameterStart);
	}

	public FutureTaskThread(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J,
			TenFutureTaskThread.FutureTaskThreadStart<T, T, T, T, T, T, T, T, T, T, V> parameterStart) {
		callable = new TenFutureTaskThread<>(A, B, C, D, E, F, G, H, I, J, parameterStart);
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

	public record OneFutureTaskThread<T, V>(T A, FutureTaskThreadStart<T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, V> {
			V call(A A) throws Exception;
		}
	}

	public record TwoFutureTaskThread<T, V>(T A, T B, FutureTaskThreadStart<T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, V> {
			V call(A A, B B) throws Exception;
		}
	}

	public record ThreeFutureTaskThread<T, V>(T A, T B, T C, FutureTaskThreadStart<T, T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, V> {
			V call(A A, B B, C C) throws Exception;
		}
	}

	public record FourFutureTaskThread<T, V>(T A, T B, T C, T D, FutureTaskThreadStart<T, T, T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, V> {
			V call(A A, B B, C C, D D) throws Exception;
		}
	}

	public record FiveFutureTaskThread<T, V>(T A, T B, T C, T D, T E, FutureTaskThreadStart<T, T, T, T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, V> {
			V call(A A, B B, C C, D D, E E) throws Exception;
		}
	}

	public record SixFutureTaskThread<T, V>(T A, T B, T C, T D, T E, T F, FutureTaskThreadStart<T, T, T, T, T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E, F);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, F, V> {
			V call(A A, B B, C C, D D, E E, F F) throws Exception;
		}
	}

	public record SevenFutureTaskThread<T, V>(T A, T B, T C, T D, T E, T F, T G, FutureTaskThreadStart<T, T, T, T, T, T, T, V> futureTaskStart)
			implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E, F, G);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, F, G, V> {
			V call(A A, B B, C C, D D, E E, F F, G G) throws Exception;
		}
	}

	public record EightFutureTaskThread<T, V>(T A, T B, T C, T D, T E, T F, T G, T H, FutureTaskThreadStart<T, T, T, T, T, T, T, T, V> futureTaskStart)
			implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E, F, G, H);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, F, G, H, V> {
			V call(A A, B B, C C, D D, E E, F F, G G, H H) throws Exception;
		}
	}

	public record NineFutureTaskThread<T, V>(T A, T B, T C, T D, T E, T F, T G, T H, T I, FutureTaskThreadStart<T, T, T, T, T, T, T, T, T, V> futureTaskStart)
			implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E, F, G, H, I);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, F, G, H, I, V> {
			V call(A A, B B, C C, D D, E E, F F, G G, H H, I I) throws Exception;
		}
	}

	public record TenFutureTaskThread<T, V>(T A, T B, T C, T D, T E, T F, T G, T H, T I, T J,
											FutureTaskThreadStart<T, T, T, T, T, T, T, T, T, T, V> futureTaskStart) implements Callable<V> {
		/**
		 * run method to be called in that separately executing thread.
		 */
		@Override public V call() throws Exception {
			return futureTaskStart.call(A, B, C, D, E, F, G, H, I, J);
		}

		/**
		 * FutureTaskThread defines the start method for starting a thread.
		 */
		public interface FutureTaskThreadStart<A, B, C, D, E, F, G, H, I, J, V> {
			V call(A A, B B, C C, D D, E E, F F, G G, H H, I I, J J) throws Exception;
		}
	}

}