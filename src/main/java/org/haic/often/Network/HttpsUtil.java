package org.haic.often.Network;

import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
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

/**
 * Https 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/3/9 14:26
 */
public class HttpsUtil {

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

	protected static class HttpConnection extends Connection {

		protected String url; // URL
		protected String params = ""; // 请求参数
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected int timeout; // 连接超时时间
		protected boolean unlimitedRetry;// 请求异常无限重试
		protected boolean errorExit; // 错误退出
		protected boolean followRedirects = true; // 重定向
		protected Proxy proxy = Proxy.NO_PROXY; // 代理
		protected Method method = Method.GET;
		protected Parser parser = Parser.htmlParser();
		protected Map<String, String> headers = new HashMap<>(); // 请求头
		protected Map<String, String> cookies = new HashMap<>(); // cookies
		protected List<Integer> retryStatusCodes = new ArrayList<>();
		protected ThreeTuple<String, InputStream, String> file;
		protected SSLSocketFactory sslSocketFactory;

		protected HttpConnection(@NotNull String url) {
			this.url = url;
			sslSocketFactory = IgnoreSSLSocket.MyX509TrustManager().getSocketFactory();
			header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
			header("user-agent", UserAgent.chrome()); // 设置随机请求头;
		}

		/**
		 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
		 *
		 * @param url 要连接的 URL
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public Connection url(@NotNull String url) {
			this.url = url;
			return this;
		}

		@Contract(pure = true) public Connection newRequest() {
			params = "";
			headers = new HashMap<>();
			method = Method.GET;
			return this;
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

		@Contract(pure = true) public Connection authorization(@NotNull String authorization) {
			return header("authorization", authorization.startsWith("Bearer ") ? authorization : "Bearer " + authorization);
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			this.timeout = millis;
			return this;
		}

		@Contract(pure = true) public Connection parser(@NotNull Parser parser) {
			this.parser = parser;
			return this;
		}

		@Contract(pure = true) public Connection header(@NotNull String name, @NotNull String value) {
			this.headers.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			this.headers = headers;
			return this;
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			cookies.put(name, value);
			return this;
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			this.cookies = cookies;
			return this;
		}

		@Contract(pure = true) public Map<String, String> cookieStore() {
			return cookies;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			params += Judge.isEmpty(params) ? key + "=" + value : "&" + key + "=" + value;
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			this.params = "";
			params.forEach(this::data);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
			ThreeTuple<String, String, String> fromData = URIUtils.getFormData(key, fileName);
			file = Tuple.of(fromData.second, inputStream, fromData.third);
			return header("content-type", "multipart/form-data; boundary=" + fromData.first);
		}

		@Contract(pure = true) public Connection file(@NotNull String fileName, @NotNull InputStream inputStream) {
			return data("file", fileName, inputStream);
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String requestBody) {
			params = requestBody;
			return URIUtils.isJson(requestBody) ?
					header("content-type", "application/json;charset=UTF-8") :
					header("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
		}

		@Contract(pure = true) public Connection socks(@NotNull String proxyHost, int proxyPort) {
			return proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort)));
		}

		@Contract(pure = true) public Connection proxy(@NotNull String proxyHost, int proxyPort) {
			return proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
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
			Response response = method(Method.GET).execute();
			return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body(), parser) : null;
		}

		@Contract(pure = true) public Document post() {
			method(Method.POST);
			Response response = execute();
			return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body(), parser) : null;
		}

		@Contract(pure = true) public Response execute() {
			Response response = executeProgram(url);
			int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
				response = executeProgram(url);
				statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
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
		@Contract(pure = true) protected Response executeProgram(@NotNull String url) {
			try {
				HttpURLConnection conn = null;
				switch (method) {
				case GET -> conn = connection(Judge.isEmpty(params) ? url : url + (url.contains("?") ? "&" : "?") + params);
				case OPTIONS, DELETE, HEAD, TRACE -> conn = connection(url);
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
						return null;
					}
				}
				}

				// 维护cookies
				Response response = new HttpResponse(conn);
				cookies.putAll(response.cookies());

				String redirectUrl; // 修复重定向
				if (followRedirects && URIUtils.statusIsRedirect(response.statusCode()) && !Judge.isEmpty(redirectUrl = conn.getHeaderField("location"))) {
					return executeProgram(redirectUrl);
				}
				return response;
			} catch (IOException e) {
				return null;
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
		protected HttpURLConnection conn; // HttpURLConnection对象
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(HttpURLConnection conn) {
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

		@Contract(pure = true) public String header(@NotNull String name) {
			String header = headers().get(name);
			header = header.startsWith("[") ? header.substring(1) : header;
			return header.endsWith("]") ? header.substring(0, header.length() - 1) : header;
		}

		@Contract(pure = true) public Map<String, String> headers() {
			Map<String, String> headers = conn.getHeaderFields().entrySet().stream()
					.collect(Collectors.toMap(l -> String.valueOf(l.getKey()).toLowerCase(), l -> l.getValue().toString()));
			headers.remove("null");
			return headers;
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

		@Contract(pure = true) public Response charset(@NotNull String charsetName) {
			return charset(Charset.forName(charsetName));
		}

		@Contract(pure = true) public Response charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		@Contract(pure = true) public String body() {
			String result;
			try (InputStream inputStream = bodyStream()) {
				result = StreamUtils.stream(inputStream).charset(charset).getString();
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

	}

}