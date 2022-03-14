package org.haic.often.ChromeBrowser;

import com.protonail.leveldb.jna.*;
import org.haic.often.FilesUtils;
import org.haic.often.Judge;
import org.haic.often.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 获取本地浏览器 Local Storage 数据
 * <p>
 * 需要注意的是当前为初始版本,如果键值存在中文,会存在乱码问题
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/3/14 4:03
 */
public class LocalStorage {

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

	public static abstract class Storage {

		protected String domain;
		protected String key;
		protected String value;

		public Storage(String domain, String key, String value) {
			this.domain = domain;
			this.key = key;
			this.value = value;
		}

		public String getDomain() {
			return domain;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

	}

	public abstract static class Browser {
		protected File userHome;
		protected File localStorageStore;
		protected Charset charset = StandardCharsets.UTF_8;

		/**
		 * A file that should be used to make a temporary copy of the browser's localStorage leveldb
		 */
		protected File localStorageStoreCopy = new File(".leveldb");

		/**
		 * Returns all localStorage leveldb
		 */

		public Map<String, Map<String, String>> getStorages() {
			return processLevelDB(localStorageStore);
		}

		/**
		 * Returns localStorage leveldb for a given domain
		 */
		public Map<String, String> getStoragesForDomain(String domain) {
			Optional<Map.Entry<String, Map<String, String>>> result = getStorages().entrySet().stream().filter(l -> l.getKey().contains(domain)).findFirst();
			return result.isPresent() ? result.get().getValue() : new HashMap<>();
		}

		/**
		 * Processes all processLevelDB in the processLevelDB store for a given domain or all
		 * domains if domainFilter is null
		 *
		 * @param localStorage localStorage
		 * @return cookie set
		 */
		protected abstract Map<String, Map<String, String>> processLevelDB(File localStorage);

	}

	public static class ChromeBrowser extends Browser {

		public ChromeBrowser(File userHome) {
			this.userHome = userHome;
			localStorageStore = new File(new File(userHome, "Default"), "Local Storage\\leveldb");
		}

		public Map<String, Map<String, String>> processLevelDB(File localStorageStore) {
			FilesUtils.copyDirectory(localStorageStore, localStorageStoreCopy);
			Map<String, Map<String, String>> result = new HashMap<>();
			try (LevelDB levelDB = new LevelDB(localStorageStoreCopy.getPath(), new LevelDBOptions());
					LevelDBKeyValueIterator iterator = new LevelDBKeyValueIterator(levelDB, new LevelDBReadOptions() {{
						setFillCache(false);// 遍历中swap出来的数据，不应该保存在memtable中
						setSnapshot(levelDB.createSnapshot());
					}})) {
				while (iterator.hasNext()) {
					KeyValuePair entry = iterator.next();
					String key = new String(entry.getKey(), charset);
					if (key.startsWith("_http")) {
						key = key.substring(1);
						int index = key.indexOf((char) 0);
						String domain = key.substring(0, index);
						String name = StringUtils.filter(key.substring(index + 1));
						String value = StringUtils.filter(new String(entry.getValue(), charset));
						Map<String, String> info = result.get(domain);
						if (Judge.isNull(info)) {
							result.put(domain, new HashMap<>() {
								{
									put(name, value);
								}
							});
						} else {
							info.put(name, value);
						}
					}
				}
			} finally {
				FilesUtils.deleteDirectory(localStorageStoreCopy);
			}
			return result;
		}

	}
}