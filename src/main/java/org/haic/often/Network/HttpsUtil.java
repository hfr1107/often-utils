package org.haic.often.Network;

import org.brotli.dec.BrotliInputStream;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Https 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/3/9 14:26
 */
public class HttpsUtil {

	protected HttpsUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		return new HttpConnection(url);
	}

	/**
	 * 公共静态连接newSession ()
	 * <p>
	 * 创建一个新Connection以用作会话。将为会话维护连接设置（用户代理、超时、URL 等）和 cookie
	 *
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection newSession() {
		return new HttpConnection("");
	}

	protected static class HttpConnection extends Connection {

		protected String url; // URL
		protected String auth; // 身份识别标识
		protected String params = ""; // 请求参数
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected int timeout; // 连接超时时间
		protected boolean unlimit;// 请求异常无限重试
		protected boolean errorExit; // 错误退出
		protected boolean followRedirects = true; // 重定向
		protected Proxy proxy = Proxy.NO_PROXY; // 代理
		protected Method method = Method.GET;
		protected Parser parser = Parser.htmlParser();
		protected Map<String, String> headers = new HashMap<>(); // 请求头
		protected Map<String, String> cookies = new HashMap<>(); // cookies
		protected List<Integer> retryStatusCodes = new ArrayList<>();
		protected ThreeTuple<String, InputStream, String> file;
		protected SSLSocketFactory sslSocketFactory = IgnoreSSLSocket.MyX509TrustManager().getSocketFactory();

		protected HttpConnection(@NotNull String url) {
			System.setProperty("http.keepAlive", "false"); // 关闭长连接复用,防止流阻塞
			initialization(url);
		}

		@Contract(pure = true) protected Connection initialization(@NotNull String url) {
			header("accept", "text/html, application/xhtml+xml, application/json;q=0.9, */*;q=0.8");
			header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
			header("accept-encoding", "gzip, deflate, br"); // 允许压缩gzip,br-Brotli
			header("user-agent", UserAgent.chrome()); // 设置随机请求头;
			return url(url);
		}

		@Contract(pure = true) public Connection url(@NotNull String url) {
			this.url = url;
			return this;
		}

		@Contract(pure = true) public Connection newRequest() {
			params = "";
			headers = new HashMap<>();
			method = Method.GET;
			initialization("");
			return Judge.isEmpty(auth) ? this : auth(auth);
		}

		@Contract(pure = true) public Connection sslSocketFactory(SSLContext sslSocket) {
			sslSocketFactory = sslSocket.getSocketFactory();
			return this;
		}

		@Contract(pure = true) public Connection userAgent(@NotNull String userAgent) {
			return header("user-agent", userAgent);
		}

		@Contract(pure = true) public Connection isPhone(boolean isPhone) {
			return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
		}

		@Contract(pure = true) public Connection followRedirects(boolean followRedirects) {
			this.followRedirects = followRedirects;
			return this;
		}

		@Contract(pure = true) public Connection referrer(@NotNull String referrer) {
			return header("referer", referrer);
		}

		@Contract(pure = true) public Connection auth(@NotNull String auth) {
			return header("authorization", (this.auth = auth.contains(" ") ? auth : "Bearer " + auth));
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			this.timeout = millis;
			return this;
		}

		@Contract(pure = true) public Connection parser(@NotNull Parser parser) {
			this.parser = parser;
			return this;
		}

		@Contract(pure = true) public Connection contentType(@NotNull String type) {
			return header("content-type", type);
		}

		@Contract(pure = true) public Connection header(@NotNull String name, @NotNull String value) {
			this.headers.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			this.headers.putAll(headers);
			return this;
		}

		@Contract(pure = true) public Connection setHeaders(@NotNull Map<String, String> headers) {
			this.headers = new HashMap<>();
			return headers(headers);
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			cookies.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			this.cookies.putAll(cookies);
			return this;
		}

		@Contract(pure = true) public Connection setCookies(@NotNull Map<String, String> cookies) {
			this.cookies = new HashMap<>();
			return cookies(cookies);
		}

		@Contract(pure = true) public Map<String, String> cookieStore() {
			return cookies;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			params += Judge.isEmpty(params) ? key + "=" + value : "&" + key + "=" + value;
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			this.params = params.entrySet().stream().map(l -> l.getKey() + "=" + l.getValue()).collect(Collectors.joining("&"));
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
			ThreeTuple<String, String, String> fromData = URIUtils.getFormData(key, fileName);
			file = Tuple.of(fromData.second, inputStream, fromData.third);
			return contentType("multipart/form-data; boundary=" + fromData.first);
		}

		@Contract(pure = true) public Connection file(@NotNull String fileName, @NotNull InputStream inputStream) {
			return data("file", fileName, inputStream);
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String body) {
			params = body;
			return URIUtils.isJson(body) ? contentType("application/json;charset=UTF-8") : contentType("application/x-www-form-urlencoded;charset=UTF-8");
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

		@Contract(pure = true) public Connection method(@NotNull Method method) {
			this.method = method;
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

		@Contract(pure = true) public Connection retry(boolean unlimit) {
			this.unlimit = unlimit;
			return this;
		}

		@Contract(pure = true) public Connection retry(boolean unlimit, int millis) {
			this.unlimit = unlimit;
			this.MILLISECONDS_SLEEP = millis;
			return this;
		}

		@Contract(pure = true) public Connection retryStatusCodes(int... statusCode) {
			retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
			return this;
		}

		@Contract(pure = true) public Connection retryStatusCodes(List<Integer> retryStatusCodes) {
			this.retryStatusCodes = retryStatusCodes;
			return this;
		}

		@Contract(pure = true) public Connection errorExit(boolean errorExit) {
			this.errorExit = errorExit;
			return this;
		}

		@Contract(pure = true) public Document get() {
			return method(Method.GET).execute().parse();
		}

		@Contract(pure = true) public Document post() {
			return method(Method.POST).execute().parse();
		}

		@NotNull @Contract(pure = true) public Response execute() {
			Response response = executeProgram(url);
			int statusCode = response.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimit); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
				response = executeProgram(url);
				statusCode = response.statusCode();
			}
			if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
				throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + url);
			}
			return response;
		}

		/**
		 * 主程序
		 *
		 * @return this
		 */
		@NotNull @Contract(pure = true) protected Response executeProgram(@NotNull String url) {
			try {
				HttpURLConnection conn = null;
				Response res = new HttpResponse(this, null);
				switch (method) {
				case GET -> {
					conn = connection(Judge.isEmpty(params) ? url : url + (url.contains("?") ? "&" : "?") + params);
					conn.connect();
				}
				case OPTIONS, DELETE, HEAD, TRACE -> {
					conn = connection(url);
					conn.connect();
				}
				case POST, PUT, PATCH -> {
					conn = connection(url);
					// 发送POST请求必须设置如下
					conn.setUseCaches(false); // POST请求不能使用缓存（POST不能被缓存）
					conn.setDoOutput(true); // 设置是否向HttpUrlConnction输出，因为这个是POST请求，参数要放在http正文内，因此需要设为true，默认情况下是false
					conn.setDoInput(true); // 设置是否向HttpUrlConnection读入，默认情况下是true
					try (DataOutputStream output = new DataOutputStream(conn.getOutputStream())) {
						if (!Judge.isEmpty(params)) {
							output.writeBytes(params); // 发送请求参数
						}
						if (!Judge.isNull(file)) { // 发送文件
							output.writeBytes(file.first);
							file.second.transferTo(output);
							output.writeBytes(file.third);
						}
						output.flush(); // flush输出流的缓冲
					} catch (IOException e) {
						conn.disconnect();
						return res;
					}
				}
				}
				conn.disconnect();
				res = new HttpResponse(this, conn);
				cookies(res.cookies()); // 维护cookies

				String redirectUrl; // 修复重定向
				if (followRedirects && URIUtils.statusIsNormal(res.statusCode()) && !Judge.isEmpty(redirectUrl = res.header("location"))) {
					res = executeProgram(redirectUrl);
				}
				return res;
			} catch (IOException e) {
				return new HttpResponse(this, null);
			}
		}

		/**
		 * 创建HttpURLConnection实例
		 *
		 * @param url url链接
		 * @return HttpURLConnection实例
		 * @throws IOException 如果发生 I/O 异常
		 */
		protected HttpURLConnection connection(@NotNull String url) throws IOException {
			HttpURLConnection conn = (HttpURLConnection) URIUtils.getURL(url).openConnection(proxy);
			conn.setRequestProperty("connection", "close");
			conn.setRequestMethod(method.name()); // 请求方法
			conn.setReadTimeout(timeout); // 设置超时
			conn.setInstanceFollowRedirects(followRedirects); // 重定向,http和https之间无法遵守重定向

			// https 忽略证书验证
			if (url.startsWith("https")) { // 在握手期间，如果 URL 的主机名和服务器的标识主机名不匹配，则验证机制可以回调此接口的实现程序来确定是否应该允许此连接。
				((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
				((HttpsURLConnection) conn).setHostnameVerifier((arg0, arg1) -> true);
			}

			// 设置通用的请求属性
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}

			// 设置cookies
			conn.setRequestProperty("cookie", cookies.entrySet().stream().map(l -> l.getKey() + "=" + l.getValue()).collect(Collectors.joining("; ")));

			return conn;
		}

	}

	/**
	 * 响应接口
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/3/16 10:28
	 */
	protected static class HttpResponse extends Response {
		protected HttpConnection connection;
		protected HttpURLConnection conn;// HttpURLConnection
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(HttpConnection connection, HttpURLConnection conn) {
			this.connection = connection;
			this.conn = conn;
		}

		@Contract(pure = true) public String url() {
			return conn.getURL().toExternalForm();
		}

		@Contract(pure = true) public int statusCode() {
			try {
				return conn.getResponseCode();
			} catch (IOException e) {
				return HttpStatus.SC_REQUEST_TIMEOUT;
			}
		}

		@Contract(pure = true) public String statusMessage() {
			try {
				return conn.getResponseMessage();
			} catch (IOException e) {
				return null;
			}
		}

		@Contract(pure = true) public String header(@NotNull String name) {
			String header = headers().get(name);
			if (Judge.isNull(header)) {
				return null;
			}
			header = header.startsWith("[") ? header.substring(1) : header;
			return header.endsWith("]") ? header.substring(0, header.length() - 1) : header;
		}

		@Contract(pure = true) public Map<String, String> headers() {
			Map<String, String> headers = conn.getHeaderFields().entrySet().stream()
					.collect(Collectors.toMap(l -> String.valueOf(l.getKey()).toLowerCase(), l -> l.getValue().toString(), (e1, e2) -> e2));
			headers.remove("null");
			return headers;
		}

		@Contract(pure = true) public Response header(@NotNull String key, @NotNull String value) {
			connection.header(key, value);
			return this;
		}

		@Contract(pure = true) public Response removeHeader(@NotNull String key) {
			connection.headers.remove(key);
			return this;
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return cookies().get(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
			return Judge.isNull(cookies) ?
					new HashMap<>() :
					cookies.stream().map(l -> l.substring(0, l.indexOf(";")))
							.collect(Collectors.toMap(l -> l.substring(0, l.indexOf("=")), l -> l.substring(l.indexOf("=") + 1), (e1, e2) -> e2));
		}

		@Contract(pure = true) public Response cookie(@NotNull String name, @NotNull String value) {
			connection.cookie(name, value);
			return this;
		}

		@Contract(pure = true) public Response removeCookie(@NotNull String name) {
			connection.cookies.remove(name);
			return this;
		}

		@Contract(pure = true) public Response charset(@NotNull String charsetName) {
			return charset(Charset.forName(charsetName));
		}

		@Contract(pure = true) public Response charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		@Contract(pure = true) public String contentType() {
			return conn.getContentType();
		}

		@Contract(pure = true) public Document parse() {
			return URIUtils.statusIsNormal(statusCode()) ? Jsoup.parse(body(), connection.parser) : null;
		}

		@Contract(pure = true) public String body() {
			String result;
			String encoding = header("content-encoding");
			try (InputStream in = bodyStream();
					InputStream body = Judge.isNull(encoding) ?
							in :
							encoding.equals("gzip") ?
									new GZIPInputStream(in) :
									encoding.equals("deflate") ?
											new InflaterInputStream(in, new Inflater(true)) :
											encoding.equals("br") ? new BrotliInputStream(in) : in) {
				result = StreamUtils.stream(body).charset(charset).getString();
			} catch (IOException e) {
				return null;
			}
			return result;
		}

		@Contract(pure = true) public InputStream bodyStream() throws IOException {
			return URIUtils.statusIsNormal(statusCode()) ? conn.getInputStream() : conn.getErrorStream();
		}

		@Contract(pure = true) public byte[] bodyAsBytes() {
			byte[] result;
			try (InputStream inputStream = bodyStream()) {
				result = StreamUtils.stream(inputStream).toByteArray();
			} catch (IOException e) {
				return null;
			}
			return result;
		}

		@Contract(pure = true) public Response method(@NotNull Method method) {
			connection.method(method);
			return this;
		}

	}

}
