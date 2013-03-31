package com.zigvine.android.http;

import java.util.LinkedList;

import android.util.Log;

import com.zigvine.android.http.Request.Resp;
import com.zigvine.android.http.Request.ResponseListener;

public class RequestPool {
	
	private static final String TAG = "RequestPool";
	private static final boolean DEBUG = false;
	
	private LinkedList<Requester> pool = new LinkedList<Requester>();
	private int Count;
	private volatile int RunningCount;
	
	public RequestPool(int count) {
		Count = count;
		RunningCount = 0;
	}
	
	public void addRequest(final Request request, final ResponseListener listener, final int id, final Object...obj) {
		final Requester r = new Requester(request, listener, id, obj);
		ResponseListener l = new ResponseListener() {
			@Override
			public void onResp(int id, Resp resp, Object... obj) {
				end();
				listener.onResp(id, resp, obj);
			}

			@Override
			public void onErr(int id, String err, int httpCode, Object... obj) {
				end();
				listener.onErr(id, err, httpCode, obj);
			}
			
			private void end() {
				pool.remove(r);
				RunningCount--;
				if (DEBUG) Log.i(TAG, "remove running = " + RunningCount);
				checkPool();
			}
			
		};
		r.l = l;
		pool.add(r);
		checkPool();
	}
	
	private void checkPool() {
		if (pool.size() > RunningCount && RunningCount < Count) {
			Requester r = pool.get(RunningCount);
			r.r.asyncRequest(r.l, r.id, r.obj);
			RunningCount++;
			if (DEBUG) Log.i(TAG, "add running = " + RunningCount);
		}
	}
	
	public static class Requester {
		public Request r;
		public ResponseListener l;
		public int id;
		public Object[] obj;
		public Requester(Request r, ResponseListener l, int id, Object[] obj) {
			this.r = r;
			this.l = l;
			this.id = id;
			this.obj = obj;
		}
	}
	
	public int getRunningCount() {
		return RunningCount;
	}
	
	public void clearAndShutDownAll() {
		for (Requester r : pool) {
			r.r.shutdown();
		}
	}
	
	public boolean hasID(int id) {
		for (Requester r : pool) {
			if (r.id == id) return true;
		}
		return false;
	}
	
	public Requester getByID(int id) {
		for (Requester r : pool) {
			if (r.id == id) return r;
		}
		return null;
	}

}
