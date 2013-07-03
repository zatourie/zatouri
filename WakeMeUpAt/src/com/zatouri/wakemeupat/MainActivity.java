package com.zatouri.wakemeupat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "WakeMeUpAt_MainActivity";

	/*Thread variables*/
	private final Handler guiThread = new Handler();
	private ExecutorService transThread = Executors.newSingleThreadExecutor();
	private Future transPending;
	
	//UI Controls
	private WebView webView;
	private TextView textView;

	//Private variables
	private String subwayLine;
	private String currentTrainNo;
	private String offStatnNo;
	private String result;
	
   /** Object exposed to JavaScript */
	private class AndroidBridge {
		public void callAndroid(final String arg) { // must be final
			guiThread.post(new Runnable() {
				public void run() {
					Log.d(TAG, "callAndroid(" + arg + ")");
					//textView.setText(arg);
				}
			});
		}
	  
		public void showToast(final String msg){
			guiThread.post(new Runnable() {
				public void run() {
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
				}
			});

		}
		
		public void getStatnInfo(final String lineNo){
			guiThread.post(new Runnable() {
				public void run() {
//					textView.setText(null);
					MainActivity.this.getStatnInfo(lineNo);
					Log.d(TAG, "result:"+result);

				}
			});
		}
	}
	
	public void setText(JSONObject json){
		
		try{
			JSONArray ja = json.getJSONArray("resultList");
			result = ja.toString();
			Log.d(TAG, result);
			
			//여기서 테이블을 만들자
			StringBuilder sb = new StringBuilder();
			
			for(int i = 0 ; i < ja.length() ; i++){
				JSONObject obj = ja.getJSONObject(i);
				sb.append("<tr><td>");
				sb.append(obj.getString("statnNm"));
				sb.append("</td><td>");
				sb.append(obj.getString("existYn1"));
				sb.append("</td><td>");
				sb.append(obj.getString("existYn2"));
				sb.append("</td></tr>");
			}
			
			webView.loadUrl("javascript:callJS('"+sb.toString()+"')");
//			textView.setText(result);


			
		}catch(JSONException je){
			toastMsg(je.getMessage());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
//		textView = (TextView)findViewById(R.id.result);
		
		webView = (WebView)findViewById(R.id.webView);
		
		//Enable Javascript in webview
		webView.getSettings().setJavaScriptEnabled(true);
		
		webView.addJavascriptInterface(new AndroidBridge(), "android");
		
		// Set up a function to be called when JavaScript tries
		// to open an alert window
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onJsAlert(final WebView view,final String url, final String message,JsResult result) {
				Log.d(TAG, "onJsAlert(" + view + ", " + url + ", " + message + ", " + result + ")");
				Toast.makeText(MainActivity.this, message, 3000).show();
				result.confirm();
				return true; // I handled it
			}
			
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm) 
		    {
		        Log.d(TAG, cm.message() + " -- From line "
		                             + cm.lineNumber() + " of "
		                             + cm.sourceId() );
		        return true;
		    }
		});	
		
		webView.loadUrl("file:///android_asset/index.html");
		webView.loadUrl("javascript:alert('hi')");
		
	}

	@Override
	protected void onDestroy() {
	  // Terminate extra threads here
	  transThread.shutdownNow();
	  super.onDestroy();
	}

	private void getStatnInfo(String lineNo){
		fetchJSON("subwayId="+lineNo);
	}
	private void fetchJSON(String queryString) {
		try {
		  JSONFetcher fetcher = new JSONFetcher(MainActivity.this, "http://m.bus.go.kr/mBus/subway/getStatnByRoute.do?"+queryString);
		  transPending = transThread.submit(fetcher); 
		} catch (RejectedExecutionException e) {
		   Log.e(TAG, "RejectedExcutionException", e);
		   toastMsg("Oooops. can't bring data.");
		}
	}	
	

	private void setCurrentTrainNo(String trainNo){
		this.currentTrainNo = trainNo;
		
		setAlarm();
	}
	
	private void setOffStatnNo(String StatnNo){
		this.offStatnNo = StatnNo;
		
		setAlarm();
	}
	
	private void setAlarm(){
		if(this.currentTrainNo != null && this.currentTrainNo != "" && this.offStatnNo != null && this.offStatnNo != ""){
			//1분마다 기차도착을 확인하는 알람을 맞춘다.
			
		}
	}
	
	
	//지하철 현황을 그린다
	public void drawLineStatus(String json){

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    private void toastMsg(String msg){
    	Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

}
