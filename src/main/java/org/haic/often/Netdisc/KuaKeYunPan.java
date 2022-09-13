package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONObject;
import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.haic.often.Network.Connection;
import org.haic.often.Network.HttpsUtil;
import org.haic.often.Network.Method;
import org.haic.often.Network.URIUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 蓝奏云盘API
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/18 23:59
 */
public class KuaKeYunPan {

	protected KuaKeYunPan() {
	}

	@Contract(pure = true) public static KuaKeYunPanAPI login(@NotNull Map<String, String> cookies) {
		return new KuaKeYunPanAPI(cookies);
	}

	public static class KuaKeYunPanAPI {

		public static final String sortUrl = "https://drive.quark.cn/1/clouddrive/file/sort?pr=ucpro&fr=pc&_size=2147483647&pdir_fid=";
		public static final String flushUrl = "https://drive.quark.cn/1/clouddrive/auth/pc/flush?pr=ucpro&fr=pc";
		public static final String fileUrl = "https://drive.quark.cn/1/clouddrive/file?pr=ucpro&fr=pc";
		public static final String renameUrl = "https://drive.quark.cn/1/clouddrive/file/rename?pr=ucpro&fr=pc";
		public static final String deleteUrl = "https://drive.quark.cn/1/clouddrive/file/delete?pr=ucpro&fr=pc";
		public static final String shareDeleteUrl = "https://drive.quark.cn/1/clouddrive/share/delete?pr=ucpro&fr=pc";
		public static final String moveUrl = "https://drive.quark.cn/1/clouddrive/file/move?pr=ucpro&fr=pc";
		public static final String shareUrl = "https://drive.quark.cn/1/clouddrive/share?pr=ucpro&fr=pc";
		public static final String taskUrl = "https://drive.quark.cn/1/clouddrive/task?pr=ucpro&fr=pc&retry_index=0&task_id=";
		public static final String passwordUrl = "https://drive.quark.cn/1/clouddrive/share/password?pr=ucpro&fr=pc";
		public static final String detailUrl = "https://drive.quark.cn/1/clouddrive/share/mypage/detail?pr=ucpro&fr=pc&_size=2147483647";
		public static final String categoryUrl = "https://drive.quark.cn/1/clouddrive/file/category?pr=ucpro&fr=pc&_size=10240&cat=";
		public static final String preUrl = "https://drive.quark.cn/1/clouddrive/file/upload/pre?pr=ucpro&fr=pc";
		public static final String authUrl = "https://drive.quark.cn/1/clouddrive/file/upload/auth?pr=ucpro&fr=pc";
		public static final String hashUrl = "https://drive.quark.cn/1/clouddrive/file/update/hash?pr=ucpro&fr=pc";

		protected Connection conn = HttpsUtil.newSession();

		protected KuaKeYunPanAPI(Map<String, String> cookies) {
			conn.cookies(cookies);
			JSONObject loginInfo = JSONObject.parseObject(conn.url(flushUrl).get().text());
			if (!URIUtils.statusIsOK(loginInfo.getInteger("status"))) {
				throw new RuntimeException(loginInfo.getString("message"));
			}
		}

		/**
		 * 获取指定类型的文件信息,限制最多显示前10240个文件
		 *
		 * @param ext 文件类型<br/>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;bt - BT种子<br/>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;document - 文档<br/>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;video - 视频<br/>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;audio - 音频<br/>
		 *            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;image - 图片<br/>
		 * @return 文件信息列表
		 */
		@Contract(pure = true) public List<JSONObject> getInfosOfExt(@NotNull String ext) {
			return JSONObject.parseArray(JSONObject.parseObject(conn.url(categoryUrl + ext).get().text()).getJSONObject("data").getString("list"),
					JSONObject.class);
		}

		/**
		 * 获取分享文件列表
		 *
		 * @return 分享文件列表
		 */
		@Contract(pure = true) public List<JSONObject> shareList() {
			return JSONObject.parseArray(JSONObject.parseObject(conn.url(detailUrl).get().text()).getJSONObject("data").getString("list"), JSONObject.class);
		}

		/**
		 * 删除指定的分享
		 *
		 * @param shareId 分享ID
		 * @return 删除状态
		 */
		@Contract(pure = true) public boolean cancelShare(@NotNull String... shareId) {
			return cancelShare(Arrays.asList(shareId));
		}

