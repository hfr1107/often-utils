package org.haic.often.ChromeBrowser;

import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取本地浏览器账号密码
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/1/20 20:04
 */
public class LoginDatas {

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

	public static abstract class LoginData {

		protected String name;
		protected byte[] encryptedValue;
		protected Date created;
		protected String domain;
		protected File loginData;

		public LoginData(String name, byte[] encryptedValue, Date created, String domain, File loginData) {
			this.name = name;
			this.encryptedValue = encryptedValue;
			this.domain = domain;
			this.loginData = loginData;
		}

		public String getName() {
			return name;
		}

		public Date getCreated() {
			return created;
		}

		public byte[] getEncryptedValue() {
			return encryptedValue;
		}

		public String getDomain() {
			return domain;
		}

		public File getCookieStore() {
			return loginData;
		}

		public String getValue() {
			return new String(encryptedValue);
		}

		public abstract boolean isDecrypted();

	}

	public static class DecryptedLoginData extends LoginData {

		protected String decryptedValue;

		public DecryptedLoginData(String name, byte[] encryptedValue, String decryptedValue, Date created, String domain, File loginData) {
			super(name, encryptedValue, created, domain, loginData);
			this.decryptedValue = decryptedValue;
		}

		public String getDecryptedValue() {
			return decryptedValue;
		}

		@Override public boolean isDecrypted() {
			return true;
		}

		@Override public String toString() {
			return "LoginData [name=" + name + ", value=" + decryptedValue + "]";
		}

		@Override public String getValue() {
			return decryptedValue;
		}

	}

	public static class EncryptedLoginData extends LoginData {

		public EncryptedLoginData(String name, byte[] encryptedValue, Date created, String domain, File cookieStore) {
			super(name, encryptedValue, created, domain, cookieStore);
		}

		@Override public boolean isDecrypted() {
			return false;
		}

		@Override public String toString() {
			return "LoginData [name=" + name + " (encrypted)]";
		}

	}

	public abstract static class Browser {
		protected File userHome;
		protected File loginDataStore;

		/**
		 * A file that should be used to make a temporary copy of the browser's login data
		 */
		protected File loginDataStoreCopy = new File(".loginData.db");

		/**
		 * Returns all login data
		 */

		public Set<LoginData> getLoginDatas() {
			return processLoginDatas(loginDataStore, null);
		}

		/**
		 * Returns login data for a given domain
		 */
		public Map<String, String> getLoginDatasForDomain(String domain) {
			return processLoginDatas(loginDataStore, domain).parallelStream().filter(loginData -> !Judge.isEmpty(loginData.getValue()))
					.collect(Collectors.toMap(LoginData::getName, LoginData::getValue, (e1, e2) -> e1));
		}

		/**
		 * Processes all loginData in the loginData store for a given domain or all
		 * domains if domainFilter is null
		 *
		 * @param loginData    loginData
		 * @param domainFilter 域名
		 * @return cookie set
		 */
		protected abstract Set<LoginData> processLoginDatas(File loginData, String domainFilter);

		/**
		 * Decrypts an encrypted loginData
		 *
		 * @param encryptedLoginData decrypted loginData
		 * @return decrypted cookie
		 */
		protected abstract DecryptedLoginData decrypt(EncryptedLoginData encryptedLoginData);

	}

	public static class ChromeBrowser extends Browser {

		public ChromeBrowser(File userHome) {
			this.userHome = userHome;
			loginDataStore = new File(new File(userHome, "Default"), "Login Data");
		}

		/**
		 * Processes all loginData in the loginDataStore for a given domain or all
		 * domains if domainFilter is null
		 *
		 * @param loginData    cookie
		 * @param domainFilter domain
		 * @return decrypted cookie
		 */
		@Override protected Set<LoginData> processLoginDatas(File loginData, String domainFilter) {
			Set<LoginData> cookies = new HashSet<>();
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + loginDataStoreCopy.getAbsolutePath())) {
				Class.forName("org.sqlite.JDBC"); // load the sqlite-JDBC driver using the current class loader
				loginDataStoreCopy.delete();
				FilesUtils.copyFile(loginData, loginDataStoreCopy);
				Statement statement = connection.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 seconds
				ResultSet result;
				if (domainFilter == null || domainFilter.isEmpty()) {
					result = statement.executeQuery("select * from logins");
				} else {
					result = statement.executeQuery("select * from logins where signon_realm like \"%" + domainFilter + "%\"");
				}
				while (result.next()) {
					String name = result.getString("username_value");
					byte[] encryptedBytes = result.getBytes("password_value");
					String domain = result.getString("signon_realm");
					Date created = result.getDate("date_created");
					EncryptedLoginData encryptedLoginData = new EncryptedLoginData(name, encryptedBytes, created, domain, loginData);
					DecryptedLoginData decryptedCookie = decrypt(encryptedLoginData);
					cookies.add(Objects.requireNonNullElse(decryptedCookie, encryptedLoginData));
				}
			} catch (Exception e) {
				e.printStackTrace(); // if the error message is "out of memory", it probably means no database file is found
			} finally {
				loginDataStoreCopy.delete(); // 删除备份
			}
			return cookies;
		}

		/**
		 * Decrypts an encrypted login data
		 *
		 * @param encryptedLoginData encrypted login data
		 * @return decrypted login data
		 */
		@Override protected DecryptedLoginData decrypt(EncryptedLoginData encryptedLoginData) {
			byte[] decryptedBytes = LocalCookies.encryptedValueDecrypt(encryptedLoginData.encryptedValue, LocalCookies.getEncryptedKey(userHome));
			return Judge.isNull(decryptedBytes) ?
					null :
					new DecryptedLoginData(encryptedLoginData.getName(), encryptedLoginData.getEncryptedValue(), new String(decryptedBytes),
							encryptedLoginData.getCreated(), encryptedLoginData.getDomain(), encryptedLoginData.getCookieStore());
		}
	}

}