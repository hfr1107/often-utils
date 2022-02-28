package org.haic.often.Network;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Base64Utils;
import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.haic.often.Multithread.MultiThreadUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Aria2 工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2020/2/18 18:43
 */
public class Aria2Util {

	protected final String jsonrpc = "2.0";
	protected String aria2RpcUrl;
	protected String token = ""; // 密钥
	protected Proxy proxy = Proxy.NO_PROXY;
	protected Map<String, Map<String, String>> urlsInfo = new HashMap<>();
	protected Map<String, String> mixinParams = new HashMap<>();

	/**
	 * 默认设置
	 */
	protected Aria2Util() {
	}

	/**
	 * aria2RpcUrl: http://localhost:6800/jsonrpc
	 *
	 * @return this
	 */
	@Contract(pure = true) public static Aria2Util connect() {
		return connect("localhost", 6800);
	}

	/**
	 * 设置 aria2RpcUrl: localhost:6800
	 *
	 * @param method URI类型
	 * @return this
	 */
	@Contract(pure = true) public static Aria2Util connect(@NotNull URIMethod method) {
		return connect(method, "localhost", 6800);
	}

	/**
	 * 设置 aria2RpcUrl
	 *
	 * @param host URL
	 * @param port 端口
	 * @return this
	 */
	@Contract(pure = true) public static Aria2Util connect(@NotNull String host, int port) {
		return connect(URIMethod.HTTP, host, port);
	}

	/**
	 * 设置 aria2RpcUrl
	 *
	 * @param method URI类型
	 * @param host   URL
	 * @param port   端口
	 * @return this
	 */
	@Contract(pure = true) public static Aria2Util connect(@NotNull URIMethod method, @NotNull String host, int port) {
		return config().setAria2RpcUrl(method.value + "://" + host + ":" + port + "/jsonrpc");
	}

	/**
	 * 获取新的 Aria2Utils 对象
	 *
	 * @return new Aria2Utils
	 */
	@Contract(pure = true) protected static Aria2Util config() {
		return new Aria2Util();
	}

	/**
	 * 设置 文件夹路径
	 *
	 * @param folderPath 文件夹路径
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util setFolderPath(@NotNull String folderPath) {
		this.setMiXinParams("dir", folderPath);
		return this;
	}

	/**
	 * 设置 公共参数
	 *
	 * @param name  key
	 * @param value value
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util setMiXinParams(@NotNull String name, @NotNull String value) {
		this.mixinParams.put(name, value);
		return this;
	}

	/**
	 * 设置 aria2RpcUrl
	 *
	 * @param aria2RpcUrl RpcUrl
	 * @return this
	 */
	@Contract(pure = true) protected Aria2Util setAria2RpcUrl(@NotNull String aria2RpcUrl) {
		this.aria2RpcUrl = aria2RpcUrl;
		return this;
	}

	/**
	 * 设置密钥
	 *
	 * @param token 密钥
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util setToken(@NotNull String token) {
		this.token = token;
		return this;
	}

	/**
	 * 设置Aria2代理
	 *
	 * @param proxyHost 代理URL
	 * @param proxyPort 代理端口
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util setProxy(@NotNull String proxyHost, int proxyPort) {
		this.mixinParams.put("all-proxy", proxyHost + ":" + proxyPort);
		return this;
	}

	/**
	 * 设置访问PRC接口代理
	 *
	 * @param proxyHost 代理地址
	 * @param proxyPort 代理端口
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util proxy(@NotNull String proxyHost, int proxyPort) {
		proxy = aria2RpcUrl.startsWith("http") ?
				new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)) :
				new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
		return this;
	}

	/**
	 * 添加 URL和参数
	 *
	 * @param url    URL
	 * @param params dir:文件夹路径 out:文件名 referer:上一页
	 * @return this
	 */

	@Contract(pure = true) public Aria2Util addUrl(@NotNull String url, @NotNull Map<String, String> params) {
		urlsInfo.put(url, params);
		return this;
	}

	/**
	 * 添加 Url or Magnet 数组
	 *
	 * @param urls URL数组
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addUrl(@NotNull List<String> urls) {
		urlsInfo.putAll(urls.stream().collect(Collectors.toMap(l -> l, l -> new HashMap<>())));
		return this;
	}

	/**
	 * 添加 Url or Magnet
	 *
	 * @param url 链接
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addUrl(@NotNull String url) {
		return addUrl(url, new HashMap<>());
	}

	/**
	 * 添加 Url or Magnet
	 *
	 * @param url      链接
	 * @param fileName 文件名
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addUrl(@NotNull String url, @NotNull String fileName) {
		return addUrl(url, fileName, "*");
	}

	/**
	 * 添加 Url or Magnet
	 *
	 * @param url      链接
	 * @param fileName 文件名
	 * @param referrer 上一页
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addUrl(@NotNull String url, @NotNull String fileName, @NotNull String referrer) {
		Map<String, String> params = new HashMap<>();
		fileName = FilesUtils.illegalFileName(fileName);
		if (fileName.length() > 240) {
			throw new RuntimeException("URL: " + url + " Error: File name length is greater than 240");
		}
		if (!Judge.isEmpty(fileName)) {
			params.put("out", fileName);
		}
		params.put("referer", referrer);
		this.addUrl(url, params);
		return this;
	}

	/**
	 * 添加 Torrent or Metalink 文件路径
	 *
	 * @param torrentPath 种子路径
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addTorrent(@NotNull String torrentPath) {
		this.addUrl(Base64Utils.encryptToBase64(torrentPath));
		return this;
	}

	/**
	 * 添加 Torrent or Metalink 文件路径数组
	 *
	 * @param torrentPath 种子路径列表
	 * @return this
	 */
	@Contract(pure = true) public Aria2Util addTorrent(@NotNull List<String> torrentPath) {
		for (String torrentpath : torrentPath) {
			this.addTorrent(torrentpath);
		}
		return this;
	}