		/**
		 * 删除指定的分享
		 *
		 * @param shareIds 分享ID
		 * @return 删除状态
		 */
		@Contract(pure = true) public boolean cancelShare(@NotNull List<String> shareIds) {
			return URIUtils.statusIsOK(
					JSONObject.parseObject(conn.url(shareDeleteUrl).requestBody(new JSONObject().fluentPut("share_ids", shareIds).toJSONString()).post().text())
							.getInteger("status"));
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareCode 分享密码
		 * @param fid       文件ID
		 * @return 含有分享链接等JSON格式信息
		 */
		@Contract(pure = true) public JSONObject share(@NotNull String shareCode, @NotNull String... fid) {
			return share(shareCode, Arrays.asList(fid));
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareCode 分享密码
		 * @param fids      文件ID
		 * @return 含有分享链接等JSON格式信息
		 */
		@Contract(pure = true) public JSONObject share(@NotNull String shareCode, @NotNull List<String> fids) {
			JSONObject data = new JSONObject();
			data.put("expired_type", 2);
			data.put("fid_list", fids);
			data.put("passcode", shareCode);
			data.put("url_type", shareCode.isEmpty() ? 1 : 2);
			JSONObject shareInfo = JSONObject.parseObject(conn.url(shareUrl).requestBody(data.toJSONString()).post().text()).getJSONObject("data");
			String taskId = shareInfo.getString("task_id");
			if (shareInfo.getBoolean("task_sync")) {
				JSONObject taskResp = shareInfo.getJSONObject("task_resp");
				JSONObject result = new JSONObject();
				result.put("status", taskResp.getInteger("status"));
				result.put("message", taskResp.getString("message"));
				return result.fluentPut("task_id", taskId);
			}
			String shareId = null;
			while (Judge.isNull(shareId)) {
				JSONObject taskInfo = JSONObject.parseObject(conn.url(taskUrl + taskId).get().text()).getJSONObject("data");
				shareId = taskInfo.getInteger("status") == 2 ? taskInfo.getString("share_id") : null;
			}
			return JSONObject.parseObject(conn.url(passwordUrl).requestBody(new JSONObject().fluentPut("share_id", shareId).toJSONString()).post().text())
					.getJSONObject("data").fluentPut("share_id", shareId);
		}

		/**
		 * 删除指定文件
		 *
		 * @param fid 文件ID
		 * @return 删除状态
		 */
		@Contract(pure = true) public boolean delete(@NotNull String... fid) {
			return delete(Arrays.asList(fid));
		}

		/**
		 * 删除指定文件
		 *
		 * @param fids 文件ID
		 * @return 删除状态
		 */
		@Contract(pure = true) public boolean delete(@NotNull List<String> fids) {
			JSONObject data = new JSONObject();
			data.put("action_type", 2);
			data.put("exclude_fids", new ArrayList<>());
			data.put("filelist", fids);
			return URIUtils.statusIsOK(JSONObject.parseObject(conn.url(deleteUrl).requestBody(data.toJSONString()).post().text()).getInteger("status"));
		}

		/**
		 * 重命名文件或文件夹
		 *
		 * @param fid  文件ID
		 * @param name 新的名称
		 * @return 重命名状态
		 */
		@Contract(pure = true) public boolean rename(@NotNull String fid, @NotNull String name) {
			JSONObject data = new JSONObject();
			data.put("fid", fid);
			data.put("file_name", name);
			return URIUtils.statusIsOK(JSONObject.parseObject(conn.url(renameUrl).requestBody(data.toJSONString()).post().text()).getInteger("status"));
		}

		/**
		 * 移动文件
		 *
		 * @param tofid 移动至指定目录
		 * @param fid   待移动的文件ID
		 * @return 移动状态
		 */
		@Contract(pure = true) public boolean move(@NotNull String tofid, @NotNull String... fid) {
			return move(tofid, Arrays.asList(fid));
		}

		/**
		 * 移动文件
		 *
		 * @param tofid 移动至指定目录
		 * @param fids  待移动的文件ID
		 * @return 移动状态
		 */
		@Contract(pure = true) public boolean move(@NotNull String tofid, @NotNull List<String> fids) {
			JSONObject data = new JSONObject();
			data.put("action_type", 1);
			data.put("exclude_fids", new ArrayList<>());
			data.put("filelist", fids);
			data.put("to_pdir_fid", tofid);
			return URIUtils.statusIsOK(JSONObject.parseObject(conn.url(moveUrl).requestBody(data.toJSONString()).post().text()).getInteger("status"));
		}

		/**
		 * 创建文件夹
		 *
		 * @param parentId 父文件夹ID,根目录为"0"
		 * @param name     文件夹名称
		 * @return 包含文件夹ID等JSON格式信息
		 */
		@Contract(pure = true) public JSONObject createFolder(@NotNull String parentId, @NotNull String name) {
			JSONObject data = new JSONObject();
			data.put("dir_init_lock", false);
			data.put("dir_path", "");
			data.put("file_name", name);
			data.put("pdir_fid", "0");
			return JSONObject.parseObject(conn.url(fileUrl).requestBody(data.toJSONString()).post().text());
		}

		/**
		 * 获取用户主页的文件信息
		 *
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHome() {
			return getInfosAsHomeOfFolder("0");
		}

		/**
		 * 获取用户主页的指定文件夹下的文件信息
		 *
		 * @param folderId 文件夹ID,根目录为0
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHomeOfFolder(@NotNull String folderId) {
			return JSONObject.parseArray(JSONObject.parseObject(conn.url(sortUrl + folderId).get().text()).getJSONObject("data").getString("list"),
					JSONObject.class);
		}

		/**
		 * 上传文件(大文件可能不会成功)
		 *
		 * @param src 待上传的文件路径
		 * @param fid 存放目录ID
		 * @return 上传状态
		 */
		@Contract(pure = true) public boolean upload(@NotNull String src, @NotNull String fid) {
			return upload(new File(src), fid);
		}

		/**
		 * 上传文件(大文件可能不会成功)
		 *
		 * @param file 待上传的文件
		 * @param fid  存放目录ID
		 * @return 上传状态
		 */
		@Contract(pure = true) public boolean upload(@NotNull File file, @NotNull String fid) {
			try (FileInputStream in = new FileInputStream(file)) {
				SimpleDateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				String date = format.format(new Date());
				JSONObject preData = new JSONObject();
				preData.put("ccp_hash_update", true);
				preData.put("parallel_upload", true);
				preData.put("pdir_fid", fid);
				preData.put("dir_name", "");
				preData.put("size", file.length());
				preData.put("file_name", file.getName());
				preData.put("format_type", URLConnection.guessContentTypeFromName(file.getName()));
				JSONObject preInfo = JSONObject.parseObject(conn.url(preUrl).requestBody(preData.toJSONString()).post().text()).getJSONObject("data");
				String authInfo = preInfo.getString("auth_info");
				String taskId = preInfo.getString("task_id");
				String bucket = preInfo.getString("bucket");
				String key = preInfo.getString("obj_key");
				String uploadId = preInfo.getString("upload_id");
				String userAgent = "aliyun-sdk-js/1.0.0 Chrome 102.0.5139.184 on Linux i686";
				JSONObject authData = new JSONObject();
				authData.put("auth_info", authInfo);
				authData.put("task_id", taskId);
				authData.put("auth_meta", "PUT\n\nimage/jpeg\n" + date + "\nx-oss-date:" + date + "\nx-oss-user-agent:" + userAgent + "\n" + bucket + "/" + key
						+ "?partNumber=1&uploadId=" + uploadId);
				String auth = JSONObject.parseObject(conn.url(authUrl).requestBody(authData.toJSONString()).post().text()).getJSONObject("data")
						.getString("auth_key");
				String uploadUrl = "https://" + bucket + ".oss-cn-zhangjiakou.aliyuncs.com/" + key + "?uploadId=" + uploadId + "&partNumber=1";
				Map<String, String> headers = new HashMap<>();
				headers.put("x-oss-date", date);
				headers.put("x-oss-user-agent", userAgent);
				conn.url(uploadUrl).file(file.getName(), in).headers(headers).auth(auth).method(Method.PUT).execute();
				JSONObject hashData = new JSONObject();
				hashData.put("md5", FilesUtils.getMD5(file));
				hashData.put("sha1", FilesUtils.getSHA1(file));
				hashData.put("task_id", taskId);
				return JSONObject.parseObject(conn.newRequest().url(hashUrl).requestBody(hashData.toJSONString()).post().text()).getJSONObject("data")
						.getBoolean("finish");
			} catch (IOException e) {
				return false;
			}
		}

	}

}