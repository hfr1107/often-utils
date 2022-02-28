package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Network.JsoupUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 123云盘API
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/2/26 16:01
 */
public class YunPan123 {

	protected static final String loginApi = "https://www.123pan.com/a/api/user/sign_in";
	protected static final String userApi = "https://www.123pan.com/a/api/user/info";
	protected static final String listApi = "https://www.123pan.com/a/api/file/list/new";
	protected static final String downApi = "https://www.123pan.com/a/api/file/download_info";

	/**
	 * 获取用户主页的所有文件信息
	 *
	 * @param authorization 身份识别标识
	 * @return Map - 文件名, Map ( 文件信息)
	 */
	public static Map<String, Map<String, String>> getInfosAsHome(@NotNull String authorization) {
		return getInfosAsHome("", authorization);
	}

	/**
	 * 获取用户主页的匹配搜索项的文件信息
	 *
	 * @param authorization 身份识别标识
	 * @param searchData    待搜索数据
	 * @return Map - 文件名, Map ( 文件信息)
	 */
	public static Map<String, Map<String, String>> getInfosAsHome(@NotNull String searchData, @NotNull String authorization) {
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
		int fileCount = JSONObject.parseObject(JSONObject.parseObject(JsoupUtil.connect(userApi).authorization(authorization).get().text()).getString("data"))
				.getInteger("FileCount");
		int pageCount = (int) Math.ceil((double) fileCount / (double) 100);
		for (int i = 1; i <= pageCount; i++) {
			JSONArray infoList = JSONObject.parseArray(JSONObject.parseObject(
					JSONObject.parseObject(JsoupUtil.connect(listApi).data(data).data("Page", String.valueOf(i)).authorization(authorization).get().text())
							.getString("data")).getString("InfoList"));
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
	 * @param fileName      文件名
	 * @param fileInfo      文件信息
	 * @param authorization 身份识别标识
	 * @return 文件直链
	 */
	public static String getStraight(@NotNull String fileName, @NotNull Map<String, String> fileInfo, @NotNull String authorization) {
		JSONObject dataObject = new JSONObject();
		dataObject.put("driveId", "0");
		dataObject.put("type", "0");
		dataObject.put("fileName", fileName);
		dataObject.put("fileId", fileInfo.get("fileId"));
		dataObject.put("size", fileInfo.get("size"));
		dataObject.put("etag", fileInfo.get("etag"));
		dataObject.put("s3KeyFlag", fileInfo.get("s3KeyFlag"));
		return JSONObject.parseObject(
				JSONObject.parseObject(JsoupUtil.connect(downApi).requestBody(dataObject.toJSONString()).authorization(authorization).post().text())
						.getString("data")).getString("DownloadUrl");
	}

	/**
	 * 登陆并获得用户身份识别标识,可在请求头中使用
	 *
	 * @param username 用户名
	 * @param password 密码
	 * @return authorization 身份识别标识
	 */
	public static String login(String username, String password) {
		return JSONObject.parseObject(JSONObject.parseObject(JsoupUtil.connect(loginApi).requestBody(new JSONObject() {{
			put("passport", username);
			put("password", password);
		}}.toJSONString()).post().text()).getString("data")).getString("token");
	}

}