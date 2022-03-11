package org.haic.often.Network;

import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.TupleUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
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
	protected Map<String, String> headers = new HashMap<>(); // 请求头
	protected List<Integer> retryStatusCodes = new ArrayList<>();
	protected ThreeTuple<String, InputStream, String> file;

	protected HttpsUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static HttpsUtil connect(@NotNull String url) {
		return config().url(url);
	}

	/**
	 * 获取新的连接对象
	 *
	 * @return 新的连接，用于链接
	 */
	@Contract(pure = true) protected static HttpsUtil config() {
		return new HttpsUtil();
	}

	/**
	 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) protected HttpsUtil url(@NotNull String url) {
		this.url = url;
		header("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		return header("user-agent", UserAgent.chrome()); // 设置随机请求头
	}

	/**
	 * 设置 POST（或 PUT）请求正文<br/>
	 * 当服务器需要一个普通的请求正文，而不是一组 URL 编码形式的键/值对时很有用<br/>
	 * 一般为JSON格式,若不是则作为普通数据发送
	 *
	 * @param requestBody 请求正文
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil requestBody(@NotNull String requestBody) {
		params = requestBody;
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
	@Contract(pure = true) public HttpsUtil userAgent(@NotNull String userAgent) {
		return header("user-agent", userAgent);
	}

	/**
	 * 添加请求头user-agent，以移动端方式访问页面
	 *
	 * @param isPhone true or false
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil isPhone(boolean isPhone) {
		return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
	}

	/**
	 * 连接followRedirects （布尔followRedirects）<br/>
	 * 将连接配置为（不）遵循服务器重定向，默认情况下这是true
	 *
	 * @param followRedirects 如果应该遵循服务器重定向，则为 true
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil followRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
		return this;
	}

	/**
	 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/>
	 * 设置请求引荐来源网址（又名“引荐来源网址”）标头
	 *
	 * @param referrer 要使用的来源网址
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil referrer(@NotNull String referrer) {
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
	@Contract(pure = true) public HttpsUtil authorization(@NotNull String authorization) {
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
	@Contract(pure = true) public HttpsUtil timeout(int millis) {
		this.timeout = millis;
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
	@Contract(pure = true) public HttpsUtil header(@NotNull String name, @NotNull String value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * 连接头（ Map  < String  , String  > 头）<br/>
	 * 将每个提供的标头添加到请求中
	 *
	 * @param headers 标头名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil headers(@NotNull Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 * 设置要在请求中发送的 cookie
	 *
	 * @param name  cookie 的名称
	 * @param value cookie 的值
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil cookie(@NotNull String name, @NotNull String value) {
		String cookie = headers.get("cookie");
		return header("cookie", Judge.isEmpty(cookie) ? name + "=" + value : cookie + "; " + name + "=" + value);
	}

	/**
	 * 连接 cookies （ Map < String  , String  >cookies）<br/>
	 * 将每个提供的 cookie 添加到请求中
	 *
	 * @param cookies 名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil cookies(@NotNull Map<String, String> cookies) {
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
	@Contract(pure = true) public HttpsUtil data(@NotNull String key, @NotNull String value) {
		params += Judge.isEmpty(params) ? key + "=" + value : "&" + key + "=" + value;
		return this;
	}

	/**
	 * 将所有提供的数据添加到请求数据参数
	 *
	 * @param params 数据参数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil data(@NotNull Map<String, String> params) {
		this.params = "";
		params.forEach(this::data);
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
	@Contract(pure = true) public HttpsUtil data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
		ThreeTuple<String, String, String> fromData = URIUtils.getFormData(key, fileName);
		file = TupleUtil.tuple(fromData.second, inputStream, fromData.third);
		return header("content-type", "multipart/form-data; boundary=" + fromData.first);
	}

	/**
	 * 设置 文件，GET方法无效，一般用于上传，因为正常情况下使用并不会多，而判断控制流关闭会消耗资源,所以交由外部处理
	 *
	 * @param fileName    文件名
	 * @param inputStream 流
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil file(@NotNull String fileName, @NotNull InputStream inputStream) {
		return data("file", fileName, inputStream);
	}

	/**
	 * 设置用于此请求的 SOCKS 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil socks(@NotNull String proxyHost, int proxyPort) {
		return proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort)));
	}

	/**
	 * 设置用于此请求的 HTTP 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil proxy(@NotNull String proxyHost, int proxyPort) {
		return proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
	}

	/**
	 * 连接代理（ @NotNull  Proxy 代理）<br/>
	 * 设置用于此请求的代理
	 *
	 * @param proxy 要使用的代理
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil proxy(@NotNull Proxy proxy) {
		this.proxy = proxy;
		return this;
	}

	/**
	 * 连接方法（ Connection.Method方法）
	 * 设置要使用的请求方法，GET 或 POST。默认为 GET。
	 *
	 * @param method HTTP 请求方法
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil method(@NotNull Method method) {
		this.method = method;
		return this;
	}

	/**
	 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
	 * 默认情况下为false
	 *
	 * @param errorExit 启用错误退出
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil errorExit(boolean errorExit) {
		this.errorExit = errorExit;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry 重试次数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil retry(int retry) {
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
	@Contract(pure = true) public HttpsUtil retry(int retry, int millis) {
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
	@Contract(pure = true) public HttpsUtil retry(boolean unlimitedRetry) {
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
	@Contract(pure = true) public HttpsUtil retry(boolean unlimitedRetry, int millis) {
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
	@Contract(pure = true) public HttpsUtil retryStatusCodes(int... statusCode) {
		retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
		return this;
	}

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param retryStatusCodes 状态码列表
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpsUtil retryStatusCodes(List<Integer> retryStatusCodes) {
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
		return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body()) : null;
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
	 * @return Response
	 */
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
			String redirectUrl; // 修复重定向
			if (followRedirects && URIUtils.statusIsRedirect(conn.getResponseCode()) && !Judge.isEmpty(redirectUrl = conn.getHeaderField("location"))) {
				return executeProgram(redirectUrl);
			}
			return new Response(conn);
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
			((HttpsURLConnection) conn).setSSLSocketFactory(SSLSocketFactory.getSocketFactory());
			((HttpsURLConnection) conn).setHostnameVerifier((arg0, arg1) -> true);
		}

		// 设置通用的请求属性
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			conn.setRequestProperty(entry.getKey(), entry.getValue());
		}
		return conn;
	}

	/**
	 * 响应接口
	 */
	public static class Response {
		protected HttpURLConnection conn; // HttpURLConnection对象
		protected Charset charset = StandardCharsets.UTF_8;

		/**
		 * Constructor for the HttpsResult.
		 *
		 * @param conn HttpURLConnection
		 */
		protected Response(HttpURLConnection conn) {
			this.conn = conn;
		}

		/**
		 * 返回此页面的 URL
		 *
		 * @return 此页面的 URL
		 */
		@Contract(pure = true) public String url() {
			return conn.getURL().toExternalForm();
		}

		/**
		 * 获取 HttpURLConnection
		 *
		 * @return HttpURLConnection
		 */
		@Contract(pure = true) public HttpURLConnection connection() {
			return conn;
		}

		/**
		 * 获取 请求响应代码
		 *
		 * @return 请求响应代码
		 */
		@Contract(pure = true) public int statusCode() {
			try {
				return conn.getResponseCode();
			} catch (IOException e) {
				return HttpStatus.SC_REQUEST_TIMEOUT;
			}
		}

		/**
		 * 获取 请求头
		 *
		 * @return 请求头
		 */
		@Contract(pure = true) public Map<String, String> headers() {
			return conn.getHeaderFields().entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, stringListEntry -> stringListEntry.getValue().toString()));
		}

		/**
		 * 获取 请求头的值
		 *
		 * @return 请求头的值
		 */
		@Contract(pure = true) public String header(@NotNull String name) {
			return conn.getHeaderField(name);
		}

		/**
		 * 获取 cookies
		 *
		 * @return cookies
		 */
		@Contract(pure = true) public Map<String, String> cookies() {
			Map<String, String> cookies = new HashMap<>();
			for (String str : conn.getHeaderFields().get("set-cookie")) {
				String[] cookie = str.split("=");
				cookies.put(cookie[0], Judge.isEmpty(cookie[1]) ? "" : cookie[1]);
			}
			return cookies;
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
		 * 获取 响应正文
		 *
		 * @return 响应正文
		 */
		@Contract(pure = true) public String body() {
			String result;
			try (InputStream inputStream = bodyStream()) {
				result = StreamUtils.stream(inputStream).charset(charset).getString();
			} catch (IOException e) {
				return null;
			}
			return result;
		}

		/**
		 * 获取 响应流
		 *
		 * @return 响应流
		 */
		@Contract(pure = true) public InputStream bodyStream() throws IOException {
			return URIUtils.statusIsOK(statusCode()) ? conn.getInputStream() : conn.getErrorStream();
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

		/**
		 * 设置 响应流字符集格式
		 *
		 * @param charsetName 字符集格式名称
		 * @return this
		 */
		@Contract(pure = true) public Response charset(@NotNull String charsetName) {
			return charset(Charset.forName(charsetName));
		}

		/**
		 * 设置 响应流字符集格式
		 *
		 * @param charset 字符集格式
		 * @return this
		 */
		@Contract(pure = true) public Response charset(@NotNull Charset charset) {
			this.charset = charset;
			return this;
		}

	}

}