package org.haic.often.Network;

import com.alibaba.fastjson.JSONObject;
import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.Multithread.ParameterizedThread;
import org.haic.often.ReadWriteUtils;
import org.haic.often.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络文件 工具类
 * <p>
 * 用于文件的上传和下载,非线程安全
 * <p>
 * 默认10线程下载,不应设置过高
 *
 * @author haicdust
 * @version 1.8.2
 * @since 2021/12/24 23:07
 */
public class NetworkUtil {

	protected NetworkUtil() {
	}

	/**
	 * 公共静态连接newSession ()
	 * <p>
	 * 创建一个新Connection以用作会话。将为会话维护连接设置（用户代理、超时、URL 等）和 cookie
	 *
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection newSession() {
		return new HttpConnection();
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/> 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		return new HttpConnection().alterUrl(url);
	}

	/**
	 * 获取新的NetworkFileUtil对象并设置配置文件<br/> 配置文件 -> 包含待下载文件的下载信息的文件
	 *
	 * @param path session文件路径
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection session(@NotNull String path) {
		return session(new File(path));
	}

	/**
	 * 获取新的NetworkFileUtil对象并设置配置文件<br/> 配置文件 -> 包含待下载文件的下载信息的文件
	 *
	 * @param file session文件
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection session(@NotNull File file) {
		return new HttpConnection().session(file);
	}

	/**
	 * 下载方法枚举<br/> FILE - 配置文件下载<br/> FULL - 全量下载模式<br/> MULTITHREAD - 多线程模式<br/> MANDATORY - 强制多线程模式<br/> 如果下载文件的配置文件存在,将会自动跳转FILE模式,配置信息将会被文件中的配置覆盖
	 */
	public enum Method {
		/**
		 * 通过配置文件下载,所有的下载配置都会被文件中配置信息覆盖
		 */
		FILE(true),
		/**
		 * 全量下载模式,不受多线程配置影响
		 */
		FULL(true),
		/**
		 * 分块多线程模式,按照pieceSize大小分块下载<br/> 如pieceSize大小为1M,文件大小为100M,那么文件将会被分为100个块下载
		 */
		PIECE(true),
		/**
		 * 经典多线程模式,按照pieceSize大小进行最小分块<br/> 如pieceSize大小为1M,线程为10,不到1M大小文件一个线程,那么跑满线程文件就需不低于10M
		 */
		MULTITHREAD(true),
		/**
		 * 强制多线程模式,不受pieceSize大小影响,无论文件大小,都将以满线程下载<br/> 极端条件下,如果文件大小过小(文件字节大小小于线程),会发生错误
		 */
		MANDATORY(true);

		private final boolean hasBody;

		Method(boolean hasBody) {
			this.hasBody = hasBody;
		}

		/**
		 * 获得 枚举方法的值
		 *
		 * @return value
		 */
		@Contract(pure = true) public boolean hasBody() {
			return hasBody;
		}
	}

	protected static class HttpResponse extends Response {

		protected Connection conn;
		protected Request request;

		protected HttpResponse(Connection conn, Request request) {
			this.conn = conn;
			this.request = request;
		}

		@Contract(pure = true) public int statusCode() {
			return request.statusCode();
		}

		@Contract(pure = true) public String fileName() {
			return request.getStorage().getName();
		}

		@Contract(pure = true) public String filePath() {
			return request.getStorage().getAbsolutePath();
		}

		@Contract(pure = true) public long fileSize() {
			return request.getFileSize();
		}

		@Contract(pure = true) public String header(@NotNull String name) {
			return request.headers().get(name);
		}

		@Contract(pure = true) public Map<String, String> headers() {
			return request.headers();
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return request.cookies().get(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			return request.cookies();
		}

		@Contract(pure = true) public String hash() {
			return request.getHash();
		}

		@Contract(pure = true) public String url() {
			return request.getUrl();
		}

		@Contract(pure = true) public String uploadBody() {
			return request.getBody();
		}

		@Contract(pure = true) public Response restart() {
			return URIUtils.statusIsOK(statusCode()) ? this : conn.download(request.getStorage().getParentFile());
		}

		@Contract(pure = true) public boolean clear() {
			return new File(request.getStorage().getPath() + ".session").exists() && delete();
		}

		@Contract(pure = true) public boolean delete() {
			File storage = request.getStorage();
			File session = new File(storage.getPath() + ".session");
			return (!storage.exists() || storage.delete()) && (!session.exists() || session.delete());
		}
	}

