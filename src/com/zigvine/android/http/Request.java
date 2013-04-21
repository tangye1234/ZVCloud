package com.zigvine.android.http;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

public class Request {
	
	public static final boolean GLOBAL_DBG = false; /* FIXME set to false to avoid info leaks */
	public static final String TAG = Request.class.getSimpleName();
	public static final String HOST;
	static {
		if (GLOBAL_DBG) {
			HOST = "http://218.246.112.92";
		} else {
			HOST = "http://218.246.112.92";
		}
	}
	public static final String URL = HOST + "/dservice";
	
	public static final String MobileBound = "/mobilebound";
	public static final String SafeVerify = "/safeverify";
	public static final String GetGroupList = "/getgrouplist";
	public static final String SnapShotData = "/snapshotdata";
	public static final String GetAlarm = "/getalarm";
	public static final String GetControl = "/controldata";
	public static final String SendCommand = "/sendcommand";
	public static final String DataChart = "/datachart";
	public static final String LogOff = "/logoff";
	public static final String GetConsu = "/getconsu";
	public static final String SUBMITCONSU = "/submitconsu";
	
	private HttpManager httpManager;
	private HttpRequestBase httpRequest;
	private String errorMsg = null;
	private List<BasicNameValuePair> params;
	private Map<String, FileBody> multiparts = null;
	private JSONObject requestJson;
	private String path;
	private Resp resp;
	private int httpStatusCode;
	private volatile boolean ignoreException;
	private boolean isGetRequest;
	private boolean requestDone;
	private boolean DBG;
	
	public static class Resp /**FIXME serialized ?**/ {
		public JSONObject json;
		public Date time;
		public boolean success;
		public int statusCode;
		public Object obj;
		public Resp(JSONObject jSon) {
			json = jSon;
			time = new Date();
			try {
				statusCode = -1;
				statusCode = json.getInt("Status");
			} catch (JSONException e) {}
			success = statusCode == 0;
		}
	}
	
	public Request(String uri_path) {
		this(uri_path, false);
	}
	
	public Request(String uri_path, boolean isGet) {
		DBG = GLOBAL_DBG;
		httpManager = HttpManager.getMessageManager();
		isGetRequest = isGet;
		if (isGetRequest) {
			httpRequest = new HttpGet();
		} else {
			httpRequest = new HttpPost();
		}
		path = uri_path;
		ignoreException = false;
		params = new ArrayList<BasicNameValuePair>();
		requestDone = true;
	}
	
	public void setDebug(boolean isDebug) {
		DBG = isDebug;
	}
	
	public boolean request() throws JSONException {
		return request(URL);
	}
	
	public void setConnManagerTimeout(int millisec) {
		HttpParams params = httpRequest.getParams();
		if (params == null) {
			params = new BasicHttpParams();
		}
		ConnManagerParams.setTimeout(params, millisec);
		httpRequest.setParams(params);
	}
	
	public void setSoTimeout(int millisec) {
		HttpParams params = httpRequest.getParams();
		if (params == null) {
			params = new BasicHttpParams();
		}
		HttpConnectionParams.setSoTimeout(params, millisec);
		httpRequest.setParams(params);
	}
	
	public boolean isOnFetching() {
		return !requestDone;
	}
	
