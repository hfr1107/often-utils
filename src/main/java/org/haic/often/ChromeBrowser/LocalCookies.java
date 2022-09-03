package org.haic.often.ChromeBrowser;

import com.alibaba.fastjson.JSONObject;
import com.github.windpapi4j.WinDPAPI;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.haic.often.ReadWriteUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 获取本地浏览器cookie
 *
 * @author haicdust
 * @version 1.0
 * @since 2021/12/24 23:15
 */
public class LocalCookies {

	protected LocalCookies() {
	}

	/**
	 * 用户文件夹路径 C:\\users\\xxx\\AppData\\Local\\Microsoft\\Edge\\User Data
	 *
	 * @return new ChromeBrowser
	 */
	@Contract(pure = true) public static ChromeBrowser home() {
		return home(new File(System.getProperty("user.home"), "AppData\\Local\\Microsoft\\Edge\\User Data"));
	}

	/**
	 * 自定义 用户文件夹路径
	 *
	 * @param userHome 用户文件夹路径
	 * @return new ChromeBrowser
	 */
	@NotNull @Contract(pure = true) public static ChromeBrowser home(@NotNull String userHome) {
		return home(new File(userHome));
	}

	/**
	 * 自定义 用户文件夹路径
	 *
	 * @param userHome 用户文件夹路径
	 * @return new ChromeBrowser
	 */
	@NotNull @Contract(pure = true) public static ChromeBrowser home(@NotNull File userHome) {
		return new ChromeBrowser(userHome);
	}

	/**
	 * Get encryptedKey
	 *
	 * @param userHome userData home
	 * @return encryptedKey
	 */
	protected static String getEncryptedKey(File userHome) {
		return JSONObject.parseObject(JSONObject.parseObject(ReadWriteUtils.orgin(new File(userHome, "Local State")).read()).getString("os_crypt"))
				.getString("encrypted_key");
	}

