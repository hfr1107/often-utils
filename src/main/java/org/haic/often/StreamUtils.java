package org.haic.often;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java 8 Stream 流工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2021/3/13 12:01
 */
public class StreamUtils {

	/**
	 * Iterable ( list ( T )  ) to list ( T )<br/>
	 * 内嵌数组转换为单个数组
	 *
	 * @param list 内嵌数组
	 * @param <T>  泛型
	 * @return 单数组
	 */
	@NotNull @Contract(pure = true) public static <T> List<T> listlistToList(Collection<List<T>> list) {
		return list.stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
	}

	/**
	 * 多个数组合并为单个数组
	 *
	 * @param list 数组
	 * @param <T>  泛型
	 * @return 单数组
	 */
	@SafeVarargs @NotNull @Contract(pure = true) public static <T> List<T> listsToList(Collection<T>... list) {
		return Arrays.stream(list).flatMap(Collection::stream).collect(Collectors.toList());
	}

	/**
	 * 两个数组去除重复项
	 *
	 * @param frist  第一个动态数组
	 * @param second 第二个动态数组
	 * @param <T>    泛型
	 * @return 返回未重复的数组
	 */
	@NotNull @Contract(pure = true) public static <T> List<T> listDeduplication(List<T> frist, List<T> second) {
		return listDeduplication(frist.parallelStream(), second.parallelStream());
	}

	/**
	 * 两个数组去除重复项
	 *
	 * @param frist  第一个动态数组
	 * @param second 第二个动态数组 流
	 * @param <T>    泛型
	 * @return 返回未重复的数组
	 */
	@NotNull @Contract(pure = true) public static <T> List<T> listDeduplication(List<T> frist, Stream<T> second) {
		return listDeduplication(frist.parallelStream(), second);
	}

	/**
	 * 两个数组去除重复项
	 *
	 * @param frist  第一个动态数组 流
	 * @param second 第二个动态数组
	 * @param <T>    泛型
	 * @return 返回未重复的数组
	 */
	@NotNull @Contract(pure = true) public static <T> List<T> listDeduplication(Stream<T> frist, List<T> second) {
		return listDeduplication(frist, second.parallelStream());
	}

	/**
	 * 两个数组去除重复项
	 *
	 * @param frist  第一个动态数组 流
	 * @param second 第二个动态数组 流
	 * @param <T>    泛型
	 * @return 返回未重复的数组
	 */
	@NotNull @Contract(pure = true) public static <T> List<T> listDeduplication(Stream<T> frist, Stream<T> second) {
		return frist.filter(one -> second.noneMatch(two -> Objects.equals(one, two))).collect(Collectors.toList());
	}

