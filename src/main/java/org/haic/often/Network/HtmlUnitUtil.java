package org.haic.often.Network;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.logging.LogFactory;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * HtmlUnit 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/25 21:05
 */
public class HtmlUnitUtil {

	protected HtmlUnitUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）
	 * <p>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 * <p>
	 * 需要注意方法会构造一个新的WebClient,用于链接,由于启动缓慢,不会再执行后关闭,在所有请求完成后使用close()关闭WebClient
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		return new HttpConnection().url(url);
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

	@Contract(pure = true) protected static WebClient createClient() {
		// 屏蔽HtmlUnit等系统 log
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);

		// HtmlUnit 模拟浏览器,浏览器基本设置
		WebClient webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getCookieManager().setCookiesEnabled(true); // 启动cookie
		webClient.getOptions().setThrowExceptionOnScriptError(false);// 当JS执行出错的时候是否抛出异常
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);// 当HTTP的状态非200时是否抛出异常
		webClient.getOptions().setPrintContentOnFailingStatusCode(false); // 响应失败不打印控制台
		webClient.getOptions().setDoNotTrackEnabled(true); // 启用不跟踪
		webClient.getOptions().setDownloadImages(false); // 不下载图片
		webClient.getOptions().setPopupBlockerEnabled(true); // 开启阻止弹窗程序
		webClient.getOptions().setCssEnabled(false); // 关闭CSS
		webClient.getOptions().setJavaScriptEnabled(true); //启用JS
		webClient.getOptions().isUseInsecureSSL(); // 允许不安全SSL
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 设置支持AJAX
		webClient.getOptions().setTimeout(0); // 设置连接超时时间
		return webClient;
	}

	protected static class HttpConnection extends Connection {

		protected String auth; // 身份识别标识
		protected boolean errorExit; // 错误退出
		protected boolean unlimit;// 请求异常无限重试
		protected int waitJSTime = 1000; // JS最大运行时间
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间

		protected Parser parser = Parser.htmlParser();
		protected Map<String, String> cookies = new HashMap<>(); // cookes
		protected List<Integer> retryStatusCodes = new ArrayList<>();

		protected WebClient webClient = createClient();
		protected WebRequest request; // 会话

		protected HttpConnection() {
			initialization(new WebRequest(null));
		}

		@Contract(pure = true) protected Connection initialization(@NotNull WebRequest request) {
			this.request = request;
			header("accept", "text/html, application/xhtml+xml, application/json;q=0.9, */*;q=0.8");
			header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
			header("accept-encoding", "gzip, deflate, br"); // 允许压缩gzip,br-Brotli
			return header("user-agent", UserAgent.chrome()); // 设置随机请求头
		}

		@Contract(pure = true) public Connection url(@NotNull String url) {
			request.setUrl(URIUtils.getURL(url));
			return this;
		}

		@Contract(pure = true) public Connection newRequest() {
			initialization(new WebRequest(null)).cookies(cookies);
			return Judge.isEmpty(auth) ? this : auth(auth);
		}

		@Contract(pure = true) public Connection userAgent(@NotNull String userAgent) {
			return header("user-agent", userAgent);
		}

		@Contract(pure = true) public Connection isPhone(boolean isPhone) {
			return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
		}

		@Contract(pure = true) public Connection followRedirects(boolean followRedirects) {
			webClient.getOptions().setRedirectEnabled(followRedirects); // 是否启用重定向
			return this;
		}

		@Contract(pure = true) public Connection referrer(@NotNull String referrer) {
			return header("referer", referrer);
		}

		@Contract(pure = true) public Connection auth(@NotNull String auth) {
			return header("authorization", (this.auth = auth.contains(" ") ? auth : "Bearer " + auth));
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			webClient.getOptions().setTimeout(millis); // 设置连接超时时间
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
			request.setAdditionalHeader(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			request.setAdditionalHeaders(headers);
			return this;
		}

		@Contract(pure = true) public Connection setHeaders(@NotNull Map<String, String> headers) {
			request.getAdditionalHeaders().clear();
			headers(headers);
			String cookie = this.cookies.toString().replaceAll(",", ";");
			return header("cookie", cookie.substring(1, cookie.length() - 1));
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			cookies.put(name, value);
			String cookie = request.getAdditionalHeader("cookie");
			return header("cookie", (Judge.isEmpty(cookie) ? "" : cookie + "; ") + name + "=" + value);
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			this.cookies.putAll(cookies);
			String cookie = this.cookies.toString().replaceAll(",", ";");
			return header("cookie", cookie.substring(1, cookie.length() - 1));
		}

		@Contract(pure = true) public Connection setCookies(@NotNull Map<String, String> cookies) {
			this.cookies = new HashMap<>();
			return cookies(cookies);
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			request.getRequestParameters().add(new NameValuePair(key, value));
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			request.setRequestParameters(params.entrySet().stream().map(l -> new NameValuePair(l.getKey(), l.getValue())).collect(Collectors.toList()));
			return this;
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String body) {
			request.setRequestBody(body);
			return URIUtils.isJson(body) ? contentType("application/json;charset=UTF-8") : contentType("application/x-www-form-urlencoded;charset=UTF-8");
		}

		@Contract(pure = true) public Connection socks(@NotNull String proxyHost, int proxyPort) {
			webClient.getOptions().setWebSocketEnabled(true); // WebSocket支持
			webClient.getOptions().setProxyConfig(new ProxyConfig() {{
				setSocksProxy(true);
				setProxyHost(proxyHost);
				setProxyPort(proxyPort);
			}});
			return this;
		}

		@Contract(pure = true) public Connection socks(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password) {
			((DefaultCredentialsProvider) webClient.getCredentialsProvider()).addCredentials(username, password);
			return socks(proxyHost, proxyPort);
		}

		@Contract(pure = true) public Connection proxy(@NotNull String proxyHost, int proxyPort) {
			webClient.getOptions().setWebSocketEnabled(false); // WebSocket支持
			webClient.getOptions().setProxyConfig(new ProxyConfig() {{
				setSocksProxy(false);
				setProxyHost(proxyHost);
				setProxyPort(proxyPort);
			}});
			return this;
		}

		@Contract(pure = true) public Connection proxy(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password) {
			((DefaultCredentialsProvider) webClient.getCredentialsProvider()).addCredentials(username, password);
			return proxy(proxyHost, proxyPort);
		}

		@Contract(pure = true) public Connection proxy(@NotNull Proxy proxy) {
			String proxyText = proxy.toString();
			if (proxyText.equals("DIRECT")) {
				return this;
			}
			String[] proxyStr = proxyText.substring(proxyText.indexOf("/") + 1).split(":");
			if (proxyText.startsWith("SOCKS")) {
				return socks(proxyStr[0], Integer.parseInt(proxyStr[1]));
			} else {
				return proxy(proxyStr[0], Integer.parseInt(proxyStr[1]));
			}
		}

		@Contract(pure = true) public Connection method(@NotNull Method method) {
			request.setHttpMethod(HttpMethod.valueOf(method.name()));
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

		/**
		 * 启用/禁用 CSS 支持。默认情况下，禁用此属性。如果禁用 HtmlUnit 将不会下载链接的 css 文件，也不会触发相关的 onload/onerror 事件
		 *
		 * @param enableCSS true启用 CSS 支持
		 * @return 此链接, 用于链接
		 */
		@Contract(pure = true) public Connection enableCSS(boolean enableCSS) {
			webClient.getOptions().setCssEnabled(enableCSS);
			return this;
		}

		@Contract(pure = true) public Connection waitJSTime(int millis) {
			webClient.getOptions().setJavaScriptEnabled(!Judge.isEmpty(millis)); // 是否启用JS
			webClient.setJavaScriptTimeout(waitJSTime);
			waitJSTime = millis;
			return this;
		}

		@Contract(pure = true) public Connection close() {
			webClient.close(); // 设置连接超时时间
			return this;
		}

		/**
		 * 将请求作为 GET 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public Document get() {
			return method(Method.GET).execute().parse();
		}

		/**
		 * 将请求作为 POST 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public Document post() {
			return method(Method.POST).execute().parse();
		}

		@Contract(pure = true) public Response execute() {
			Response response = executeProgram(request);
			int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimit); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP);
				response = executeProgram(request);
				statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
			}

			// webClient.close(); // 关闭webClient

			if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
				throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + request.getUrl());
			}

			return response;
		}

		@Contract(pure = true) protected Response executeProgram(@NotNull WebRequest request) {
			Response response;
			try { // 获得页面
				response = new HttpResponse(this, webClient.getPage(request));
			} catch (IOException e) {
				return null;
			}
			webClient.waitForBackgroundJavaScript(waitJSTime); // 阻塞并执行JS
			cookies(response.cookies()); // 维护cookies

			String redirectUrl; // 修复重定向
			if (webClient.getOptions().isRedirectEnabled() && URIUtils.statusIsOK(response.statusCode()) && !Judge.isEmpty(
					redirectUrl = response.header("location"))) {
				url(redirectUrl);
				response = executeProgram(request);
			}

			return response;
		}
	}

	protected static class HttpResponse extends Response {

		protected HttpConnection conn;
		protected Page page; // Page对象
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(HttpConnection conn, Page page) {
			this.conn = conn;
			this.page = page;
		}

		protected Page getPage() {
			return page;
		}

		@Contract(pure = true) public String url() {
			return page.getUrl().toExternalForm();
		}

		@Contract(pure = true) public int statusCode() {
			return Judge.isNull(page) ? HttpStatus.SC_REQUEST_TIMEOUT : page.getWebResponse().getStatusCode();
		}

		@Contract(pure = true) public String statusMessage() {
			return page.getWebResponse().getStatusMessage();
		}

		@Contract(pure = true) public boolean isHtmlPage() {
			return page.isHtmlPage();
		}

		@Contract(pure = true) public HtmlPage getHtmlPage() {
			return isHtmlPage() ? (HtmlPage) page : null;
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
			Map<String, String> headers = new HashMap<>();
			for (NameValuePair header : page.getWebResponse().getResponseHeaders()) {
				String name = header.getName().toLowerCase();
				String value = header.getValue();
				if (name.equals("set-cookie")) {
					String cookie = headers.get(name);
					headers.put(name, Judge.isEmpty(cookie) ? "[" + value + "]" : cookie.substring(0, cookie.length() - 1) + ", " + value + "]");
				} else {
					headers.put(name, value);
				}
			}
			return headers;
		}

		@Contract(pure = true) public Response header(@NotNull String key, @NotNull String value) {
			conn.header(key, value);
			return this;
		}

		@Contract(pure = true) public Response removeHeader(@NotNull String key) {
			conn.request.removeAdditionalHeader(key);
			return this;
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return cookies().get(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			return page.getWebResponse().getResponseHeaders().stream().filter(l -> l.getName().equalsIgnoreCase("set-cookie"))
					.map(l -> l.getValue().substring(0, l.getValue().indexOf(";")))
					.collect(Collectors.toMap(l -> l.substring(0, l.indexOf("=")), l -> l.substring(l.indexOf("=") + 1), (e1, e2) -> e2));
		}

		@Contract(pure = true) public Response cookie(@NotNull String name, @NotNull String value) {
			conn.cookie(name, value);
			return this;
		}

		@Contract(pure = true) public Response removeCookie(@NotNull String name) {
			conn.cookies.remove(name);
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
			return page.getWebResponse().getStatusMessage();
		}

		@Contract(pure = true) public Document parse() {
			return URIUtils.statusIsNormal(statusCode()) ? Jsoup.parse(body(), conn.parser) : null;
		}

		@Contract(pure = true) public String body() {
			return isHtmlPage() ? ((HtmlPage) page).asXml() : page.getWebResponse().getContentAsString(charset);
		}

		@Contract(pure = true) public InputStream bodyStream() throws IOException {
			return page.getWebResponse().getContentAsStream();
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
			conn.method(method);
			return this;
		}

	}

	/**
	 * Connection 接口是一个方便的 HTTP 客户端和会话对象，用于从 Web 获取内容，并将它们解析为 Documents。
	 * <p>
	 * 使用的“连接”并不意味着在连接对象的生命周期内与服务器保持长期连接。套接字连接仅在请求执行（ execute() 、 get()或post() ）时建立，并消耗服务器的响应。
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/3/16 14:04
	 */
	public static abstract class Connection {
		/**
		 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
		 *
		 * @param url 要连接的 URL
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection url(@NotNull String url);

		/**
		 * 连接newRequest ()
		 * 创建一个新请求，使用此 Connection 作为会话状态并初始化连接设置（然后可以独立于返回的 Connection.Request 对象）。
		 *
		 * @return 一个新的 Connection 对象，具有共享的 Cookie 存储和来自此 Connection 和 Request 的初始化设置
		 */
		@Contract(pure = true) public abstract Connection newRequest();

		/**
		 * 连接用户代理（ 字符串 用户代理）<br/>
		 * 设置请求用户代理标头
		 *
		 * @param userAgent 要使用的用户代理
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection userAgent(@NotNull String userAgent);

		/**
		 * 添加请求头user-agent，以移动端方式访问页面
		 *
		 * @param isPhone true or false
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection isPhone(boolean isPhone);

		/**
		 * 连接followRedirects （布尔followRedirects）<br/>
		 * 将连接配置为（不）遵循服务器重定向，默认情况下这是true
		 *
		 * @param followRedirects 如果应该遵循服务器重定向，则为 true
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection followRedirects(boolean followRedirects);

		/**
		 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/>
		 * 设置请求引荐来源网址（又名“引荐来源网址”）标头
		 *
		 * @param referrer 要使用的来源网址
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection referrer(@NotNull String referrer);

		/**
		 * 设置授权码或身份识别标识
		 * <p>
		 * 有些服务器不使用cookie验证身份,使用authorization进行验证
		 * <p>
		 * 一般信息在cookie或local Storage中存储
		 * <p>
		 * 如果没有协议类型,默认使用Bearer
		 *
		 * @param auth 授权码或身份识别标识
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection auth(@NotNull String auth);

		/**
		 * 设置总请求超时时间，连接超时（ int millis）<br/>
		 * 默认超时为 0，超时为零被视为无限超时<br/>
		 * 请注意，此超时指定连接时间的组合最大持续时间和读取完整响应的时间
		 *
		 * @param millis 超时连接或读取之前的毫秒数（千分之一秒）
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection timeout(int millis);

		/**
		 * 连接解析器（ Parser parser）
		 * 在解析对文档的响应时提供备用解析器。如果未设置，则默认使用 HTML 解析器，除非响应内容类型是 XML，在这种情况下使用 XML 解析器。
		 * 参数：
		 * parser - 备用解析器
		 * 回报：
		 * 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection parser(@NotNull Parser parser);

		/**
		 * 设置连接请求类型参数,用于服务器识别内容类型
		 *
		 * @param type 请求类型
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection contentType(@NotNull String type);

		/**
		 * 连接头（ 字符串 名称， 字符串 值）<br/>
		 * 设置请求标头
		 *
		 * @param name  标题名称
		 * @param value 标头值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection header(@NotNull String name, @NotNull String value);

		/**
		 * 连接头（ Map  < String  , String  > 头）<br/>
		 * 将每个提供的标头添加到请求中
		 *
		 * @param headers 标头名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection headers(@NotNull Map<String, String> headers);

		/**
		 * 连接头（ Map  < String  , String  > 头）
		 * <p>
		 * 将为连接设置全新的请求标头
		 *
		 * @param headers 标头名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection setHeaders(@NotNull Map<String, String> headers);

		/**
		 * 设置要在请求中发送的 cookie
		 *
		 * @param name  cookie 的名称
		 * @param value cookie 的值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection cookie(@NotNull String name, @NotNull String value);

		/**
		 * 连接 cookies （ Map < String  , String  >cookies）<br/>
		 * 将每个提供的 cookie 添加到请求中
		 *
		 * @param cookies 名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection cookies(@NotNull Map<String, String> cookies);

		/**
		 * 连接 cookies （ Map < String  , String  >cookies）
		 * <p>
		 * 将为连接设置全新的 cookie
		 *
		 * @param cookies 名称映射 -> 值对
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection setCookies(@NotNull Map<String, String> cookies);

		/**
		 * 连接数据（ 字符串 键、 字符串 值）<br/>
		 * 添加请求数据参数。请求参数在 GET 的请求查询字符串中发送，在 POST 的请求正文中发送。一个请求可能有多个同名的值。
		 *
		 * @param key   数据键
		 * @param value 数据值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection data(@NotNull String key, @NotNull String value);

		/**
		 * 根据所有提供的数据设置全新的请求数据参数
		 *
		 * @param params 数据参数
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection data(@NotNull Map<String, String> params);

		/**
		 * 设置 POST（或 PUT）请求正文<br/>
		 * 当服务器需要一个普通的请求正文，而不是一组 URL 编码形式的键/值对时很有用<br/>
		 * 一般为JSON格式,若不是则作为普通数据发送
		 *
		 * @param body 请求正文
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection requestBody(@NotNull String body);

		/**
		 * 设置用于此请求的 SOCKS 代理
		 *
		 * @param proxyHost 代理主机名
		 * @param proxyPort 代理端口
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection socks(@NotNull String proxyHost, int proxyPort);

		/**
		 * 设置用于此请求的 SOCKS 代理<br/>
		 * 需要验证的代理服务器<br/>
		 * 接口存在问题，SOCKS协议代理访问外网会失败，应使用Http协议代理
		 *
		 * @param proxyHost 代理URL
		 * @param proxyPort 代理端口
		 * @param username  代理用户名
		 * @param password  代理用户密码
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection socks(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password);

		/**
		 * 连接代理（ @NotNull  Proxy 代理）<br/>
		 * * 设置用于此请求的代理
		 *
		 * @param proxyHost 代理地址
		 * @param proxyPort 代理端口
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection proxy(@NotNull String proxyHost, int proxyPort);

		/**
		 * 设置用于此请求的 HTTP 代理<br/>
		 * 需要验证的代理服务器
		 *
		 * @param proxyHost 代理URL
		 * @param proxyPort 代理端口
		 * @param username  代理用户名
		 * @param password  代理用户密码
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection proxy(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password);

		/**
		 * 连接代理（ @NotNull  Proxy 代理）<br/>
		 * 设置用于此请求的代理
		 *
		 * @param proxy 要使用的代理
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection proxy(@NotNull Proxy proxy);

		/**
		 * 连接方法（ Connection.Method方法）
		 * 设置要使用的请求方法，GET 或 POST。默认为 GET。
		 *
		 * @param method HTTP 请求方法
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection method(@NotNull Method method);

		/**
		 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
		 *
		 * @param retry 重试次数
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection retry(int retry);

		/**
		 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
		 *
		 * @param retry  重试次数
		 * @param millis 重试等待时间(毫秒)
		 * @return 此链接, 用于链接
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
		 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
		 * 默认情况下为false
		 *
		 * @param errorExit 启用错误退出
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Connection errorExit(boolean errorExit);

		/**
		 * 启用/禁用 CSS 支持。默认情况下，禁用此属性。如果禁用 HtmlUnit 将不会下载链接的 css 文件，也不会触发相关的 onload/onerror 事件
		 *
		 * @param enableCSS true启用 CSS 支持
		 * @return 此链接, 用于链接
		 */
		@Contract(pure = true) public abstract Connection enableCSS(boolean enableCSS);

		/**
		 * 设置 JavaScript 最大运行时间,默认1000毫秒.值为0则不加载JS
		 *
		 * @param millis JS超时时间(毫秒)
		 * @return 此链接, 用于链接
		 */
		@Contract(pure = true) public abstract Connection waitJSTime(int millis);

		/**
		 * 关闭WebClient客户端
		 *
		 * @return 此链接, 用于链接
		 */

		@Contract(pure = true) public abstract Connection close();

		/**
		 * 将请求作为 GET 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public abstract Document get();

		/**
		 * 将请求作为 POST 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public abstract Document post();

		/**
		 * 运行程序，获取 响应结果
		 *
		 * @return Response
		 */
		@Contract(pure = true) public abstract Response execute();

	}

	/**
	 * 响应接口
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/3/16 11:52
	 */
	public static abstract class Response {

		/**
		 * 返回此页面的 URL
		 *
		 * @return 此页面的 URL
		 */
		@Contract(pure = true) public abstract String url();

		/**
		 * 获取响应的状态码
		 *
		 * @return 请求响应代码
		 */
		@Contract(pure = true) public abstract int statusCode();

		/**
		 * 获取与响应代码一起从服务器返回的 HTTP 响应消息（如果有）。来自以下回复：
		 * <p>
		 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;HTTP/1.0 200 OK
		 * <p>
		 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;HTTP/1.0 404 Not Found
		 * <p>
		 * 分别提取字符串“OK”和“Not Found”。如果无法从响应中辨别出任何内容（结果不是有效的 HTTP），则返回 null。
		 *
		 * @return 状态消息
		 */
		@Contract(pure = true) public abstract String statusMessage();

		/**
		 * 如果此页面是 HtmlPage，则返回 true。
		 *
		 * @return true or false
		 */
		@Contract(pure = true) public abstract boolean isHtmlPage();

		/**
		 * 获取当前响应的HtmlPage
		 *
		 * @return HtmlPage
		 */
		@Contract(pure = true) public abstract HtmlPage getHtmlPage();

		/**
		 * 获取 请求头的值
		 *
		 * @return 请求头的值
		 */
		@Contract(pure = true) public abstract String header(@NotNull String name);

		/**
		 * 获取 请求头
		 *
		 * @return 请求头
		 */
		@Contract(pure = true) public abstract Map<String, String> headers();

		/**
		 * 在此请求/响应中设置 header。
		 *
		 * @param key   header的键
		 * @param value header的值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response header(@NotNull String key, @NotNull String value);

		/**
		 * 删除在此请求/响应中设置 header。
		 *
		 * @param key header的键
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response removeHeader(@NotNull String key);

		/**
		 * 获取 cookie
		 *
		 * @param name cookie name
		 * @return cookie value
		 */
		@Contract(pure = true) public abstract String cookie(@NotNull String name);

		/**
		 * 获取 cookies
		 *
		 * @return cookies
		 */
		@Contract(pure = true) public abstract Map<String, String> cookies();

		/**
		 * @param name  cookie的名称
		 * @param value cookie的值
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response cookie(@NotNull String name, @NotNull String value);

		/**
		 * 删除在此请求/响应中设置 cookie。
		 *
		 * @param name cookie的名称
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response removeCookie(@NotNull String name);

		/**
		 * Response字符集（ 字符串 字符集）<br/>
		 * 设置/覆盖响应字符集。解析文档正文时，它将使用此字符集。
		 *
		 * @param charsetName 字符集格式名称
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response charset(@NotNull String charsetName);

		/**
		 * Response字符集（ 字符串 字符集）<br/>
		 * 设置/覆盖响应字符集。解析文档正文时，它将使用此字符集。
		 *
		 * @param charset 字符集格式
		 * @return 此连接，用于链接
		 */

		@Contract(pure = true) public abstract Response charset(@NotNull Charset charset);

		/**
		 * 获取响应内容类型（例如“text/html”）
		 *
		 * @return 响应内容类型，如果未设置则为null
		 */
		@Contract(pure = true) public abstract String contentType();

		/**
		 * 读取响应的正文并将其解析为文档,如果连接超时或IO异常会返回null
		 *
		 * @return 已解析的文档
		 */
		@Contract(pure = true) public abstract Document parse();

		/**
		 * Get the body of the response as a plain string.
		 *
		 * @return body
		 */
		@Contract(pure = true) public abstract String body();

		/**
		 * Get the body of the response as a (buffered) InputStream. You should close the input stream when you're done with it.
		 * Other body methods (like bufferUp, body, parse, etc) will not work in conjunction with this method.
		 * <p>This method is useful for writing large responses to disk, without buffering them completely into memory first.</p>
		 *
		 * @return the response body input stream
		 */
		@Contract(pure = true) public abstract InputStream bodyStream() throws IOException;

		/**
		 * Get the body of the response as an array of bytes.
		 *
		 * @return body bytes
		 */
		@Contract(pure = true) public abstract byte[] bodyAsBytes();

		/**
		 * 设置请求方式
		 * <p>
		 * method - 新方法
		 *
		 * @return 此连接，用于链接
		 */
		@Contract(pure = true) public abstract Response method(@NotNull Method method);

	}

}
