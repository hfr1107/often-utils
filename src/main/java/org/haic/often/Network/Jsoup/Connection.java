package org.haic.often.Network.Jsoup;

import org.haic.often.Network.Method;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Connection 接口是一个方便的 HTTP 客户端和会话对象，用于从 Web 获取内容，并将它们解析为 Documents。
 * 要启动新会话，请使用Jsoup.newSession()或Jsoup.connect(String) 。 Connections 包含Connection.Request和Connection.Response对象（一旦执行）。默认情况下，在会话上设置的配置设置（URL、超时、用户代理等）将应用于每个后续请求。
 * 要从会话开始新请求，请使用newRequest() 。
 * Cookie 在会话期间存储在内存中。出于这个原因，不要对长期存在的应用程序中的所有请求使用一个会话，否则您可能会耗尽内存，除非注意清理 cookie 存储。会话的 cookie 存储可通过cookieStore()获得。您可以在发出请求之前通过cookieStore(java.net.CookieStore)提供您自己的实现。
 * 可以使用 Connection 中的快捷方法（例如userAgent(String) ）或直接通过 Connection.Request 对象中的方法进行请求配置。所有请求配置必须在请求执行之前进行。当用作正在进行的会话时，在进行多线程newRequest()之前初始化所有默认值。
 * 请注意，此处使用的术语“连接”并不意味着在连接对象的生命周期内与服务器保持长期连接。套接字连接仅在请求执行（ execute() 、 get()或post() ）时建立，并消耗服务器的响应。
 * 对于多线程实现，对每个请求使用newRequest()很重要。会话可以跨线程共享，但不是给定请求。
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/16 7:20
 */
public interface Connection {
	/**
	 * 连接url （ 字符串 url）
	 * <p>
	 * 设置要获取的请求 URL。协议必须是 HTTP 或 HTTPS。
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	Connection url(URL url);

	/**
	 * 连接url （ 字符串 url）
	 * <p>
	 * 设置要获取的请求 URL。协议必须是 HTTP 或 HTTPS。
	 *
	 * @param url 要连接的 URL
	 * @return 此连接，用于链接
	 */
	Connection url(String url);

	/**
	 * 设置连接请求
	 *
	 * @param request 新的请求对象
	 * @return 此连接，用于链接
	 */
	Connection request(org.jsoup.Connection.Request request);

	/**
	 * 设置授权码或身份识别标识<br/>
	 * 有些服务器不使用cookie验证身份,使用authorization进行验证<br/>
	 * 一般信息在cookie或local Storage中存储
	 *
	 * @param auth 授权码或身份识别标识
	 * @return 此连接，用于链接
	 */
	Connection authorization(String auth);

	/**
	 * 设置用于此请求的 SOCKS 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	Connection socks(String proxyHost, int proxyPort);

	/**
	 * 设置用于此请求的 HTTP 代理
	 *
	 * @param proxyHost 代理主机名
	 * @param proxyPort 代理端口
	 * @return 此连接，用于链接
	 */
	Connection proxy(String proxyHost, int proxyPort);

	/**
	 * 连接代理（ @NotNull  Proxy 代理）<br/>
	 * 设置用于此请求的代理
	 *
	 * @param proxy 要使用的代理
	 * @return 此连接，用于链接
	 */
	Connection proxy(Proxy proxy);

	/**
	 * 连接用户代理（ 字符串 用户代理）<br/>
	 * 设置请求用户代理标头
	 *
	 * @param userAgent 要使用的用户代理
	 * @return 此连接，用于链接
	 */
	Connection userAgent(String userAgent);

	/**
	 * 添加请求头user-agent，以移动端方式访问页面
	 *
	 * @param isPhone true or false
	 * @return 此连接，用于链接
	 */
	Connection isPhone(boolean isPhone);

	/**
	 * 设置总请求超时时间，连接超时（ int millis）<br/>
	 * 默认超时为 0，超时为零被视为无限超时<br/>
	 * 请注意，此超时指定连接时间的组合最大持续时间和读取完整响应的时间
	 *
	 * @param millis 超时连接或读取之前的毫秒数（千分之一秒）
	 * @return 此连接，用于链接
	 */
	Connection timeout(int millis);

	/**
	 * 连接maxBodySize （ int bytes）<br/>
	 * 设置在连接关闭之前从（未压缩的）连接读取到正文的最大字节数，并且输入被截断（即正文内容将被修剪）<br/>
	 * 默认值为 0，最大大小0被视为无限量（仅受您的耐心和机器上可用内存的限制）
	 *
	 * @param bytes 截断前从输入读取的字节数
	 * @return 此连接，用于链接
	 */
	Connection maxBodySize(int bytes);

