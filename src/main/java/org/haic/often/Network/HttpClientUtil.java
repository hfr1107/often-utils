package org.haic.often.Network;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.haic.often.StreamUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HttpClient工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/2/20 9:51
 */
public class HttpClientUtil {
	protected String url; // URL
	protected String requestBody;
	protected int retry; // 请求异常重试次数
	protected int MILLISECONDS_SLEEP; // 重试等待时间
	protected int timeout; // 连接超时时间
	protected boolean unlimitedRetry;// 请求异常无限重试
	protected boolean errorExit; // 错误退出
	protected boolean followRedirects = true; // 重定向
	protected boolean isSocksProxy; // 是否Socks代理
	protected HttpHost proxy;

	protected Map<String, String> headers = new HashMap<>(); // 请求头
	protected List<Integer> retryStatusCodes = new ArrayList<>();
	protected List<NameValuePair> params = new ArrayList<>();
	protected HttpClientContext context = HttpClientContext.create();
	protected CloseableHttpClient httpclient;
	protected HttpEntity entity;

	protected HttpClientUtil() {
	}

	/**
	 * 公共静态连接连接（ 字符串 网址）<br/>
	 * 使用定义的请求 URL 创建一个新的Connection （会话），用于获取和解析 HTML 页面
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static HttpClientUtil connect(@NotNull String url) {
		return config().url(url);
	}

	/**
	 * 获取新的连接对象
	 *
	 * @return 新的连接，用于链接
	 */
	@Contract(pure = true) protected static HttpClientUtil config() {
		return new HttpClientUtil();
	}

	/**
	 * 设置要获取的请求 URL，协议必须是 HTTP 或 HTTPS
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) protected HttpClientUtil url(@NotNull String url) {
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
	@Contract(pure = true) public HttpClientUtil requestBody(@NotNull String requestBody) {
		try {
			entity = new StringEntity(requestBody);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
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
	@Contract(pure = true) public HttpClientUtil userAgent(@NotNull String userAgent) {
		return header("user-agent", userAgent);
	}

	/**
	 * 添加请求头user-agent，以移动端方式访问页面
	 *
	 * @param isPhone true or false
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil isPhone(boolean isPhone) {
		return isPhone ? userAgent(UserAgent.chromeAsPhone()) : userAgent(UserAgent.chrome());
	}

	/**
	 * 连接followRedirects （布尔followRedirects）<br/>
	 * 将连接配置为（不）遵循服务器重定向，默认情况下这是true
	 *
	 * @param followRedirects 如果应该遵循服务器重定向，则为 true
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil followRedirects(boolean followRedirects) {
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
	@Contract(pure = true) public HttpClientUtil referrer(String referrer) {
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
	@Contract(pure = true) public HttpClientUtil authorization(@NotNull String authorization) {
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
	@Contract(pure = true) public HttpClientUtil timeout(int millis) {
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
	@Contract(pure = true) public HttpClientUtil header(@NotNull String name, @NotNull String value) {
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
	@Contract(pure = true) public HttpClientUtil headers(@NotNull Map<String, String> headers) {
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
	@Contract(pure = true) public HttpClientUtil cookie(@NotNull String name, @NotNull String value) {
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
	@Contract(pure = true) public HttpClientUtil cookies(@NotNull Map<String, String> cookies) {
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
	@Contract(pure = true) public HttpClientUtil data(@NotNull String key, @NotNull String value) {
		this.params.add(new BasicNameValuePair(key, value));
		return this;
	}

	/**
	 * 将所有提供的数据添加到请求数据参数
	 *
	 * @param params 请求数据参数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil data(@NotNull Map<String, String> params) {
		this.params = params.entrySet().stream().map(l -> new BasicNameValuePair(l.getKey(), l.getValue())).collect(Collectors.toList());
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
	@Contract(pure = true) public HttpClientUtil data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
		String boundary = UUID.randomUUID().toString();
		entity = MultipartEntityBuilder.create().addBinaryBody(key, inputStream, ContentType.MULTIPART_FORM_DATA, fileName).setBoundary(boundary).build();
		return header("content-type", "multipart/form-data; boundary=" + boundary);
	}

	/**
	 * 设置 文件，GET方法无效，一般用于上传，因为正常情况下使用并不会多，而判断控制流关闭会消耗资源,所以交由外部处理
	 *
	 * @param fileName    文件名
	 * @param inputStream 流
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil file(@NotNull String fileName, @NotNull InputStream inputStream) {
		return data("file", fileName, inputStream);
	}

	/**
	 * 设置用于此请求的 SOCKS 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil socks(@NotNull String proxyHost, int proxyPort) {
		httpclient = HttpClientHelper.createClient();
		this.isSocksProxy = true;
		this.context.setAttribute("socks.address", new InetSocketAddress(proxyHost, proxyPort));
		return this;
	}

	/**
	 * 设置用于此请求的 HTTP 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil proxy(@NotNull String proxyHost, int proxyPort) {
		return proxy(new HttpHost(proxyHost, proxyPort, "HTTP"));
	}

	/**
	 * 连接代理（ @NotNull  HttpHost 代理）<br/>
	 * 设置用于此请求的代理
	 *
	 * @param proxy 要使用的代理
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil proxy(@NotNull HttpHost proxy) {
		if (proxy.getSchemeName().equals("socks")) {
			return socks(proxy.getHostName(), proxy.getPort());
		}
		httpclient = HttpClients.createDefault();
		this.isSocksProxy = false;
		this.proxy = proxy;
		return this;
	}

	/**
	 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
	 * 默认情况下为false
	 *
	 * @param errorExit 启用错误退出
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil errorExit(boolean errorExit) {
		this.errorExit = errorExit;
		return this;
	}

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry 重试次数
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public HttpClientUtil retry(int retry) {
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
	@Contract(pure = true) public HttpClientUtil retry(int retry, int millis) {
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
	@Contract(pure = true) public HttpClientUtil retry(boolean unlimitedRetry) {
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
	@Contract(pure = true) public HttpClientUtil retry(boolean unlimitedRetry, int millis) {
		this.unlimitedRetry = unlimitedRetry;
		this.MILLISECONDS_SLEEP = millis;
		return this;
	}

	/**
	 * 额外指定错误状态码码,在指定状态发生时,也进行重试,可指定多个
	 *
	 * @param statusCode 状态码
	 * @return this
	 */
	@Contract(pure = true) public HttpClientUtil retryStatusCodes(int... statusCode) {
		retryStatusCodes = Arrays.stream(statusCode).boxed().toList();
		return this;
	}

