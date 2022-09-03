package org.haic.often;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * @author haicdust
 * @version 1.0
 * @since 2021/4/12 11:04
 */
public class ReadWriteUtils {

	protected File source; // 目标文件或文件夹
	protected int DEFAULT_BUFFER_SIZE = 8192; // 缓冲区大小
	protected Charset charset = StandardCharsets.UTF_8; // 字符集编码格式
	protected boolean append = true; // 默认追加写入

	protected ReadWriteUtils() {
	}

	/**
	 * 设置目标文件或文件夹并获取 new ReadWriteUtils
	 *
	 * @param source 文件或文件夹路径
	 * @return this
	 */
	@Contract(pure = true) public static ReadWriteUtils orgin(String source) {
		return orgin(new File(source));
	}

	/**
	 * 设置目标文件或文件夹并获取 new ReadWriteUtils
	 *
	 * @param source 文件或文件夹
	 * @return this
	 */
	@Contract(pure = true) public static ReadWriteUtils orgin(File source) {
		return config().file(source);
	}

	/**
	 * 获取 new ReadWriteUtils
	 *
	 * @return this
	 */
	@Contract(pure = true) protected static ReadWriteUtils config() {
		return new ReadWriteUtils();
	}

	/**
	 * 设置 文件或文件夹
	 *
	 * @param source 文件或文件夹
	 * @return this
	 */
	@Contract(pure = true) protected ReadWriteUtils file(File source) {
		this.source = source;
		return this;
	}

	/**
	 * 设置 缓冲区大小,用于写入数据时使用
	 *
	 * @param bufferSize 缓冲区大小
	 * @return this
	 */
	@Contract(pure = true) public ReadWriteUtils bufferSize(int bufferSize) {
		this.DEFAULT_BUFFER_SIZE = bufferSize;
		return this;
	}

	/**
	 * 设置 字符集编码格式
	 *
	 * @param charset 字符集编码格式
	 * @return this
	 */
	@Contract(pure = true) public ReadWriteUtils charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * 设置 字符集格式
	 *
	 * @param charsetName 字符集格式
	 * @return this
	 */
	@Contract(pure = true) public ReadWriteUtils charset(String charsetName) {
		return charset(Charset.forName(charsetName));
	}

	/**
	 * 设置 追加写入
	 *
	 * @param append 启用追加写入,默认true
	 * @return this
	 */
	@Contract(pure = true) public ReadWriteUtils append(boolean append) {
		this.append = append;
		return this;
	}

	// ================================================== WriteUtils ==================================================