	/**
	 * 连接引荐来源网址（ 字符串 引荐来源网址）<br/>
	 * 设置请求引荐来源网址（又名“引荐来源网址”）标头
	 *
	 * @param referrer 要使用的来源网址
	 * @return 此连接，用于链接
	 */
	Connection referrer(String referrer);

	/**
	 * 连接followRedirects （布尔followRedirects）<br/>
	 * 将连接配置为（不）遵循服务器重定向，默认情况下这是true
	 *
	 * @param followRedirects 如果应该遵循服务器重定向，则为 true
	 * @return 此连接，用于链接
	 */
	Connection followRedirects(boolean followRedirects);

	/**
	 * 连接sslSocketFactory （ SSLSocketFactory  sslSocketFactory）<br/>
	 * 设置自定义 SSL 套接字工厂
	 *
	 * @param sslSocketFactory 自定义 SSL 套接字工厂
	 * @return 此连接，用于链接
	 */
	Connection sslSocketFactory(SSLSocketFactory sslSocketFactory);

	/**
	 * 连接数据（ String  ... keyvals）
	 * 添加一个或多个请求key, val数据参数对。
	 * 可以一次设置多个参数，例如： .data("name", "jsoup", "language", "Java", "language", "English");创建一个查询字符串，如： ?name=jsoup&language=Java&language=English
	 * 对于 GET 请求，将在请求查询字符串上发送数据参数。对于 POST（和其他包含正文的方法），它们将作为正文形式参数发送，除非正文由requestBody(String)显式设置，在这种情况下，它们将是查询字符串参数。
	 *
	 * @param keyvals 一组键值对
	 * @return 此连接，用于链接
	 */
	Connection data(String... keyvals);

	/**
	 * 连接数据（ 字符串 键、 字符串 值）<br/>
	 * 添加请求数据参数。请求参数在 GET 的请求查询字符串中发送，在 POST 的请求正文中发送。一个请求可能有多个同名的值。
	 *
	 * @param key   数据键
	 * @param value 数据值
	 * @return 此连接，用于链接
	 */
	Connection data(String key, String value);

	/**
	 * 将所有提供的数据添加到请求数据参数
	 *
	 * @param data 请求数据参数
	 * @return 此连接，用于链接
	 */
	Connection data(Map<String, String> data);

	/**
	 * 添加输入流作为请求数据参数，对于 GET 没有效果，但对于 POST 这将上传输入流
	 *
	 * @param key         数据键（表单项名称）
	 * @param fileName    要呈现给删除服务器的文件的名称。通常只是名称，而不是路径，组件
	 * @param inputStream 要上传的输入流，您可能从FileInputStream获得。您必须在finally块中关闭 InputStream
	 * @return 此连接，用于链接
	 */
	Connection data(String key, String fileName, InputStream inputStream);

	/**
	 * 连接数据（ String  key、 String  filename、 InputStream  inputStream、 String  contentType）
	 * <p>
	 * 添加输入流作为请求数据参数。对于 GET，没有效果，但对于 POST，这将上传输入流。
	 *
	 * @param key         数据键（表单项名称）
	 * @param fileName    要呈现给删除服务器的文件的名称。通常只是名称，而不是路径，组件。
	 * @param inputStream 要上传的输入流，您可能从FileInputStream获得。
	 * @param contentType 要为此文件指定的内容类型（又名 mimetype）。您必须在finally块中关闭 InputStream。
	 * @return 这个连接，用于链接
	 */
	Connection data(String key, String fileName, InputStream inputStream, String contentType);

	/**
	 * 设置 文件，GET方法无效，一般用于上传，因为正常情况下使用并不会多，而判断控制流关闭会消耗资源,所以交由外部处理
	 *
	 * @param fileName    文件名
	 * @param inputStream 流
	 * @return 此连接，用于链接
	 */
	Connection file(String fileName, InputStream inputStream);

	/**
	 * 设置 POST（或 PUT）请求正文<br/>
	 * 当服务器需要一个普通的请求正文，而不是一组 URL 编码形式的键/值对时很有用<br/>
	 * 一般为JSON格式,若不是则作为普通数据发送
	 *
	 * @param body 请求正文
	 * @return 此连接，用于链接
	 */
	Connection requestBody(String body);

	/**
	 * 连接头（ 字符串 名称， 字符串 值）<br/>
	 * 设置请求标头
	 *
	 * @param name  标题名称
	 * @param value 标头值
	 * @return 此连接，用于链接
	 */
	Connection header(String name, String value);

