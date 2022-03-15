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

import java.io.IOException;
import java.io.InputStream;
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

	protected String url; // 请求URL
	protected boolean errorExit; // 错误退出
	protected boolean unlimitedRetry;// 请求异常无限重试
	protected int waitJSTime = 1000; // JS最大运行时间
	protected int retry; // 请求异常重试次数
	protected int MILLISECONDS_SLEEP; // 重试等待时间

	protected List<NameValuePair> params = new ArrayList<>(); // params
	protected List<Integer> retryStatusCodes = new ArrayList<>();

	protected WebClient webClient;
	protected WebRequest request; // 会话

	protected HtmlUnitUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static HtmlUnitUtil connect(@NotNull String url) {
		HtmlUnitUtil htmlUnit = HtmlUnitUtil.config();
		// 屏蔽HtmlUnit等系统 log
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		Logger.getLogger("org.apache.http.client").setLevel(Level.OFF);
		// HtmlUnit 模拟浏览器,浏览器基本设置
		htmlUnit.webClient = new WebClient(BrowserVersion.CHROME);
		htmlUnit.webClient.getCookieManager().setCookiesEnabled(true); // 启动cookie
		htmlUnit.webClient.getOptions().setThrowExceptionOnScriptError(false);// 当JS执行出错的时候是否抛出异常
		htmlUnit.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);// 当HTTP的状态非200时是否抛出异常
		htmlUnit.webClient.getOptions().setPrintContentOnFailingStatusCode(false); // 响应失败不打印控制台
		htmlUnit.webClient.getOptions().setDoNotTrackEnabled(true); // 启用不跟踪
		htmlUnit.webClient.getOptions().setDownloadImages(false); // 不下载图片
		htmlUnit.webClient.getOptions().setPopupBlockerEnabled(true); // 开启阻止弹窗程序
		htmlUnit.webClient.getOptions().setCssEnabled(false); // 关闭CSS
		htmlUnit.webClient.getOptions().setJavaScriptEnabled(true); //启用JS
		htmlUnit.webClient.getOptions().isUseInsecureSSL(); // 允许不安全SSL
		htmlUnit.webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 设置支持AJAX
		htmlUnit.webClient.getOptions().setTimeout(0); // 设置连接超时时间
		return htmlUnit.url(url);
	}

	/**
	 * 获取新的连接对象
	 *
	 * @return 新的连接，用于链接
	 */
	@Contract(pure = true) protected static HtmlUnitUtil config() {
		return new HtmlUnitUtil();
	}

	/**
	 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) protected HtmlUnitUtil url(@NotNull String url) {
		this.request = new WebRequest(URIUtils.getURL(this.url = url));
		header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		return header("user-agent", UserAgent.chrome()); // 设置随机请求头
	}

	/**
	 * 设置连接请求
	 *
	 * @param request 新的请求对象
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil request(@NotNull WebRequest request) {
		this.request = request;
		return this;
	}

	/**
	 * 设置 POST（或 PUT）请求正文<br/>
	 * 当服务器需要一个普通的请求正文，而不是一组 URL 编码形式的键/值对时很有用<br/>
	 * 一般为JSON格式,若不是则作为普通数据发送
	 *
	 * @param requestBody 请求正文
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil requestBody(@NotNull String requestBody) {
		request.setRequestBody(requestBody);
		return URIUtils.isJson(requestBody) ?
				header("content-type", "application/json;charset=UTF-8") :
				header("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
	}

	/**
	 * 连接用户代理（ 字符串 用户代理）<br/>
	 * 设置请求用户代理标头
	 *
	 * @param userAgent 要使用的用户代理
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil userAgent(@NotNull String userAgent) {
		return header("user-agent", userAgent);
	}

	/**
	 * 添加请求头user-agent，以移动端方式访问页面
	 *
	 * @param isPhone true or false
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil isPhone(boolean isPhone) {
		return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
	}

	/**
	 * 设置是否在收到来自服务器的重定向状态代码时自动遵循重定向<br/>
	 * 默认情况下为true
	 *
	 * @param followRedirects 启用自动重定向
	 * @return this
	 */
	@Contract(pure = true) public HtmlUnitUtil followRedirects(boolean followRedirects) {
		webClient.getOptions().setRedirectEnabled(followRedirects); // 是否启用重定向
		return this;
	}

	/**
	 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/>
	 * 设置请求引荐来源网址（又名“引荐来源网址”）标头
	 *
	 * @param referrer 要使用的来源网址
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil referrer(@NotNull String referrer) {
		return header("referer", referrer);
	}

	/**
	 * 设置授权码或身份识别标识<br/>
	 * 有些服务器不使用cookie验证身份,使用authorization进行验证<br/>
	 * 一般信息在cookie或local Storage中存储
	 *
	 * @param authorization 授权码或身份识别标识
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil authorization(@NotNull String authorization) {
		return header("authorization", authorization.startsWith("Bearer ") ? authorization : "Bearer " + authorization);
	}

	/**
	 * 设置总请求超时时间，连接超时（ int millis）<br/>
	 * 默认超时为 0，超时为零被视为无限超时<br/>
	 * 请注意，此超时指定连接时间的组合最大持续时间和读取完整响应的时间
	 *
	 * @param millis 超时连接或读取之前的毫秒数（千分之一秒）
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil timeout(int millis) {
		webClient.getOptions().setTimeout(millis); // 设置连接超时时间
		return this;
	}

	/**
	 * 连接头（ 字符串 名称， 字符串 值）<br/>
	 * 设置请求标头
	 *
	 * @param name  标题名称
	 * @param value 标头值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil header(@NotNull String name, @NotNull String value) {
		request.setAdditionalHeader(name, value);
		return this;
	}

	/**
	 * 连接头（ Map  < String  , String  > 头）<br/>
	 * 将每个提供的标头添加到请求中
	 *
	 * @param headers 标头名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil headers(@NotNull Map<String, String> headers) {
		request.setAdditionalHeaders(headers);
		return this;
	}

	/**
	 * 设置要在请求中发送的 cookie
	 *
	 * @param name  cookie 的名称
	 * @param value cookie 的值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil cookie(@NotNull String name, @NotNull String value) {
		String cookie = request.getAdditionalHeader("cookie");
		return header("cookie", Judge.isEmpty(cookie) ? name + "=" + value : cookie + "; " + name + "=" + value);
	}

	/**
	 * 连接 cookies （ Map < String  , String  >cookies）<br/>
	 * 将每个提供的 cookie 添加到请求中
	 *
	 * @param cookies 名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil cookies(@NotNull Map<String, String> cookies) {
		String cookie = cookies.toString().replaceAll(",", ";");
		return header("cookie", cookie.substring(1, cookie.length() - 1));
	}

	/**
	 * 连接数据（ 字符串 键、 字符串 值）<br/>
	 * 添加请求数据参数。请求参数在 GET 的请求查询字符串中发送，在 POST 的请求正文中发送。一个请求可能有多个同名的值。
	 *
	 * @param key   数据键
	 * @param value 数据值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil data(@NotNull String key, @NotNull String value) {
		params.add(new NameValuePair(key, value));
		return this;
	}

	/**
	 * 将所有提供的数据添加到请求数据参数
	 *
	 * @param params 请求数据参数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil data(@NotNull Map<String, String> params) {
		this.params = params.entrySet().stream().map(l -> new NameValuePair(l.getKey(), l.getValue())).collect(Collectors.toList());
		return this;
	}

	/**
	 * 设置用于此请求的 SOCKS 代理<br/>
	 * 接口存在问题，SOCKS协议代理访问外网会失败，应使用Http协议代理
	 *
	 * @param proxyHost 代理URL
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil socks(@NotNull String proxyHost, int proxyPort) {
		webClient.getOptions().setWebSocketEnabled(true); // WebSocket支持
		webClient.getOptions().setProxyConfig(new ProxyConfig() {{
			setSocksProxy(true);
			setProxyHost(proxyHost);
			setProxyPort(proxyPort);
		}});
		return this;
	}

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
	@Contract(pure = true) public HtmlUnitUtil socks(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password) {
		((DefaultCredentialsProvider) webClient.getCredentialsProvider()).addCredentials(username, password);
		return socks(proxyHost, proxyPort);
	}

	/**
	 * 设置用于此请求的 HTTP 代理
	 *
	 * @param proxyHost 代理URL
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil proxy(@NotNull String proxyHost, int proxyPort) {
		webClient.getOptions().setWebSocketEnabled(false); // WebSocket支持
		webClient.getOptions().setProxyConfig(new ProxyConfig() {{
			setSocksProxy(false);
			setProxyHost(proxyHost);
			setProxyPort(proxyPort);
		}});
		return this;
	}

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
	@Contract(pure = true) public HtmlUnitUtil proxy(@NotNull String proxyHost, int proxyPort, @NotNull String username, @NotNull String password) {
		((DefaultCredentialsProvider) webClient.getCredentialsProvider()).addCredentials(username, password);
		return proxy(proxyHost, proxyPort);
	}

	/**
	 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
	 * 默认情况下为false
	 *
	 * @param errorExit 启用错误退出
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil errorExit(boolean errorExit) {
		this.errorExit = errorExit;
		return this;
	}

	/**
	 * 连接方法（ HttpMethod方法）
	 * 设置要使用的请求方法，GET 或 POST。默认为 GET。
	 *
	 * @param method HTTP 请求方法
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil method(@NotNull HttpMethod method) {
		request.setHttpMethod(method);
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry 重试次数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil retry(int retry) {
		this.retry = retry;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry  重试次数
	 * @param millis 重试等待时间(毫秒)
	 * @return this
	 */
	@Contract(pure = true) public HtmlUnitUtil retry(int retry, int millis) {
		this.retry = retry;
		this.MILLISECONDS_SLEEP = millis;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
	 *
	 * @param unlimitedRetry 启用无限重试, 默认false
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil retry(boolean unlimitedRetry) {
		this.unlimitedRetry = unlimitedRetry;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
	 *
	 * @param unlimitedRetry 启用无限重试, 默认false
	 * @param millis         重试等待时间(毫秒)
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil retry(boolean unlimitedRetry, int millis) {
		this.unlimitedRetry = unlimitedRetry;
		this.MILLISECONDS_SLEEP = millis;
		return this;
	}

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param statusCode 状态码
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil retryStatusCodes(int... statusCode) {
		retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
		return this;
	}

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param retryStatusCodes 状态码列表
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HtmlUnitUtil retryStatusCodes(List<Integer> retryStatusCodes) {
		this.retryStatusCodes = retryStatusCodes;
		return this;
	}

	/**
	 * 启用/禁用 CSS 支持。默认情况下，禁用此属性。如果禁用 HtmlUnit 将不会下载链接的 css 文件，也不会触发相关的 onload/onerror 事件
	 *
	 * @param enableCSS true启用 CSS 支持
	 * @return this
	 */
	@Contract(pure = true) public HtmlUnitUtil enableCSS(boolean enableCSS) {
		webClient.getOptions().setCssEnabled(enableCSS);
		return this;
	}

	/**
	 * 设置 JavaScript 最大运行时间,默认1000毫秒.值为0则不加载JS
	 *
	 * @param waitJSTime JS超时时间(毫秒)
	 * @return this
	 */
	@Contract(pure = true) public HtmlUnitUtil waitJSTime(int waitJSTime) {
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
		return get(HttpMethod.POST);
	}

	/**
	 * 将请求作为 GET 执行，并解析结果
	 *
	 * @return HTML文档
	 */
	@Contract(pure = true) public Document get() {
		return get(HttpMethod.GET);
	}

	/**
	 * 指定方法执行请求，并解析结果
	 *
	 * @param method Method类型，请求方法
	 * @return HTML文档
	 */
	@Contract(pure = true) public Document get(@NotNull HttpMethod method) {
		Response response = execute(method);
		return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body()) : null;
	}

	/**
	 * 运行程序，获取 响应结果
	 *
	 * @param method HttpMethod类型，请求方法
	 * @return 响应接口
	 */
	@Contract(pure = true) public Response execute(@NotNull HttpMethod method) {
		return method(method).execute();
	}

	/**
	 * 运行程序，获取 响应结果
	 *
	 * @return 响应接口
	 */
	@Contract(pure = true) public Response execute() {
		request.setRequestParameters(params);

		Response response = executeProgram(request);
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

	/**
	 * 主程序
	 *
	 * @param request WebRequest
	 * @return Response
	 */
	@Contract(pure = true) protected Response executeProgram(@NotNull WebRequest request) {
		Response response;
		try { // 获得页面
			response = new Response(webClient.getPage(request));
		} catch (IOException e) {
			return null;
		}
		webClient.waitForBackgroundJavaScriptStartingBefore(waitJSTime); // 设置JS超时时间
		return response;
	}

	/**
	 * 响应接口
	 */
	public static class Response {
		protected Page page; // Page对象

		/**
		 * Constructor for the HtmlUnitsResult.
		 *
		 * @param page The Page
		 */
		protected Response(Page page) {
			this.page = page;
		}

		/**
		 * Get Page
		 *
		 * @return Page
		 */
		protected Page getPage() {
			return page;
		}

		/**
		 * 返回此页面的 URL
		 *
		 * @return 此页面的 URL
		 */
		@Contract(pure = true) public String url() {
			return page.getUrl().toExternalForm();
		}

		/**
		 * Get HtmlPage
		 *
		 * @return HtmlPage
		 */
		@Contract(pure = true) public HtmlPage getHtmlPage() {
			return isHtmlPage() ? (HtmlPage) page : null;
		}

		/**
		 * Get statusCode
		 *
		 * @return 网页连接状态码
		 */
		@Contract(pure = true) public int statusCode() {
			return Judge.isNull(page) ? HttpStatus.SC_REQUEST_TIMEOUT : page.getWebResponse().getStatusCode();
		}

		/**
		 * Get cookies
		 *
		 * @return cookies
		 */
		@Contract(pure = true) public Map<String, String> cookies() {
			return page.getWebResponse().getResponseHeaders().stream().filter(l -> l.getName().equalsIgnoreCase("set-cookie")).map(l -> l.getValue().split("="))
					.collect(Collectors.toMap(l -> l[0], l -> Judge.isEmpty(l[1]) ? l[1] : l[1].substring(0, l[1].indexOf(";"))));
		}

		/**
		 * 获取 cookie
		 *
		 * @param name cookie name
		 * @return cookie value
		 */
		@Contract(pure = true) public String cookie(@NotNull String name) {
			return cookies().get(name);
		}

		/**
		 * Get headers
		 *
		 * @return Map
		 */
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
			//return page.getWebResponse().getResponseHeaders().stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
		}

		/**
		 * Get header value
		 *
		 * @return value
		 */
		@Contract(pure = true) public String header(@NotNull String name) {
			String header = headers().get(name);
			header = header.startsWith("[") ? header.substring(1) : header;
			return header.endsWith("]") ? header.substring(0, header.length() - 1) : header;
		}

		/**
		 * 如果此页面是 HtmlPage，则返回 true。
		 *
		 * @return true or false
		 */
		@Contract(pure = true) public boolean isHtmlPage() {
			return page.isHtmlPage();
		}

		/**
		 * 获取 响应正文
		 *
		 * @return 响应正文
		 */
		@Contract(pure = true) public String body() {
			return isHtmlPage() ? ((HtmlPage) page).asXml() : page.getWebResponse().getContentAsString();
		}

		/**
		 * 获取 响应流
		 *
		 * @return 响应流
		 */
		@Contract(pure = true) public InputStream bodyStream() throws IOException {
			return page.getWebResponse().getContentAsStream();
		}

		/**
		 * 获取 响应流
		 *
		 * @return 响应流
		 */
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