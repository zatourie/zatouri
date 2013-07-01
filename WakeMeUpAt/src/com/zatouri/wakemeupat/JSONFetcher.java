package com.zatouri.wakemeupat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	JSONFetcher(MainActivity main, String urlStr){
		this.main = main;
		this.urlStr = urlStr;
	}
	
	public void run() {
		String result = fetchJSON();
		main.setText(result);
		
	}
	
	private String fetchJSON(){
		String result = "";
		HttpURLConnection con = null;
		try {
			// Check if task has been interrupted
			if (Thread.interrupted())
				throw new InterruptedException();

//			String q = URLEncoder.encode(urlStr, "UTF-8");
			URL url = new URL(urlStr);

			con = (HttpURLConnection) url.openConnection();
			con.setReadTimeout(10000 /* milliseconds */);
			con.setConnectTimeout(15000 /* milliseconds */);
			con.setRequestMethod("GET");
			con.addRequestProperty("Referer","http://www.pragprog.com/titles/eband2/hello-android");
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
			Log.d("JSON", jsonObject.toString());
			result = jsonObject.toString();

			// Check if task has been interrupted
			if (Thread.interrupted())
				throw new InterruptedException();

		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
		} catch (JSONException e) {
			Log.e(TAG, "JSONException", e);
		} catch (InterruptedException e) {
			Log.d(TAG, "InterruptedException", e);
			//result = translate.getResources().getString(R.string.translation_interrupted);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}		
		
		//Log.d(TAG, result);

		
		return result;
	}

}
