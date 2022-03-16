package org.haic.often.Network.Jsoup;

import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.Network.HttpStatus;
import org.haic.often.Network.URIUtils;
import org.haic.often.Network.UserAgent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieStore;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Jsoup 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/25 18:40
 */
public class JsoupUtil {

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		return new HttpConnection2(url, Jsoup.connect(url).maxBodySize(0).timeout(0).ignoreContentType(true).ignoreHttpErrors(true)).header("accept-language",
				"zh-CN,zh;q=0.9,en;q=0.8").header("user-agent", UserAgent.chrome());
	}

	protected static class HttpConnection2 extends Connection {
		protected String url; // 请求URL
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected boolean unlimitedRetry;// 请求异常无限重试
		protected boolean errorExit; // 错误退出

		protected List<Integer> retryStatusCodes = new ArrayList<>(); // 重试的错误状态码

		protected org.jsoup.Connection conn;

		protected HttpConnection2(String url, org.jsoup.Connection conn) {
			this.url = url;
			this.conn = conn;
		}

		@Contract(pure = true) public Connection url(URL url) {
			return url(url.toExternalForm());
		}

		@Contract(pure = true) public Connection url(String url) {
			conn.url(this.url = url);
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

		@Contract(pure = true) public Connection authorization(@NotNull String authorization) {
			return header("authorization", authorization.startsWith("Bearer ") ? authorization : "Bearer " + authorization);
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			conn.timeout(millis);
			return this;
		}

		@Contract(pure = true) public Connection maxBodySize(int bytes) {
			conn.maxBodySize(bytes);
			return this;
		}

		@Contract(pure = true) public Connection sslSocketFactory(SSLSocketFactory sslSocketFactory) {
			conn.sslSocketFactory(sslSocketFactory);
			return this;
		}

		@Contract(pure = true) public Connection header(@NotNull String name, @NotNull String value) {
			conn.header(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
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

		@Contract(pure = true) public Connection data(String... keyvals) {
			conn.data(keyvals);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			conn.data(key, value);
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
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
			return URIUtils.isJson(body) ?
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
			conn.proxy(proxy);
			return this;
		}

		@Contract(pure = true) public Connection cookieStore(CookieStore cookieStore) {
			conn.cookieStore(cookieStore);
			return this;
		}

		@Contract(pure = true) public CookieStore cookieStore() {
			return conn.cookieStore();
		}

		@Contract(pure = true) public Connection parser(Parser parser) {
			conn.parser(parser);
			return this;
		}

		@Contract(pure = true) public Connection postDataCharset(@NotNull String charset) {
			conn.postDataCharset(charset);
			return this;
		}

		@Contract(pure = true) public Connection method(@NotNull Method method) {
			conn.method(method);
			return this;
		}

		@Contract(pure = true) public Connection newRequest() {
			conn.newRequest();
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
			return Judge.isNull(response) ? null : Jsoup.parse(response.body());
		}

		@Contract(pure = true) public Document post() {
			Response response = method(Method.POST).execute();
			return Judge.isNull(response) ? null : Jsoup.parse(response.body());
		}

		@Contract(pure = true) public Response execute() {
			Response response = executeProgram(conn);
			int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
				response = executeProgram(conn);
				statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
			}
			if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
				throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + url);
			}
			return response;
		}

		@Contract(pure = true) protected Response executeProgram(@NotNull org.jsoup.Connection conn) {
			Response response;
			try {
				response = conn.execute();
			} catch (IOException e) {
				return null;
			}
			return response;
		}
	}

}