	/**
	 * 连接头（ Map  < String  , String  > 头）<br/>
	 * 将每个提供的标头添加到请求中
	 *
	 * @param headers 标头名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	Connection headers(Map<String, String> headers);

	/**
	 * 设置要在请求中发送的 cookie
	 *
	 * @param name  cookie 的名称
	 * @param value cookie 的值
	 * @return 此连接，用于链接
	 */
	Connection cookie(String name, String value);

	/**
	 * 连接 cookies （ Map < String  , String  >cookies）<br/>
	 * 将每个提供的 cookie 添加到请求中
	 *
	 * @param cookies 名称映射 -> 值对
	 * @return 此连接，用于链接
	 */
	Connection cookies(Map<String, String> cookies);

	/**
	 * 连接cookieStore （ CookieStore  cookieStore）<br/>
	 * 提供一个自定义或预填充的 CookieStore，用于此 Connection 发出的请求。
	 *
	 * @param cookieStore 用于后续请求的 cookie 存储
	 * @return 此连接，用于链接
	 */
	Connection cookieStore(CookieStore cookieStore);

	/**
	 * CookieStore 对象表示 cookie 的存储。可以存储和检索cookies。<br/>
	 * CookieManager将调用CookieStore.add为每个传入的 HTTP 响应保存 cookie，并调用CookieStore.get为每个传出的 HTTP 请求检索 cookie。 CookieStore 负责删除已过期的 HttpCookie 实例。
	 *
	 * @return 接口 Connection 中的 cookieStore
	 */
	CookieStore cookieStore();

	/**
	 * 连接解析器（ Parser parser）<br/>
	 * 在解析对文档的响应时提供备用解析器。如果未设置，则默认使用 HTML 解析器，除非响应内容类型是 XML，在这种情况下使用 XML 解析器。
	 *
	 * @param parser 备用解析器
	 * @return 此连接，用于链接
	 */
	Connection parser(Parser parser);

	/**
	 * 连接postDataCharset （ 字符串 字符集）<br/>
	 * 为 x-www-form-urlencoded 发布数据设置默认发布数据字符集
	 *
	 * @param charset 用于编码帖子数据的字符集
	 * @return 此连接，用于链接
	 */
	Connection postDataCharset(String charset);

	/**
	 * 连接方法（ Connection.Method方法）
	 * 设置要使用的请求方法，GET 或 POST。默认为 GET。
	 *
	 * @param method HTTP 请求方法
	 * @return 此连接，用于链接
	 */
	Connection method(Method method);

	/**
	 * 连接newRequest () <br/>
	 * 创建一个新请求，使用此 Connection 作为会话状态并初始化连接设置（然后可以独立于返回的 Connection.Request 对象）。
	 *
	 * @return 一个新的 Connection 对象，具有共享的 Cookie 存储和来自此 Connection 和 Request 的初始化设置
	 */
	Connection newRequest();

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry 重试次数
	 * @return 此连接，用于链接
	 */
	Connection retry(int retry);

	/**
	 * 在请求超时或者指定状态码发生时，进行重试，重试超过次数或者状态码正常返回
	 *
	 * @param retry  重试次数
	 * @param millis 重试等待时间(毫秒)
	 * @return this
	 */
	Connection retry(int retry, int millis);

	/**
	 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
	 *
	 * @param unlimitedRetry 启用无限重试, 默认false
	 * @return 此连接，用于链接
	 */
	Connection retry(boolean unlimitedRetry);

	/**
	 * 在请求超时或者指定状态码发生时，无限进行重试，直至状态码正常返回
	 *
	 * @param unlimitedRetry 启用无限重试, 默认false
	 * @param millis         重试等待时间(毫秒)
	 * @return 此连接，用于链接
	 */
	Connection retry(boolean unlimitedRetry, int millis);

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param statusCode 状态码
	 * @return 此连接，用于链接
	 */
	Connection retryStatusCodes(int... statusCode);

	/**
	 * 额外指定错误状态码码，在指定状态发生时，也进行重试，可指定多个
	 *
	 * @param statusCodes 状态码列表
	 * @return 此连接，用于链接
	 */
	Connection retryStatusCodes(List<Integer> statusCodes);

	/**
	 * 在状态码不为200+或300+时，抛出执行异常，并获取一些参数，一般用于调试<br/>
	 * 默认情况下为false
	 *
	 * @param exit 启用错误退出
	 * @return 此连接，用于链接
	 */
	Connection errorExit(boolean exit);

	/**
	 * 将请求作为 GET 执行，并解析结果
	 *
	 * @return HTML文档
	 */
	Document get();

	/**
	 * 将请求作为 POST 执行，并解析结果
	 *
	 * @return HTML文档
	 */
	Document post();

	/**
	 * 运行程序，获取 响应结果
	 *
	 * @return 响应接口
	 */
	org.jsoup.Connection.Response execute();

}