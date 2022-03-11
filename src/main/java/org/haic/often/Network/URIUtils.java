package org.haic.often.Network;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpStatus;
import org.haic.often.Base64Utils;
import org.haic.often.Judge;
import org.haic.often.RunTime;
import org.haic.often.StringUtils;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.TupleUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * URI工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/18 17:13
 */
public class URIUtils {

	/**
	 * 根据输入的信息返回multipart/form-data表单数据,一般用于文件上传
	 *
	 * @param key      数据键（表单项名称）
	 * @param fileName 要呈现给删除服务器的文件的名称。通常只是名称，而不是路径，组件
	 * @return ThreeTuple ( String, String, String )<br/>
	 * first - boundary<br/>
	 * second - 表单body头部数据<br/>
	 * third - 表单body尾部数据
	 */
	@Contract(pure = true) public static ThreeTuple<String, String, String> getFormData(@NotNull String key, @NotNull String fileName) {
		String boundary = UUID.randomUUID().toString();
		return TupleUtil.tuple(boundary, "\r\n--" + boundary + "\r\n" + "content-disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName
				+ "\"\r\ncontent-type: application/octet-stream; charset=utf-8\r\n\r\n", "\r\n--" + boundary + "--\r\n");
	}

	/**
	 * 创建一个URI对象
	 *
	 * @param url URL
	 * @return URI对象
	 */
	@Contract(pure = true) public static URI getURI(@NotNull String url) {
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return uri;
	}

	/**
	 * 获取URL
	 *
	 * @param url URL
	 * @return URL对象
	 */
	@Contract(pure = true) public static URL getURL(@NotNull String url) {
		URL uri = null;
		try {
			uri = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return uri;
	}

	/**
	 * 获取域名
	 *
	 * @param url URL
	 * @return 字符串
	 */
	@NotNull @Contract(pure = true) public static String getDomain(@NotNull String url) {
		return getURI(url).getHost();
	}

	/**
	 * 判断连接是否正常,状态码200+或300+
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsNormal(int statusCode) {
		return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_BAD_REQUEST;
	}

	/**
	 * 判断连接是否成功,状态码200+
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsOK(int statusCode) {
		return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES;
	}

	/**
	 * 判断连接是否超时,或中断,状态码0或408
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsTimeout(int statusCode) {
		return statusCode == HttpStatus.SC_REQUEST_TIMEOUT || Judge.isEmpty(statusCode);
	}

	/**
	 * 判断连接是否重定向,状态码300+
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsRedirect(int statusCode) {
		return statusCode >= HttpStatus.SC_MULTIPLE_CHOICES && statusCode < HttpStatus.SC_BAD_REQUEST;
	}

	/**
	 * 判断连接是否请求错误,状态码400+(排除408)
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsError(int statusCode) {
		return statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode != HttpStatus.SC_REQUEST_TIMEOUT;
	}

	/**
	 * 判断连接是否请求错误,状态码500+
	 *
	 * @param statusCode 状态码
	 * @return 连接状态 boolean
	 */
	@Contract(pure = true) public static boolean statusIsServerError(int statusCode) {
		return statusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR;
	}

	/**
	 * 判断字符串是否是JSON
	 *
	 * @param str 字符串
	 * @return 判断后结果
	 */
	@Contract(pure = true) public static boolean isJson(@NotNull String str) {
		boolean result;
		try {
			JSONObject.parseObject(str);
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * CMD命令获取IP连接状态
	 *
	 * @param host 域名或IP
	 * @return 连接状态
	 */
	@Contract(pure = true) public static boolean pingIp(@NotNull String host) {
		return Judge.isEmpty(RunTime.dos("ping", host, "-n", "1", "-w", "5000").execute());
	}

	/**
	 * 获取HOST连接状态
	 *
	 * @param host 域名或IP
	 * @return 连接状态
	 */
	@Contract(pure = true) public static boolean pingHost(@NotNull String host) {
		return pingHost(host, 80);
	}

	/**
	 * 获取HOST连接状态
	 *
	 * @param host 域名或IP
	 * @param port 端口
	 * @return 连接状态
	 */
	@Contract(pure = true) public static boolean pingHost(@NotNull String host, int port) {
		boolean isReachable;
		try (Socket socket = new Socket()) {
			InetSocketAddress endpointSocketAddr = new InetSocketAddress(host, port);
			socket.connect(endpointSocketAddr, 5000);
			isReachable = socket.isConnected();
		} catch (IOException e) {
			isReachable = false;
		}
		return isReachable;
	}

	/**
	 * 获取 URL请求头Content-Disposition文件名属性
	 *
	 * @param disposition ontent-Disposition
	 * @return 文件名
	 */
	@Contract(pure = true) public static String getFileNameForDisposition(@NotNull String disposition) {
		String filename = disposition.substring(disposition.lastIndexOf("filename"));
		filename = filename.substring(filename.indexOf("=") + 1).replaceAll("\"", "");
		filename = filename.contains("'") ? filename.substring(filename.lastIndexOf("'") + 1) : filename;
		return StringUtils.decodeByURL(filename);
	}

	/**
	 * 迅雷磁链转换直链
	 *
	 * @param thunder 迅雷磁力链接
	 * @return URL直链
	 */
	@NotNull @Contract(pure = true) public static String thunderToURL(@NotNull String thunder) {
		String thunderUrl = Base64Utils.decryptByBase64(StringUtils.deleteSuffix(thunder, StringUtils.EQUAL_SIGN).replaceFirst("thunder://", ""), "GBK");
		return thunderUrl.substring(2, thunderUrl.length() - 2);
	}

	/**
	 * 获取 新浪微博临时访客Cookies
	 *
	 * @return 新浪微博Cookies
	 */
	@NotNull @Contract(pure = true) public static Map<String, String> getWeiBoCookies() {
		// API
		String ajaxUrl = "https://weibo.com/ajax/";
		String genvisitor = "https://passport.weibo.com/visitor/genvisitor";
		String visitor = "https://passport.weibo.com/visitor/visitor";

		Map<String, String> cookies = JsoupUtil.connect(ajaxUrl).execute().cookies(); // 获取XSRF-TOKEN

		String genvisitorInfo = JsoupUtil.connect(genvisitor).data("cb", "gen_callback").post().text();
		genvisitorInfo = genvisitorInfo.substring(genvisitorInfo.indexOf("{"), genvisitorInfo.lastIndexOf("}") + 1);
		String tid = JSONObject.parseObject(JSONObject.parseObject(genvisitorInfo).getString("data")).getString("tid");
		Map<String, String> visitorData = new HashMap<>();
		visitorData.put("a", "incarnate");
		visitorData.put("cb", "cross_domain");
		visitorData.put("from", "weibo");
		visitorData.put("t", tid);
		String visitorInfo = JsoupUtil.connect(visitor).data(visitorData).get().text();
		visitorInfo = visitorInfo.substring(visitorInfo.indexOf("{"), visitorInfo.lastIndexOf("}") + 1);

		JSONObject visitorInfoData = JSONObject.parseObject(JSONObject.parseObject(visitorInfo).getString("data"));
		cookies.put("SUB", visitorInfoData.getString("sub"));
		cookies.put("SUBP", visitorInfoData.getString("subp"));

		return cookies;
	}

}