	/**
	 * 响应接口
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/8/18 00:48
	 */
	public static abstract class Response {
		/**
		 * 获取当前下载的完成状态
		 *
		 * @return 当前下载的完成状态
		 */
		@Contract(pure = true) public abstract int statusCode();

		/**
		 * 获取当前下载的文件名
		 *
		 * @return 文件名
		 */
		@Contract(pure = true) public abstract String fileName();

		/**
		 * 获取当前下载的存储在本地的文件路径
		 *
		 * @return 本地的文件路径
		 */
		@Contract(pure = true) public abstract String filePath();

		/**
		 * 获取当前下载的的文件大小
		 *
		 * @return 文件大小
		 */
		@Contract(pure = true) public abstract long fileSize();

		/**
		 * 获取当前下载的的请求头值
		 *
		 * @return 请求头值
		 */
		@Contract(pure = true) public abstract String header(@NotNull String name);

		/**
		 * 获取当前下载的的所有请求头
		 *
		 * @return 所有请求头
		 */
		@Contract(pure = true) public abstract Map<String, String> headers();

		/**
		 * 获取当前下载的的cookie值
		 *
		 * @return cookie值
		 */
		@Contract(pure = true) public abstract String cookie(@NotNull String name);

		/**
		 * 获取当前下载的的cookies
		 *
		 * @return cookies
		 */
		@Contract(pure = true) public abstract Map<String, String> cookies();

		/**
		 * 获取当前下载的的网络md5
		 * <p>
		 * 此md5可能是hash()方法设置的,也可能是服务器返回的,如果都没有则返回null
		 *
		 * @return md5
		 */
		@Contract(pure = true) public abstract String hash();

		/**
		 * 获取当前下载的URL
		 *
		 * @return URL
		 */
		@Contract(pure = true) public abstract String url();

		/**
		 * 在上传文件后服务端返回的数据
		 *
		 * @return 服务器返回数据
		 */
		@Contract(pure = true) public abstract String uploadBody();

		/**
		 * 如果完成状态不为成功,则重启当前下载任务
		 *
		 * @return 此连接, 用于连接
		 */
		@Contract(pure = true) public abstract Response restart();

		/**
		 * 清理未完成的存储文件,如果文件下载完成则不做处理
		 *
		 * @return 执行状态
		 */
		@Contract(pure = true) public abstract boolean clear();

		/**
		 * 删除当前下载的本地存储文件,无论是否完成
		 *
		 * @return 执行状态
		 */
		@Contract(pure = true) public abstract boolean delete();
	}

	protected static class HttpConnection extends Connection {

		protected String url; // 请求URL
		protected String lastHash; // hash值,md5算法,用于判断服务器文件损坏
		protected String fileName; // 文件名
		protected String hash; // hash值,md5算法
		protected String SESSION_SUFFIX = ".session";

		protected long fileSize; // 文件大小

		protected int DEFAULT_BUFFER_SIZE = 8192;
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected int retry; // 请求异常重试次数
		protected int MAX_THREADS = 10; // 默认10线程下载

		protected long PIECE_MAX_SIZE = 1048576; // 默认块大小，1M
		protected boolean unlimitedRetry;// 请求异常无限重试
		protected boolean errorExit; // 错误退出
		protected Proxy proxy = Proxy.NO_PROXY; // 代理
		protected File storage; // 本地存储文件
		protected File session; // 配置信息文件
		protected List<Integer> retryStatusCodes = new ArrayList<>();

		protected Map<String, String> headers = new HashMap<>(); // headers
		protected Map<String, String> cookies = new HashMap<>(); // cookies

		protected ExecutorService executorService; // 下载线程池
		protected Method method = Method.MULTITHREAD;// 下载模式

		protected Request request = new Request();
		protected JSONObject fileInfo = new JSONObject();
		protected Thread abnormal;
		protected long MAX_COMPLETED;
		protected AtomicLong schedule = new AtomicLong(0);
		protected Map<Long, Long> status = new ConcurrentHashMap<>();

		protected HttpConnection() {
		}

		@Contract(pure = true) public Connection url(@NotNull String url) {
			request.setHash(this.hash = null);
			this.fileName = null;
			method = method == Method.FILE ? Method.MULTITHREAD : method;
			return alterUrl(url);
		}