	/**
	 * Socket推送 JSON数据
	 *
	 * @return 返回的json信息
	 */
	@Contract(pure = true) public String send() {
		final StringBuilder result = new StringBuilder();
		WebSocketClient socket = new WebSocketClient(URIUtils.getURI(aria2RpcUrl)) {
			@Override @Contract(pure = true) public void onOpen(ServerHandshake handshakedata) {
				send(getJsonArray().toJSONString());
			}

			@Override @Contract(pure = true) public void onMessage(String message) {
				result.append(message);
				close();
			}

			@Override @Contract(pure = true) public void onError(Exception e) {

			}

			@Override @Contract(pure = true) public void onClose(int code, String reason, boolean remote) {

			}
		};
		socket.setProxy(proxy);
		socket.connect();
		// 判断连接状态
		while (socket.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
			MultiThreadUtils.WaitForThread(100);
		}
		return String.valueOf(result);
	}

	/**
	 * GET请求 JSON数据
	 *
	 * @return result or webstatus
	 */
	@Contract(pure = true) public String get() {
		Response response = JsoupUtil.connect(aria2RpcUrl).data("params", Base64Utils.encryptToBase64(getJsonArray().toJSONString())).proxy(proxy).execute();
		int statusCode = Judge.isNull(response) ? 0 : response.statusCode();
		return URIUtils.statusIsOK(statusCode) ? response.body() : String.valueOf(statusCode);
	}

	/**
	 * POST请求 JSON数据
	 *
	 * @return result or webstatus
	 */
	@Contract(pure = true) public String post() {
		Response response = JsoupUtil.connect(aria2RpcUrl).header("Content-Type", "application/json;charset=UTF-8").requestBody(getJsonArray().toJSONString())
				.proxy(proxy).execute(Method.POST);
		int statusCode = Judge.isNull(response) ? 0 : response.statusCode();
		return URIUtils.statusIsOK(statusCode) ? response.body() : String.valueOf(statusCode);
	}

	/**
	 * 获取链接类型
	 *
	 * @param url 链接
	 * @return Aria2Method
	 */
	@Contract(pure = true) protected Aria2Method getType(@NotNull String url) {
		Aria2Method method = Aria2Method.ADD_URI;
		if (url.endsWith("torrent") || Base64Utils.isBase64(url)) {
			method = Aria2Method.ADD_TORRENT;
		} else if (url.endsWith("xml")) {
			method = Aria2Method.ADD_METALINK;
		}
		return method;
	}

	/***
	 * 获取 JSONArray
	 *
	 * @return JSONArray
	 */
	@Contract(pure = true) protected JSONArray getJsonArray() {
		JSONArray jsonArray = new JSONArray();
		for (Entry<String, Map<String, String>> urlinfo : urlsInfo.entrySet()) {
			String url = urlinfo.getKey();
			Map<String, String> params = urlinfo.getValue();
			if (!mixinParams.isEmpty()) {
				params.putAll(mixinParams);
			}
			jsonArray.add(getJsonObject(getType(url), url, params));
		}
		return jsonArray;
	}

	/**
	 * 获取Aria2 jsonArray对象
	 *
	 * @return JSONObject
	 */
	@Contract(pure = true) protected JSONObject getJsonObject(@NotNull Aria2Method method, @NotNull String url, @NotNull Map<String, String> params) {
		JSONArray jsonArray = new JSONArray();
		jsonArray.add("token:" + token);
		jsonArray.add(Collections.singletonList(url));
		jsonArray.add(params);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", UUID.randomUUID().toString());
		jsonObject.put("jsonrpc", jsonrpc);
		jsonObject.put("method", method.hasBody());
		jsonObject.fluentPut("params", jsonArray);
		return jsonObject;
	}

	/**
	 * 方法名常量
	 */
	public enum Aria2Method {
		ADD_URI("aria2.addUri"), ADD_TORRENT("aria2.addTorrent"), ADD_METALINK("aria2.addMetalink");

		private final String hasBody;

		Aria2Method(String hasBody) {
			this.hasBody = hasBody;
		}

		@Contract(pure = true) public String hasBody() {
			return hasBody;
		}
	}

	/**
	 * URI协议常量
	 */
	public enum URIMethod {
		/**
		 * http 协议
		 */
		HTTP("http"),
		/**
		 * https 协议
		 */
		HTTPS("https"),
		/**
		 * ws 协议
		 */
		WS("ws"),
		/**
		 * wws 协议
		 */
		WWS("wws");

		private final String value;

		URIMethod(String value) {
			this.value = value;
		}

		/**
		 * 获得 枚举方法的值
		 *
		 * @return value
		 */
		@Contract(pure = true) public String getValue() {
			return value;
		}
	}

}