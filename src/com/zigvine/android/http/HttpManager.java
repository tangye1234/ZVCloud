package com.zigvine.android.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;

import com.zigvine.zagriculture.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class HttpManager {
	
	public static final boolean DBG = false;
	public static final String TAG = "HttpManager";
	
	/**
	 * http message manager
	 */
	private static HttpManager httpManager;
	
	public static void createMessageManager(Context context) {
		if (httpManager == null) {
			HttpParams params = new BasicHttpParams();
	        // 设置一些基本参数
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
	        //HttpProtocolParams.setUseExpectContinue(params, true);
	        //HttpProtocolParams.setUserAgent(params, MainApp.getAgent());
	        // 超时设置
	        Resources res = context.getResources();
	        ConnManagerParams.setTimeout(params, res.getInteger(R.integer.connection_manager_timeout));
	        HttpConnectionParams.setConnectionTimeout(params, res.getInteger(R.integer.connection_timeout));
	        HttpConnectionParams.setSoTimeout(params, res.getInteger(R.integer.socket_timeout));
			
			SchemeRegistry schReg = new SchemeRegistry();
			schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        ClientConnectionManager connMgr = new ThreadSafeClientConnManager(params, schReg);
			httpManager = new HttpManager(connMgr, params);
		}
	}
	
	public static void destroyMessageManager() {
		if (httpManager != null) {
			httpManager.connMgr.shutdown();
			// FIXME how to recreate
		}
	}
	
	public static HttpManager getMessageManager() {
		return httpManager;
	}
	
	//private Context mContext; /*Application Context*/
	private ClientConnectionManager connMgr;
	private BasicHttpContext httpContext;
	private HttpClient httpClient;
	
	private HttpManager(ClientConnectionManager ccm, HttpParams params) {
		httpClient = new DefaultHttpClient(ccm, params);
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
		connMgr = ccm;
	}
	
	public HttpResponse execute(HttpUriRequest request) throws ClientProtocolException, IOException {
		HttpResponse resp =  httpClient.execute(request, httpContext);
		if (DBG) {
			Log.i(TAG, "Response Header = {");
			for(Header h : resp.getAllHeaders()) {
				Log.i(TAG, "    \"" + h.getName() + "\": \"" + h.getValue() + "\",");
			}
			Log.i(TAG, "}");
		}
		return resp;
	}
	
	/**package**/ HttpClient getHttpClient() {
		return httpClient;
	}
	
	public HttpResponse nativeExecute(HttpUriRequest request) throws
			IOException, ClientProtocolException {
		if (DBG) {
			Log.i(TAG, "native http request: " + request.getURI().toString());
		}
		if (httpClient != null) {
			return httpClient.execute(request, httpContext);
		}
		return null;
	}
	
	public static void clean() {
		if (httpManager != null) {
			httpManager.connMgr.closeExpiredConnections();
			httpManager.connMgr.closeIdleConnections(20, TimeUnit.SECONDS);
		}
	}
	
}
