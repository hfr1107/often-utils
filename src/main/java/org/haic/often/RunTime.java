package org.haic.often;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 终端控制台
 *
 * @author haicdust
 * @version 1.0
 * @since 2021/12/25 06:50
 */
public class RunTime {

	protected Charset charset = StandardCharsets.UTF_8; // 字符集格式
	protected File directory = new File(System.getProperty("user.dir")); // 工作目录

	protected List<String> command = new ArrayList<>(); // 命令
	protected String terminal; // 默认终端

	protected RunTime() {
	}

	/**
	 * 设置待执行的命令
	 *
	 * @param dos 待执行的命令
	 * @return new RunCmd
	 */
	@Contract(pure = true) public static RunTime dos(@NotNull String... dos) {
		return dos(Arrays.stream(dos).toList());
	}

	/**
	 * 设置待执行的命令
	 *
	 * @param dos 待执行的命令
	 * @return new RunCmd
	 */
	@Contract(pure = true) public static RunTime dos(@NotNull List<String> dos) {
		return config().command(dos);
	}

	/**
	 * 获取新的实例
	 *
	 * @return new RunTime
	 */
	@Contract(pure = true) protected static RunTime config() {
		return new RunTime();
	}

	/**
	 * 设置 终端命令
	 *
	 * @param command 终端命令
	 * @return this
	 */
	@Contract(pure = true) protected RunTime command(@NotNull List<String> command) {
		if (Judge.isEmpty(terminal)) {
			this.command = command;
		} else {
			this.command = new ArrayList<>();
			this.command.add(terminal);
			this.command.add("/c");
			this.command.addAll(command);
		}
		return this;
	}

	/**
	 * 使用终端执行命令,默认为cmd终端
	 *
	 * @return this
	 */
	@Contract(pure = true) public RunTime terminal() {
		return terminal(Terminal.CMD);
	}

	/**
	 * 使用终端执行命令
	 *
	 * @param terminal 枚举Terminal类可执行终端
	 * @return this
	 */
	@Contract(pure = true) public RunTime terminal(@NotNull Terminal terminal) {
		return terminal(terminal.value);
	}

	/**
	 * 使用终端执行命令
	 *
	 * @param terminal 终端路径
	 * @return this
	 */
	@Contract(pure = true) public RunTime terminal(@NotNull String terminal) {
		this.terminal = terminal;
		return command(this.command);
	}

	/**
	 * 设置 字符集编码名
	 *
	 * @param CharsetName 字符集编码名
	 * @return this
	 */
	@Contract(pure = true) public RunTime charset(@NotNull String CharsetName) {
		charset(Charset.forName(CharsetName));
		return this;
	}

	/**
	 * 设置 字符集编码
	 *
	 * @param charset 字符集编码
	 * @return this
	 */
	@Contract(pure = true) public RunTime charset(@NotNull Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * 设置 工作目录
	 *
	 * @param directory 工作目录路径
	 * @return this
	 */
	@Contract(pure = true) public RunTime directory(@NotNull String directory) {
		return directory(new File(directory));
	}

	/**
	 * 设置 工作目录
	 *
	 * @param directory 工作目录
	 * @return this
	 */
	@Contract(pure = true) public RunTime directory(@NotNull File directory) {
		this.directory = directory;
		return this;
	}

	/**
	 * 执行终端命令并获取执行的信息
	 *
	 * @return 执行的信息
	 */
	@NotNull @Contract(pure = true) public String readInfo() {
		String result = "";
		Process process;
		try (InputStream inputStream = (process = new ProcessBuilder(command).redirectErrorStream(true).directory(directory).start()).getInputStream()) {
			result = StreamUtils.stream(inputStream).charset(charset).getString();
			process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return StringUtils.deletePefixAndSuffix(result.replaceAll("\r\n", "\n"), StringUtils.LF);
	}

	/**
	 * 执行终端命令并获取退出值
	 *
	 * @return 进程的退出值, 一般情况下, 0为正常终止
	 */
	@Contract(pure = true) public int execute() {
		int status = 1;
		Process process;
		try (InputStream inputStream = (process = new ProcessBuilder(command).redirectErrorStream(true).directory(directory).start()).getInputStream()) {
			inputStream.close();
			status = process.waitFor();
			process.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * 终端枚举类
	 */
	public enum Terminal {
		/**
		 * windows环境变量默认终端
		 */
		DEFAULT(new File(System.getenv("ComSpec")).getName()),
		/**
		 * CMD 终端
		 */
		CMD("cmd"),
		/**
		 * powershell 终端
		 */
		POWERSHELL("powershell");

		private final String value;

		Terminal(String value) {
			this.value = value;
		}

		/**
		 * 获得 枚举方法的值
		 *
		 * @return value
		 */
		@Contract(pure = true) public String getValue() {
			return value;
		}
	}

}