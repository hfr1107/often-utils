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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
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

	protected HttpClientUtil() {
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
		protected String requestBody;
		protected String auth; // 身份识别标识
		protected int retry; // 请求异常重试次数
		protected int MILLISECONDS_SLEEP; // 重试等待时间
		protected int timeout; // 连接超时时间
		protected boolean unlimitedRetry;// 请求异常无限重试
		protected boolean errorExit; // 错误退出
		protected boolean followRedirects = true; // 重定向
		protected HttpHost proxy;
		protected Method method = Method.GET;
		protected Parser parser = Parser.htmlParser();

		protected Map<String, String> headers = new HashMap<>(); // 请求头
		protected Map<String, String> cookies = new HashMap<>(); // 请求头
		protected List<Integer> retryStatusCodes = new ArrayList<>();
		protected List<NameValuePair> params = new ArrayList<>();
		protected HttpClientContext context = HttpClientContext.create();
		protected CloseableHttpClient httpclient;
		protected HttpClientBuilder httpClientBuilder = HttpClients.custom();
		protected HttpEntity entity;

		protected HttpConnection(@NotNull String url) {
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
			requestBody = "";
			params = new ArrayList<>();
			headers = new HashMap<>();
			method = Method.GET;
			initialization("");
			return Judge.isEmpty(auth) ? this : authorization(auth);
		}

		@Contract(pure = true) public Connection sslSocketFactory(SSLContext sslSocket) {
			httpClientBuilder.setSSLContext(IgnoreSSLSocket.MyX509TrustManager());
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

		@Contract(pure = true) public Connection authorization(@NotNull String auth) {
			return header("authorization", (this.auth = auth.startsWith("Bearer ") ? auth : "Bearer " + auth));
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
			this.cookies.put(name, value);
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
			this.params.add(new BasicNameValuePair(key, value));
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull Map<String, String> params) {
			this.params = params.entrySet().stream().map(l -> new BasicNameValuePair(l.getKey(), l.getValue())).collect(Collectors.toList());
			return this;
		}

		@Contract(pure = true) public Connection data(@NotNull String key, @NotNull String fileName, @NotNull InputStream inputStream) {
			String boundary = UUID.randomUUID().toString();
			entity = MultipartEntityBuilder.create().addBinaryBody(key, inputStream, ContentType.MULTIPART_FORM_DATA, fileName).setBoundary(boundary).build();
			return header("content-type", "multipart/form-data; boundary=" + boundary);
		}

		@Contract(pure = true) public Connection file(@NotNull String fileName, @NotNull InputStream inputStream) {
			return data("file", fileName, inputStream);
		}

		@Contract(pure = true) public Connection requestBody(@NotNull String body) {
			try {
				entity = new StringEntity(body);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			header("accept", "application/json;charset=UTF-8");
			return URIUtils.isJson(body) ?
					header("content-type", "application/json;charset=UTF-8") :
					header("content-type", "application/x-www-form-urlencoded;charset=UTF-8");
		}

		@Contract(pure = true) public Connection socks(@NotNull String proxyHost, int proxyPort) {
			httpClientBuilder = httpClientBuilder.setConnectionManager(HttpClientHelper.PoolingHttpClientConnectionManager());
			this.context.setAttribute("socks.address", new InetSocketAddress(proxyHost, proxyPort));
			return this;
		}

		@Contract(pure = true) public Connection proxy(@NotNull String proxyHost, int proxyPort) {
			this.proxy = new HttpHost(proxyHost, proxyPort, "HTTP");
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
			return method(Method.GET).execute().parse();
		}

		@Contract(pure = true) public Document post() {
			return method(Method.POST).execute().parse();
		}

		@Contract(pure = true) public Response execute() {
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

			// 设置cookies
			request.setHeader("cookie", cookies.entrySet().stream().map(l -> l.getKey() + "=" + l.getValue()).collect(Collectors.joining("; ")));

			httpclient = Judge.isNull(httpclient) ? httpClientBuilder.build() : httpclient;

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
			Response httpHesponse = new HttpResponse(this, request, response);
			cookies.putAll(httpHesponse.cookies());
			return httpHesponse;
		}
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
			return HttpClients.custom().setConnectionManager(PoolingHttpClientConnectionManager()).build();
		}

		public static PoolingHttpClientConnectionManager PoolingHttpClientConnectionManager() {
			return new PoolingHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("http", new MyConnectionSocketFactory())
					.register("https", new MySSLConnectionSocketFactory()).build());
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
	 *
	 * @author haicdust
	 * @version 1.0
	 * @since 2022/3/16 10:33
	 */
	protected static class HttpResponse extends Response {
		protected HttpConnection conn;
		protected CloseableHttpResponse response;
		protected HttpUriRequest request;
		protected Charset charset = StandardCharsets.UTF_8;

		protected HttpResponse(HttpConnection conn, HttpUriRequest request, CloseableHttpResponse response) {
			this.conn = conn;
			this.request = request;
			this.response = response;
		}

		@Contract(pure = true) public String url() {
			List<URI> reLocs = conn.context.getRedirectLocations();
			return Judge.isNull(reLocs) ? request.getURI().toString() : reLocs.get(reLocs.size() - 1).toString();
		}

		@Contract(pure = true) public int statusCode() {
			return Judge.isNull(response) ? HttpStatus.SC_REQUEST_TIMEOUT : response.getStatusLine().getStatusCode();
		}

		@Contract(pure = true) public String statusMessage() {
			return response.getStatusLine().getReasonPhrase();
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

		@Contract(pure = true) public Response header(@NotNull String key, @NotNull String value) {
			conn.header(key, value);
			return this;
		}

		@Contract(pure = true) public Response removeHeader(@NotNull String key) {
			conn.headers.remove(key);
			return this;
		}

		@Contract(pure = true) public String cookie(@NotNull String name) {
			return cookies().get(name);
		}

		@Contract(pure = true) public Map<String, String> cookies() {
			Header[] cookies = response.getHeaders("Set-Cookie");
			return Judge.isEmpty(cookies.length) ?
					new HashMap<>() :
					Arrays.stream(cookies).map(l -> l.getValue().substring(0, l.getValue().indexOf(";")))
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
			return response.getEntity().getContentType().getValue();
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

		@Contract(pure = true) public InputStream bodyStream() throws IOException {
			return response.getEntity().getContent();
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

}