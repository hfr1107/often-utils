package org.haic.often.Network.HtmlUnit;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.logging.LogFactory;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.Network.HttpStatus;
import org.haic.often.Network.Response;
import org.haic.often.Network.URIUtils;
import org.haic.often.Network.UserAgent;
import org.haic.often.StreamUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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

	protected static WebClient webClient;

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static Connection connect(@NotNull String url) {
		// 屏蔽HtmlUnit等系统 log
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);

		// HtmlUnit 模拟浏览器,浏览器基本设置
		webClient = new WebClient(BrowserVersion.CHROME);
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

		return new HttpConnection().url(url).header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8").header("user-agent", UserAgent.chrome());
	}

	protected static class HttpConnection extends Connection {

		protected String url; // 请求URL
		protected boolean errorExit; // 错误退出
		protected boolean unlimitedRetry;// 请求异常无限重试
		protected int waitJSTime = 1000; // JS最大运行时间
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间

		protected List<NameValuePair> params = new ArrayList<>(); // params
		protected List<Integer> retryStatusCodes = new ArrayList<>();

		protected WebRequest request; // 会话

		@Contract(pure = true) public Connection url(@NotNull String url) {
			this.request = new WebRequest(URIUtils.getURL(this.url = url));
			header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
			return header("user-agent", UserAgent.chrome()); // 设置随机请求头
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

		@Contract(pure = true) public Connection authorization(@NotNull String authorization) {
			return header("authorization", authorization.startsWith("Bearer ") ? authorization : "Bearer " + authorization);
		}

		@Contract(pure = true) public Connection timeout(int millis) {
			webClient.getOptions().setTimeout(millis); // 设置连接超时时间
			return this;
		}

		@Contract(pure = true) public Connection header(@NotNull String name, @NotNull String value) {
			request.setAdditionalHeader(name, value);
			return this;
		}

		@Contract(pure = true) public Connection headers(@NotNull Map<String, String> headers) {
			request.setAdditionalHeaders(headers);
			return this;
		}

		@Contract(pure = true) public Connection cookie(@NotNull String name, @NotNull String value) {
			String cookie = request.getAdditionalHeader("cookie");
			return header("cookie", Judge.isEmpty(cookie) ? name + "=" + value : cookie + "; " + name + "=" + value);
		}

		@Contract(pure = true) public Connection cookies(@NotNull Map<String, String> cookies) {
			String cookie = cookies.toString().replaceAll(",", ";");
			return header("cookie", cookie.substring(1, cookie.length() - 1));
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String value) {
			params.add(new NameValuePair(key, value));
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			this.params = params.entrySet().stream().map(l -> new NameValuePair(l.getKey(), l.getValue())).collect(Collectors.toList());
			return this;
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String requestBody) {
			request.setRequestBody(requestBody);
			return URIUtils.isJson(requestBody) ?
					header("content-type", "application/json;charset=UTF-8") :
					header("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
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

		@Contract(pure = true) public Connection proxy(@NotNull String proxyHost, int proxyPort) {
			webClient.getOptions().setWebSocketEnabled(false); // WebSocket支持
			webClient.getOptions().setProxyConfig(new ProxyConfig() {{
				setSocksProxy(false);
				setProxyHost(proxyHost);
				setProxyPort(proxyPort);
			}});
			return this;
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

		/**
		 * 启用/禁用 CSS 支持。默认情况下，禁用此属性。如果禁用 HtmlUnit 将不会下载链接的 css 文件，也不会触发相关的 onload/onerror 事件
		 *
		 * @param enableCSS true启用 CSS 支持
		 * @return this
		 */
		@Contract(pure = true) public Connection enableCSS(boolean enableCSS) {
			webClient.getOptions().setCssEnabled(enableCSS);
			return this;
		}

		/**
		 * 设置 JavaScript 最大运行时间,默认1000毫秒.值为0则不加载JS
		 *
		 * @param waitJSTime JS超时时间(毫秒)
		 * @return this
		 */
		@Contract(pure = true) public Connection waitJSTime(int waitJSTime) {
			webClient.getOptions().setJavaScriptEnabled(!Judge.isEmpty(waitJSTime)); // 是否启用JS
			this.waitJSTime = waitJSTime;
			return this;
		}

		/**
		 * 将请求作为 POST 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public Document post() {
			method(Method.POST);
			HttpResponse response = execute();
			return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body()) : null;
		}

		/**
		 * 将请求作为 GET 执行，并解析结果
		 *
		 * @return HTML文档
		 */
		@Contract(pure = true) public Document get() {
			method(Method.GET);
			HttpResponse response = execute();
			return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body()) : null;
		}

		@Contract(pure = true) public HttpResponse execute() {
			request.setRequestParameters(params);

			HttpResponse response = executeProgram(request);
			int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
			for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
				MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP);
				response = executeProgram(request);
				statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
			}

			webClient.close(); // 关闭webClient

			if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
				throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + url);
			}

			return response;
		}

		@Contract(pure = true) protected HttpResponse executeProgram(@NotNull WebRequest request) {
			HttpResponse response;
			try { // 获得页面
				response = new HttpResponse(webClient.getPage(request));
			} catch (IOException e) {
				return null;
			}
			webClient.waitForBackgroundJavaScriptStartingBefore(waitJSTime); // 设置JS超时时间
			return response;
		}
	}

	protected static class HttpResponse extends Response {
		protected Page page; // Page对象
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(Page page) {
			this.page = page;
		}

		protected Page getPage() {
			return page;
		}

		@Contract(pure = true) public String url() {
			return page.getUrl().toExternalForm();
		}

		@Contract(pure = true) public HtmlPage getHtmlPage() {
			return isHtmlPage() ? (HtmlPage) page : null;
		}

		@Contract(pure = true) public int statusCode() {
			return Judge.isNull(page) ? HttpStatus.SC_REQUEST_TIMEOUT : page.getWebResponse().getStatusCode();
		}

		@Contract(pure = true) public HttpResponse charset(@NotNull String charsetName) {
			return charset(Charset.forName(charsetName));
		}

		@Contract(pure = true) public HttpResponse charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return cookies().get(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			return page.getWebResponse().getResponseHeaders().stream().filter(l -> l.getName().equalsIgnoreCase("set-cookie")).map(l -> l.getValue().split("="))
					.collect(Collectors.toMap(l -> l[0], l -> Judge.isEmpty(l[1]) ? l[1] : l[1].substring(0, l[1].indexOf(";"))));
		}

		@Contract(pure = true) public String header(@NotNull String name) {
			String header = headers().get(name);
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

		/**
		 * 如果此页面是 HtmlPage，则返回 true。
		 *
		 * @return true or false
		 */
		@Contract(pure = true) public boolean isHtmlPage() {
			return page.isHtmlPage();
		}

		@Contract(pure = true) public String body() {
			return isHtmlPage() ? ((HtmlPage) page).asXml() : page.getWebResponse().getContentAsString();
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

	}

}