	/**
	 * 将数组合按行行写入文件
	 *
	 * @param lists 字符串数组
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean writeAsLine(@NotNull List<String> lists) {
		return write(lists, StringUtils.LF);
	}

	/**
	 * 将字符串写入文件
	 *
	 * @param s 字符串
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean write(@NotNull String s) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (BufferedWriter outStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(source, append), charset), DEFAULT_BUFFER_SIZE)) {
			outStream.write(s); // 文件输出流用于将数据写入文件
			outStream.flush();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 将byte数组写入文件
	 *
	 * @param b byte数组
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean write(byte[] b) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (FileOutputStream outStream = new FileOutputStream(source, append)) {
			outStream.write(b); // 文件输出流用于将数据写入文件
			outStream.flush();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 将数组按照分隔符写入文件,默认为空格
	 *
	 * @param lists 字符串数组
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean write(@NotNull List<String> lists) {
		return write(lists, StringUtils.SPACE);
	}

	/**
	 * 将数组按照分隔符写入文件
	 *
	 * @param lists 字符串数组
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean write(@NotNull List<String> lists, @NotNull String regex) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (BufferedWriter outStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(source, append), charset), DEFAULT_BUFFER_SIZE)) {
			outStream.write(StringUtils.join(lists, regex)); // 文件输出流用于将数据写入文件
			outStream.flush();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * FileChannel方式写入文件文本
	 *
	 * @param s 字符串
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean channelWrite(@NotNull String s) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (FileChannel channel = new FileOutputStream(source, append).getChannel()) {
			channel.write(charset.encode(s));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * RandomAccessFile方式写入文本
	 *
	 * @param s 字符串
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean randomWrite(@NotNull String s) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (RandomAccessFile randomAccess = new RandomAccessFile(source, "rw")) {
			if (append) {
				randomAccess.seek(source.length());
			}
			randomAccess.write(s.getBytes(charset));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * MappedByteBuffer内存映射方法写入文件
	 *
	 * @param s 字符串
	 * @return 写入是否成功
	 */
	@Contract(pure = true) public boolean mappedWrite(String s) {
		File parent = source.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		byte[] params = s.getBytes(charset);
		try (FileChannel fileChannel = append ?
				FileChannel.open(source.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE) :
				FileChannel.open(source.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			fileChannel.map(FileChannel.MapMode.READ_WRITE, append ? source.length() : 0, params.length).put(params);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 文件复制
	 *
	 * @param out 指定输出文件路径
	 * @return 文件复制状态
	 */
	@Contract(pure = true) public boolean copy(String out) {
		return copy(new File(out));
	}

	/**
	 * 文件复制
	 *
	 * @param out 指定输出文件
	 * @return 文件复制状态
	 */
	@Contract(pure = true) public boolean copy(File out) {
		File parent = out.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (InputStream input = new FileInputStream(source); OutputStream output = new FileOutputStream(out)) {
			input.transferTo(output);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * RandomAccessFile 文件复制
	 *
	 * @param out 指定输出文件
	 * @return 文件复制
	 */
	@Contract(pure = true) public boolean randomCopy(@NotNull String out) {
		return randomCopy(new File(out));
	}

	/**
	 * RandomAccessFile 文件复制
	 *
	 * @param out 指定输出文件
	 * @return 文件复制
	 */
	@Contract(pure = true) public boolean randomCopy(@NotNull File out) {
		File parent = out.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (RandomAccessFile inputRandomAccess = new RandomAccessFile(source, "r"); RandomAccessFile outputRandomAccess = new RandomAccessFile(out, "rw")) {
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while (!Judge.isMinusOne(length = inputRandomAccess.read(buffer))) {
				outputRandomAccess.write(buffer, 0, length);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * FileChannel 文件复制
	 *
	 * @param out 指定输出文件
	 * @return 文件复制状态
	 */
	@Contract(pure = true) public boolean channelCopy(String out) {
		return channelCopy(new File(out));
	}

	/**
	 * FileChannel 文件复制
	 *
	 * @param out 指定输出文件
	 * @return 文件复制状态
	 */
	@Contract(pure = true) public boolean channelCopy(File out) {
		File parent = out.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (FileChannel inputChannel = new FileInputStream(source).getChannel(); FileChannel outputChannel = new FileOutputStream(out).getChannel()) {
			int count = 0;
			long size = inputChannel.size();
			while (count < size) { // 循环支持2G以上文件
				int position = count;
				count += outputChannel.transferFrom(inputChannel, position, size - position);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Contract(pure = true) public boolean mappedCopy(String out) {
		return mappedCopy(new File(out));
	}

	/**
	 * MappedByteBuffer 文件复制
	 *
	 * @param out 指定输出文件路径
	 * @return 文件复制状态
	 */
	@Contract(pure = true) public boolean mappedCopy(File out) {
		File parent = out.getParentFile();
		if (!Judge.isNull(parent)) {
			FilesUtils.createFolder(parent);
		}
		try (FileChannel inputChannel = new FileInputStream(source).getChannel(); FileChannel outputChannel = new RandomAccessFile(out, "rw").getChannel()) {
			long size = inputChannel.size();
			for (long i = 0; i < size; i += Integer.MAX_VALUE) {
				long position = Integer.MAX_VALUE * i;
				outputChannel.map(FileChannel.MapMode.READ_WRITE, position, size - position)
						.put(inputChannel.map(FileChannel.MapMode.READ_ONLY, position, size - position));
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	// ================================================== ReadUtils ==================================================

	/**
	 * 遍历文件或文件夹,按行读取内容
	 *
	 * @return 文本信息列表
	 */
	@Contract(pure = true) public List<String> readAsLine() {
		List<String> result = null;
		try (InputStream in = new FileInputStream(source)) {
			result = StreamUtils.stream(in).charset(charset).readAsLine();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return result;
	}

	/**
	 * 读取指定文件的内容
	 *
	 * @return 文本信息
	 */
	@Contract(pure = true) public String read() {
		String result = null;
		try (InputStream inputStream = new FileInputStream(source)) {
			result = StreamUtils.stream(inputStream).charset(charset).read();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return result;
	}

	/**
	 * 读取指定文件的内容
	 *
	 * @return byte数组
	 */
	@Contract(pure = true) public byte[] readBytes() {
		byte[] result = null;
		try (InputStream inputStream = new FileInputStream(source)) {
			result = StreamUtils.stream(inputStream).charset(charset).toByteArray();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return result;
	}

	/**
	 * FileChannel 读取文件文本
	 *
	 * @return 文本字符串
	 */
	@Contract(pure = true) public String channelRead() {
		CharBuffer result = null;
		try (FileChannel channel = new FileInputStream(source).getChannel()) {
			ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(source.length()));
			channel.read(buffer);
			buffer.flip();
			result = charset.decode(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return String.valueOf(result);
	}

	/**
	 * RandomAccessFile 随机存储读取
	 *
	 * @return 文本
	 */
	@Contract(pure = true) public String randomRead() {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		try (RandomAccessFile randomAccess = new RandomAccessFile(source, "r")) {
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while (!Judge.isMinusOne(length = randomAccess.read(buffer))) {
				result.write(buffer, 0, length);
			}
			result.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString(charset);
	}

	/**
	 * MappedByteBuffer 内存映射方法读取文件文本
	 *
	 * @return 文本
	 */
	@Contract(pure = true) public String mappedRead() {
		CharBuffer result = null;
		try (FileChannel channel = new FileInputStream(source).getChannel()) {
			result = charset.decode(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).asReadOnlyBuffer());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return String.valueOf(result);
	}

}
