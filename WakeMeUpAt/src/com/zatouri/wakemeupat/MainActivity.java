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
	private String direction;
	private String destStatnId;
	private String result;
	
   /** Object exposed to JavaScript */
	private class AndroidBridge {
	  
		public void getStatnByRoute(final String lineNo){
			guiThread.post(new Runnable() {
				public void run() {
					MainActivity.this.getStatnByRoute(lineNo);
				}
			});
		}
		
		//public void setCurTrainNo(final String subwayId, final String statnId, final String direction){
		public void setCurTrainNo(final String bigStr){

			try {
				final JSONObject jso = new JSONObject(bigStr);
				final String subwayId = jso.getString("subwayId");
				final String statnId = jso.getString("statnId");
				final String direction = jso.getString("direction");
				
				toastMsg("android:"+statnId);
				
				if(jso != null){
					guiThread.post(new Runnable() {
						public void run() {
							MainActivity.this.getStatnTrainInfo(subwayId, statnId, direction);
						}
					});
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
	
		public void setDestStatnId(final String statnId){
			guiThread.post(new Runnable() {
				public void run() {
					MainActivity.this.setDestStatnId(statnId);
				}
			});			
		}
		
	}
	

	public void drawLineStatus(JSONObject json){
		
		try{
			JSONArray ja = json.getJSONArray("resultList");
			result = ja.toString();
			Log.d(TAG, result);
		
			//여기서 테이블을 만들자
			StringBuilder sb = new StringBuilder();
			
			for(int i = 0 ; i < ja.length() ; i++){
				JSONObject obj = ja.getJSONObject(i);

				String subwayId = obj.getString("subwayId");
				String statnId = obj.getString("statnId");
				String onclickFunc = "setCurTrainNo('"+subwayId+"','"+statnId+"','#_direction_#');";
				
	            sb.append("<div class=\"ui-block-a\">");
				sb.append(obj.getString("statnNm"));
				sb.append("</div>");
	            sb.append("<div class=\"ui-block-b\" data-subwayid=\""+ subwayId +"\" data-statnid=\""+ statnId +"\">");				
	            //sb.append("Y".equals(obj.getString("existYn2")) ? "<a onclick=\""+onclickFunc.replace("#_direction_#", "하행")+"\">■</a>" : "|");
	            sb.append("Y".equals(obj.getString("existYn1")) ? "777" : "|");
				sb.append("</div>");
				sb.append("<div class=\"ui-block-c\" data-subwayid=\""+ subwayId +"\" data-statnid=\""+ statnId +"\">");			
	            //sb.append("Y".equals(obj.getString("existYn2")) ? "<a onclick=\""+onclickFunc.replace("#_direction_#", "상행")+"\">■</a>" : "|");
	            sb.append("Y".equals(obj.getString("existYn2")) ? "777" : "|");
				sb.append("</div>");
			}
			
			Log.d(TAG, sb.toString());
			//toastMsg("before setStatnInfo");
			webView.loadUrl("javascript:setStatnInfo('"+sb.toString()+"')");

			
		}catch(JSONException je){
			toastMsg(je.getMessage());
		}
	}
	
	public void setTrainId(JSONObject json){
		try{
			JSONArray ja = json.getJSONArray("resultList");
			result = ja.toString();
			Log.d(TAG, result);
		
			if(direction != null){
				for(int i = 0 ; i < ja.length() ; i++){
					JSONObject obj = ja.getJSONObject(i);
	
					if(direction.equals(obj.getString("updnLine"))){
						this.currentTrainNo = obj.getString("trainNo");
						toastMsg("열차번호:"+currentTrainNo);
						setAlarm();
					}
				}
			}			
			
		}catch(JSONException je){
			toastMsg(je.getMessage());
		}		
		
	}
	
	public void setDestStatnId(String statnId){
		this.destStatnId = statnId;
		setAlarm();
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
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
				Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
				result.confirm();
				return true; // I handled it
			}
			
			@Override
			public boolean onConsoleMessage(ConsoleMessage cm) 
		    {
		        Log.d(TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId() );
		        return true;
		    }
		});	
		
		//load HTML
		webView.loadUrl("file:///android_asset/index.html");
	
	}

	@Override
	protected void onDestroy() {
	  // Terminate extra threads here
	  transThread.shutdownNow();
	  super.onDestroy();
	}

	private void getStatnByRoute(String lineNo){
		fetchJSON("http://m.bus.go.kr/mBus/subway/getStatnByRoute.do", "subwayId="+lineNo, "drawLineStatus");
	}
	
	private void getStatnTrainInfo(String subwayId, String statnId, String direction){
		toastMsg("android:"+statnId);
		this.direction = direction;
		fetchJSON("http://m.bus.go.kr/mBus/subway/getStatnTrainInfo.do", "subwayId="+subwayId+"&statnId="+statnId,"setTrainId");
		
	}
	
	private void fetchJSON(String url, String queryString, String callback) {
		Log.d(TAG, url+queryString);
		try {
		  JSONFetcher fetcher = new JSONFetcher(MainActivity.this, url + "?" + queryString, callback);
		  transPending = transThread.submit(fetcher); 
		} catch (RejectedExecutionException e) {
		   Log.e(TAG, "RejectedExcutionException", e);
		   toastMsg("Oooops. can't bring data.");
		}
	}	
	

	private void setAlarm(){
		if(this.currentTrainNo != null && this.currentTrainNo != "" && this.destStatnId != null && this.destStatnId != ""){
			//1분마다 기차도착을 확인하는 알람을 맞춘다.
			toastMsg("trainId:"+currentTrainNo+", destStatnId:"+ destStatnId + "으로 알람설정");
		}
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
