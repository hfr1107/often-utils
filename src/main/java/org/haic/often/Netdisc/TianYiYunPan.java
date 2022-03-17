package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.haic.often.Network.Connection;
import org.haic.often.Network.JsoupUtil;
import org.haic.often.Network.Response;
import org.haic.often.StringUtils;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 天翼云盘API,获取直链需要登陆
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/18 22:43
 */
public class TianYiYunPan {

	public static final String listShareDirUrl = "https://cloud.189.cn/api/open/share/listShareDir.action";
	public static final String shareInfoByCodeUrl = "https://cloud.189.cn/api/open/share/getShareInfoByCode.action";

	public static final Map<String, String> headers = new HashMap<>() {{
		put("accept", "application/json;charset=UTF-8");
	}};

	protected TianYiYunPan() {
	}

	/**
	 * 登陆账户,进行需要是否验证的API操作
	 *
	 * @param userName 用户名 (不含@189.cn后缀)
	 * @param password 密码
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static TianYiYunPanAPI login(@NotNull String userName, @NotNull String password) {
		return login(TianYiYunPanLogin.login(userName, password));
	}

	/**
	 * 登陆账户,进行需要是否验证的API操作
	 *
	 * @param cookies cookies
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static TianYiYunPanAPI login(@NotNull Map<String, String> cookies) {
		return new TianYiYunPanAPI(cookies);
	}

	/**
	 * 获取分享URL的ID等信息
	 *
	 * @param shareUrl 天翼URL
	 * @return fileId, shareId, isFolder, shareMode
	 */
	@Contract(pure = true) public static Map<String, String> getshareUrlInfo(@NotNull String shareUrl) {
		String code = shareUrl.contains("code") ? StringUtils.extractRegex(shareUrl, "code=.*").substring(5) : shareUrl.substring(shareUrl.lastIndexOf("/"));
		Document docData = JsoupUtil.connect(shareInfoByCodeUrl).data("shareCode", code).headers(headers).get();
		JSONObject creator = JSONObject.parseObject(docData.text()).getJSONObject("creator");
		Map<String, String> info = new HashMap<>();
		info.put("fileId", creator.getString("fileId"));
		info.put("shareId", creator.getString("shareId"));
		info.put("isFolder", creator.getString("isFolder"));
		info.put("shareMode", creator.getString("shareMode"));
		return info;
	}

	/**
	 * 获得分享页面所有文件的信息
	 *
	 * @param url        天翼URL
	 * @param accessCode 提取码
	 * @return List - fileName, fileId, shareId
	 */
	@Contract(pure = true) public static List<ThreeTuple<String, String, String>> getFilesInfoAsPage(@NotNull String url, @NotNull String accessCode) {
		Map<String, String> info = getshareUrlInfo(url);
		info.put("accessCode", accessCode);
		List<ThreeTuple<String, String, String>> fileInfos = new ArrayList<>();
		for (Element element : JsoupUtil.connect(listShareDirUrl).data(info).get().select("file")) {
			String fileName = Objects.requireNonNull(element.selectFirst("name")).text();
			String id = Objects.requireNonNull(element.selectFirst("id")).text();
			fileInfos.add(Tuple.of(fileName, id, info.get("shareId")));
		}
		return fileInfos;
	}

	/**
	 * 天翼云盘的API操作
	 */
	public static class TianYiYunPanAPI {

		public static final String fileDownloadUrl = "https://cloud.189.cn/api/open/file/getFileDownloadUrl.action";
		public static final String listFilesUrl = "https://cloud.189.cn/api/open/file/listFiles.action";
		public static final String createBatchTaskUrl = "https://cloud.189.cn/api/open/batch/createBatchTask.action";
		public static final String createShareLinkUrl = "https://cloud.189.cn/api/open/share/createShareLink.action?noCache=0.009576706659646606&fileId=5130319904099345&expireTime=1&shareType=3";

		public Map<String, String> cookies;

		protected TianYiYunPanAPI(@NotNull Map<String, String> cookies) {
			this.cookies = cookies;
		}