	public boolean request(String host) throws JSONException {
		String content = null;
		requestDone = false;
		if (!ignoreException) {
			try {
				HttpEntity entity = null;
				URI uri = null;
				if (isGetRequest) {
					StringBuilder sb = new StringBuilder(host + path + "?");
					for (BasicNameValuePair p:params) {
						sb.append(p.getName());
						if (p.getValue() != null) {
							sb.append("=");
							sb.append(URLEncoder.encode(p.getValue(), HTTP.UTF_8));
						}
						sb.append("&");
					}
					uri = URI.create(sb.substring(0,  sb.length() - 1));
				} else {
					uri = URI.create(host + path);
					if (requestJson != null) {
						entity = new StringEntity(requestJson.toString(), HTTP.UTF_8);
					} else {
						if (multiparts != null) {
							entity = createMultipartEntity(HTTP.UTF_8);
						} else {
							entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
							//entity = createMultipartEntity(HTTP.UTF_8);
						}
					}
					((HttpPost) httpRequest).setEntity(entity);
				}
				httpRequest.setURI(uri);
				if (DBG) log(httpRequest.getURI().toString());
				if (DBG) {
					Log.i(TAG, "Request Header = {");
					for(Header h : httpRequest.getAllHeaders()) {
						Log.i(TAG, "    \"" + h.getName() + "\": \"" + h.getValue() + "\",");
					}
					Log.i(TAG, "}");
				}
				if (DBG) {
					log("Request = " + (isGetRequest?"GET":"POST") + (requestJson == null?" {":""));
					if (requestJson != null) {
						log(requestJson.toString(4));
					} else {
						for (BasicNameValuePair p:params) {
							log("    \"" + p.getName() + "\": \"" + p.getValue() + "\",");
						}
						if (multiparts != null) {
							for (String key : multiparts.keySet()) {
								log("    \"" + key + "\": \"" + multiparts.get(key).getFilename() + "\",");
							}
						}
					}
					if (requestJson == null) {
						log("}");
					}
				}
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			IOException exception = null;
			HttpResponse httpResponse;
			try {
				httpResponse = httpManager.execute(httpRequest);
				httpStatusCode = httpResponse.getStatusLine().getStatusCode();
				HttpEntity entity = httpResponse.getEntity();
				if (entity != null) {
					if (httpStatusCode == HttpStatus.SC_OK) {
						content = EntityUtils.toString(entity, HTTP.UTF_8);
					} else {
						errorMsg = getBadResponse(httpStatusCode);
						entity.consumeContent();
					}
				}
			} catch (ClientProtocolException e) {
				exception = e;
			} catch (IOException e) {
				exception = e;
			} catch (IllegalStateException e) {
				exception = new IOException();
				exception.initCause(e);
				e.printStackTrace();
			} finally {
				if (!httpRequest.isAborted()) {
					httpRequest.abort();
				}
			}
			if (exception != null) {
				errorMsg = processIOExceptionCallback(ignoreException, exception);
				httpStatusCode = -1;
			}
		}
		resp = null;
		if (content != null) {
			JSONObject json = new JSONObject(content);
			resp = new Resp(json);
			if (DBG) log("Response = " + json.toString(4));
		}
		if (DBG) log("request done");
		requestDone = true;
		return resp != null;
	}
	
	private HttpEntity createMultipartEntity(String charset) throws UnsupportedEncodingException {
		MultipartEntity entity = new MultipartEntity();
        // Don't try this. Server does not appear to support chunking.
        // entity.addPart("media", new InputStreamBody(imageStream, "media"));
		for (BasicNameValuePair param : params) {
        	entity.addPart(param.getName(), new StringBody(param.getValue(), Charset.forName(charset)));
        }
		if (multiparts != null) {
			for (String key : multiparts.keySet()) {
				entity.addPart(key, multiparts.get(key));
				//entity.addPart(key, new FileBody(new java.io.File("/sdcard/_camera.jpg")));
			}
		}
        return entity;
	}

	public Resp getResponse() {
		return resp;
	}
	
	public int getHttpStatusCode() {
		return httpStatusCode;
	}
	
	public void shutdown() {
		if (httpRequest != null) {
			ignoreException = true;
			if (!httpRequest.isAborted()) {
				if (DBG) log("abort http request");
				try {
					httpRequest.abort();
				} catch(Exception e) {
					e.printStackTrace();
					// TODO ignore but some time this method may truely throw
					// network on ui thread exception
				}
			}
		}
	}
	
	public String getErrorMessage() {
		return errorMsg;
	}
	
	public void setErrorMessage(String err) {
		errorMsg = err;
	}
	
	public final void setParam(String name, String value) {
		params.add(new BasicNameValuePair(name, value));
	}
	
	public final void setFile(String key, File file) {
		if (isGetRequest) throw new RuntimeException("Get Request cannot add file martipart entity");
		if (multiparts == null) {
			multiparts = new WeakHashMap<String, FileBody>();
		}
		multiparts.put(key, new FileBody(file));
	}
	
	public final void setJSONEntity(JSONObject json) {
		if (isGetRequest) throw new IllegalArgumentException("Get request should not contain a json object entity");
		httpRequest.addHeader("Content-Type", "application/json");
		requestJson = json;
		// ((HttpPost) httpRequest).setEntity(new StringEntity(requestJson.toString()));
		
	}
	
	protected void log(String s) {
		android.util.Log.i(getClass().getSimpleName(), s);
	}
	
	protected String getBadResponse(int status) {
		return "服务器错误：" + status;
	}
	
	/* need to be overridden, the same thread as request function*/
	protected String processIOExceptionCallback(boolean ignore, IOException e) {
		log(ignore + "=ignore, excepetion:");
		e.printStackTrace();
		//return ignore? null:e.getMessage();
		if (ignore) return null;
		
		if (e instanceof SocketTimeoutException) {
			return "连接超时，如果频繁出现，请检查网络";
		} else if (e instanceof UnknownHostException) {
			return "无法连接到服务器，请检查网络";
		} else if (e instanceof java.net.ProtocolException) {
			return "连接服务失败，请更新您的客户端";
		} else if (e.getCause() instanceof IllegalStateException) {
			return "连接池关闭错误";
		} else if (e instanceof SSLPeerUnverifiedException) {
			return "无法验证服务器证书，请联系客服";
		} else if (e instanceof SSLHandshakeException) {
			return "服务器证书验证失败，请联系客服";
		} else {
			if (e instanceof ConnectionPoolTimeoutException) {
            	HttpManager.clean();
            }
			return "您的网络不给力啊";
		}
	}
	
	public static interface ResponseListener {
		public void onResp(int id, Resp resp, Object...obj);
		public void onErr(int id, String err, int httpCode, Object...obj);
	}
	
	/**
	 * must call this function in a looper thread. the callback listener will be executed
	 * in the current caller's looper thread
	 * @param rl the response callback listener
	 * @param requestId this request id will be delivered as a message id to the callback
	 * @param obj this obj will be delivered as a message object to the callback
	 */
	public void asyncRequest(final ResponseListener rl, final int requestId, final Object...obj) {
		final Handler handler = new Handler();
		new Thread() {
			public void run() {
				String error = null;
				Resp resp = null;
				try {
		            if(request()) {
		            	resp = getResponse();
		            } else {
		                error = getErrorMessage();
		            }
		        } catch (JSONException e) {
		            error = "系统错误";
		            e.printStackTrace();
		        }
				
				if (resp != null) {
					final Resp data = resp;
					handler.post(new Runnable() {
						public void run() {
							if (rl != null) {
								rl.onResp(requestId, data, obj);
							}
						}
					});
				} else if (error != null) {
					final String data = error;
					handler.post(new Runnable() {
						public void run() {
							if (rl != null) {
								rl.onErr(requestId, data, httpStatusCode, obj);
							}
						}
					});
				}
			}
		}.start();
	}

}
