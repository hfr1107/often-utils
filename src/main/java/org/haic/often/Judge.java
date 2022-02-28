package org.haic.often;

import org.jetbrains.annotations.Contract;

/**
 * 判断是否为空
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/18 17:40
 */
public class Judge {

	/**
	 * 判断所给参数是否为负数
	 *
	 * @param index 数字
	 * @return 判断结果
	 */
	public static boolean isMinus(long index) {
		return index < 0;
	}

	/**
	 * 判断所给参数是否为负一
	 *
	 * @param index 数字
	 * @return 判断结果
	 */
	public static boolean isMinusOne(long index) {
		return index == -1;
	}

	/**
	 * 判断所给参数是否为Null
	 *
	 * @param <T> 泛型
	 * @param T   泛型
	 * @return 判断结果
	 */
	public static <T> boolean isNull(T T) {
		return T == null;
	}

	/**
	 * 字符串 String
	 *
	 * @param str 字符串
	 * @return 判断结果
	 */
	@Contract(pure = true) public static boolean isEmpty(String str) {
		return Judge.isNull(str) || str.equals("");
	}

	/**
	 * 输入数字，判断是否为0
	 *
	 * @param index 数字
	 * @return 判断结果
	 */
	@Contract(pure = true) public static boolean isEmpty(long index) {
		return index == 0;
	}

	/**
	 * 判断char array数组是否为空
	 *
	 * @param c char array数组
	 * @return 判断结果
	 */
	public static boolean isEmpty(char... c) {
		return Judge.isNull(c) || c.length == 0;
	}

	/**
	 * 判断泛型array数组是否为空
	 *
	 * @param T   泛型array数组
	 * @param <T> 泛型
	 * @return 判断结果
	 */
	public static <T> boolean isEmpty(T[] T) {
		return Judge.isNull(T) || T.length == 0;
	}

	/**
	 * 判断所给参数是否为正数
	 *
	 * @param index 数字
	 * @return 判断结果
	 */
	public boolean isPositive(long index) {
		return index > 0;
	}

}