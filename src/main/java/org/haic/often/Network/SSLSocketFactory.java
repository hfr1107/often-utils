package org.haic.often.Network;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;

/**
 * SSL Socket 工厂
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/2/25 18:16
 */
public class SSLSocketFactory {
	/**
	 * 获得HTTPS忽略证书验证的SocketFactory对象
	 *
	 * @return SocketFactory
	 */
	public static javax.net.ssl.SSLSocketFactory getSocketFactory() {
		return SSLSocketFactory.MyX509TrustManagerUtil().getSocketFactory();
	}

	/**
	 * 获得HTTPS忽略证书验证的SSLContext
	 *
	 * @return SSLContext
	 */
	public static SSLContext MyX509TrustManagerUtil() {
		TrustManager[] tm = { new MyX509TrustManager() };
		SSLContext ctx = null;
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(null, tm, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ctx;
	}

	/**
	 * HTTPS忽略证书验证,防止高版本jdk因证书算法不符合约束条件,使用继承X509ExtendedTrustManager的方式
	 */
	protected static class MyX509TrustManager extends X509ExtendedTrustManager {

		@Override public void checkClientTrusted(X509Certificate[] arg0, String arg1) {

		}

		@Override public void checkServerTrusted(X509Certificate[] arg0, String arg1) {

		}

		@Override public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) {

		}

		@Override public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) {

		}

		@Override public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) {

		}

		@Override public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) {

		}

	}
}