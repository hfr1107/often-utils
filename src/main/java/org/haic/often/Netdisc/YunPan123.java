package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Judge;
import org.haic.often.Network.Connection;
import org.haic.often.Network.HttpsUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

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
	 * @param auth 身份识别标识
	 * @return 此链接, 用于API操作
	 */
	@Contract(pure = true) public static YunPan123API login(@NotNull String auth) {
		return new YunPan123API(auth);
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
		protected static final String uploadRequestUrl = "https://www.123pan.com/a/api/file/upload_request";
		protected static final String modPidUrl = "https://www.123pan.com/a/api/file/mod_pid";
		protected static final String trashUrl = "https://www.123pan.com/a/api/file/trash";
		protected static final String renameUrl = "https://www.123pan.com/a/api/file/rename";
		protected static final String createShareUrl = "https://www.123pan.com/a/api/share/create";
		protected static final String shareListUrl = "https://www.123pan.com/a/api/share/list";
		protected static final String shareDeleteUrl = "https://www.123pan.com/a/api/share/delete";
		protected static final String fileDeleteUrl = "https://www.123pan.com/a/api/file/delete";
		protected static final String trashDeleteAllUrl = "https://www.123pan.com/a/api/file/trash_delete_all";
		protected static final String fileTrashUrl = "https://www.123pan.com/a/api/file/trash";

		protected Connection conn = HttpsUtil.newSession();

		protected YunPan123API(@NotNull String auth) {
			conn = conn.authorization(auth);
		}

		/**
		 * 获取回收站的文件列表
		 *
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> listRecycleBinFiles() {
			return InfoListAsHome("0", "", true);
		}

		/**
		 * 还原回收站的文件或文件夹
		 *
		 * @param fileId 文件或文件夹ID,可指定多个
		 * @return 操作返回的结果状态码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int restore(@NotNull String... fileId) {
			return restore(Arrays.asList(fileId));
		}

		/**
		 * 还原回收站的文件或文件夹
		 *
		 * @param fileIdList 文件或文件夹ID列表
		 * @return 操作返回的结果状态码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int restore(@NotNull List<String> fileIdList) {
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("operation", "false");
			data.put("fileTrashInfoList", fileIdList.stream().map(l -> new JSONObject() {{
				put("fileId", l);
			}}).toList());
			return JSONObject.parseObject(conn.url(fileTrashUrl).requestBody(data.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 清空回收站
		 *
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int emptyRecycle() {
			return JSONObject.parseObject(conn.url(trashDeleteAllUrl).requestBody(new JSONObject().toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 删除多个回收站的文件或文件夹
		 *
		 * @param fileId 指定的文件或文件夹,可指定多个
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int clearRecycle(@NotNull String... fileId) {
			return clearRecycle(Arrays.asList(fileId));
		}

		/**
		 * 删除多个回收站的文件或文件夹
		 *
		 * @param fileIdList 指定的文件或文件夹ID列表
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int clearRecycle(@NotNull List<String> fileIdList) {
			return JSONObject.parseObject(conn.url(fileDeleteUrl).requestBody(new JSONObject() {{
				put("fileIdList", fileIdList.stream().map(l -> new JSONObject() {{
					put("fileId", l);
				}}).toList());
			}}.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 取消已分享的文件
		 *
		 * @param shareId 分享ID,可指定多个
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int cancelShare(@NotNull String... shareId) {
			return cancelShare(Arrays.asList(shareId));
		}

		/**
		 * 取消已分享的文件
		 *
		 * @param shareIdList 分享ID列表
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int cancelShare(@NotNull List<String> shareIdList) {
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("shareInfoList", shareIdList.stream().map(l -> new JSONObject() {{
				put("shareId", l);
			}}).toList());
			return JSONObject.parseObject(conn.url(shareDeleteUrl).requestBody(data.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 获取已分享列表
		 *
		 * @return 文件列表信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> listShares() {
			return listShares("");
		}

		/**
		 * 获取匹配搜索项的已分享列表
		 *
		 * @param search 搜索数据
		 * @return 文件列表信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> listShares(@NotNull String search) {
			return JSONObject.parseArray(JSONObject.parseObject(
							conn.url(shareListUrl).requestBody("driveId=0&limit=10000&next=0&orderBy=fileId&orderDirection=desc&SearchData=" + search).get().text())
					.getJSONObject("data").getJSONArray("InfoList").toJSONString(), JSONObject.class);
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareName 分享显示的名称
		 * @param fileId    分享文件ID,如果存在多个,用','分割
		 * @return 返回的JSON数据
		 */
		@Contract(pure = true) public JSONObject createShare(@NotNull String shareName, @NotNull String fileId) {
			return createShare(shareName, fileId, "", 1);
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareName 分享显示的名称
		 * @param fileId    分享文件ID,如果存在多个,用','分割
		 * @param day       分享时间
		 * @return 返回的JSON数据
		 */
		@Contract(pure = true) public JSONObject createShare(@NotNull String shareName, @NotNull String fileId, int day) {
			return createShare(shareName, fileId, "", day);
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareName 分享显示的名称
		 * @param fileId    分享文件ID,如果存在多个,用','分割
		 * @param sharePwd  分享密码
		 * @return 返回的JSON数据
		 */
		@Contract(pure = true) public JSONObject createShare(@NotNull String shareName, @NotNull String fileId, @NotNull String sharePwd) {
			return createShare(shareName, fileId, sharePwd, 1);
		}

		/**
		 * 分享指定文件
		 *
		 * @param shareName 分享显示的名称
		 * @param fileId    分享文件ID,如果存在多个,用','分割
		 * @param sharePwd  分享密码
		 * @param day       分享时间
		 * @return 返回的JSON数据
		 */
		@Contract(pure = true) public JSONObject createShare(@NotNull String shareName, @NotNull String fileId, @NotNull String sharePwd, int day) {
			Calendar time = Calendar.getInstance();
			time.add(Calendar.DAY_OF_MONTH, day);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssXXX");
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("expiration", format.format(time.getTime()));
			data.put("fileIdList", fileId);
			data.put("shareName", shareName);
			data.put("sharePwd", sharePwd);
			return JSONObject.parseObject(conn.url(createShareUrl).requestBody(data.toJSONString()).post().text());
		}

		/**
		 * 创建文件夹
		 *
		 * @param parentFileId 父文件夹ID,0为根目录
		 * @param fileName     文件夹名称
		 * @return 返回的JSON数据
		 */
		@Contract(pure = true) public JSONObject createFolder(@NotNull String parentFileId, @NotNull String fileName) {
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("etag", "");
			data.put("fileName", fileName);
			data.put("parentFileId", parentFileId);
			data.put("size", "0");
			data.put("type", "1");
			data.put("duplicate", "1");
			data.put("NotReuse", "true");
			return JSONObject.parseObject(conn.url(uploadRequestUrl).requestBody(data.toJSONString()).post().text());
		}

		/**
		 * 重命名文件或文件夹
		 *
		 * @param fileId   文件ID
		 * @param fileName 重命名后的文件名
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int rename(@NotNull String fileId, @NotNull String fileName) {
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("fileId", fileId);
			data.put("fileName", fileName);
			return JSONObject.parseObject(conn.url(renameUrl).requestBody(data.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 删除文件或文件夹
		 *
		 * @param fileId 待删除的文件ID,可指定多个
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int delete(@NotNull String... fileId) {
			return delete(List.of(fileId));
		}

		/**
		 * 删除文件或文件夹
		 *
		 * @param fileIdList 待删除的文件ID列表
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int delete(@NotNull List<String> fileIdList) {
			JSONObject data = new JSONObject();
			data.put("driveId", "0");
			data.put("operation", "true");
			data.put("fileTrashInfoList", new JSONArray() {{
				addAll(fileIdList.stream().map(l -> new JSONObject() {{
					put("fileId", l);
				}}).toList());
			}});
			return JSONObject.parseObject(conn.url(trashUrl).requestBody(data.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 移动文件到指定文件夹下
		 *
		 * @param parentFileId 移动后的文件夹ID
		 * @param fileId       需要移动的文件ID,可指定多个
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int move(@NotNull String parentFileId, @NotNull String... fileId) {
			return move(parentFileId, List.of(fileId));
		}

		/**
		 * 移动文件到指定文件夹下
		 *
		 * @param parentFileId 移动后的文件夹ID
		 * @param fileIdList   需要移动的文件ID列表
		 * @return 返回执行结果代码, 一般情况下, 0为成功
		 */
		@Contract(pure = true) public int move(@NotNull String parentFileId, @NotNull List<String> fileIdList) {
			JSONObject data = new JSONObject();
			data.put("parentFileId", parentFileId);
			data.put("fileIdList", new JSONArray() {{
				addAll(fileIdList.stream().map(l -> new JSONObject() {{
					put("fileId", l);
				}}).toList());
			}});
			return JSONObject.parseObject(conn.url(modPidUrl).requestBody(data.toJSONString()).post().text()).getInteger("code");
		}

		/**
		 * 获取用户主页的所有文件信息
		 *
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHome() {
			return getInfosAsHome("");
		}

		/**
		 * 获取用户主页的匹配搜索项的文件信息
		 *
		 * @param search 待搜索数据
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHome(@NotNull String search) {
			return getInfosAsHomeOfFolder("0", search);
		}

		/**
		 * 获取用户主页的指定文件夹下的文件信息
		 *
		 * @param fileId 文件夹ID
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHomeOfFolder(@NotNull String fileId) {
			return getInfosAsHomeOfFolder(fileId, "");
		}

		/**
		 * 获取用户主页的指定文件夹下的匹配搜索项的文件信息
		 *
		 * @param fileId 文件夹ID
		 * @param search 待搜索数据
		 * @return 文件信息JSON数组
		 */
		@Contract(pure = true) public List<JSONObject> getInfosAsHomeOfFolder(@NotNull String fileId, @NotNull String search) {
			return InfoListAsHome(fileId, search, false);
		}

		/**
		 * 获取文件夹内文件信息
		 *
		 * @param fileId  文件夹ID
		 * @param search  搜索数据
		 * @param trashed 是否为垃圾站
		 * @return 文件列表
		 */
		@Contract(pure = true) protected List<JSONObject> InfoListAsHome(@NotNull String fileId, @NotNull String search, boolean trashed) {
			Map<String, String> data = new HashMap<>();
			data.put("driveId", "0");
			data.put("limit", "1000");
			data.put("next", "0");
			data.put("orderBy", "fileId");
			data.put("orderDirection", "desc");
			data.put("parentFileId", fileId);
			data.put("trashed", String.valueOf(trashed));
			data.put("SearchData", search);
			data.put("Page", "1");
			JSONArray filesInfo = new JSONArray();
			JSONObject info = JSONObject.parseObject(conn.url(filelistUrl).data(data).get().text()).getJSONObject("data");
			filesInfo.addAll(info.getJSONArray("InfoList"));
			for (int i = 2; !info.getString("Next").equals("-1"); i++) {
				info = JSONObject.parseObject(conn.url(filelistUrl).data("Page", String.valueOf(i)).get().text()).getJSONObject("data");
				filesInfo.addAll(info.getJSONArray("InfoList"));
			}
			return JSONObject.parseArray(filesInfo.toJSONString(), JSONObject.class);
		}

		/**
		 * 通过文件信息配置获取文件直链
		 *
		 * @param fileInfo 文件信息
		 * @return 文件直链
		 */
		@Contract(pure = true) public String getStraight(@NotNull JSONObject fileInfo) {
			JSONObject data = new JSONObject();
			data.put("type", fileInfo.getString("Type"));
			if (data.getInteger("type") == 1) {
				return null;
			}
			data.put("driveId", "0");
			data.put("fileName", fileInfo.getString("FileName"));
			data.put("fileId", fileInfo.getString("FileId"));
			data.put("size", fileInfo.getString("Size"));
			data.put("etag", fileInfo.getString("Etag"));
			data.put("s3KeyFlag", fileInfo.get("S3KeyFlag"));
			return JSONObject.parseObject(conn.url(fileDownloadInfoUrl).requestBody(data.toJSONString()).post().text()).getJSONObject("data")
					.getString("DownloadUrl");
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
			return JSONObject.parseObject(HttpsUtil.connect(signinUrl).requestBody(new JSONObject() {{
				put("passport", username);
				put("password", password);
			}}.toJSONString()).post().text()).getJSONObject("data").getString("token");
		}
	}

}