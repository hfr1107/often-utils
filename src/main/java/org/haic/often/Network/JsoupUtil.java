package org.haic.often.Network;

import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Request;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
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

	protected String url; // 请求URL
	protected int retry; // 请求异常重试次数
	protected int MILLISECONDS_SLEEP; // 重试等待时间
	protected boolean unlimitedRetry;// 请求异常无限重试
	protected boolean errorExit; // 错误退出

	protected List<Integer> retryStatusCodes = new ArrayList<>(); // 重试的错误状态码

	protected Connection conn;

	protected JsoupUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static JsoupUtil connect(@NotNull String url) {
		return JsoupUtil.config().url(url);
	}

	/**
	 * 获取新的连接对象
	 *
	 * @return 新的连接，用于链接
	 */
	@Contract(pure = true) protected static JsoupUtil config() {
		return new JsoupUtil();
	}

	/**
	 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) protected JsoupUtil url(@NotNull String url) {
		conn = Jsoup.connect(this.url = url).maxBodySize(0).timeout(0).ignoreContentType(true).ignoreHttpErrors(true);
		header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		return header("user-agent", UserAgent.chrome()); // 设置随机请求头
	}

	/**
	 * 连接sslSocketFactory （ SSLSocketFactory  sslSocketFactory）<br/>
	 * 设置自定义 SSL 套接字工厂
	 *
	 * @param sslSocketFactory 自定义 SSL 套接字工厂
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil sslSocketFactory(SSLSocketFactory sslSocketFactory) {
		conn.sslSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * 设置连接请求
	 *
	 * @param request 新的请求对象
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil request(@NotNull Request request) {
		conn.request(request);
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
	@Contract(pure = true) public JsoupUtil requestBody(@NotNull String requestBody) {
		conn.requestBody(requestBody);
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
	@Contract(pure = true) public JsoupUtil userAgent(@NotNull String userAgent) {
		return header("user-agent", userAgent);
	}

	/**
	 * 添加请求头user-agent，以移动端方式访问页面
	 *
	 * @param isPhone true or false
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil isPhone(boolean isPhone) {
		return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
	}

	/**
	 * 连接followRedirects （布尔followRedirects）<br/>
	 * 将连接配置为（不）遵循服务器重定向，默认情况下这是true
	 *
	 * @param followRedirects 如果应该遵循服务器重定向，则为 true
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil followRedirects(boolean followRedirects) {
		conn.followRedirects(followRedirects);
		return this;
	}

	/**
	 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/>
	 * 设置请求引荐来源网址（又名“引荐来源网址”）标头
	 *
	 * @param referrer 要使用的来源网址
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil referrer(@NotNull String referrer) {
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
	@Contract(pure = true) public JsoupUtil authorization(@NotNull String authorization) {
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
	@Contract(pure = true) public JsoupUtil timeout(int millis) {
		conn.timeout(millis);
		return this;
	}

	/**
	 * 连接maxBodySize （ int bytes）<br/>
	 * 设置在连接关闭之前从（未压缩的）连接读取到正文的最大字节数，并且输入被截断（即正文内容将被修剪）<br/>
	 * 默认值为 0，最大大小0被视为无限量（仅受您的耐心和机器上可用内存的限制）
	 *
	 * @param bytes 截断前从输入读取的字节数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil maxBodySize(int bytes) {
		conn.maxBodySize(bytes);
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
	@Contract(pure = true) public JsoupUtil header(@NotNull String name, @NotNull String value) {
		conn.header(name, value);
		return this;
	}

	/**
	 * 连接头（ Map  < String  , String  > 头）<br/>
	 * 将每个提供的标头添加到请求中
	 *
	 * @param headers 标头名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil headers(@NotNull Map<String, String> headers) {
		conn.headers(headers);
		return this;
	}

	/**
	 * 设置要在请求中发送的 cookie
	 *
	 * @param name  cookie 的名称
	 * @param value cookie 的值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil cookie(@NotNull String name, @NotNull String value) {
		conn.cookie(name, value);
		return this;
	}

	/**
	 * 连接 cookies （ Map < String  , String  >cookies）<br/>
	 * 将每个提供的 cookie 添加到请求中
	 *
	 * @param cookies 名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil cookies(@NotNull Map<String, String> cookies) {
		conn.cookies(cookies);
		return this;
	}

	/**
	 * 连接数据（ 字符串 键、 字符串 值）<br/>
	 * 添加请求数据参数。请求参数在 GET 的请求查询字符串中发送，在 POST 的请求正文中发送。一个请求可能有多个同名的值。
	 *
	 * @param key   数据键
	 * @param value 数据值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil data(@NotNull String key, @NotNull String value) {
		conn.data(key, value);
		return this;
	}

	/**
	 * 将所有提供的数据添加到请求数据参数
	 *
	 * @param params 请求数据参数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil data(@NotNull Map<String, String> params) {
		conn.data(params);
		return this;
	}

	/**
	 * 添加输入流作为请求数据参数，对于 GET 没有效果，但对于 POST 这将上传输入流
	 *
	 * @param key         数据键（表单项名称）
	 * @param fileName    要呈现给删除服务器的文件的名称。通常只是名称，而不是路径，组件
	 * @param inputStream 要上传的输入流，您可能从FileInputStream获得。您必须在finally块中关闭 InputStream
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
		conn.data(key, fileName, inputStream);
		return this;
	}

	/**
	 * 设置 文件，GET方法无效，一般用于上传，因为正常情况下使用并不会多，而判断控制流关闭会消耗资源,所以交由外部处理
	 *
	 * @param fileName    文件名
	 * @param inputStream 流
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil file(@NotNull String fileName, @NotNull InputStream inputStream) {
		return data("file", fileName, inputStream);
	}

	/**
	 * 设置用于此请求的 SOCKS 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil socks(@NotNull String proxyHost, int proxyPort) {
		return proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort)));
	}

	/**
	 * 设置用于此请求的 HTTP 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil proxy(@NotNull String proxyHost, int proxyPort) {
		return proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
	}

	/**
	 * 连接代理（ @NotNull  Proxy 代理）<br/>
	 * 设置用于此请求的代理
	 *
	 * @param proxy 要使用的代理
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil proxy(@NotNull Proxy proxy) {
		conn.proxy(proxy);
		return this;
	}

	/**
	 * 连接方法（ Connection.Method方法）
	 * 设置要使用的请求方法，GET 或 POST。默认为 GET。
	 *
	 * @param method HTTP 请求方法
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil method(@NotNull Method method) {
		conn.method(method);
		return this;
	}

	/**
	 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
	 * 默认情况下为false
	 *
	 * @param errorExit 启用错误退出
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil errorExit(boolean errorExit) {
		this.errorExit = errorExit;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry 重试次数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil retry(int retry) {
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
	@Contract(pure = true) public JsoupUtil retry(int retry, int millis) {
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
	@Contract(pure = true) public JsoupUtil retry(boolean unlimitedRetry) {
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
	@Contract(pure = true) public JsoupUtil retry(boolean unlimitedRetry, int millis) {
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
	@Contract(pure = true) public JsoupUtil retryStatusCodes(int... statusCode) {
		retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
		return this;
	}

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param retryStatusCodes 状态码列表
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public JsoupUtil retryStatusCodes(List<Integer> retryStatusCodes) {
		this.retryStatusCodes = retryStatusCodes;
		return this;
	}

	/**
	 * 将请求作为 POST 执行，并解析结果
	 *
	 * @return HTML文档
	 */
	@Contract(pure = true) public Document post() {
		return get(Method.POST);
	}

	/**
	 * 将请求作为 GET 执行，并解析结果
	 *
	 * @return HTML文档
	 */
	@Contract(pure = true) public Document get() {
		return get(Method.GET);
	}

	/**
	 * 指定方法执行请求，并解析结果
	 *
	 * @param method Method类型，请求方法
	 * @return HTML文档
	 */
	@Contract(pure = true) public Document get(@NotNull Method method) {
		Response response = execute(method);
		return Judge.isNull(response) ? null : Jsoup.parse(response.body());
	}

	/**
	 * 运行程序，获取 响应结果
	 *
	 * @param method Method类型，请求方法
	 * @return 响应接口
	 */
	@Contract(pure = true) public Response execute(@NotNull Method method) {
		return method(method).execute();
	}

	/**
	 * 运行程序，获取 响应结果
	 *
	 * @return 响应接口
	 */
	@Contract(pure = true) public Response execute() {
		Response response = executeProgram(conn);
		int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
		for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
			MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
			response = executeProgram(conn);
			statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
		}
		if (errorExit && !URIUtils.statusIsOK(statusCode) && !URIUtils.statusIsRedirect(statusCode)) {
			throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + url);
		}
		return response;
	}

	/**
	 * 主程序
	 *
	 * @param conn Connection
	 * @return 响应接口
	 */
	@Contract(pure = true) protected Response executeProgram(@NotNull Connection conn) {
		Response response;
		try {
			response = conn.execute();
		} catch (IOException e) {
			return null;
		}
		return response;
	}

}