package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Network.JsoupUtil;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云盘API
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/22 9:50
 */
public class ALiYunPan {

	public static final String shareByAnonymousUrl = "https://api.aliyundrive.com/adrive/v3/share_link/get_share_by_anonymous";
	public static final String shareTokenUrl = "https://api.aliyundrive.com/v2/share_link/get_share_token";

	/**
	 * 登陆账户,进行需要是否验证的API操作
	 *
	 * @param authorization 身份识别信息,登录后,可在开发者本地存储(Local Storage)获取token项access_token值,或者在网络请求头中查找
	 * @return 此链接, 用于API操作
	 */
	public static ALiYunPanAPI login(@NotNull String authorization) {
		return new ALiYunPanAPI(authorization);
	}

	/**
	 * 获取分享页面文件的信息
	 *
	 * @param shareId 分享链接ID
	 * @return Map - 文件名,文件ID
	 */
	public static Map<String, String> getInfosAsPage(String shareId) {
		Document doc = JsoupUtil.connect(shareByAnonymousUrl).requestBody(new JSONObject().fluentPut("share_id", shareId).toJSONString()).post();
		JSONArray fileInfoArray = JSONObject.parseObject(doc.text()).getJSONArray("file_infos");
		Map<String, String> filesInfo = new HashMap<>();
		for (int i = 0; i < fileInfoArray.size(); i++) {
			JSONObject fileInfo = fileInfoArray.getJSONObject(i);
			filesInfo.put(fileInfo.getString("file_name"), fileInfo.getString("file_id"));
		}
		return filesInfo;
	}

	/**
	 * 获得分享链接的ShareToken
	 *
	 * @param shareId 分享链接ID
	 * @return ShareToken
	 */
	public static String getShareToken(String shareId) {
		return getShareToken(shareId, "");

	}

	/**
	 * 获得分享链接的ShareToken
	 *
	 * @param shareId  分享链接ID
	 * @param sharePwd 提取码
	 * @return ShareToken
	 */
	public static String getShareToken(String shareId, String sharePwd) {
		JSONObject apiJson = new JSONObject();
		apiJson.put("share_id", shareId);
		apiJson.put("share_pwd", sharePwd);
		Document doc = JsoupUtil.connect(shareTokenUrl).requestBody(apiJson.toString()).post();
		return JSONObject.parseObject(doc.text()).getString("share_token");
	}

	/**
	 * 阿里云盘的API操作
	 */
	public static class ALiYunPanAPI {

		public static final String shareLinkDownloadUrl = "https://api.aliyundrive.com/v2/file/get_share_link_download_url";
		// public static final String fileListApi = "https://api.aliyundrive.com/adrive/v3/file/list";

		public String authorization;

		protected ALiYunPanAPI(@NotNull String authorization) {
			this.authorization = authorization;
		}

		/**
		 * 获得分享页面所有文件直链(方法暂时废弃,阿里云盘限制,分享页面获取链接无用,需保存至个人盘内才能获取直链)
		 *
		 * @param shareUrl 分享链接
		 * @return Map - 文件名, 文件直链
		 */
		public Map<String, String> getStraightsAsPage(@NotNull String shareUrl) {
			return getStraightsAsPage(shareUrl, "");
		}

		/**
		 * 获得分享页面所有文件直链(方法暂时废弃,阿里云盘限制,分享页面获取链接无用,需保存至个人盘内才能获取直链)
		 *
		 * @param shareUrl 分享链接
		 * @param sharePwd 提取码
		 * @return Map - 文件名, 文件直链
		 */
		public Map<String, String> getStraightsAsPage(@NotNull String shareUrl, @NotNull String sharePwd) {
			String shareId = shareUrl.substring(shareUrl.lastIndexOf("/") + 1);
			String shareToken = getShareToken(shareId, sharePwd);
			Map<String, String> filesStraight = new HashMap<>();
			for (Map.Entry<String, String> entry : getInfosAsPage(shareId).entrySet()) {
				filesStraight.put(entry.getKey(), getStraight(shareId, entry.getValue(), shareToken));
			}
			return filesStraight;
		}

		/**
		 * 获取文件直链
		 *
		 * @param shareId    分享链接ID
		 * @param fileid     文件ID
		 * @param shareToken shareToken
		 * @return 文件直链
		 */
		public String getStraight(String shareId, String fileid, String shareToken) {
			JSONObject apiJson = new JSONObject();
			apiJson.put("share_id", shareId);
			apiJson.put("file_id", fileid);
			Map<String, String> headers = new HashMap<>();
			headers.put("x-share-token", shareToken);
			headers.put("authorization", authorization.startsWith("Bearer") ? authorization : "Bearer " + authorization);
			Document doc = JsoupUtil.connect(shareLinkDownloadUrl).headers(headers).requestBody(apiJson.toString()).post();
			return JSONObject.parseObject(doc.text()).getString("download_url");
		}
	}

}