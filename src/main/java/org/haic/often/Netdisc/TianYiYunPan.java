package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.haic.often.Network.Jsoup.Connection;
import org.haic.often.Network.Jsoup.JsoupUtil;
import org.haic.often.Network.Method;
import org.haic.often.StringUtils;
import org.haic.often.Tuple.FourTuple;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * 天翼云盘API,获取直链需要登陆
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/18 22:43
 */
public class TianYiYunPan {

	public static final String dataApi = "https://cloud.189.cn/api/open/share/getShareInfoByCode.action";
	public static final String listApi = "https://cloud.189.cn/api/open/share/listShareDir.action";
	public static final String downApi = "https://cloud.189.cn/api/open/file/getFileDownloadUrl.action";

	protected TianYiYunPan() {
	}

	/**
	 * 登陆账户
	 *
	 * @param userName 用户名 (不含@189.cn后缀)
	 * @param password 密码
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static TianYiYunPanApi login(@NotNull String userName, @NotNull String password) {
		return cookies(TianYiYunPanLogin.login(userName, password));
	}

	/**
	 * 登陆账户
	 *
	 * @param cookies cookies
	 * @return 此连接，用于链接
	 */
	@Contract(pure = true) public static TianYiYunPanApi cookies(@NotNull Map<String, String> cookies) {
		return new TianYiYunPanApi(cookies);
	}

	/**
	 * 获得分享页面所有文件的信息
	 *
	 * @param url        天翼URL
	 * @param accessCode 提取码
	 * @return List - fileName, fileId, shareId
	 */
	@NotNull @Contract(pure = true) private static List<ThreeTuple<String, String, String>> getFilesInfo(@NotNull String url, @NotNull String accessCode) {
		FourTuple<String, String, String, String> urlInfo = getUrlInfo(url);

		Map<String, String> listData = new HashMap<>();
		listData.put("fileId", urlInfo.first);
		listData.put("shareId", urlInfo.second);
		listData.put("isFolder", urlInfo.third);
		listData.put("shareMode", urlInfo.fourth);
		listData.put("accessCode", accessCode);

		List<ThreeTuple<String, String, String>> fileInfos = new ArrayList<>();
		for (Element element : JsoupUtil.connect(listApi).data(listData).get().select("file")) {
			String fileName = Objects.requireNonNull(element.selectFirst("name")).text();
			String id = Objects.requireNonNull(element.selectFirst("id")).text();
			fileInfos.add(Tuple.of(fileName, id, urlInfo.second));
		}
		return fileInfos;
	}

	/**
	 * 获取URL的ID等信息
	 *
	 * @param url 天翼URL
	 * @return fileId, shareId, isFolder, shareMode
	 */
	@Contract(pure = true) private static FourTuple<String, String, String, String> getUrlInfo(@NotNull String url) {
		String code = url.contains("code") ? StringUtils.extractRegex(url, "code=.*").substring(5) : url.substring(url.lastIndexOf("/"));
		Document docData = JsoupUtil.connect(dataApi).data("shareCode", code).retry(true).get();
		String fileId = Objects.requireNonNull(docData.selectFirst("fileId")).text();
		String shareId = Objects.requireNonNull(docData.selectFirst("shareId")).text();
		String isFolder = Objects.requireNonNull(docData.selectFirst("isFolder")).text();
		String shareMode = Objects.requireNonNull(docData.selectFirst("shareMode")).text();
		return Tuple.of(fileId, shareId, isFolder, shareMode);
	}

	public static class TianYiYunPanApi {
		public Map<String, String> cookies;

		protected TianYiYunPanApi(Map<String, String> cookies) {
			this.cookies = cookies;
		}

		/**
		 * 在需要提取码但自己没有时,可直接获得文件直链<br/>
		 * 如果有多个文件,返回第一个文件直链
		 *
		 * @param url 天翼URL
		 * @return 文件直链
		 */
		@Contract(pure = true) public String getStraightAsNotCode(@NotNull String url) {
			FourTuple<String, String, String, String> urlInfo = getUrlInfo(url);
			return JsoupUtil.connect(downApi + "?dt=1&fileId=" + urlInfo.first + "&shareId=" + urlInfo.second).cookies(cookies).retry(true).get().text();
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
			for (ThreeTuple<String, String, String> fileInfo : getFilesInfo(url, accessCode)) {
				Map<String, String> params = new HashMap<>();
				params.put("dt", "1");
				params.put("fileId", fileInfo.second);
				params.put("shareId", fileInfo.third);
				fileUrls.put(fileInfo.first, JsoupUtil.connect(downApi).data(params).cookies(cookies).retry(true).get().text());
			}
			return fileUrls;
		}

	}

	public static class TianYiYunPanLogin {
		public static final String loginUrl_url = "https://cloud.189.cn/api/portal/loginUrl.action";
		public static final String encryptConfUrl = "https://open.e.189.cn/api/logbox/config/encryptConf.do";
		public static final String loginSubmit_url = "https://open.e.189.cn/api/logbox/oauth2/loginSubmit.do";

		public static Map<String, String> login(@NotNull String userName, @NotNull String password) {
			Connection conn = JsoupUtil.connect(encryptConfUrl);
			JSONObject encryptConfData = JSONObject.parseObject(JSONObject.parseObject(conn.get().text()).getString("data"));
			String pre = encryptConfData.getString("pre");
			String pubKey = encryptConfData.getString("pubKey");
			userName = pre + encrypt(userName, pubKey);
			password = pre + encrypt(password, pubKey);

			Document doc = conn.url(loginUrl_url).get();
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
			String paramId = StringUtils.extractRegex(loginUrlText, "paramId.*;");
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

			return conn.url(loginSubmit_url).header("lt", lt).header("reqid", reqId).header("referer", encryptConfUrl).data(data).method(Method.POST).execute()
					.cookies();

		}

		public static String encrypt(String data, String pubKey) {
			try {
				return b64tohex(publicKeyEncrypt(data, pubKey));
			} catch (Exception e) {
				return "";
			}
		}

		public static String publicKeyEncrypt(String str, String publicKey) throws Exception {
			byte[] decoded = org.apache.commons.codec.binary.Base64.decodeBase64(publicKey); //base64编码的公钥
			RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
			Cipher cipher = Cipher.getInstance("RSA"); //RSA加密
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return Base64.encodeBase64String(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
		}

		public static String b64tohex(String data) {
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

		public static char int2char(int index) {
			return "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()[index];
		}
	}

}