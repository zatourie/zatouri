package com.zatouri.wakemeupat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
//import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

public class JSONFetcher implements Runnable {
	private static final String TAG = "WekeMeUpAt_FetchJSON";
	private final MainActivity main;
	private final String urlStr;

	private final String callback;
	
	JSONFetcher(MainActivity main,String urlStr){
		this(main, urlStr, null);
	}
	
	JSONFetcher(MainActivity main, String urlStr, String callback){
		this.main = main;
		this.urlStr = urlStr;
		this.callback = callback;
	}
	
	public void run() {
		Log.d(TAG, "JSONFetcher start to run");
		JSONObject result = fetchJSON();
		Log.d(TAG, result.toString());
		
		if(callback != null){
			try{
				Method method = main.getClass().getMethod(callback, JSONObject.class);
				method.invoke(main, result);
			} catch(NoSuchMethodException e){
				Log.e(TAG, "NoSuchMethodException", e);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "IllegalArgumentException", e);
			} catch (IllegalAccessException e) {
				Log.e(TAG, "IllegalAccessException", e);
			} catch (InvocationTargetException e) {
				Log.e(TAG, "InvocationTargetException", e);
			}
		}
		
		
	}
	
	private JSONObject fetchJSON(){
		JSONObject result = new JSONObject();
		HttpURLConnection con = null;
		try {
			// Check if task has been interrupted
			if (Thread.interrupted())
				throw new InterruptedException();

			URL url = new URL(urlStr);

			con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(10000 /* milliseconds */);
			con.setConnectTimeout(15000 /* milliseconds */);
			con.setRequestMethod("GET");
			con.addRequestProperty("Referer","http://www.zatouri.com");
			con.setDoInput(true);

			// Start the query
			con.connect();

			// Check if task has been interrupted
			if (Thread.interrupted())
				throw new InterruptedException();

			// Read results from the query
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "euc-kr"));
			String payload = reader.readLine();
			reader.close();

			// Parse to get translated text
			JSONObject jsonObject = new JSONObject(payload);
			//Log.d("JSON", jsonObject.toString());
			result = jsonObject;

			// Check if task has been interrupted
			if (Thread.interrupted())
				throw new InterruptedException();

		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		} catch (JSONException e) {
			Log.e(TAG, "JSONException", e);
		} catch (InterruptedException e) {
			Log.d(TAG, "InterruptedException", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}		
		
		//Log.d(TAG, result);

		
		return result;
	}

}
