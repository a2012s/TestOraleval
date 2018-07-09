package com.example.wangjj.testoraleval;

import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

public class WakeupOfflineActivity extends Activity {

	private EditText mTextViewResult;
	private TextView mTextViewTip;
	private TextView mTextViewStatus;

	private SpeechUnderstander mWakeUpRecognizer;
	private ImageView mLogoImageView;

	private LinearLayout status_panel;
	private TextView type;
	
	private static final String WAKEUP_TAG = "wakeup";

	/**
	 * 唤醒震动提示
	 */
	private Vibrator mVibrator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wakeup);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.status_bar_main);
		mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

		mTextViewResult = (EditText) findViewById(R.id.textViewResult);
		mTextViewStatus = (TextView) findViewById(R.id.textViewStatus);
		mTextViewTip = (TextView) findViewById(R.id.textViewTip);
		status_panel = (LinearLayout) findViewById(R.id.status_panel);
		mLogoImageView = (ImageView) findViewById(R.id.logo_imageview);

		status_panel.setVisibility(View.INVISIBLE);

		type = (TextView) findViewById(R.id.type);
		type.setText(getString(R.string.wakeup_offline_function));

		// 初始化本地离线唤醒
		initWakeUp();
	} 

	/**
	 * 初始化本地离线唤醒
	 */
	private void initWakeUp() {
		mWakeUpRecognizer = new SpeechUnderstander(this, Config.appKey, null);
		mWakeUpRecognizer.setOption(SpeechConstants.ASR_SERVICE_MODE, SpeechConstants.ASR_SERVICE_MODE_LOCAL);
		
		mWakeUpRecognizer.setListener(new SpeechUnderstanderListener() {
			@Override
			public void onResult(int type, String jsonResult) {
				showResultView();
				mTextViewResult.setText(jsonResult + "(唤醒成功)");
			}
			
			@Override
			public void onEvent(int type, int timeMs) {
				switch (type) {
				case SpeechConstants.WAKEUP_EVENT_RECOGNITION_SUCCESS:
					Log.d("TEMPLOG", "WAKEUP_EVENT_RECOGNITION_SUCCESS");
					mVibrator.vibrate(300);
					toastMessage("(唤醒成功)");
					break;
				case SpeechConstants.ASR_EVENT_RECORDING_START:
					Log.d("TEMPLOG", "ASR_EVENT_RECORDING_START");
					setStatusText("语音唤醒已开始");
					setTipText("请说 [你好魔方] 唤醒");
					toastMessage("语音唤醒已开始");
					break;
				case SpeechConstants.ASR_EVENT_RECORDING_STOP:
					Log.d("TEMPLOG", "ASR_EVENT_RECORDING_STOP");
					toastMessage("语音唤醒录音已停止");
					setStatusText("语音唤醒已停止");
					break;
				case SpeechConstants.ASR_EVENT_ENGINE_INIT_DONE:
					Log.d("TEMPLOG", "ASR_EVENT_ENGINE_INIT_DONE");
					toastMessage("引擎初始化完成");
					wakeUpStart();
					break;
				default:
					break;
				}
			}
			
			@Override
			public void onError(int type, String errorMSG) {
				toastMessage("errorMSG = "+errorMSG);
				toastMessage("语音唤醒服务异常  异常信息：" + errorMSG);
				setTipText(errorMSG);
			}
		});
		
		mWakeUpRecognizer.init("");
	}

	private void showResultView() {
		status_panel.setVisibility(View.VISIBLE);
		mLogoImageView.setVisibility(View.GONE);
	}

	protected void setTipText(String tip) {

		mTextViewTip.setText(tip);
	}

	protected void setStatusText(String status) {

		mTextViewStatus.setText(getString(R.string.lable_status) + "(" + status + ")");
	}

	/**
	 * 启动语音唤醒
	 */
	protected void wakeUpStart() {

		toastMessage("开始语音唤醒");
		/** ---设置唤醒命令词集合--- */
		mTextViewResult.setText("");
		mTextViewTip.setText("");

		mWakeUpRecognizer.start(WAKEUP_TAG);
	}

	@Override
	public void onPause() {
		super.onPause();
		// 主动停止识别
		mWakeUpRecognizer.cancel();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mWakeUpRecognizer.start(WAKEUP_TAG);
	}

	private void log_i(String log) {
		Log.i("demo", log);
	}

	@Override
	protected void onStop() {
		super.onStop();
		log_i("onStop()");
	}

	@Override
	protected void onDestroy() {
		log_i("onDestroy()");
		mWakeUpRecognizer.cancel();
		mWakeUpRecognizer.release(SpeechConstants.ASR_RELEASE_ENGINE, "");
		super.onDestroy();
	}

	private void toastMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
}