		/**
		 * 根据配置删除多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,Map ( 文件名 ( 文件信息 )) 确保文件信息里存在fileId选项
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int delete(@NotNull Map<String, Map<String, String>> filesInfo) {
			return batchTask("DELETE", filesInfo, "");
		}

		/**
		 * 删除单个个文件或文件夹到指定文件夹
		 *
		 * @param fileName 文件名
		 * @param fileId   文件ID
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int delete(@NotNull String fileName, @NotNull String fileId) {
			return batchTask("DELETE", fileName, fileId, "");
		}

		/**
		 * 根据配置复制多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,Map ( 文件名 ( 文件信息 )) 确保文件信息里存在fileId选项
		 * @param folderId  目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int copy(@NotNull Map<String, Map<String, String>> filesInfo, @NotNull String folderId) {
			return batchTask("COPY", filesInfo, folderId);
		}

		/**
		 * 复制单个文件或文件夹到指定文件夹
		 *
		 * @param fileName 文件名
		 * @param fileId   文件ID
		 * @param folderId 目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int copy(@NotNull String fileName, @NotNull String fileId, @NotNull String folderId) {
			return batchTask("COPY", fileName, fileId, folderId);
		}

		/**
		 * 根据配置移动多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,Map ( 文件名 ( 文件信息 )) 确保文件信息里存在fileId选项
		 * @param folderId  目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int move(@NotNull Map<String, Map<String, String>> filesInfo, @NotNull String folderId) {
			return batchTask("MOVE", filesInfo, folderId);
		}

		/**
		 * 移动单个文件或文件夹到指定文件夹
		 *
		 * @param fileName 文件名
		 * @param fileId   文件ID
		 * @param folderId 目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int move(@NotNull String fileName, @NotNull String fileId, @NotNull String folderId) {
			return batchTask("MOVE", fileName, fileId, folderId);
		}

		/**
		 * 对多个文件或文件夹执行批处理脚本操作,用于文件移动,删除,复制等
		 *
		 * @param type      操作类型
		 * @param filesInfo 指定的多个文件或文件夹,Map ( 文件名 ( 文件信息 )) 确保文件信息里存在fileId选项
		 * @param folderId  目标文件夹ID,注意如果当前操作(如删除)没有关联文件夹,指定空字符串
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull Map<String, Map<String, String>> filesInfo, @NotNull String folderId) {
			JSONArray taskInfos = new JSONArray();
			for (Map.Entry<String, Map<String, String>> info : filesInfo.entrySet()) {
				JSONObject taskInfo = new JSONObject();
				taskInfo.put("fileName", info.getKey());
				taskInfo.put("fileId", info.getValue().get("fileId"));
				taskInfos.add(taskInfo);
			}
			return batchTask(type, taskInfos.toJSONString(), folderId);
		}

		/**
		 * 对单个文件或文件夹执行脚本操作,用于文件移动,删除,复制等
		 *
		 * @param type     操作类型
		 * @param fileName 文件名
		 * @param fileId   文件ID
		 * @param folderId 目标文件夹ID,注意如果当前操作(如删除)没有关联文件夹,指定空字符串
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull String fileName, @NotNull String fileId, @NotNull String folderId) {
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("fileName", fileName);
			taskInfo.put("fileId", fileId);
			JSONArray taskInfos = new JSONArray();
			taskInfos.add(taskInfo);
			return batchTask(type, taskInfos.toJSONString(), folderId);
		}

		/**
		 * 执行脚本操作,用于文件移动,删除,复制等
		 *
		 * @param type      操作类型
		 * @param taskInfos 自定义待执行的Json数据(json数组,每个元素应包含fileName和fileId选项)
		 * @param folderId  目标文件夹ID,注意如果当前操作(如删除)没有关联文件夹,指定空字符串
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull String taskInfos, @NotNull String folderId) {
			Map<String, String> data = new HashMap<>();
			data.put("type", type);
			data.put("taskInfos", taskInfos);
			data.put("targetFolderId", folderId);
			return JSONObject.parseObject(JsoupUtil.connect(createBatchTaskUrl).headers(headers).data(data).cookies(cookies).execute().body())
					.getInteger("res_code");
		}

		/**
		 * 通过文件ID获取文件的直链,不能用于分享页面获取
		 *
		 * @param fileId 文件ID
		 * @return 用于下载的直链
		 */
		@Contract(pure = true) public String getStraight(@NotNull String fileId) {
			return JSONObject.parseObject(JsoupUtil.connect(fileDownloadUrl).data("fileId", fileId).headers(headers).cookies(cookies).get().text())
					.getString("fileDownloadUrl");
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件和文件夹信息
		 *
		 * @param folderId 文件夹ID
		 * @return Map < 名称 , Map < String, String > >
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getInfoAsHomeOfFolderId(@NotNull String folderId) {
			Map<String, String> data = new HashMap<>();
			data.put("pageSize", "1");
			data.put("folderId", folderId);
			Connection conn = JsoupUtil.connect(listFilesUrl);
			String urlInfo = conn.headers(headers).data(data).cookies(cookies).get().text();
			data.put("pageSize", JSONObject.parseObject(urlInfo).getJSONObject("fileListAO").getString("count"));
			urlInfo = conn.url(listFilesUrl).data(data).cookies(cookies).get().text();
			JSONObject fileListAO = JSONObject.parseObject(urlInfo).getJSONObject("fileListAO");
			JSONArray fileList = fileListAO.getJSONArray("fileList");
			Map<String, Map<String, String>> filesInfo = new HashMap<>();
			for (int i = 0; i < fileList.size(); i++) {
				JSONObject info = fileList.getJSONObject(i);
				Map<String, String> filInfo = new HashMap<>();
				filInfo.put("id", info.getString("id"));
				filInfo.put("md5", info.getString("md5"));
				filInfo.put("size", info.getString("size"));
				filInfo.put("isFolder", "0");
				filesInfo.put(info.getString("name"), filInfo);
			}
			JSONArray folderList = fileListAO.getJSONArray("folderList");
			for (int i = 0; i < folderList.size(); i++) {
				JSONObject info = folderList.getJSONObject(i);
				Map<String, String> filInfo = new HashMap<>();
				filInfo.put("id", info.getString("id"));
				filInfo.put("parentId", info.getString("parentId"));
				filInfo.put("isFolder", "1");
				filesInfo.put(info.getString("name"), filInfo);
			}
			return filesInfo;
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件信息
		 *
		 * @param folderId 文件夹ID
		 * @return Map < 名称 , Map < String, String > >
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getFilesInfoAsHomeOfFolderId(@NotNull String folderId) {
			return getInfoAsHomeOfFolderId("-11").entrySet().stream().filter(l -> l.getValue().get("isFolder").equals("0"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件夹信息
		 *
		 * @param folderId 文件夹ID
		 * @return Map < 名称 , Map < String, String > >
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getFoldersInfoAsHomeOfFolderId(@NotNull String folderId) {
			return getInfoAsHomeOfFolderId("-11").entrySet().stream().filter(l -> l.getValue().get("isFolder").equals("1"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		/**
		 * 获取主页面所有文件和文件夹信息
		 *
		 * @return Map < 名称 , Map < String, String > >
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getFilesInfoAsHome() {
			return getInfoAsHomeOfFolderId("-11"); // -11 主页
		}

		/**
		 * 获取指定文件夹下所有文件信息,[-11]为根目录,获取全部文件信息
		 *
		 * @param folderId 文件夹ID
		 * @return Map < 名称 , Map < String, String > >
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getFoldersInfoAsHomeOfAll(@NotNull String folderId) {
			Map<String, Map<String, String>> filesInfo = getFilesInfoAsHome();
			for (Map.Entry<String, Map<String, String>> fileInfo : filesInfo.entrySet()) {
				String name = fileInfo.getKey();
				Map<String, String> params = fileInfo.getValue();
				if (params.get("isFile").equals("true")) {
					filesInfo.put(name, params);
				} else {
					filesInfo.putAll(getFilesInfoAsHomeOfFolderId(params.get("id")));
				}
			}
			return filesInfo;
		}

		/**
		 * 在需要提取码但自己没有时,可直接获得文件直链<br/>
		 * 如果有多个文件,返回第一个文件直链
		 *
		 * @param url 天翼URL
		 * @return 文件直链
		 */
		@Contract(pure = true) public String getStraightAsNotCode(@NotNull String url) {
			Map<String, String> info = getshareUrlInfo(url);
			return JsoupUtil.connect(fileDownloadUrl + "?dt=1&fileId=" + info.get("fileId") + "&shareId=" + info.get("shareId")).cookies(cookies).get().text();
		}

		/**
		 * 获得分享页面所有文件的直链(无密码)
		 *
		 * @param url 天翼URL
		 * @return Map - 文件名 ,文件直链
		 */
		@Contract(pure = true) public Map<String, String> getStraightsAsPage(@NotNull String url) {
			return getStraightsAsPage(url, "");
		}

		/**
		 * 获得分享页面所有文件的直链
		 *
		 * @param url        天翼URL
		 * @param accessCode 提取码
		 * @return Map - 文件名 ,文件直链
		 */
		@Contract(pure = true) public Map<String, String> getStraightsAsPage(@NotNull String url, @NotNull String accessCode) {
			Map<String, String> fileUrls = new HashMap<>();
			for (ThreeTuple<String, String, String> fileInfo : getFilesInfoAsPage(url, accessCode)) {
				Map<String, String> params = new HashMap<>();
				params.put("dt", "1");
				params.put("fileId", fileInfo.second);
				params.put("shareId", fileInfo.third);
				fileUrls.put(fileInfo.first, JsoupUtil.connect(fileDownloadUrl).data(params).cookies(cookies).get().text());
			}
			return fileUrls;
		}

	}

	/**
	 * 天翼云盘的登陆操作
	 */
	public static class TianYiYunPanLogin {

		public static final String loginUrl = "https://cloud.189.cn/api/portal/loginUrl.action";
		public static final String loginSubmitUrl = "https://open.e.189.cn/api/logbox/oauth2/loginSubmit.do";
		public static final String encryptConfUrl = "https://open.e.189.cn/api/logbox/config/encryptConf.do";

		@Contract(pure = true) public static Map<String, String> login(@NotNull String userName, @NotNull String password) {
			Connection conn = JsoupUtil.connect(encryptConfUrl);
			JSONObject encryptConfData = JSONObject.parseObject(JSONObject.parseObject(conn.get().text()).getString("data"));
			String pre = encryptConfData.getString("pre");
			String pubKey = encryptConfData.getString("pubKey");
			userName = pre + encrypt(userName, pubKey);
			password = pre + encrypt(password, pubKey);
			Document doc = conn.url(loginUrl).get();
			String loginUrlText = doc.select("script[type='text/javascript']").toString();
			String captcha_token = doc.select("input[name='captchaToken']").attr("value");
			String appKey = StringUtils.extractRegex(loginUrlText, "appKey =.*,");
			appKey = appKey.substring(appKey.indexOf("'") + 1, appKey.lastIndexOf("'"));
			String accountType = StringUtils.extractRegex(loginUrlText, "accountType =.*,");
			accountType = accountType.substring(accountType.indexOf("'") + 1, accountType.lastIndexOf("'"));
			String clientType = StringUtils.extractRegex(loginUrlText, "clientType =.*,");
			clientType = clientType.substring(clientType.indexOf("'") + 1, clientType.lastIndexOf("'"));
			String returnUrl = StringUtils.extractRegex(loginUrlText, "returnUrl =.*,");
			returnUrl = returnUrl.substring(returnUrl.indexOf("'") + 1, returnUrl.lastIndexOf("'"));
			String mailSuffix = StringUtils.extractRegex(loginUrlText, "mailSuffix =.*;");
			mailSuffix = mailSuffix.substring(mailSuffix.indexOf("'") + 1, mailSuffix.lastIndexOf("'"));
			String isOauth2 = StringUtils.extractRegex(loginUrlText, "isOauth2 =.*;");
			isOauth2 = isOauth2.substring(isOauth2.indexOf("\"") + 1, isOauth2.lastIndexOf("\""));
			String lt = StringUtils.extractRegex(loginUrlText, "lt =.*;");
			lt = lt.substring(lt.indexOf("\"") + 1, lt.lastIndexOf("\""));
			String reqId = StringUtils.extractRegex(loginUrlText, "reqId =.*;");
			reqId = reqId.substring(reqId.indexOf("\"") + 1, reqId.lastIndexOf("\""));
			String paramId = StringUtils.extractRegex(loginUrlText, "paramId =.*;");
			paramId = paramId.substring(paramId.indexOf("\"") + 1, paramId.lastIndexOf("\""));

			Map<String, String> data = new HashMap<>();
			data.put("appKey", appKey);
			data.put("accountType", accountType);
			data.put("userName", userName);
			data.put("password", password);
			data.put("captchaToken", captcha_token);
			data.put("returnUrl", returnUrl);
			data.put("mailSuffix", mailSuffix);
			data.put("dynamicCheck", paramId);
			data.put("clientType", clientType);
			data.put("isOauth2", isOauth2);
			data.put("paramId", paramId);
			Response res = conn.url(loginSubmitUrl).header("lt", lt).header("reqid", reqId).header("referer", encryptConfUrl).data(data).method(Method.POST)
					.execute();
			JSONObject info = JSONObject.parseObject(res.body());
			if (info.getString("result").equals("0")) {
				return JsoupUtil.connect(info.getString("toUrl")).execute().cookies();
			}
			return new HashMap<>();
		}

		protected static String encrypt(String data, String pubKey) {
			try {
				return b64tohex(publicKeyEncrypt(data, pubKey));
			} catch (Exception e) {
				return "";
			}
		}

		protected static String publicKeyEncrypt(String str, String publicKey) throws Exception {
			byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(publicKey); //base64编码的公钥
			RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
			Cipher cipher = Cipher.getInstance("RSA"); //RSA加密
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return Base64.encodeBase64String(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
		}

		protected static String b64tohex(String data) {
			char[] a = data.toCharArray();
			String b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
			StringBuilder d = new StringBuilder();
			int e = 0, c = 0;
			for (char value : a) {
				if (value != '=') {
					int v = b64map.indexOf(value);
					if (e == 0) {
						e = 1;
						d.append(int2char(v >> 2));
						c = 3 & v;
					} else if (e == 1) {
						e = 2;
						d.append(int2char(c << 2 | v >> 4));
						c = 15 & v;
					} else if (e == 2) {
						e = 3;
						d.append(int2char(c));
						d.append(int2char(v >> 2));
						c = 3 & v;
					} else {
						e = 0;
						d.append(int2char(c << 2 | v >> 4));
						d.append(int2char(15 & v));
					}
				}
			}

			return e == 1 ? String.valueOf(d) + int2char(c << 2) : d.toString();
		}

		protected static char int2char(int index) {
			return "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()[index];
		}
	}

}