package com.zatouri.wakemeupat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

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
	private final Handler handler = new Handler();
	
	//UI Controls
	private WebView webView;
	private TextView textView;
	
	private String subwayLine;
	private String currentTrainNo;
	private String offStatnNo;
	
	private Handler guiThread;
	private ExecutorService transThread;
	private Runnable updateTask;
	private Future transPending;

	
   /** Object exposed to JavaScript */
	private class AndroidBridge {
		public void callAndroid(final String arg) { // must be final
			handler.post(new Runnable() {
				public void run() {
					Log.d(TAG, "callAndroid(" + arg + ")");
					//textView.setText(arg);
				}
			});
		}
	  
		public void showToast(final String msg){
			handler.post(new Runnable() {
				public void run() {
					Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
				}
			});

		}
		
		public void refresh(){
			handler.post(new Runnable() {
				public void run() {
					textView.setText(null);
					queueUpdate(200);
				}
			});

		}
	}
	
	public void setText(String txt){
		textView.setText(txt);
		webView.loadUrl("javascript:callJS('"+txt+"')");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		textView = (TextView)findViewById(R.id.result);
		
		webView = (WebView)findViewById(R.id.webView);
		webView.setWebViewClient(new MyWebViewClient());
		
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
		});		
		webView.loadUrl("file:///android_asset/index.html");

		//Binding Listener
		initThreading();
		
		//queueUpdate(200 /* milliseconds */);
		
	}

	@Override
	protected void onDestroy() {
	  // Terminate extra threads here
	  transThread.shutdownNow();
	  super.onDestroy();
	}

	private void initThreading() {
		  guiThread = new Handler();
		  transThread = Executors.newSingleThreadExecutor();

		  // This task does a translation and updates the screen
		  updateTask = new Runnable() { 
			 public void run() {

				// Cancel previous task if there was one
				if (transPending != null)
				   transPending.cancel(true); 

				   // Begin task now but don't wait for it
				   try {
					  JSONFetcher fetcher = new JSONFetcher(MainActivity.this, "http://m.bus.go.kr/mBus/subway/getStatnByRoute.do?subwayId=1001");
					  transPending = transThread.submit(fetcher); 
				   } catch (RejectedExecutionException e) {
					   Log.e(TAG, "RejectedExcutionException", e);
				   }

			 }
		  };
	}	

	/** Request an update to start after a short delay */
	private void queueUpdate(long delayMillis) {
	  // Cancel previous update if it hasn't started yet
	  guiThread.removeCallbacks(updateTask);
	  // Start an update if nothing happens after a few milliseconds
	  guiThread.postDelayed(updateTask, delayMillis);
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
	
	

	public void drawLineStatus(String json){
		
		
		
		//지하철 현황을 그린다
		//webView.loadUrl("Javascript:callJS("+json+")");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	//기본 웹브라우저 동작을 막기 위한 내부클래스
    private class MyWebViewClient extends WebViewClient {
    	
    	//To prevent to call Default Web browser
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}
