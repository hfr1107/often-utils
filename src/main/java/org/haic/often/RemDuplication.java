package org.haic.often;

import org.haic.often.Multithread.MultiThreadUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 去除数组重复项
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/18 17:20
 */
public class RemDuplication {

	/**
	 * 去重无排序,按照输入顺序返回
	 *
	 * @param lists 动态数组
	 * @param <E>   泛型
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> LinkedHashSet(@NotNull List<E> lists) {
		return new ArrayList<>(new LinkedHashSet<>(lists));
	}

	/**
	 * 去重排序
	 *
	 * @param lists 动态数组
	 * @param <E>   泛型
	 * @return 排序后的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> TreeSet(@NotNull List<E> lists) {
		return new ArrayList<>(new TreeSet<>(lists));
	}

	/**
	 * 去重排序
	 *
	 * @param lists 字符串数组
	 * @param <E>   泛型
	 * @return 排序后的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> HashSet(@NotNull List<E> lists) {
		return new ArrayList<>(new HashSet<>(lists));
	}

	/**
	 * 去重无排序,按照输入顺序返回，效率底下,建议数组10000以下
	 *
	 * @param lists 动态数组
	 * @param <E>   泛型
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> ListSet(@NotNull List<E> lists) {
		List<E> result = new CopyOnWriteArrayList<>();
		ExecutorService executorService = Executors.newCachedThreadPool(); // 线程池
		for (E list : lists) {
			executorService.submit(new Thread(() -> { // 执行多线程程
				if (!result.contains(list)) {
					result.add(list);
				}
			}));
		}
		MultiThreadUtils.WaitForEnd(executorService); // 等待线程结束
		return new ArrayList<>(result);
	}

	/**
	 * 去重无排序,流排序，隐式处理
	 *
	 * @param lists 动态数组
	 * @param <E>   泛型
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> StreamSet(@NotNull List<E> lists) {
		return StreamUtils.streamSet(lists);
	}

}