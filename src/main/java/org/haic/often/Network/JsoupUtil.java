package org.haic.often.Network;

import org.brotli.dec.BrotliInputStream;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Jsoup 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/25 18:40
 */
public class JsoupUtil {

	protected JsoupUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		return new HttpConnection(Jsoup.connect(url));
	}

	/**
	 * 公共静态连接newSession ()
	 * <p>
	 * 创建一个新Connection以用作会话。将为会话维护连接设置（用户代理、超时、URL 等）和 cookie
	 *
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection newSession() {
		return new HttpConnection(Jsoup.newSession());
	}

	protected static class HttpConnection extends Connection {
		protected String auth; // 身份识别标识
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected boolean unlimit;// 请求异常无限重试
		protected boolean errorExit; // 错误退出

		protected List<Integer> retryStatusCodes = new ArrayList<>(); // 重试的错误状态码
		protected Parser parser = Parser.htmlParser();

		protected org.jsoup.Connection conn;

		protected HttpConnection(@NotNull org.jsoup.Connection conn) {
			initialization(conn);
		}

		@Contract(pure = true) protected Connection initialization(@NotNull org.jsoup.Connection conn) {
			this.conn = conn.maxBodySize(0).timeout(0).ignoreContentType(true).ignoreHttpErrors(true);
			header("accept", "text/html, application/xhtml+xml, application/json;q=0.9, */*;q=0.8");
			header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
			header("accept-encoding", "gzip, deflate, br"); // 允许压缩gzip,br-Brotli
			return header("user-agent", UserAgent.chrome()); // 设置随机请求头
		}

		@Contract(pure = true) public Connection url(@NotNull String url) {
			conn.url(url);
			return this;
		}

		@Contract(pure = true) public Connection newRequest() {
			initialization(conn.newRequest());
			return Judge.isEmpty(auth) ? this : auth(auth);
		}

		@Contract(pure = true) public Connection sslSocketFactory(SSLContext sslSocket) {
			conn.sslSocketFactory(sslSocket.getSocketFactory());
			return this;
		}

		@Contract(pure = true) public Connection userAgent(@NotNull String userAgent) {
			return header("user-agent", userAgent);
		}

		@Contract(pure = true) public Connection isPhone(boolean isPhone) {
			return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
		}

		@Contract(pure = true) public Connection request(@NotNull org.jsoup.Connection.Request request) {
			conn.request(request);
			return this;
		}

		@Contract(pure = true) public Connection followRedirects(boolean followRedirects) {
			conn.followRedirects(followRedirects);
			return this;
		}

		@Contract(pure = true) public Connection referrer(@NotNull String referrer) {
			return header("referer", referrer);
		}

		@Contract(pure = true) public Connection auth(@NotNull String auth) {
			return header("authorization", (this.auth = auth.contains(" ") ? auth : "Bearer " + auth));
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			conn.timeout(millis);
			return this;
		}

		@Contract(pure = true) public Connection maxBodySize(int bytes) {
			conn.maxBodySize(bytes);
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
			conn.header(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			conn.headers(headers);
			return this;
		}

		@Contract(pure = true) public Connection setHeaders(@NotNull Map<String, String> headers) {
			conn.request().headers().clear();
			conn.headers(headers);
			return this;
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			conn.cookie(name, value);
			return this;
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			conn.cookies(cookies);
			return this;
		}

		@Contract(pure = true) public Connection setCookies(@NotNull Map<String, String> cookies) {
			conn.request().cookies().clear();
			conn.cookies(cookies);
			return this;
		}

		@Contract(pure = true) public Map<String, String> cookieStore() {
			return conn.cookieStore().getCookies().stream().collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue, (e1, e2) -> e2));
		}

		@Contract(pure = true) public Connection data(String... keyvals) {
			conn.data(keyvals);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			conn.data(key, value);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			conn.request().data().clear();
			conn.data(params);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
			conn.data(key, fileName, inputStream);
			return this;
		}

		@Contract(pure = true) public Connection data(String key, String fileName, InputStream inputStream, String contentType) {
			conn.data(key, fileName, inputStream, contentType);
			return this;
		}

		@Contract(pure = true) public Connection file(@NotNull String fileName, @NotNull InputStream inputStream) {
			return data("file", fileName, inputStream);
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String body) {
			conn.requestBody(body);
			return URIUtils.isJson(body) ? contentType("application/json;charset=UTF-8") : contentType("application/x-www-form-urlencoded;charset=UTF-8");
		}

		@Contract(pure = true) public Connection socks(@NotNull String host, int port) {
			return proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port)));
		}

		@Contract(pure = true) public Connection proxy(@NotNull String host, int port) {
			return proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
		}

		@Contract(pure = true) public Connection proxy(@NotNull Proxy proxy) {
			conn.proxy(proxy);
			return this;
		}

		@Contract(pure = true) public Connection method(@NotNull Method method) {
			conn.method(org.jsoup.Connection.Method.valueOf(method.name()));
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
			Response res = executeProgram(conn);
			int statusCode = res.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimit); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
				res = executeProgram(conn);
				statusCode = res.statusCode();
			}
			if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
				throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + conn.request().url());
			}
			return res;
		}

		@NotNull @Contract(pure = true) protected Response executeProgram(@NotNull org.jsoup.Connection conn) {
			org.jsoup.Connection.Response response;
			try {
				response = conn.execute();
			} catch (IOException e) {
				return new HttpResponse(this, null);
			}
			return new HttpResponse(this, response);
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

		protected HttpConnection conn;
		protected org.jsoup.Connection.Response response;
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(HttpConnection conn, org.jsoup.Connection.Response response) {
			this.conn = conn;
			this.response = response;
		}

		@Contract(pure = true) public String url() {
			return response.url().toExternalForm();
		}

		@Contract(pure = true) public int statusCode() {
			return Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
		}

		@Contract(pure = true) public String statusMessage() {
			return response.statusMessage();
		}

		@Contract(pure = true) public String header(@NotNull String name) {
			return response.header(name);
		}

		@Contract(pure = true) public Map<String, String> headers() {
			return response.headers().entrySet().stream().collect(Collectors.toMap(l -> l.getKey().toLowerCase(), Map.Entry::getValue));
		}

		@Contract(pure = true) public Response header(@NotNull String key, @NotNull String value) {
			response.header(key, value);
			return this;
		}

		@Contract(pure = true) public Response removeHeader(@NotNull String key) {
			response.removeHeader(key);
			return this;
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return response.cookie(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			return response.cookies();
		}

		@Contract(pure = true) public Response cookie(@NotNull String name, @NotNull String value) {
			response.cookie(name, value);
			return this;
		}

		@Contract(pure = true) public Response removeCookie(@NotNull String name) {
			response.removeCookie(name);
			return this;
		}

		@Contract(pure = true) public Response charset(@NotNull String charsetName) {
			return charset(Charset.forName(charsetName));
		}

		@Contract(pure = true) public Response charset(@NotNull Charset charset) {
			this.charset = charset;
			return charset(charset.name());
		}

		@Contract(pure = true) public String contentType() {
			return response.contentType();
		}

		@Contract(pure = true) public Document parse() {
			return URIUtils.statusIsNormal(statusCode()) ? Jsoup.parse(body(), conn.parser) : null;
		}

		@Contract(pure = true) public String body() {
			String result;
			String encoding = header("content-encoding");
			try (InputStream in = bodyStream(); InputStream body = "br".equals(encoding) ? new BrotliInputStream(in) : in) {
				result = StreamUtils.stream(body).charset(charset).getString();
			} catch (IOException e) {
				return null;
			}
			return result;
		}

		@Contract(pure = true) public InputStream bodyStream() {
			return response.bodyStream();
		}

		@Contract(pure = true) public byte[] bodyAsBytes() {
			return response.bodyAsBytes();
		}

		@Contract(pure = true) public Response method(@NotNull Method method) {
			conn.method(method);
			return this;
		}

	}

}
