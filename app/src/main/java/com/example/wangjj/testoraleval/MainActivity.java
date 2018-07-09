package com.example.wangjj.testoraleval;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnClickListener {
	private static final String ASR_ONLINE = "在线识别";
	private static final String FIX = "本地识别";
	private static final String OFFLINE_TTS = "本地合成";
	private static final String OFFLINE_WAKEUP = "本地唤醒";
	private ArrayList<String> mFunctionsArray;

	private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
	private static final int MY_PERMISSIONS_REQUEST_CALL_CAMERA = 2;


	String[] permissions = new String[]{
			Manifest.permission.CAMERA,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.RECORD_AUDIO
	};
	// 声明一个集合，在后面的代码中用来存储用户拒绝授权的权
	List<String> mPermissionList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.status_bar_main);
		initFunctionArray();
		((ListView) findViewById(R.id.lv_functions)).setAdapter(new FunctionsAdapter());

		for (int i = 0; i < permissions.length; i++) {
			if (ContextCompat.checkSelfPermission(MainActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
				mPermissionList.add(permissions[i]);
			}
		}
		if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
			//Toast.makeText(MainActivity.this,"已经授权",Toast.LENGTH_LONG).show();
		} else {//请求权限方法
			String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
			ActivityCompat.requestPermissions(MainActivity.this, permissions, MY_PERMISSIONS_REQUEST_CALL_CAMERA);
		}
	}





	@Override
	public void onClick(View view) {
		Intent intent = null;
		Object tag = view.getTag();
		if(tag.equals(ASR_ONLINE)){
			intent = new Intent(this, ASROnlineActivity.class);
		}
		if(tag.equals(FIX)){
			intent = new Intent(this, ASROfflineActivity.class);
		}
		if(tag.equals(OFFLINE_TTS)){
			intent = new Intent(this, TTSOfflineActivity.class);
		}
		if(tag.equals(OFFLINE_WAKEUP)){
			intent = new Intent(this, WakeupOfflineActivity.class);
		}
		if (intent != null) {
			startActivity(intent);
		}
		
	}






	private class FunctionsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mFunctionsArray.size();
		}

		@Override
		public Object getItem(int arg0) {
			return arg0;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.button_list_item, null);
				holder = new ViewHolder();
				holder.btn = (Button) convertView.findViewById(R.id.btn);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.btn.setText(mFunctionsArray.get(position));
			holder.btn.setTag(mFunctionsArray.get(position));
			holder.btn.setOnClickListener(MainActivity.this);
			return convertView;
		}
	}

	public final class ViewHolder {
		public Button btn;
	}
	private void initFunctionArray() {
		mFunctionsArray = new ArrayList<String>();
		mFunctionsArray.add(ASR_ONLINE);
		mFunctionsArray.add(FIX);
		mFunctionsArray.add(OFFLINE_TTS);
		mFunctionsArray.add(OFFLINE_WAKEUP);
	}

}
