package com.example.wangjj.testoraleval;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.unisound.client.SpeechConstants;
import com.unisound.client.SpeechUnderstander;
import com.unisound.client.SpeechUnderstanderListener;

public class ASROfflineActivity extends Activity {

	private ProgressBar mProgressBarVolume;
	private EditText mTextViewResult;
	private TextView mTextViewTip;
	private Button mRecognizerButton;
	private View mStatusView;
	private View mStatusLayout;
	private TextView mStatusTextView;
	private SpeechUnderstander mSpeechUnderstander;
	private ImageView mLogoImageView;
	private LinearLayout status_panel;
	private TextView type;
	enum AsrStatus {
		idle, recording, recognizing
	}

	private AsrStatus statue = AsrStatus.idle;
	/**
	 * 目前离线识别支持的词表
	 */
	private static final String mAsrFixWords = "请说以下命令：打开电视/关闭电视/打开空调/关闭空调/打开蓝牙/关闭蓝牙/增大音量/减小音量/播放音乐/停止播放";
	private static final String mGrammarTag = "main";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_offline_asr);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.status_bar_main);

		mProgressBarVolume = (ProgressBar) findViewById(R.id.progressBarVolume);
		mTextViewResult = (EditText) findViewById(R.id.textViewResult);
		mTextViewTip = (TextView) findViewById(R.id.textViewTip);
		status_panel = (LinearLayout) findViewById(R.id.status_panel);
		mLogoImageView = (ImageView) findViewById(R.id.logo_imageview);
		mRecognizerButton = (Button) findViewById(R.id.recognizer_btn);
		mProgressBarVolume.setVisibility(View.INVISIBLE);
		mStatusView = findViewById(R.id.status_panel);
		status_panel.setVisibility(View.INVISIBLE);
		mStatusTextView = (TextView) findViewById(R.id.status_show_textview);
		mStatusLayout = findViewById(R.id.status_layout);

		type = (TextView) findViewById(R.id.type);
		type.setText(getString(R.string.asr_offline_function));
		mTextViewTip.setText(mAsrFixWords);

		// 初始化本地离线识别
		initFixRecognizer();
		mRecognizerButton.setEnabled(true);
	}

	/**
	 * 初始化本地离线识别
	 */
	private void initFixRecognizer() {
		// please apply your appKey at http://dev.hivoice.cn/
		mSpeechUnderstander = new SpeechUnderstander(this, Config.appKey,Config.secret);
		mSpeechUnderstander.setOption(SpeechConstants.ASR_SERVICE_MODE,
				SpeechConstants.ASR_SERVICE_MODE_LOCAL);
		mSpeechUnderstander.setListener(new SpeechUnderstanderListener() {
			@Override
			public void onResult(int type, String jsonResult) {
				if (type == SpeechConstants.ASR_RESULT_LOCAL) {
					// 返回离线识别结果
					log_i("FixRecognizer onResult");
					mTextViewResult.append(jsonResult);
				}
			}
			
			@Override
			public void onEvent(int type, int timeMs) {
				// TODO Auto-generated method stub
				switch (type) {
				case SpeechConstants.ASR_EVENT_VAD_TIMEOUT:
					// VAD 超时回调
					log_i("FixRecognizer onVADTimeout");
					stopRecord();
					break;
				case SpeechConstants.ASR_EVENT_RECORDING_START:
					log_i("FixRecognizer onRecognizerStart");
					mProgressBarVolume.setVisibility(View.VISIBLE);
					status_panel.setVisibility(View.VISIBLE);
					break;
				case SpeechConstants.ASR_EVENT_VOLUMECHANGE:
					// 说话音量实时返回
					int volume = (Integer) mSpeechUnderstander.getOption(SpeechConstants.GENERAL_UPDATE_VOLUME);
					mProgressBarVolume.setProgress(volume);
					break;
				case SpeechConstants.ASR_EVENT_LOCAL_END:
					// 语音识别结束
					log_i("FixRecognizer onEnd");
					mRecognizerButton.setEnabled(true);
					statue = AsrStatus.idle;
					mRecognizerButton.setText(R.string.click_say);
					mStatusLayout.setVisibility(View.GONE);
					break;
				default:
					break;
				}
			}

			@Override
			public void onError(int type, String errorMSG) {
				// TODO Auto-generated method stub
				if (errorMSG != null || !errorMSG.equals("")) {
					// 显示错误信息
					mTextViewResult.setText(errorMSG);
				} else {
					if ("".equals(mTextViewResult.getText().toString())) {
						mTextViewResult.setText(R.string.no_hear_sound);
					}
				}
			}
		});

		mSpeechUnderstander.init("");

		mRecognizerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (statue == AsrStatus.idle) {
					mTextViewResult.setVisibility(View.VISIBLE);
					mRecognizerButton.setEnabled(false);
					mTextViewResult.setText("");
					
					mStatusView.setVisibility(View.VISIBLE);
					mStatusLayout.setVisibility(View.VISIBLE);
					mLogoImageView.setVisibility(View.GONE);
					// 在收到 onRecognizerStart 回调前，录音设备没有打开，请添加界面等待提示，
					// 录音设备打开前用户说的话不能被识别到，影响识别效果。
					mStatusTextView.setText(R.string.opening_recode_devices);
					recognizerStart();
				} else if (statue == AsrStatus.recording) {
					stopRecord();
				} else if (statue == AsrStatus.recognizing) {
					// 取消识别
					mSpeechUnderstander.cancel();

					mRecognizerButton.setText(R.string.click_say);
					statue = AsrStatus.idle;
				}
			}
		});
	}

	private void stopRecord() {
		mStatusTextView.setText(R.string.just_recognizer);
		mSpeechUnderstander.stop();
	}

	protected void recognizerStart() {
		mTextViewResult.setText("");
		status_panel.setVisibility(View.VISIBLE);
		mLogoImageView.setVisibility(View.GONE);
		mSpeechUnderstander.start(mGrammarTag);
	}

	@Override
	public void onPause() {
		super.onPause();
		// 取消识别
		mSpeechUnderstander.cancel();
		statue = AsrStatus.idle;
	}

	private void log_i(String log) {
		Log.i("demo", log);
	}

	@Override
	protected void onDestroy() {
		mSpeechUnderstander.cancel();
		mSpeechUnderstander.release(SpeechConstants.ASR_RELEASE_ENGINE, "");
		super.onDestroy();
	}

	/**
	 * 打印日志信息
	 * 
	 * @param msg
	 */
	private void log_v(String msg) {
		Log.v("demo", msg);
	}

}