	/**
	 * 流排序，隐式处理
	 *
	 * @param list 动态数组
	 * @param <E>  泛型
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> sort(@NotNull List<E> list) {
		return list.stream().sorted().collect(Collectors.toList());
	}

	/**
	 * 流排序，隐式处理
	 *
	 * @param list       动态数组
	 * @param <E>        泛型
	 * @param comparator Comparator排序参数
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> sort(@NotNull List<E> list, @NotNull Comparator<E> comparator) {
		return list.stream().sorted(comparator).collect(Collectors.toList());
	}

	/**
	 * 去重无排序,流排序，隐式处理
	 *
	 * @param list 动态数组
	 * @param <E>  泛型
	 * @return 无排序的数组
	 */
	@NotNull @Contract(pure = true) public static <E> List<E> streamSet(@NotNull List<E> list) {
		return list.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * int类型数组转换Integer类型动态数组
	 *
	 * @param nums int类型静态数组
	 * @return Integer类型动态数组
	 */
	@NotNull @Contract(pure = true) public static List<Integer> intToInteger(int[] nums) {
		return Arrays.stream(nums).boxed().collect(Collectors.toList());
	}

	/**
	 * 字符串转流
	 *
	 * @param str 字符串
	 * @return 流
	 */
	@NotNull @Contract(pure = true) public static InputStream streamByString(@NotNull String str) {
		return new ByteArrayInputStream(str.getBytes());
	}

	/**
	 * InputStream 流工具
	 *
	 * @param inputStream InputStream
	 * @return new InputStreamUtil
	 */
	@NotNull @Contract(pure = true) public static InputStreamUtil stream(@NotNull InputStream inputStream) {
		return new InputStreamUtil(inputStream);
	}

	/**
	 * BufferedInputStream 流工具
	 *
	 * @param inputStream BufferedInputStream
	 * @return new InputStreamReaderUtil
	 */
	@NotNull @Contract(pure = true) public static BufferedInputStreamUtil stream(@NotNull BufferedInputStream inputStream) {
		return new BufferedInputStreamUtil(inputStream);
	}

	/**
	 * InputStreamReader 流工具
	 *
	 * @param inputStream InputStreamReader
	 * @return new InputStreamReaderUtil
	 */
	@NotNull @Contract(pure = true) public static InputStreamReaderUtil stream(@NotNull InputStreamReader inputStream) {
		return new InputStreamReaderUtil(inputStream);
	}

	/**
	 * 流工具类
	 */
	protected abstract static class InputStreamBuilder {

		protected Charset charset = StandardCharsets.UTF_8;

		protected InputStreamBuilder() {
		}

		/**
		 * 设置 字符集编码(默认UTF8)
		 *
		 * @param charsetName 字符集编码名称
		 * @return this
		 */
		@Contract(pure = true) public abstract InputStreamBuilder charset(@NotNull String charsetName);

		/**
		 * 设置 字符集编码(默认UTF8)
		 *
		 * @param charset 字符集编码
		 * @return this
		 */
		@Contract(pure = true) public abstract InputStreamBuilder charset(@NotNull Charset charset);

		/**
		 * 获取 Stream 中字符串信息
		 *
		 * @return 字符串文本
		 */
		@NotNull @Contract(pure = true) public abstract String read() throws IOException;

		/**
		 * 获取 Stream 中字符串信息
		 *
		 * @return 按行分割的字符串列表
		 */
		@NotNull @Contract(pure = true) public abstract List<String> readAsLine() throws IOException;

		/**
		 * 获取 Stream 中字符信息,转为 ByteArrayOutputStream
		 *
		 * @return ByteArrayOutputStream
		 */
		@NotNull @Contract(pure = true) public abstract ByteArrayOutputStream toByteArrayOutputStream() throws IOException;

		/**
		 * 获取 Stream 中字符信息
		 *
		 * @return byte数组
		 */
		@Contract(pure = true) public abstract byte[] toByteArray() throws IOException;

	}

	/**
	 * InputStreamUtil 工具类
	 */
	public static class InputStreamUtil extends InputStreamBuilder {
		protected InputStream stream;

		protected InputStreamUtil(@NotNull InputStream in) {
			this.stream = in;
		}

		@Override @Contract(pure = true) public InputStreamUtil charset(@NotNull String charsetName) {
			this.charset = Charset.forName(charsetName);
			return this;
		}

		@Override @Contract(pure = true) public InputStreamUtil charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		@NotNull @Contract(pure = true) public String read() throws IOException {
			return toByteArrayOutputStream().toString(charset);
		}

		@NotNull @Contract(pure = true) public List<String> readAsLine() throws IOException {
			return stream(new InputStreamReader(stream, charset)).readAsLine();
		}

		@Contract(pure = true) public byte[] toByteArray() throws IOException {
			return toByteArrayOutputStream().toByteArray();
		}

		@NotNull @Contract(pure = true) public ByteArrayOutputStream toByteArrayOutputStream() throws IOException {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			stream.transferTo(result);
			return result;
		}

	}

	/**
	 * BufferedInputStreamUtil 工具类
	 */
	public static class BufferedInputStreamUtil extends InputStreamBuilder {
		protected BufferedInputStream stream;

		protected BufferedInputStreamUtil(BufferedInputStream in) {
			this.stream = in;
		}

		@Contract(pure = true) public BufferedInputStreamUtil charset(@NotNull String charsetName) {
			this.charset = Charset.forName(charsetName);
			return this;
		}

		@Contract(pure = true) public BufferedInputStreamUtil charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		@NotNull @Contract(pure = true) public String read() throws IOException {
			return toByteArrayOutputStream().toString(charset);
		}

		@NotNull @Contract(pure = true) public List<String> readAsLine() throws IOException {
			return stream(new InputStreamReader(stream, charset)).readAsLine();
		}

		@NotNull @Contract(pure = true) public ByteArrayOutputStream toByteArrayOutputStream() throws IOException {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			stream.transferTo(result);
			return result;
		}

		@Contract(pure = true) public byte[] toByteArray() throws IOException {
			return toByteArrayOutputStream().toByteArray();
		}

	}

	/**
	 * InputStreamReader 工具类
	 */
	public static class InputStreamReaderUtil {
		protected InputStreamReader stream;

		protected InputStreamReaderUtil(@NotNull InputStreamReader in) {
			this.stream = in;
		}

		/**
		 * 获取 Stream 中字符串信息
		 *
		 * @return 字符串文本
		 */
		@NotNull @Contract(pure = true) public String read() throws IOException {
			StringWriter result = new StringWriter();
			stream.transferTo(result);
			return String.valueOf(result);
		}

		/**
		 * 获取 Stream 中字符串信息
		 *
		 * @return 按行分割的字符串列表
		 */
		@NotNull @Contract(pure = true) public List<String> readAsLine() throws IOException {
			List<String> result = new ArrayList<>();
			BufferedReader bufferedReader = new BufferedReader(stream);
			String line;
			while (!Judge.isNull(line = bufferedReader.readLine())) {
				result.add(line);
			}
			return result;
		}

	}

	protected abstract static class OutputStreamBuilder {

		protected Charset charset = StandardCharsets.UTF_8;

		protected OutputStreamBuilder() {
		}

		/**
		 * 设置 字符集编码(默认UTF8)
		 *
		 * @param charsetName 字符集编码名称
		 * @return this
		 */
		@Contract(pure = true) public abstract OutputStreamBuilder charset(@NotNull String charsetName);

		/**
		 * 设置 字符集编码(默认UTF8)
		 *
		 * @param charset 字符集编码
		 * @return this
		 */
		@Contract(pure = true) public abstract OutputStreamBuilder charset(@NotNull Charset charset);

	}

	public static class OutputStreamUtil extends OutputStreamBuilder {

		protected int DEFAULT_BUFFER_SIZE = 8192;

		protected FileOutputStream stream;

		protected OutputStreamUtil(@NotNull FileOutputStream stream) {
			this.stream = stream;
		}

		@Contract(pure = true) public OutputStreamUtil charset(@NotNull String charsetName) {
			this.charset = Charset.forName(charsetName);
			return this;
		}

		@Contract(pure = true) public OutputStreamUtil charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		/**
		 * 使用BufferedWriter方式向输出流内写入字符数据
		 *
		 * @param s 字符数据
		 */
		@Contract(pure = true) public void write(String s) throws IOException {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream, charset), DEFAULT_BUFFER_SIZE);
			out.write(s); // 文件输出流用于将数据写入文件
			out.flush();
		}

	}

}
