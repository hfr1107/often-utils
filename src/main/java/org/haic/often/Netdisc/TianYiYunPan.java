package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.haic.often.Judge;
import org.haic.often.Network.*;
import org.haic.often.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.Function;
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
	 * @return JSON数据类型
	 */
	@Contract(pure = true) public static JSONObject getshareUrlInfo(@NotNull String shareUrl) {
		String code = shareUrl.contains("code") ? StringUtils.extractRegex(shareUrl, "code=.*").substring(5) : shareUrl.substring(shareUrl.lastIndexOf("/"));
		return JSONObject.parseObject(JsoupUtil.connect(shareInfoByCodeUrl).data("shareCode", code).headers(headers).get().text());
	}

	/**
	 * 获得分享页面所有文件的信息
	 *
	 * @param url       天翼URL
	 * @param shareCode 提取码
	 * @return List - JSON数据类型,包含文件所有信息
	 */
	@Contract(pure = true) public static List<JSONObject> getFilesInfoAsPage(@NotNull String url, @NotNull String shareCode) {
		JSONObject info = getshareUrlInfo(url);
		String shareId = info.getString("shareId");
		Map<String, String> data = new HashMap<>();
		data.put("fileId", info.getString("fileId"));
		data.put("shareId", shareId);
		data.put("isFolder", info.getString("isFolder"));
		data.put("shareMode", info.getString("shareMode"));
		data.put("accessCode", shareCode);
		List<JSONObject> result = getFilesInfoAsPage(data);
		result.forEach(l -> l.put("shareId", shareId));
		return result;
	}

	/**
	 * 通过配置获得分享页面所有文件的信息,如果文件非常多的话,可能要花较长时间
	 *
	 * @param data 配置信息,必须包含key
	 *             <p>
	 *             &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"fileId"
	 *             <p>
	 *             &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"shareId"
	 *             <p>
	 *             &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"isFolder"
	 *             <p>
	 *             &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"shareMode"
	 *             <p>
	 *             &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"accessCode"
	 * @return List - JSON数据类型,包含文件所有信息
	 */
	@Contract(pure = true) public static List<JSONObject> getFilesInfoAsPage(@NotNull Map<String, String> data) {
		Connection conn = JsoupUtil.connect(listShareDirUrl).headers(headers);
		JSONObject infos = JSONObject.parseObject(conn.data(data).execute().body()).getJSONObject("fileListAO");
		if (Judge.isEmpty(infos.getInteger("count"))) {
			return new ArrayList<>();
		}
		List<JSONObject> filesInfo = new ArrayList<>(JSONObject.parseArray(infos.getJSONArray("fileList").toJSONString(), JSONObject.class));
		Map<String, String> thisData = new HashMap<>(data);
		for (JSONObject folderInfo : JSONObject.parseArray(infos.getJSONArray("folderList").toJSONString(), JSONObject.class)) {
			thisData.put("fileId", folderInfo.getString("id"));
			filesInfo.addAll(getFilesInfoAsPage(thisData));
		}
		return filesInfo;
	}

	/**
	 * 天翼云盘的API操作
	 */
	public static class TianYiYunPanAPI {

		public static final String fileDownloadUrl = "https://cloud.189.cn/api/open/file/getFileDownloadUrl.action";
		public static final String listFilesUrl = "https://cloud.189.cn/api/open/file/listFiles.action";
		public static final String createBatchTaskUrl = "https://cloud.189.cn/api/open/batch/createBatchTask.action";
		public static final String createShareLinkUrl = "https://cloud.189.cn/api/open/share/createShareLink.action";
		public static final String cancelShareUrl = "https://cloud.189.cn/api/portal/cancelShare.action";
		public static final String listSharesUrl = "https://cloud.189.cn/api/portal/listShares.action?";
		public static final String renameFileUrl = "https://cloud.189.cn/api/open/file/renameFile.action";
		public static final String renameFolderUrl = "https://cloud.189.cn/api/open/file/renameFolder.action";
		public static final String createFolderUrl = "https://cloud.189.cn/api/open/file/createFolder.action";
		public static final String listRecycleBinFilesUrl = "https://cloud.189.cn/api/open/file/listRecycleBinFiles.action";

		public Connection conn;
		public Map<String, String> cookies;

		protected TianYiYunPanAPI(@NotNull Map<String, String> cookies) {
			this.cookies = cookies;
			conn = HttpsUtil.connect(listSharesUrl).headers(headers).cookies(cookies);
		}

		/**
		 * 获取分享页面所有文件信息
		 *
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> listShares() {
			Map<String, String> data = new HashMap<>();
			data.put("pageNum", "1");
			data.put("pageSize", "1");
			data.put("shareType", "1");
			data.put("pageSize", JSONObject.parseObject(conn.url(listSharesUrl).data(data).execute().body()).getString("recordCount"));
			return JSONObject.parseArray(JSONObject.parseObject(conn.url(listSharesUrl).data(data).execute().body()).getJSONArray("data").toJSONString(),
					JSONObject.class);
		}

		/**
		 * 取消分享文件
		 *
		 * @param shareId 分享ID,可指定多个
		 * @return 返回的响应结果状态码
		 */
		@Contract(pure = true) public int cancelShare(@NotNull String... shareId) {
			return cancelShare(Arrays.asList(shareId));
		}

		/**
		 * 取消分享文件
		 *
		 * @param shareIdList 分享ID列表
		 * @return 返回的响应结果状态码
		 */
		@Contract(pure = true) public int cancelShare(@NotNull List<String> shareIdList) {
			return JSONObject.parseObject(
							conn.url(cancelShareUrl).requestBody("shareIdList=" + String.join(",", shareIdList) + "&cancelType=" + 1).execute().body())
					.getInteger("res_code");
		}

		/**
		 * 自定义分享文件
		 *
		 * @param fileId 待分享的文件ID
		 * @param time   分享的时间(例: 1为1天), 永久为2099
		 * @param type   分享类型: 2-公开,3 - 私密,other - 社交
		 * @return 响应结果
		 */
		@Contract(pure = true) public String createShareLink(@NotNull String fileId, int time, int type) {
			return conn.url(createShareLinkUrl).requestBody("fileId=" + fileId + "&expireTime=" + type + "&shareType=" + type).execute().body();
		}

		/**
		 * 获取回收站的文件列表
		 *
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> listRecycleBinFiles() {
			Map<String, String> data = new HashMap<>();
			data.put("pageNum", "1");
			data.put("pageSize", "1");
			data.put("iconOption", "1");
			data.put("family", "false");
			data.put("pageSize", JSONObject.parseObject(conn.url(listSharesUrl).data(data).execute().body()).getString("recordCount"));
			return JSONObject.parseArray(JSONObject.parseObject(conn.url(listSharesUrl).data(data).execute().body()).getJSONArray("data").toJSONString(),
					JSONObject.class);
		}

		/**
		 * 还原多个回收站的文件或文件夹
		 *
		 * @param fileInfo 指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int restore(@NotNull JSONObject fileInfo) {
			return batchTask("RESTORE", fileInfo, "");
		}

		/**
		 * 还原回收站的文件或文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0位成功
		 */
		@Contract(pure = true) public int restore(@NotNull List<JSONObject> filesInfo) {
			return batchTask("RESTORE", filesInfo, "");
		}

		/**
		 * 清空回收站
		 *
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int emptyRecycle() {
			return batchTask("EMPTY_RECYCLE", "[]", "");
		}

		/**
		 * 删除多个回收站的文件或文件夹
		 *
		 * @param fileInfo 指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int clearRecycle(@NotNull JSONObject fileInfo) {
			return batchTask("CLEAR_RECYCLE", fileInfo, "");
		}

		/**
		 * 删除回收站的文件或文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int clearRecycle(@NotNull List<JSONObject> filesInfo) {
			return batchTask("CLEAR_RECYCLE", filesInfo, "");
		}

		/**
		 * 重命名文件夹名称
		 *
		 * @param folderId   文件夹ID
		 * @param folderName 重命名后的文件夹名称
		 * @return 返回的响应结果状态码
		 */
		@Contract(pure = true) public int renameFolder(@NotNull String folderId, String folderName) {
			return JSONObject.parseObject(conn.url(renameFileUrl).requestBody("folderId=" + folderId + "&destFolderName=" + folderName).execute().body())
					.getInteger("res_code");
		}

		/**
		 * 重命名文件名称
		 *
		 * @param fileId   文件ID
		 * @param fileName 重命名后的文件名称
		 * @return 返回的响应结果状态码
		 */
		@Contract(pure = true) public int renameFile(@NotNull String fileId, String fileName) {
			return JSONObject.parseObject(conn.url(renameFileUrl).requestBody("fileId=" + fileId + "&destFileName=" + fileName).execute().body())
					.getInteger("res_code");
		}

		/**
		 * 创建文件夹
		 *
		 * @param folderId   文件夹ID
		 * @param folderName 文件夹名称
		 * @return 返回的响应结果状态码
		 */
		@Contract(pure = true) public int createFolder(@NotNull String folderId, String folderName) {
			return JSONObject.parseObject(conn.url(renameFileUrl).requestBody("parentFolderId=" + folderId + "&folderName=" + folderName).execute().body())
					.getInteger("res_code");
		}

		/**
		 * 根据配置删除多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int delete(@NotNull List<JSONObject> filesInfo) {
			return batchTask("DELETE", filesInfo, "");
		}

		/**
		 * 删除单个个文件或文件夹到指定文件夹
		 *
		 * @param fileInfo 文指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int delete(@NotNull JSONObject fileInfo) {
			return batchTask("DELETE", fileInfo, "");
		}

		/**
		 * 根据配置复制多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @param folderId  目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int copy(@NotNull List<JSONObject> filesInfo, @NotNull String folderId) {
			return batchTask("COPY", filesInfo, folderId);
		}

		/**
		 * 复制单个文件或文件夹到指定文件夹
		 *
		 * @param fileInfo 指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @param folderId 目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int copy(@NotNull JSONObject fileInfo, @NotNull String folderId) {
			return batchTask("COPY", fileInfo, folderId);
		}

		/**
		 * 根据配置移动多个文件或文件夹到指定文件夹
		 *
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @param folderId  目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int move(@NotNull List<JSONObject> filesInfo, @NotNull String folderId) {
			return batchTask("MOVE", filesInfo, folderId);
		}

		/**
		 * 移动单个文件或文件夹到指定文件夹
		 *
		 * @param fileInfo 指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @param folderId 目标文件夹ID
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int move(@NotNull JSONObject fileInfo, @NotNull String folderId) {
			return batchTask("MOVE", fileInfo, folderId);
		}

		/**
		 * 对多个文件或文件夹执行批处理脚本操作,用于文件移动,删除,复制等
		 *
		 * @param type      操作类型
		 * @param filesInfo 指定的多个文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull List<JSONObject> filesInfo, @NotNull String folderId) {
			JSONArray taskInfos = new JSONArray();
			for (JSONObject fileInfo : filesInfo) {
				JSONObject taskInfo = new JSONObject();
				taskInfo.put("fileName", fileInfo.getString("name"));
				taskInfo.put("fileId", fileInfo.getString("id"));
				taskInfos.add(taskInfo);
			}
			return batchTask(type, taskInfos.toJSONString(), folderId);
		}

		/**
		 * 对单个文件或文件夹执行脚本操作,用于文件移动,删除,复制等
		 *
		 * @param type     操作类型
		 * @param fileInfo 指定的文件或文件夹,JSON类型数据,需包含"name"和"id"选项
		 * @param folderId 目标文件夹ID,注意如果当前操作(如删除)没有关联文件夹,指定空字符串
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull JSONObject fileInfo, @NotNull String folderId) {
			JSONObject taskInfo = new JSONObject();
			taskInfo.put("fileName", fileInfo.getString("name"));
			taskInfo.put("fileId", fileInfo.getString("id"));
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
		 * @return 操作返回的结果状态码, 一般情况下, 0表示成功
		 */
		@Contract(pure = true) public int batchTask(@NotNull String type, @NotNull String taskInfos, @NotNull String folderId) {
			Map<String, String> data = new HashMap<>();
			data.put("type", type);
			data.put("taskInfos", taskInfos);
			data.put("targetFolderId", folderId);
			return JSONObject.parseObject(conn.url(createBatchTaskUrl).data(data).execute().body()).getInteger("res_code");
		}

		/**
		 * 通过文件ID获取文件的直链,不能用于分享页面获取
		 *
		 * @param fileId 文件ID
		 * @return 用于下载的直链
		 */
		@Contract(pure = true) public String getStraight(@NotNull String fileId) {
			return JSONObject.parseObject(conn.url(fileDownloadUrl).data("fileId", fileId).get().text()).getString("fileDownloadUrl");
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件和文件夹信息
		 *
		 * @param folderId 文件夹ID
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> getInfoAsHomeOfFolderId(@NotNull String folderId) {
			Map<String, String> data = new HashMap<>();
			data.put("pageSize", "1");
			data.put("folderId", folderId);
			String urlInfo = conn.url(listFilesUrl).data(data).get().text();
			data.put("pageSize", JSONObject.parseObject(urlInfo).getJSONObject("fileListAO").getString("count"));
			urlInfo = conn.url(listFilesUrl).data(data).get().text();
			JSONObject fileListAO = JSONObject.parseObject(urlInfo).getJSONObject("fileListAO");
			JSONArray filesList = new JSONArray();
			filesList.addAll(fileListAO.getJSONArray("fileList"));
			filesList.addAll(fileListAO.getJSONArray("folderList"));
			return JSONObject.parseArray(filesList.toJSONString(), JSONObject.class);
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件信息
		 *
		 * @param folderId 文件夹ID
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> getFilesInfoAsHomeOfFolderId(@NotNull String folderId) {
			return getInfoAsHomeOfFolderId("-11").stream().filter(l -> !l.containsKey("fileCount")).collect(Collectors.toList());
		}

		/**
		 * 通过文件夹ID获取当前页面所有文件夹信息
		 *
		 * @param folderId 文件夹ID
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> getFoldersInfoAsHomeOfFolderId(@NotNull String folderId) {
			return getInfoAsHomeOfFolderId("-11").stream().filter(l -> l.containsKey("fileCount")).collect(Collectors.toList());
		}

		/**
		 * 获取主页面所有文件和文件夹信息
		 *
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> getFilesInfoAsHome() {
			return getInfoAsHomeOfFolderId("-11"); // -11 主页
		}

		/**
		 * 获取指定文件夹下所有文件信息,[-11]为根目录,获取全部文件信息
		 *
		 * @param folderId 文件夹ID
		 * @return List - JSON类型数据,包含了文件的所有信息
		 */
		@Contract(pure = true) public List<JSONObject> getFilesInfoAsHomeOfAll(@NotNull String folderId) {
			List<JSONObject> filesInfo = new ArrayList<>();
			for (JSONObject fileInfo : getInfoAsHomeOfFolderId(folderId)) {
				if (fileInfo.containsKey("fileCount")) {
					filesInfo.addAll(Judge.isEmpty(fileInfo.getInteger("fileCount")) ? new ArrayList<>() : getInfoAsHomeOfFolderId(fileInfo.getString("id")));
				} else {
					filesInfo.add(fileInfo);
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
			JSONObject info = getshareUrlInfo(url);
			return JSONObject.parseObject(
							conn.url(fileDownloadUrl + "?dt=1&fileId=" + info.getString("fileId") + "&shareId=" + info.getString("shareId")).get().text())
					.getString("fileDownloadUrl");
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
			for (JSONObject fileInfo : getFilesInfoAsPage(url, accessCode)) {
				Map<String, String> params = new HashMap<>();
				params.put("dt", "1");
				params.put("fileId", fileInfo.getString("id"));
				params.put("shareId", fileInfo.getString("shareId"));
				fileUrls.put(fileInfo.getString("name"),
						JSONObject.parseObject(conn.url(fileDownloadUrl).data(params).get().text()).getString("fileDownloadUrl"));
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

		/**
		 * 通过账号密码登录获得用户cookies
		 * <p>
		 * js逆向方法,直接通过api获取登陆cookie
		 *
		 * @param userName 用户名
		 * @param password 密码
		 * @return 此链接, 用于API操作
		 */
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
			Function<Integer, Character> intTochar = i -> "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray()[i];
			String b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
			StringBuilder d = new StringBuilder();
			int e = 0, c = 0;
			for (char value : a) {
				if (value != '=') {
					int v = b64map.indexOf(value);
					switch (e) {
					case 0 -> {
						e = 1;
						d.append(intTochar.apply(v >> 2));
						c = 3 & v;
					}
					case 1 -> {
						e = 2;
						d.append(intTochar.apply(c << 2 | v >> 4));
						c = 15 & v;
					}
					case 2 -> {
						e = 3;
						d.append(intTochar.apply(c));
						d.append(intTochar.apply(v >> 2));
						c = 3 & v;
					}
					default -> {
						e = 0;
						d.append(intTochar.apply(c << 2 | v >> 4));
						d.append(intTochar.apply(15 & v));
					}
					}
				}
			}
			return e == 1 ? String.valueOf(d) + intTochar.apply(c << 2) : String.valueOf(d);
		}

	}

}