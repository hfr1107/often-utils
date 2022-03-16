package org.haic.often.Network.HtmlUnit;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.haic.often.Judge;
import org.haic.often.Network.HttpStatus;
import org.haic.often.StreamUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 响应接口
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/16 9:24
 */
public class Response {
	protected Page page; // Page对象

	/**
	 * Constructor for the HtmlUnitsResult.
	 *
	 * @param page The Page
	 */
	protected Response(Page page) {
		this.page = page;
	}

	/**
	 * Get Page
	 *
	 * @return Page
	 */
	protected Page getPage() {
		return page;
	}

	/**
	 * 返回此页面的 URL
	 *
	 * @return 此页面的 URL
	 */
	@Contract(pure = true) public String url() {
		return page.getUrl().toExternalForm();
	}

	/**
	 * Get HtmlPage
	 *
	 * @return HtmlPage
	 */
	@Contract(pure = true) public HtmlPage getHtmlPage() {
		return isHtmlPage() ? (HtmlPage) page : null;
	}

	/**
	 * Get statusCode
	 *
	 * @return 网页连接状态码
	 */
	@Contract(pure = true) public int statusCode() {
		return Judge.isNull(page) ? HttpStatus.SC_REQUEST_TIMEOUT : page.getWebResponse().getStatusCode();
	}

	/**
	 * Get cookies
	 *
	 * @return cookies
	 */
	@Contract(pure = true) public Map<String, String> cookies() {
		return page.getWebResponse().getResponseHeaders().stream().filter(l -> l.getName().equalsIgnoreCase("set-cookie")).map(l -> l.getValue().split("="))
				.collect(Collectors.toMap(l -> l[0], l -> Judge.isEmpty(l[1]) ? l[1] : l[1].substring(0, l[1].indexOf(";"))));
	}

	/**
	 * 获取 cookie
	 *
	 * @param name cookie name
	 * @return cookie value
	 */
	@Contract(pure = true) public String cookie(@NotNull String name) {
		return cookies().get(name);
	}

	/**
	 * Get headers
	 *
	 * @return Map
	 */
	@Contract(pure = true) public Map<String, String> headers() {
		Map<String, String> headers = new HashMap<>();
		for (NameValuePair header : page.getWebResponse().getResponseHeaders()) {
			String name = header.getName().toLowerCase();
			String value = header.getValue();
			if (name.equals("set-cookie")) {
				String cookie = headers.get(name);
				headers.put(name, Judge.isEmpty(cookie) ? "[" + value + "]" : cookie.substring(0, cookie.length() - 1) + ", " + value + "]");
			} else {
				headers.put(name, value);
			}
		}
		return headers;
		//return page.getWebResponse().getResponseHeaders().stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
	}

	/**
	 * Get header value
	 *
	 * @return value
	 */
	@Contract(pure = true) public String header(@NotNull String name) {
		String header = headers().get(name);
		header = header.startsWith("[") ? header.substring(1) : header;
		return header.endsWith("]") ? header.substring(0, header.length() - 1) : header;
	}

	/**
	 * 如果此页面是 HtmlPage，则返回 true。
	 *
	 * @return true or false
	 */
	@Contract(pure = true) public boolean isHtmlPage() {
		return page.isHtmlPage();
	}

	/**
	 * 获取 响应正文
	 *
	 * @return 响应正文
	 */
	@Contract(pure = true) public String body() {
		return isHtmlPage() ? ((HtmlPage) page).asXml() : page.getWebResponse().getContentAsString();
	}

	/**
	 * 获取 响应流
	 *
	 * @return 响应流
	 */
	@Contract(pure = true) public InputStream bodyStream() throws IOException {
		return page.getWebResponse().getContentAsStream();
	}

	/**
	 * 获取 响应流
	 *
	 * @return 响应流
	 */
	@Contract(pure = true) public byte[] bodyAsBytes() {
		byte[] result;
		try (InputStream inputStream = bodyStream()) {
			result = StreamUtils.stream(inputStream).toByteArray();
		} catch (IOException e) {
			return null;
		}
		return result;
	}

}