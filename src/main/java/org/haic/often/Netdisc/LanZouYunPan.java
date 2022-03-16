package org.haic.often.Netdisc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.haic.often.Judge;
import org.haic.often.Network.Jsoup.JsoupUtil;
import org.haic.often.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 蓝奏云盘API
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/18 23:59
 */
public class LanZouYunPan {

	public static final String domain = "https://www.lanzoui.com/";
	public static final String downApi = domain + "ajaxm.php";

	/**
	 * 获取分享页面的文件列表的所有文件信息<br/>
	 * 文件列表页面
	 *
	 * @param lanzouUrl 蓝奏URL
	 * @param passwd    访问密码
	 * @return Map - 文件名, 文件ID链接
	 */
	public static Map<String, String> getInfosAsPage(@NotNull String lanzouUrl, @NotNull String passwd) {
		String infos = Objects.requireNonNull(JsoupUtil.connect(lanzouUrl).get().selectFirst("body script")).toString();
		infos = infos.substring(32, infos.indexOf("json") - 20).replaceAll("\t*　* *'*;*", "");

		// 获取post参数
		Map<String, String> params = new HashMap<>();
		for (String data : StringUtils.extractRegex(infos.replaceAll("\n", ""), "data.*").substring(6).split(",")) {
			String[] entry = data.split(":");
			params.put(entry[0], entry[1]);
		}

		// 获取修正后的参数
		String pgs = StringUtils.extractRegex(infos, "pgs=.*");
		pgs = pgs.substring(pgs.indexOf("=") + 1);
		String t = StringUtils.extractRegex(infos, params.get("t") + "=.*");
		t = t.substring(t.indexOf("=") + 1);
		String k = StringUtils.extractRegex(infos, params.get("k") + "=.*");
		k = k.substring(k.indexOf("=") + 1);

		// 修正post参数
		params.put("pg", pgs);
		params.put("t", t);
		params.put("k", k);
		params.put("pwd", passwd);

		// 处理json数据
		JSONArray jsonArray = JSONObject.parseObject(JsoupUtil.connect(domain + "filemoreajax.php").data(params).post().text()).getJSONArray("text");
		Map<String, String> result = new HashMap<>();
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject info = jsonArray.getJSONObject(i);
			result.put(info.getString("name_all"), domain + info.getString("id"));
		}
		return result;
	}

	/**
	 * 获取分享页面的文件列表的所有文件信息<br/>
	 * 文件列表页面
	 *
	 * @param lanzouUrl 蓝奏URL
	 * @return Map - 文件名, 文件ID链接
	 */
	public static Map<String, String> getInfosAsPage(@NotNull String lanzouUrl) {
		return getInfosAsPage(lanzouUrl, "");
	}

	/**
	 * 获取分享页面的文件列表的所有文件的直链<br/>
	 * 文件列表页面
	 *
	 * @param lanzouUrl 蓝奏URL
	 * @param passwd    访问密码
	 * @return 文件直链集合
	 */
	@NotNull @Contract(pure = true) public static Map<String, String> getStraightsAsPage(@NotNull String lanzouUrl, @NotNull String passwd) {
		return getInfosAsPage(lanzouUrl, passwd).entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, l -> getStraight(l.getValue())));
	}

	/**
	 * 获取分享页面的文件列表的所有文件的直链<br/>
	 * 文件列表页面
	 *
	 * @param lanzouUrl 蓝奏URL
	 * @return 文件直链集合
	 */
	@NotNull @Contract(pure = true) public static Map<String, String> getStraightsAsPage(@NotNull String lanzouUrl) {
		return getInfosAsPage(lanzouUrl).entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, l -> getStraight(l.getValue())));
	}

	/**
	 * 获取单个文件的直链<br/>
	 * 单个文件分享页面
	 *
	 * @param lanzouUrl 蓝奏云文件链接
	 * @return 蓝奏云URL直链
	 */
	@Contract(pure = true) public static String getStraight(@NotNull String lanzouUrl) {
		Document doc = JsoupUtil.connect(lanzouUrl).get();
		String downUrl = domain;
		Element ifr2 = doc.selectFirst("iframe[class='ifr2']");
		if (Judge.isNull(ifr2)) {
			Element downlink = doc.selectFirst("iframe[class='n_downlink']");
			if (Judge.isNull(downlink)) {
				return "";
			}
			downUrl += downlink.attr("src");
		} else {
			downUrl += ifr2.attr("src");
		}
		String infos = Objects.requireNonNull(JsoupUtil.connect(downUrl).get().selectFirst("body script")).toString();
		infos = infos.substring(32, infos.indexOf("json") - 17).replaceAll("\t*　* *'*;*", "");

		// 获取post参数
		Map<String, String> params = new HashMap<>();
		for (String data : StringUtils.extractRegex(infos.replaceAll("\n", ""), "data:[\\s\\S]*websignkey").substring(6).split(",")) {
			String[] entry = data.split(":");
			params.put(entry[0], entry[1]);
		}

		// 获取修正后的参数
		String signs = StringUtils.extractRegex(infos, params.get("signs") + "=.*");
		signs = signs.substring(signs.indexOf("=") + 1);
		String websign = StringUtils.extractRegex(infos, params.get("websign") + "=.*");
		websign = websign.substring(websign.indexOf("=") + 1);
		String websignkey = StringUtils.extractRegex(infos, params.get("websignkey") + "=.*");
		websignkey = websignkey.substring(websignkey.indexOf("=") + 1);

		// 修正post参数
		params.put("signs", signs);
		params.put("websign", websign);
		params.put("websignkey", websignkey);

		// 处理json数据
		JSONObject fileInfo = JSONObject.parseObject(JsoupUtil.connect(downApi).referrer(downUrl).data(params).post().text());
		return JsoupUtil.connect(fileInfo.getString("dom") + "/file/" + fileInfo.getString("url")).execute().url().toExternalForm();
	}

	/**
	 * 获取单个文件的直链<br/>
	 * 单个文件分享页面
	 *
	 * @param lanzouUrl 蓝奏云文件链接
	 * @param password  提取码
	 * @return 蓝奏云URL直链
	 */
	@Contract(pure = true) public static String getStraight(@NotNull String lanzouUrl, @NotNull String password) {
		JSONObject fileInfo = JSONObject.parseObject(
				JsoupUtil.connect(downApi).requestBody(StringUtils.extractRegex(JsoupUtil.connect(lanzouUrl).execute().body(), "action=.*&p=") + password)
						.referrer(lanzouUrl).post().text());
		return JsoupUtil.connect(fileInfo.getString("dom") + "/file/" + fileInfo.getString("url")).execute().url().toExternalForm();

	}

}