	/**
	 * 额外指定错误状态码码,在指定状态发生时,也进行重试,可指定多个
	 *
	 * @param retryStatusCodes 状态码列表
	 * @return this
	 */
	@Contract(pure = true) public HttpClientUtil retryStatusCodes(List<Integer> retryStatusCodes) {
		this.retryStatusCodes = retryStatusCodes;
		return this;
	}

	/**
	 * 运行程序，获取 Document 结果
	 *
	 * @return 响应结果
	 */
	@Contract(pure = true) public Document post() {
		return get(Method.POST);
	}

	/**
	 * 运行程序，获取 Document 结果
	 *
	 * @return 响应结果
	 */
	@Contract(pure = true) public Document get() {
		return get(Method.GET);
	}

	/**
	 * 运行程序，获取 Document 结果
	 *
	 * @param method 请求方法 HttpMethod
	 * @return 响应结果
	 */
	@Contract(pure = true) public Document get(@NotNull Method method) {
		Response response = execute(method);
		return URIUtils.statusIsNormal(response.statusCode()) ? Jsoup.parse(response.body()) : null;
	}

	/**
	 * 运行程序，获取 响应对象
	 *
	 * @return Response
	 */
	@Contract(pure = true) public Response execute() {
		return execute(Method.GET);
	}

