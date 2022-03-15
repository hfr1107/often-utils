package org.haic.often;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 字符串常用工具类
 *
 * @author haicdust
 * @version 1.0
 * @since 2021/3/27 15:12
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

	/**
	 * 行分隔符 "\r\n"
	 */
	public static final String CRLF = "\r\n";
	/**
	 * 双引号
	 */
	public static final String DOUBLE_QUOTES = "\"";

	/**
	 * 单引号 "'"
	 */
	public static final String APOSTROPHE = "'";

	/**
	 * 等于号 "="
	 */
	public static final String EQUAL_SIGN = "=";

	/**
	 * 替换最后一个匹配的字符串
	 *
	 * @param str         源字符串
	 * @param regex       待匹配的字符串
	 * @param replacement 替换的字符串
	 * @return 替换后的字符串
	 */
	@Contract(pure = true) public static String replaceLast(@NotNull String str, @NotNull @NonNls String regex, @NotNull String replacement) {
		return str.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	/**
	 * JSON格式字符串转换Map类
	 *
	 * @param str 源字符串
	 * @return Map - String String
	 */
	@Contract(pure = true) public static Map<String, String> jsonToMap(@NotNull String str) {
		return JSONObject.parseObject(str, new TypeReference<>() {
		});
	}

	/**
	 * Map格式字符串转换Map类<br/>
	 * 字符串应不包含字符','和';',这两个字符为分割符号,键或值内存在会导致严重错误<br/>
	 * ( name=value,name=value or name=value;name=value )
	 *
	 * @param str 源字符串
	 * @return Map - name, value
	 */
	@Contract(pure = true) public static Map<String, String> toMap(@NotNull String str) {
		str = str.startsWith("{") ? str.substring(1) : str;
		str = str.endsWith("}") ? str.substring(0, str.length() - 1) : str;
		return Arrays.stream(str.split(",")).map(l -> l.split("=")).collect(Collectors.toMap(l -> StringUtils.deletePefix(l[0], " "), l -> l[1]));
	}

	/**
	 * Map格式字符串转换Map类<br/>
	 * 字符串应不包含指定的分隔符,否则键或值内存在会导致严重错误
	 *
	 * @param str   源字符串
	 * @param split 分隔符
	 * @return Map - name, value
	 */
	@Contract(pure = true) public static Map<String, String> toMap(@NotNull String str, @NotNull String split) {
		str = str.startsWith("{") ? str.substring(1) : str;
		str = str.endsWith("}") ? str.substring(0, str.length() - 1) : str;
		return Arrays.stream(str.split(split)).map(l -> l.split("=")).collect(Collectors.toMap(l -> StringUtils.deletePefix(l[0], " "), l -> l[1]));
	}

	/**
	 * Map格式字符串转换Json格式<br/>
	 * 字符串应不包含字符','和';',这两个字符为分割符号,键或值内存在会导致严重错误
	 *
	 * @param str 源字符串
	 * @return Json String
	 */
	@Contract(pure = true) public static String toJson(@NotNull String str) {
		return JSONObject.toJSONString(toMap(str));
	}

	/**
	 * 生成一个随机邮箱
	 *
	 * @return 随机邮箱
	 */
	@Contract(pure = true) public static String randomEmail() {
		return (RandomStringUtils.randomAlphanumeric(8, 16) + (char) 64 + RandomStringUtils.randomAlphabetic(4, 8) + (char) 46
				+ RandomStringUtils.randomAlphabetic(2, 4)).toLowerCase();
	}

	/**
	 * 生成一个指定域名的随机邮箱
	 *
	 * @param domain 域名
	 * @return 指定域名的随机邮箱
	 */
	@Contract(pure = true) public static String randomEmail(@NotNull String domain) {
		domain = Judge.isEmpty(domain.indexOf(46)) ? domain.substring(1) : domain;
		int count = StringUtils.countMatches(domain, (char) 46);
		if (Judge.isEmpty(count)) {
			throw new RuntimeException(domain + " not is domain");
		}
		String[] subdomain = domain.split("\\.");
		return (RandomStringUtils.randomAlphanumeric(8, 16) + (char) 64 + subdomain[subdomain.length - 2] + (char) 46 + subdomain[subdomain.length
				- 1]).toLowerCase();
	}

	/**
	 * 生成一个随机手机号
	 *
	 * @return 随机手机号
	 */
	@Contract(pure = true) public static String getPhoneNumber() {
		String[] identifier = { "134", "135", "136", "137", "138", "139", "150", "151", "152", "157", "158", "159", "182", "183", "184", "187", "188", "178",
				"147", "172", "198", "130", "131", "132", "145", "155", "156", "166", "171", "175", "176", "185", "186", "166", "133", "149", "153", "173",
				"177", "180", "181", "189", "199" };
		return identifier[(int) (Math.random() * identifier.length)] + RandomStringUtils.randomNumeric(8);
	}

	/**
	 * 正则提取第一个匹配的字符串
	 *
	 * @param str   待提取的字符串
	 * @param regex 正则表达式
	 * @return 匹配的字符串
	 */
	@Contract(pure = true) public static String extractRegex(@NotNull String str, @NotNull @NonNls String regex) {
		String result = null;
		Matcher matcher = Pattern.compile(regex).matcher(str);
		if (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	/**
	 * 正则提取所有匹配的字符串
	 *
	 * @param str   待提取的字符串
	 * @param regex 正则表达式
	 * @return 匹配的字符串列表
	 */
	@Contract(pure = true) public static List<String> extractRegexList(@NotNull String str, @NotNull @NonNls String regex) {
		List<String> result = new ArrayList<>();
		Matcher matcher = Pattern.compile(regex).matcher(str);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	/**
	 * 正则提取最后一个匹配的字符串
	 *
	 * @param str   待提取的字符串
	 * @param regex 正则表达式
	 * @return 匹配的字符串
	 */
	@Contract(pure = true) public static String extractLastRegex(@NotNull String str, @NotNull @NonNls String regex) {
		String result = null;
		Matcher matcher = Pattern.compile(regex).matcher(str);
		while (matcher.find()) {
			result = matcher.group();
		}
		return result;
	}

	/**
	 * 删除字符串头部和尾部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符串
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deletePefixAndSuffix(@NotNull String str, @NotNull String term) {
		return deleteSuffix(deletePefix(str, term), term);
	}

	/**
	 * 删除字符串头部和尾部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deletePefixAndSuffix(@NotNull String str, char term) {
		return deletePefixAndSuffix(str, String.valueOf(term));
	}

	/**
	 * 删除字符串头部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符串
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deletePefix(@NotNull String str, @NotNull String term) {
		while (!Judge.isEmpty(term) && str.startsWith(term)) {
			str = str.substring(term.length());
		}
		return str;
	}

	/**
	 * 删除字符串头部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deletePefix(@NotNull String str, char term) {
		return deletePefix(str, String.valueOf(term));
	}

	/**
	 * 删除字符串尾部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符串
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deleteSuffix(@NotNull String str, @NotNull String term) {
		while (!Judge.isEmpty(term) && str.endsWith(term)) {
			str = str.substring(0, str.length() - term.length());
		}
		return str;
	}

	/**
	 * 删除字符串尾部符合条件的字符串
	 *
	 * @param str  字符串
	 * @param term 条件字符
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String deleteSuffix(@NotNull String str, char term) {
		return deleteSuffix(str, String.valueOf(term));
	}

	/**
	 * 删除字符串不可见字符
	 *
	 * @param str 字符串
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String filter(@NotNull String str) {
		return filter(str, "");
	}

	/**
	 * 替换字符串不可见字符
	 *
	 * @param str   字符串
	 * @param shift 替换字符
	 * @return 处理后的字符串
	 */
	@NotNull @Contract(pure = true) public static String filter(@NotNull String str, @NotNull String shift) {
		return str.replaceAll("\\p{C}", "");
	}

	/**
	 * UrlEncode转中文
	 *
	 * @param url 链接
	 * @return String
	 */
	@NotNull @Contract(pure = true) public static String decodeByURL(@NotNull String url) {
		String prevURL = "";
		String decodeURL = url;
		while (!prevURL.equals(decodeURL)) {
			prevURL = decodeURL;
			decodeURL = URLDecoder.decode(decodeURL, StandardCharsets.UTF_8);
		}
		return decodeURL;
	}

	/**
	 * 中文转UrlEncode
	 *
	 * @param url 链接
	 * @return String
	 */
	@NotNull @Contract(pure = true) public static String encodeByURL(@NotNull String url) {
		return URLEncoder.encode(url, StandardCharsets.UTF_8);
	}

	/**
	 * 含有unicode 的字符串转一般字符串
	 *
	 * @param unicodeStr 混有 Unicode 的字符串
	 * @return 一般字符串
	 */
	@NotNull @Contract(pure = true) public static String unicodeStrToString(@NotNull String unicodeStr) {
		final String regex = "\\\\u[a-f0-9A-F]{1,4}";
		int count = 0;
		Matcher matcher = Pattern.compile(regex).matcher(unicodeStr);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String oldChar = matcher.group();// 原本的Unicode字符
			String newChar = unicodeToString(oldChar);// 转换为普通字符
			// 在遇见重复出现的unicode代码的时候会造成从源字符串获取非unicode编码字符的时候截取索引越界等
			int index = matcher.start();
			result.append(unicodeStr, count, index);// 添加前面不是unicode的字符
			result.append(newChar);// 添加转换后的字符
			count = index + oldChar.length();// 统计下标移动的位置
		}
		result.append(unicodeStr, count, unicodeStr.length());// 添加末尾不是Unicode的字符
		return String.valueOf(result);
	}

	/**
	 * 字符串转换unicode
	 *
	 * @param str 一般字符串
	 * @return Unicode字符串
	 */
	@NotNull @Contract(pure = true) public static String stringToUnicode(@NotNull String str) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			result.append("\\u").append(Integer.toHexString(str.charAt(i))); // 转换为unicode
		}
		return String.valueOf(result);
	}

	/**
	 * unicode 转字符串
	 *
	 * @param unicode 全为 Unicode 的字符串
	 * @return 一般字符串
	 */
	@NotNull @Contract(pure = true) public static String unicodeToString(@NotNull String unicode) {
		StringBuilder result = new StringBuilder();
		String[] hex = unicode.split("\\\\u");
		for (int i = 1; i < hex.length; i++) {
			result.append((char) Integer.parseInt(hex[i], 16)); // 转换出每一个代码点
		}
		return String.valueOf(result);
	}

	/**
	 * gbk编码格式字符串转换为utf8编码格式字符串
	 *
	 * @param str gbk编码格式字符串
	 * @return utf8编码格式字符串
	 */
	@NotNull @Contract(pure = true) public static String utf8ByGBK(@NotNull String str) {
		String result = null;
		try {
			result = new String(str.getBytes("GBK"), StandardCharsets.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return Objects.requireNonNull(result);
	}

	/**
	 * utf8编码格式字符串转换为gbk编码格式字符串
	 *
	 * @param str utf8编码格式字符串
	 * @return gbk编码格式字符串
	 */
	@NotNull @Contract(pure = true) public static String utf8ToGBK(@NotNull String str) {
		String result = null;
		try {
			result = new String(str.getBytes(StandardCharsets.UTF_8), "GBK");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return Objects.requireNonNull(result);
	}

}