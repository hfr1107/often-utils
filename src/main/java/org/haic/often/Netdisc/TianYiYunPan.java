package org.haic.often.Netdisc;

import org.haic.often.Network.Jsoup.JsoupUtil;
import org.haic.often.StringUtils;
import org.haic.often.Tuple.FourTuple;
import org.haic.often.Tuple.ThreeTuple;
import org.haic.often.Tuple.Tuple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

/**
 * 天翼云盘API
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/18 22:43
 */
public class TianYiYunPan {
	public static final String dataApi = "https://cloud.189.cn/api/open/share/getShareInfoByCode.action";
	public static final String listApi = "https://cloud.189.cn/api/open/share/listShareDir.action";
	public static final String downApi = "https://cloud.189.cn/api/open/file/getFileDownloadUrl.action";

	/**
	 * 在需要提取码但自己没有时,可直接获得文件直链<br/>
	 * 如果有多个文件,返回第一个文件直链
	 *
	 * @param url     天翼URL
	 * @param cookies cookies
	 * @return 文件直链
	 */
	@NotNull @Contract(pure = true) public static String getStraightAsNotCode(@NotNull String url, @NotNull Map<String, String> cookies) {
		FourTuple<String, String, String, String> urlInfo = getUrlInfo(url);
		return JsoupUtil.connect(downApi + "?dt=1&fileId=" + urlInfo.first + "&shareId=" + urlInfo.second).cookies(cookies).retry(true).get().text();
	}

	/**
	 * 获得分享页面所有文件的直链(无密码)
	 *
	 * @param url     天翼URL
	 * @param cookies cookies
	 * @return Map - 文件名 ,文件直链
	 */
	@NotNull @Contract(pure = true) public static Map<String, String> getStraightsAsPage(@NotNull String url, @NotNull Map<String, String> cookies) {
		return getStraightsAsPage(url, "", cookies);
	}

	/**
	 * 获得分享页面所有文件的直链
	 *
	 * @param url        天翼URL
	 * @param accessCode 提取码
	 * @param cookies    cookies
	 * @return Map - 文件名 ,文件直链
	 */
	@NotNull @Contract(pure = true) public static Map<String, String> getStraightsAsPage(@NotNull String url, @NotNull String accessCode,
			@NotNull Map<String, String> cookies) {
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

}