		@Contract(pure = true) public Connection alterUrl(@NotNull String url) {
			fileInfo.put("url", url);
			request.setUrl(this.url = url);
			return this;
		}

		@Contract(pure = true) protected Connection session(@NotNull File session) {
			if (!session.getName().endsWith(SESSION_SUFFIX)) {
				throw new RuntimeException("Not is session file: " + session);
			} else if (session.isFile()) { // 如果设置配置文件下载，并且配置文件存在，获取信息
				fileInfo.putAll(JSONObject.parseObject(ReadWriteUtils.orgin(session).read()));
				url = fileInfo.getString("url");
				fileName = fileInfo.getString("fileName");
				if (Judge.isEmpty(url) || Judge.isEmpty(fileName)) {
					throw new RuntimeException("Info is error -> " + session);
				}
				request.setHash(hash = fileInfo.getString("md5"));
				fileSize = fileInfo.getLong("fileSize");
				headers = StringUtils.jsonToMap(fileInfo.getString("header"));
				cookies = StringUtils.jsonToMap(fileInfo.getString("cookie"));
			} else { // 配置文件不存在，抛出异常
				throw new RuntimeException("Not found or not is file " + session);
			}
			this.method = Method.FILE;
			this.session = session;
			return this;
		}

		@Contract(pure = true) public Connection userAgent(@NotNull String userAgent) {
			return header("user-agent", userAgent);
		}

		@Contract(pure = true) public Connection referrer(@NotNull String referrer) {
			return header("referer", referrer);
		}

		@Contract(pure = true) public Connection header(@NotNull String name, @NotNull String value) {
			headers.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			this.headers.putAll(headers);
			return this;
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			cookies.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			this.cookies.putAll(cookies);
			return this;
		}

		@Contract(pure = true) public Connection auth(@NotNull String auth) {
			return header("authorization", auth.startsWith("Bearer ") ? auth : "Bearer " + auth);
		}

		@Contract(pure = true) public Connection multithread(int nThread) {
			if (nThread < 1) {
				throw new RuntimeException("thread Less than 1");
			}
			this.MAX_THREADS = nThread;
			return this;
		}

		@Contract(pure = true) public Connection fileSize(long fileSize) {
			this.fileSize = fileSize;
			return this;
		}

		@Contract(pure = true) public Connection method(@NotNull Method method) {
			this.method = method;
			return this;
		}

		@Contract(pure = true) public Connection fileName(@NotNull String fileName) {
			this.fileName = FilesUtils.illegalFileName(StringUtils.decodeByURL(fileName));
			fileNameValidity(fileName);
			return this;
		}