	/**
	 * 运行程序，获取 响应对象
	 *
	 * @param method Method类型
	 * @return Response
	 */
	@Contract(pure = true) public Response execute(@NotNull Method method) {
		HttpUriRequest request = null;
		try {
			URI builder = new URIBuilder(url).setParameters(params).build();
			RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(followRedirects).setConnectTimeout(timeout).setProxy(proxy).build();
			entity = Judge.isNull(entity) ? new UrlEncodedFormEntity(params) : entity;
			switch (method) {
			case GET -> request = new HttpGet(builder) {{
				setConfig(requestConfig);
			}};
			case POST -> request = new HttpPost(builder) {{
				setConfig(requestConfig);
				setEntity(entity);
			}};
			case PUT -> request = new HttpPut(builder) {{
				setConfig(requestConfig);
				setEntity(entity);
			}};
			case DELETE -> request = new HttpDelete(builder) {{
				setConfig(requestConfig);
			}};
			case HEAD -> request = new HttpHead(builder) {{
				setConfig(requestConfig);
			}};
			case OPTIONS -> request = new HttpOptions(builder) {{
				setConfig(requestConfig);
			}};
			case PATCH -> request = new HttpPatch(builder) {{
				setConfig(requestConfig);
				setEntity(entity);
			}};
			case TRACE -> request = new HttpTrace(builder) {{
				setConfig(requestConfig);
			}};
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// 设置通用的请求属性
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			request.setHeader(entry.getKey(), entry.getValue());
		}

		httpclient = Judge.isNull(httpclient) ? HttpClients.createDefault() : httpclient;

		Response response = executeProgram(request);
		int statusCode = Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.statusCode();
		for (int i = 0; (URIUtils.statusIsTimeout(statusCode) || retryStatusCodes.contains(statusCode)) && (i < retry || unlimitedRetry); i++) {
			MultiThreadUtils.WaitForThread(MILLISECONDS_SLEEP); // 程序等待
			response = executeProgram(request);
			statusCode = Judge.isNull(response) ? statusCode : response.statusCode();
		}
		if (errorExit && !URIUtils.statusIsNormal(statusCode)) {
			throw new RuntimeException("连接URL失败，状态码: " + statusCode + " URL: " + url);
		}
		return response;
	}

	@Contract(pure = true) protected Response executeProgram(@NotNull HttpUriRequest request) {
		CloseableHttpResponse response;
		try {
			response = httpclient.execute(request, context);
		} catch (IOException e) {
			return null;
		}
		return new Response(request, response, context);
	}

	/**
	 * 实现socks代理 HttpClient 类
	 */
	public static class HttpClientHelper {
		/**
		 * 创建 支持socks代理 CloseableHttpClient 实例
		 *
		 * @return CloseableHttpClient实例
		 */
		public static CloseableHttpClient createClient() {
			return HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager(
					RegistryBuilder.<ConnectionSocketFactory>create().register("http", new MyConnectionSocketFactory())
							.register("https", new MySSLConnectionSocketFactory()).build())).build();
		}

		/**
		 * 实现 http 链接的socket 工厂
		 */
		private static class MyConnectionSocketFactory extends PlainConnectionSocketFactory {
			@Override public Socket createSocket(HttpContext context) {
				return new Socket(new Proxy(Proxy.Type.SOCKS, (InetSocketAddress) context.getAttribute("socks.address")));
			}
		}

		/**
		 * 实现 https 链接的socket 工厂
		 */
		private static class MySSLConnectionSocketFactory extends SSLConnectionSocketFactory {
			public MySSLConnectionSocketFactory() {
				super(SSLContexts.createDefault(), getDefaultHostnameVerifier());
			}

			@Override public Socket createSocket(HttpContext context) {
				return new Socket(new Proxy(Proxy.Type.SOCKS, (InetSocketAddress) context.getAttribute("socks.address")));
			}
		}
	}

	/**
	 * 响应接口
	 */
	public static class Response {
		protected CloseableHttpResponse response;
		protected HttpUriRequest request;
		protected HttpClientContext context;
		protected Charset charset = StandardCharsets.UTF_8;

		/**
		 * Constructor for the HttpsClientResult.
		 *
		 * @param request  HttpUriRequest
		 * @param response CloseableHttpResponse
		 * @param context  HttpClientContext
		 */
		protected Response(HttpUriRequest request, CloseableHttpResponse response, HttpClientContext context) {
			this.request = request;
			this.response = response;
			this.context = context;
		}

		/**
		 * 返回此页面的 URL
		 *
		 * @return 此页面的 URL
		 */
		@Contract(pure = true) public String url() {
			List<URI> reLocs = context.getRedirectLocations();
			return Judge.isNull(reLocs) ? request.getURI().toString() : reLocs.get(reLocs.size() - 1).toString();
		}

		/**
		 * 获取 请求响应代码
		 *
		 * @return 请求响应代码
		 */
		@Contract(pure = true) public int statusCode() {
			return response.getStatusLine().getStatusCode();
		}

		/**
		 * 获取 请求头
		 *
		 * @return 请求头
		 */
		@Contract(pure = true) public Map<String, String> headers() {
			Map<String, String> headers = new HashMap<>();
			for (Header header : response.getAllHeaders()) {
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
		 * 获取 请求头的值
		 *
		 * @return 请求头的值
		 */
		@Contract(pure = true) public String header(@NotNull String name) {
			String header = headers().get(name);
			header = header.startsWith("[") ? header.substring(1) : header;
			return header.endsWith("]") ? header.substring(0, header.length() - 1) : header;
		}

		/**
		 * 获取 cookies
		 *
		 * @return cookies
		 */
		@Contract(pure = true) public Map<String, String> cookies() {
			return Arrays.stream(response.getHeaders("Set-Cookie")).map(l -> l.getValue().split("="))
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
			return response.getEntity().getContent();
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