	/**
	 * Get Decrypt Value
	 *
	 * @param encryptedValue 加密值
	 * @param encryptedKey   密钥
	 * @return Decrypt Value
	 */
	protected static byte[] encryptedValueDecrypt(byte[] encryptedValue, String encryptedKey) {
		Security.addProvider(new BouncyCastleProvider());

		int keyLength = 256 / 8;
		int nonceLength = 96 / 8;
		String kEncryptionVersionPrefix = "v10";
		int GCM_TAG_LENGTH = 16;

		try {
			byte[] encryptedKeyBytes = Base64.decodeBase64(encryptedKey);
			assert new String(encryptedKeyBytes).startsWith("DPAPI");
			assertTrue(new String(encryptedKeyBytes).startsWith("DPAPI")); // 断言条件为真，如果不是，它会抛出没有消息的AssertionError
			encryptedKeyBytes = Arrays.copyOfRange(encryptedKeyBytes, "DPAPI".length(), encryptedKeyBytes.length);
			WinDPAPI winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN);
			byte[] keyBytes = winDPAPI.unprotectData(encryptedKeyBytes);
			assertEquals(keyLength, keyBytes.length); // 断言两个多头相等。如果不是，则抛出AssertionError
			byte[] nonce = Arrays.copyOfRange(encryptedValue, kEncryptionVersionPrefix.length(), kEncryptionVersionPrefix.length() + nonceLength);
			encryptedValue = Arrays.copyOfRange(encryptedValue, kEncryptionVersionPrefix.length() + nonceLength, encryptedValue.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			encryptedValue = cipher.doFinal(encryptedValue);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return encryptedValue;
	}

	public static abstract class Cookie {

		protected String name;
		protected byte[] encryptedValue;
		protected Date expires;
		protected String path;
		protected String domain;
		protected File cookieStore;

		public Cookie(String name, byte[] encryptedValue, String domain, File cookieStore) {
			this.name = name;
			this.encryptedValue = encryptedValue;
			this.domain = domain;
			this.cookieStore = cookieStore;
		}

		public Cookie(String name, byte[] encryptedValue, Date expires, String path, String domain, File cookieStore) {
			this.name = name;
			this.encryptedValue = encryptedValue;
			this.expires = expires;
			this.path = path;
			this.domain = domain;
			this.cookieStore = cookieStore;
		}

		public String getName() {
			return name;
		}

		public byte[] getEncryptedValue() {
			return encryptedValue;
		}

		public Date getExpires() {
			return expires;
		}

		public String getPath() {
			return path;
		}

		public String getDomain() {
			return domain;
		}

		public File getCookieStore() {
			return cookieStore;
		}

		public String getValue() {
			return new String(encryptedValue);
		}

		public abstract boolean isDecrypted();

	}

	public static class DecryptedCookie extends Cookie {

		protected String decryptedValue;

		public DecryptedCookie(String name, byte[] encryptedValue, String decryptedValue, Date expires, String path, String domain, File cookieStore) {
			super(name, encryptedValue, expires, path, domain, cookieStore);
			this.decryptedValue = decryptedValue;
		}

		public String getDecryptedValue() {
			return decryptedValue;
		}

		@Override public boolean isDecrypted() {
			return true;
		}

		@Override public String toString() {
			return "Cookie [name=" + name + ", value=" + decryptedValue + "]";
		}

		@Override public String getValue() {
			return decryptedValue;
		}

	}

	public static class EncryptedCookie extends Cookie {

		public EncryptedCookie(String name, byte[] encryptedValue, Date expires, String path, String domain, File cookieStore) {
			super(name, encryptedValue, expires, path, domain, cookieStore);
		}

		@Override public boolean isDecrypted() {
			return false;
		}

		@Override public String toString() {
			return "Cookie [name=" + name + " (encrypted)]";
		}

	}

	public abstract static class Browser {
		protected File userHome;
		protected File cookieStore;

		/**
		 * A file that should be used to make a temporary copy of the browser's cookie store
		 */
		protected File cookieStoreCopy = new File(".cookies.db");

		/**
		 * Returns all cookies
		 */
		public Map<String, Map<String, String>> getCookies() {
			Map<String, Map<String, String>> result = new HashMap<>();
			Set<Cookie> cookies = processCookies(cookieStore, null);
			for (Cookie cookie : cookies) {
				result.put(cookie.getDomain(), Map.of(cookie.getName(), cookie.getValue()));
			}
			return result;
		}

		/**
		 * Returns cookies for a given domain
		 */
		public Map<String, String> getCookiesForDomain(String domain) {
			return processCookies(cookieStore, domain).parallelStream().filter(cookie -> !Judge.isEmpty(cookie.getValue()))
					.collect(Collectors.toMap(LocalCookies.Cookie::getName, LocalCookies.Cookie::getValue, (e1, e2) -> e2));
		}

		/**
		 * Processes all cookies in the cookie store for a given domain or all
		 * domains if domainFilter is null
		 *
		 * @param cookieStore  cookieStore
		 * @param domainFilter 域名
		 * @return cookie set
		 */
		protected abstract Set<Cookie> processCookies(File cookieStore, String domainFilter);

		/**
		 * Decrypts an encrypted cookie
		 *
		 * @param encryptedCookie decrypted cookie
		 * @return decrypted cookie
		 */
		protected abstract DecryptedCookie decrypt(EncryptedCookie encryptedCookie);

	}

	public static class ChromeBrowser extends Browser {

		public ChromeBrowser(File userHome) {
			this.userHome = userHome;
			File defaultDirectory = new File(userHome, "Default");
			cookieStore = new File(defaultDirectory, "Cookies");
			cookieStore = cookieStore.exists() ? cookieStore : new File(new File(defaultDirectory, "Network"), "Cookies");
		}

		/**
		 * Processes all cookies in the cookie store for a given domain or all
		 * domains if domainFilter is null
		 *
		 * @param cookieStore  cookie
		 * @param domainFilter domain
		 * @return decrypted cookie
		 */
		@Override protected Set<Cookie> processCookies(File cookieStore, String domainFilter) {
			Set<Cookie> cookies = new HashSet<>();
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + cookieStoreCopy.getAbsolutePath())) {
				Class.forName("org.sqlite.JDBC");  // load the sqlite-JDBC driver using the current class loader
				cookieStoreCopy.delete();
				FilesUtils.copyFile(cookieStore, cookieStoreCopy);
				Statement statement = connection.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 seconds
				ResultSet result;
				if (domainFilter == null || domainFilter.isEmpty()) {
					result = statement.executeQuery("select * from cookies");
				} else {
					result = statement.executeQuery("select * from cookies where host_key like \"%" + domainFilter + "%\"");
				}
				while (result.next()) {
					String name = result.getString("name");
					byte[] encryptedBytes = result.getBytes("encrypted_value");
					String path = result.getString("path");
					String domain = result.getString("host_key");
					Date expires = result.getDate("expires_utc");
					EncryptedCookie encryptedCookie = new EncryptedCookie(name, encryptedBytes, expires, path, domain, cookieStore);
					DecryptedCookie decryptedCookie = decrypt(encryptedCookie);
					cookies.add(Objects.requireNonNullElse(decryptedCookie, encryptedCookie));
				}
			} catch (Exception e) {
				e.printStackTrace(); // if the error message is "out of memory", it probably means no database file is found
			} finally {
				cookieStoreCopy.delete(); // 删除备份
			}
			return cookies;
		}

		/**
		 * Decrypts an encrypted cookie
		 *
		 * @param encryptedCookie encrypted cookie
		 * @return decrypted cookie
		 */
		@Override protected DecryptedCookie decrypt(EncryptedCookie encryptedCookie) {
			byte[] decryptedBytes = encryptedValueDecrypt(encryptedCookie.encryptedValue, getEncryptedKey(userHome));
			return Judge.isNull(decryptedBytes) ?
					null :
					new DecryptedCookie(encryptedCookie.getName(), encryptedCookie.getEncryptedValue(), new String(decryptedBytes),
							encryptedCookie.getExpires(), encryptedCookie.getPath(), encryptedCookie.getDomain(), encryptedCookie.getCookieStore());

		}

	}

}