		@Contract(pure = true) public Connection socks(@NotNull String host, int port) {
			return proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port)));
		}

		@Contract(pure = true) public Connection proxy(@NotNull String host, int port) {
			return proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
		}

		@Contract(pure = true) public Connection proxy(@NotNull Proxy proxy) {
			this.proxy = proxy;
			return this;
		}

		@Contract(pure = true) public Connection errorExit(boolean errorExit) {
			this.errorExit = errorExit;
			return this;
		}

		@Contract(pure = true) public Connection retry(int retry) {
			this.retry = retry;
			return this;
		}

		@Contract(pure = true) public Connection retry(int retry, int millis) {
			this.retry = retry;
			this.MILLISECONDS_SLEEP = millis;
			return this;
		}

		@Contract(pure = true) public Connection retry(boolean unlimitedRetry) {
			this.unlimitedRetry = unlimitedRetry;
			return this;
		}

		@Contract(pure = true) public Connection retry(boolean unlimitedRetry, int millis) {
			this.unlimitedRetry = unlimitedRetry;
			this.MILLISECONDS_SLEEP = millis;
			return this;
		}

		@Contract(pure = true) public Connection retryStatusCodes(int... statusCode) {
			this.retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
			return this;
		}

		@Contract(pure = true) public Connection retryStatusCodes(List<Integer> retryStatusCodes) {
			this.retryStatusCodes = retryStatusCodes;
			return this;
		}

		@Contract(pure = true) public Connection bufferSize(int bufferSize) {
			this.DEFAULT_BUFFER_SIZE = bufferSize;
			return this;
		}

		@Contract(pure = true) public Connection hash(@NotNull String hash) {
			request.setHash(this.hash = hash);
			return this;
		}

		@Contract(pure = true) public Connection pieceSize(long kb) {
			PIECE_MAX_SIZE = kb * 1024;
			return this;
		}

		/**
		 * 受于系统限制,对文件名长度进行效验,防止致命错误
		 *
		 * @param fileName 文件名
		 */
		@Contract(pure = true) public void fileNameValidity(@NotNull String fileName) {
			if (FilesUtils.nameLength(fileName) > 240) {
				throw new RuntimeException("Error: File name length is greater than 240 URL: " + url + " FileName: " + fileName);
			}
		}

		@Contract(pure = true) public Response upload(@NotNull String filePath) {
			return upload(new File(filePath));
		}

		@Contract(pure = true) public Response upload(@NotNull File file) {
			request.setStorage(file).setFileSize(file.length()).setHash(FilesUtils.getMD5(file));
			org.haic.often.Network.Response res;
			try (InputStream in = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE)) {
				res = JsoupUtil.connect(url).proxy(proxy).headers(headers).cookies(cookies).file(Judge.isEmpty(fileName) ? file.getName() : fileName, in)
						.retry(retry, MILLISECONDS_SLEEP).retry(unlimitedRetry).errorExit(errorExit).method(org.haic.often.Network.Method.POST).execute();
				request.headers(res.headers()).cookies(res.cookies());
			} catch (IOException e) {
				return new HttpResponse(this, request.statusCode(HttpStatus.SC_REQUEST_TIMEOUT));
			}
			return new HttpResponse(this, request.statusCode(res.statusCode()).setBody(res.body()));
		}

		@Contract(pure = true) public Response download() {
			return download(FilesUtils.getDownloadsPath());
		}

		@Contract(pure = true) public Response download(@NotNull String folderPath) {
			return download(new File(folderPath));
		}

		@Contract(pure = true) public Response download(@NotNull File folder) {
			return download(folder, method);
		}

		@Contract(pure = true) protected Response download(@NotNull File folder, @NotNull Method method) {
			initializationStatus(); // 初始化
			org.haic.often.Network.Response res = null;
			switch (method) { // 配置信息
			case FILE -> {
				method = Method.valueOf(fileInfo.getString("method"));
				MAX_THREADS = fileInfo.getInteger("threads");
				storage = new File(folder, fileName); // 获取其file对象
				JSONObject renew = fileInfo.getJSONObject("renew");
				if (storage.exists() && !Judge.isNull(renew)) {
					schedule.set(MAX_COMPLETED = renew.getLong("completed"));
					String statusJson = renew.getString("status");
					if (!Judge.isNull(statusJson)) {
						status.putAll(StringUtils.jsonToMap(renew.getString("status")));
					}
				}
				fileInfo.remove("renew");
			}
			case FULL, PIECE, MULTITHREAD, MANDATORY -> {    // 获取文件信息
				res = JsoupUtil.connect(url).proxy(proxy).headers(headers).cookies(cookies).retry(retry, MILLISECONDS_SLEEP).retry(unlimitedRetry)
						.retryStatusCodes(retryStatusCodes).errorExit(errorExit).execute();
				// 获取URL连接状态
				int statusCode = res.statusCode();
				if (!URIUtils.statusIsOK(statusCode)) {
					return new HttpResponse(this, request.statusCode(statusCode));
				}
				request.headers(res.headers()).cookies(res.cookies());
				// 获取文件名
				if (Judge.isEmpty(fileName)) {
					String disposition = res.header("content-disposition");
					if (Judge.isEmpty(disposition)) {
						fileName = url.substring(url.lastIndexOf("/") + 1);
						fileName = StringUtils.decodeByURL(url.contains("?") ? url.substring(0, url.indexOf("?")) : fileName);
						fileName = fileName.contains(".") ? fileName : fileName + MimeType.getMimeSuffix(res.header("content-type")); // 尝试修复后缀
					} else {
						fileName = URIUtils.getFileNameForDisposition(disposition);
					}
					fileName = FilesUtils.illegalFileName(fileName); // 文件名排除非法字符
					fileNameValidity(fileName);
				}

				// 获取待下载文件和配置文件对象
				request.setStorage(storage = new File(folder, fileName)); // 获取其file对象
				session = new File(storage + SESSION_SUFFIX); // 配置信息文件后缀

				if (session.exists()) { // 转为会话配置
					session(session);
					return download(folder, Method.FILE);
				} else if (storage.exists()) { // 文件已存在,返回完成
					return new HttpResponse(this, request.statusCode(HttpStatus.SC_OK));
				}

				String contentLength = res.header("content-length"); // 获取文件大小
				request.setFileSize(fileSize = Judge.isNull(contentLength) ? fileSize : Long.parseLong(contentLength));
				method = Judge.isEmpty(fileSize) ? Method.FULL : method;// 如果文件大小获取失败或线程为1，使用全量下载模式
				request.setHash(hash = Judge.isEmpty(hash) ? URIUtils.getMd5(request.headers()) : hash); // 获取文件md5
				// 创建并写入文件配置信息
				fileInfo.put("fileName", fileName);
				fileInfo.put("fileSize", fileSize);
				fileInfo.put("md5", hash);
				fileInfo.put("threads", MAX_THREADS);
				fileInfo.put("method", method.name());
				fileInfo.put("header", JSONObject.toJSONString(headers));
				fileInfo.put("cookie", JSONObject.toJSONString(cookies));
				ReadWriteUtils.orgin(session).write(fileInfo.toJSONString());
			}
			default -> throw new RuntimeException("Unknown mode");
			}

			Runtime.getRuntime().addShutdownHook(abnormal = new Thread(() -> { // 异常退出时写入断点续传配置
				JSONObject renew = new JSONObject();
				renew.put("completed", MAX_COMPLETED);
				renew.put("status", status);
				ReadWriteUtils.orgin(session).append(false).write(fileInfo.fluentPut("renew", renew).toJSONString());
			}));
			FilesUtils.createFolder(folder); // 创建文件夹
			int statusCode;
			switch (method) {  // 开始下载
			case FULL -> statusCode = Judge.isNull(res) ? FULL() : FULL(res);
			case PIECE -> statusCode = MULTITHREAD((int) Math.ceil((double) fileSize / (double) PIECE_MAX_SIZE), PIECE_MAX_SIZE);
			case MULTITHREAD -> {
				int PIECE_COUNT = Math.min((int) Math.ceil((double) fileSize / (double) PIECE_MAX_SIZE), MAX_THREADS);
				statusCode = MULTITHREAD(PIECE_COUNT, (long) Math.ceil((double) fileSize / (double) PIECE_COUNT));
			}
			case MANDATORY -> statusCode = MULTITHREAD(MAX_THREADS, (long) Math.ceil((double) fileSize / (double) MAX_THREADS));
			default -> throw new RuntimeException("Unknown mode");
			}
			if (!URIUtils.statusIsOK(statusCode)) { // 验证下载状态
				if (errorExit) {
					throw new RuntimeException("文件下载失败，状态码: " + statusCode + " URL: " + url);
				}
				return new HttpResponse(this, request.statusCode(statusCode));
			}
			Runtime.getRuntime().removeShutdownHook(abnormal);

			// 效验文件完整性
			String md5;
			if (!Judge.isEmpty(hash) && !(md5 = FilesUtils.getMD5(storage)).equals(hash)) {
				storage.delete(); // 删除下载错误的文件
				ReadWriteUtils.orgin(session).append(false).write(fileInfo.toJSONString()); // 重置信息文件
				String errorText;
				if (unlimitedRetry) {
					if (md5.equals(lastHash)) {
						errorText = "Server file is corrupt";
					} else {
						lastHash = md5;
						return download(folder, Method.FILE);
					}
				} else {
					errorText = "File verification is not accurate";
				}
				if (errorExit) {
					throw new RuntimeException(errorText + ", Server md5:" + hash + " Local md5: " + md5 + " URL: " + url);
				} else {
					return new HttpResponse(this, request.statusCode(HttpStatus.SC_SERVER_RESOURCE_ERROR));
				}
			}

			session.delete(); // 删除会话信息文件
			return new HttpResponse(this, request.statusCode(HttpStatus.SC_OK));
		}

		/**
		 * 初始化下载进度
		 */
		@Contract(pure = true) protected void initializationStatus() {
			schedule.set(0);
			MAX_COMPLETED = 0;
			status.clear();
		}

		/**
		 * 全量下载，下载获取文件信息并写入文件
		 *
		 * @return 下载并写入是否成功(状态码)
		 */
		@Contract(pure = true) protected int FULL() {
			return FULL(JsoupUtil.connect(url).proxy(proxy).headers(headers).header("range", "bytes=" + MAX_COMPLETED + "-").cookies(cookies)
					.retry(retry, MILLISECONDS_SLEEP).retry(unlimitedRetry).retryStatusCodes(retryStatusCodes).errorExit(errorExit).execute());
		}

		/**
		 * 全量下载，下载获取文件信息并写入文件
		 *
		 * @param response 网页Response对象
		 * @return 下载并写入是否成功(状态码)
		 */
		@Contract(pure = true) protected int FULL(org.haic.often.Network.Response response) {
			try (InputStream in = response.bodyStream(); RandomAccessFile out = new RandomAccessFile(storage, "rw")) {
				out.seek(MAX_COMPLETED);
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				for (int len; (len = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) > -1; MAX_COMPLETED = schedule.addAndGet(len)) {
					out.write(buffer, 0, len);
				}
				if (fileSize == 0 || fileSize == MAX_COMPLETED) {
					return HttpStatus.SC_OK;
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
			return HttpStatus.SC_REQUEST_TIMEOUT;
		}

		@Contract(pure = true) protected int MULTITHREAD(int PIECE_COUNT, long PIECE_SIZE) {
			AtomicInteger statusCodes = new AtomicInteger(HttpStatus.SC_OK);
			executorService = Executors.newFixedThreadPool(MAX_THREADS); // 限制多线程;
			AtomicBoolean addCompleted = new AtomicBoolean(true);
			for (long i = MAX_COMPLETED / PIECE_SIZE; i < PIECE_COUNT; i++) {//PIECE_COUNT
				executorService.execute(new ParameterizedThread<>(i, (index) -> { // 执行多线程程
					long start = index * PIECE_SIZE;
					long end = (index + 1 == PIECE_COUNT ? fileSize : (index + 1) * PIECE_SIZE) - 1;
					long flip = status.getOrDefault(start, start);
					schedule.addAndGet(flip - start);
					int statusCode = flip == end ? HttpStatus.SC_PARTIAL_CONTENT : addPiece(start, flip, end);
					if (addCompleted.get() && end > MAX_COMPLETED) {
						addCompleted.set(false);
						long completed;
						while ((completed = status.getOrDefault(MAX_COMPLETED, MAX_COMPLETED)) == MAX_COMPLETED + PIECE_SIZE) {
							status.remove(MAX_COMPLETED);
							MAX_COMPLETED = completed;
						}
						addCompleted.set(true);
					}
					if (!URIUtils.statusIsOK(statusCode)) {
						statusCodes.set(statusCode);
						executorService.shutdownNow(); // 结束未开始的线程，并关闭线程池
					}
				}));
			}
			MultiThreadUtils.WaitForEnd(executorService); // 等待线程结束
			return statusCodes.get();
		}

		/**
		 * 添加区块线程
		 *
		 * @param start 起始位
		 * @param flip  断点位置,用于修正
		 * @param end   结束位
		 * @return 状态码
		 */
		@Contract(pure = true) protected int addPiece(long start, long flip, long end) {
			int statusCode = writePiece(start, flip, end);
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
				statusCode = writePiece(start, flip, end);
			}
			return statusCode;
		}

		/**
		 * 分块下载，下载获取文件区块信息并写入文件
		 *
		 * @param start 块起始位
		 * @param flip  断点位置,用于修正
		 * @param end   块结束位
		 * @return 下载并写入是否成功(状态码)
		 */
		@Contract(pure = true) protected int writePiece(long start, long flip, long end) {
			org.haic.often.Network.Response piece = JsoupUtil.connect(url).proxy(proxy).headers(headers).header("range", "bytes=" + flip + "-" + end)
					.cookies(cookies).execute();
			return URIUtils.statusIsOK(piece.statusCode()) ? writePiece(start, flip, end, piece) : piece.statusCode();
		}

		/**
		 * 下载获取文件区块信息并写入文件
		 *
		 * @param start 块起始位
		 * @param flip  断点位置,用于修正
		 * @param end   块结束位
		 * @param piece 块Response对象
		 * @return 下载并写入是否成功(状态码)
		 */
		@Contract(pure = true) protected int writePiece(long start, long flip, long end, org.haic.often.Network.Response piece) {
			try (InputStream inputStream = piece.bodyStream(); RandomAccessFile output = new RandomAccessFile(storage, "rw")) {
				output.seek(flip);
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				long count = 0;
				for (int len; !Judge.isMinusOne(len = inputStream.read(buffer)); count += len, status.put(start, flip + count), schedule.addAndGet(len)) {
					output.write(buffer, 0, len);
				}
				if (end - flip + 1 == count) {
					return HttpStatus.SC_PARTIAL_CONTENT;
				}

			} catch (IOException e) {
				// e.printStackTrace();
			}
			return HttpStatus.SC_REQUEST_TIMEOUT;
		}

	}

	/**
	 * Connection 接口是一个方便的 HTTP 客户端和会话对象，用于从 Web 上传下载文件。
	 * <p>
	 * 使用的“连接”并不意味着在连接对象的生命周期内与服务器保持长期连接.
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/8/18 00:44
	 */
	public abstract static class Connection {

		/**
		 * 设置新的要下载文件的 URL，协议必须是 HTTP 或 HTTPS
		 * <p>
		 * 在修改 URL 时，同步置空 fileName 和 hash 而不会修改其它参数，适用于相同域名或来源的下载
		 *
		 * @param url 要连接的 URL
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection url(@NotNull String url);

		/**
		 * 设置要下载文件的 URL，协议必须是 HTTP 或 HTTPS
		 * <p>
		 * 此方法仅用于初始化设置或特殊情况下修改同一文件的 URL
		 * <p>
		 * 不同的下载链接不应复用类，内部被改变的 fileName 和 hash 参数会导致致命的下载错误，如果复用，应该同步修改或置空上述两个参数，建议使用 url() 方法
		 *
		 * @param url 要连接的 URL
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection alterUrl(@NotNull String url);

		/**
		 * 连接用户代理（ 字符串 用户代理）<br/> 设置请求用户代理标头
		 *
		 * @param userAgent 要使用的用户代理
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection userAgent(@NotNull String userAgent);

		/**
		 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/> 设置请求引荐来源网址（又名“引荐来源网址”）标头
		 *
		 * @param referrer 要使用的来源网址
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection referrer(@NotNull String referrer);

		/**
		 * 连接头（ 字符串 名称， 字符串 值）<br/> 设置请求标头
		 *
		 * @param name  标题名称
		 * @param value 标头值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection header(@NotNull String name, @NotNull String value);

		/**
		 * 连接头（ Map  < String  , String  > 头）<br/> 将每个提供的标头添加到请求中
		 *
		 * @param headers 标头名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection headers(@NotNull Map<String, String> headers);

		/**
		 * 设置要在请求中发送的 cookie
		 *
		 * @param name  cookie 的名称
		 * @param value cookie 的值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection cookie(@NotNull String name, @NotNull String value);

		/**
		 * 连接 cookies （ Map < String  , String  >cookies）<br/> 将每个提供的 cookie 添加到请求中
		 *
		 * @param cookies 名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection cookies(@NotNull Map<String, String> cookies);

		/**
		 * 设置授权码或身份识别标识<br/> 有些服务器不使用cookie验证身份,使用authorization进行验证<br/> 一般信息在cookie或local Storage中存储
		 *
		 * @param auth 授权码或身份识别标识
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection auth(@NotNull String auth);

		/**
		 * 设置多线程下载，线程数不小于1，否则抛出异常
		 *
		 * @param nThread 线程最大值,非零或负数
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection multithread(int nThread);

		/**
		 * 设置文件大小, 请保证大小正确, 仅在多线程模式并且无法通过请求头获取文件大小时使用
		 *
		 * @param fileSize file size
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection fileSize(long fileSize);

		/**
		 * 设置文件的下载模式
		 *
		 * @param method 下载模式
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection method(@NotNull Method method);

		/**
		 * 设置将要下载文件的文件名
		 * <p>
		 * 文件名字符串字符长度不能高于240,使用 FilesUtils.nameLength 获取实际字符长度
		 *
		 * @param fileName 文件名
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection fileName(@NotNull String fileName);

		/**
		 * 设置用于此请求的 SOCKS 代理
		 *
		 * @param host 代理主机名
		 * @param port 代理端口
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection socks(@NotNull String host, int port);

		/**
		 * 设置用于此请求的 HTTP 代理
		 *
		 * @param host 代理主机名
		 * @param port 代理端口
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection proxy(@NotNull String host, int port);

		/**
		 * 连接代理（ @NotNull  Proxy 代理）<br/> 设置用于此请求的代理
		 *
		 * @param proxy 要使用的代理
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection proxy(@NotNull Proxy proxy);

		/**
		 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/> 默认情况下为false
		 *
		 * @param errorExit 启用错误退出
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection errorExit(boolean errorExit);

		/**
		 * 设置 重试次数
		 *
		 * @param retry 重试次数
		 * @return this
		 */
		@Contract(pure = true) public abstract Connection retry(int retry);

		/**
		 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
		 *
		 * @param retry  重试次数
		 * @param millis 重试等待时间(毫秒)
		 * @return this
		 */
		@Contract(pure = true) public abstract Connection retry(int retry, int millis);

		/**
		 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
		 *
		 * @param unlimit 启用无限重试, 默认false
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection retry(boolean unlimit);

		/**
		 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
		 *
		 * @param unlimit 启用无限重试, 默认false
		 * @param millis  重试等待时间(毫秒)
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection retry(boolean unlimit, int millis);

		/**
		 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
		 *
		 * @param statusCode 状态码
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection retryStatusCodes(int... statusCode);

		/**
		 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
		 *
		 * @param retryStatusCodes 状态码列表
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection retryStatusCodes(List<Integer> retryStatusCodes);

		/**
		 * 设置写入文件时缓冲区大小,默认大小为8192字节
		 *
		 * @param bufferSize 缓冲区大小
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection bufferSize(int bufferSize);

		/**
		 * 设置md5效验值进行文件完整性效验<br/> 如果效验不正确会在下载完成后删除文件并重置配置文件<br/> 抛出异常信息
		 *
		 * @param hash 文件md5值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection hash(@NotNull String hash);

		/**
		 * 设置在多线程模式下载时,分块最大大小
		 *
		 * @param kb 指定块大小(KB)
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection pieceSize(long kb);

		/**
		 * 上传网络文件,返回状态码
		 *
		 * @param filePath 待上传的文件路径
		 * @return 上传状态码
		 */
		@Contract(pure = true) public abstract Response upload(@NotNull String filePath);

		/**
		 * 上传网络文件,返回状态码
		 *
		 * @param file 待上传的文件对象
		 * @return 上传状态码
		 */
		@Contract(pure = true) public abstract Response upload(@NotNull File file);

		/**
		 * 下载网络文件,返回状态码
		 * <p>
		 * 自动寻找并存放下载文件夹路径
		 *
		 * @return 下载状态码
		 */
		@Contract(pure = true) public abstract Response download();

		/**
		 * 下载网络文件,返回状态码
		 *
		 * @param folderPath 文件存放目录路径
		 * @return 下载状态码
		 */
		@Contract(pure = true) public abstract Response download(@NotNull String folderPath);

		/**
		 * 下载网络文件,返回状态码
		 *
		 * @param folder 文件存放目录对象
		 * @return 下载状态码
		 */
		@Contract(pure = true) public abstract Response download(File folder);

	}

	private static class Request {
		private String url; // 请求URL
		private String hash; // hash值,md5算法
		private String body;

		private long fileSize; // 文件大小
		private int statusCode;

		private File storage;

		private Map<String, String> headers = new HashMap<>(); // headers
		private Map<String, String> cookies = new HashMap<>(); // cookies

		public int statusCode() {
			return statusCode;
		}

		public Request statusCode(int statusCode) {
			this.statusCode = statusCode;
			return this;
		}

		public String getUrl() {
			return url;
		}

		public Request setUrl(String url) {
			this.url = url;
			return this;
		}

		public File getStorage() {
			return storage;
		}

		public Request setStorage(File storage) {
			this.storage = storage;
			return this;
		}

		public String getHash() {
			return hash;
		}

		public Request setHash(String hash) {
			this.hash = hash;
			return this;
		}

		public long getFileSize() {
			return fileSize;
		}

		public Request setFileSize(long fileSize) {
			this.fileSize = fileSize;
			return this;
		}

		public Map<String, String> headers() {
			return headers;
		}

		public Request headers(Map<String, String> headers) {
			this.headers = headers;
			return this;
		}

		public Map<String, String> cookies() {
			return cookies;
		}

		public Request cookies(Map<String, String> cookies) {
			this.cookies = cookies;
			return this;
		}

		public String getBody() {
			return body;
		}

		public Request setBody(String body) {
			this.body = body;
			return this;
		}
	}

}