package org.haic.often.Multithread;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 多线程 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/18 17:26
 */
public class MultiThreadUtil {

	/**
	 * 关闭线程池,并等待结束
	 *
	 * @param executorService 线程池对象
	 */
	@Contract(pure = true) public static void waitForEnd(@NotNull ExecutorService executorService) {
		executorService.shutdown(); // 关闭线程
		try { // 等待线程结束
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 线程等待 MILLISECONDS_SLEEP
	 *
	 * @param millis 线程等待时间 (毫秒)
	 */
	@Contract(pure = true) public static void waitForThread(int millis) {
		try { // 程序等待
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取Future容器的多线程返回值
	 *
	 * @param future future对象
	 * @param <E>    泛型
	 * @return 返回值
	 */
	@Contract(pure = true) public static <E> E getFuture(@NotNull Future<E> future) {
		E result = null;
		try {
			result = future.get(); // 获得返回值
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 输出程序运行时间，请在程序开始处加入函数
	 */
	@Contract(pure = true) public static void runTime() {
		long start = System.currentTimeMillis(); // 获取开始时间
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			long end = System.currentTimeMillis(); // 获取结束时间
			System.out.println("程序运行时间：" + (end - start) + "ms");
		}));
	}

}