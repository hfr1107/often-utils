package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Judge;
import org.haic.often.Network.HttpsUtil;
import org.haic.often.Network.JsoupUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 123云盘API,获取直链需要登陆
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/2/26 16:01
 */
public class YunPan123 {

	protected static final String signinUrl = "https://www.123pan.com/a/api/user/sign_in";
	protected static final String shareGetUrl = "https://www.123pan.com/a/api/share/get";

	protected YunPan123() {
	}

	/**
	 * 通过账号密码登陆账号,进行需要是否验证的API操作
	 *
	 * @param username 用户名
	 * @param password 密码
	 * @return 此链接, 用于API操作
	 */
	@Contract(pure = true) public static YunPan123API login(@NotNull String username, @NotNull String password) {
		return new YunPan123API(YunPan123Login.login(username, password));
	}

	/**
	 * 通过身份识别标识登陆账号,进行需要是否验证的API操作
	 *
	 * @param authorization 身份识别标识
	 * @return 此链接, 用于API操作
	 */
	@Contract(pure = true) public static YunPan123API login(@NotNull String authorization) {
		return new YunPan123API(authorization);
	}

	/**
	 * 获取分享页所有文件链接
	 * <p>
	 * 注意获取的链接直接下载是无法获取文件名的,下载需要添加文件名参数
	 *
	 * @param shareUrl 分享URL
	 * @return Map - 文件名, 文件直链
	 */
	public static Map<String, String> getInfosAsPage(@NotNull String shareUrl) {
		return getInfosAsPage(shareUrl, "");
	}

	/**
	 * 获取分享页所有文件链接
	 * <p>
	 * 注意获取的链接直接下载是无法获取文件名的,下载需要添加文件名参数
	 *
	 * @param shareUrl 分享URL
	 * @param sharePwd 提取码
	 * @return Map - 文件名, 文件直链
	 */
	@Contract(pure = true) public static Map<String, String> getInfosAsPage(@NotNull String shareUrl, @NotNull String sharePwd) {
		return getInfosAsPage(shareUrl.substring(shareUrl.lastIndexOf("/") + 1), sharePwd, "0", 1);
	}

	@Contract(pure = true) protected static Map<String, String> getInfosAsPage(String key, String sharePwd, String parentFileId, int page) {
		Map<String, String> data = new HashMap<>();
		data.put("limit", "100");
		data.put("next", "1");
		data.put("orderBy", "share_id");
		data.put("orderDirection", "desc");
		data.put("shareKey", key);
		data.put("sharePwd", sharePwd);
		data.put("ParentFileId", parentFileId);
		data.put("Page", String.valueOf(page));

		JSONObject pageInfo = JSONObject.parseObject(HttpsUtil.connect(shareGetUrl).data(data).get().text()).getJSONObject("data");
		JSONArray files = pageInfo.getJSONArray("InfoList");
		Map<String, String> filesInfo = new HashMap<>();
		for (int i = 0; i < files.size(); i++) {
			JSONObject file = files.getJSONObject(i);
			String fileId = file.getString("FileId");
			int type = file.getInteger("Type");
			if (type == 1) {
				filesInfo.putAll(getInfosAsPage(key, sharePwd, fileId, 1));
				continue;
			}
			String fileName = file.getString("FileName");
			filesInfo.put(fileName, file.getString("Url"));
		}
		int next = pageInfo.getInteger("Next");
		if (!Judge.isMinusOne(next)) {
			filesInfo.putAll(getInfosAsPage(key, sharePwd, "0", page + 1));
		}
		return filesInfo;
	}

	/**
	 * 123云盘的API操作
	 */
	public static class YunPan123API {

		protected static final String userInfoUrl = "https://www.123pan.com/a/api/user/info";
		protected static final String filelistUrl = "https://www.123pan.com/a/api/file/list/new";
		protected static final String fileDownloadInfoUrl = "https://www.123pan.com/a/api/file/download_info";

		protected String authorization;

		protected YunPan123API(@NotNull String authorization) {
			this.authorization = authorization;
		}

		/**
		 * 获取用户主页的所有文件信息
		 *
		 * @return Map - 文件名, Map ( 文件信息)
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getInfosAsHome() {
			return getInfosAsHome("");
		}

		/**
		 * 获取用户主页的匹配搜索项的文件信息
		 *
		 * @param searchData 待搜索数据
		 * @return Map - 文件名, Map ( 文件信息)
		 */
		@Contract(pure = true) public Map<String, Map<String, String>> getInfosAsHome(@NotNull String searchData) {
			Map<String, String> data = new HashMap<>();
			data.put("driveId", "0");
			data.put("limit", "100");
			data.put("next", "0");
			data.put("orderBy", "fileId");
			data.put("orderDirection", "desc");
			data.put("parentFileId", "0");
			data.put("trashed", "false");
			data.put("SearchData", searchData);
			Map<String, Map<String, String>> filesInfo = new HashMap<>();

			int fileCount = JSONObject.parseObject(JsoupUtil.connect(userInfoUrl).authorization(authorization).get().text()).getJSONObject("data")
					.getInteger("FileCount");
			int pageCount = (int) Math.ceil((double) fileCount / (double) 100);
			for (int i = 1; i <= pageCount; i++) {
				JSONArray infoList = JSONObject.parseObject(
								JsoupUtil.connect(filelistUrl).data(data).data("Page", String.valueOf(i)).authorization(authorization).get().text())
						.getJSONObject("data").getJSONArray("InfoList");
				for (int j = 0; j < infoList.size(); j++) {
					JSONObject jsonObject = infoList.getJSONObject(j);
					Map<String, String> fileInfo = new HashMap<>();
					fileInfo.put("fileId", jsonObject.getString("FileId"));
					fileInfo.put("size", jsonObject.getString("Size"));
					fileInfo.put("etag", jsonObject.getString("Etag"));
					fileInfo.put("s3KeyFlag", jsonObject.getString("S3KeyFlag"));
					filesInfo.put(jsonObject.getString("FileName"), fileInfo);
				}
			}
			return filesInfo;
		}

		/**
		 * 通过文件信息配置获取文件直链
		 *
		 * @param fileName 文件名
		 * @param fileInfo 文件信息
		 * @return 文件直链
		 */
		@Contract(pure = true) public String getStraight(@NotNull String fileName, @NotNull Map<String, String> fileInfo) {
			JSONObject dataObject = new JSONObject();
			dataObject.put("driveId", "0");
			dataObject.put("type", "0");
			dataObject.put("fileName", fileName);
			dataObject.put("fileId", fileInfo.get("fileId"));
			dataObject.put("size", fileInfo.get("size"));
			dataObject.put("etag", fileInfo.get("etag"));
			dataObject.put("s3KeyFlag", fileInfo.get("s3KeyFlag"));
			return JSONObject.parseObject(
							JsoupUtil.connect(fileDownloadInfoUrl).requestBody(dataObject.toJSONString()).authorization(authorization).post().text())
					.getJSONObject("data").getString("DownloadUrl");
		}

	}

	public static class YunPan123Login {

		/**
		 * 通过账号密码登录获得用户身份识别标识,可在请求头中使用
		 *
		 * @param username 用户名
		 * @param password 密码
		 * @return 此链接, 用于API操作
		 */
		@Contract(pure = true) public static String login(@NotNull String username, @NotNull String password) {
			return JSONObject.parseObject(JsoupUtil.connect(signinUrl).requestBody(new JSONObject() {{
				put("passport", username);
				put("password", password);
			}}.toJSONString()).post().text()).getJSONObject("data").getString("token");
		}
	}

}