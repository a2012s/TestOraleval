package com.example.wangjj.testoraleval;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.unisound.edu.oraleval.sdk.sep15.IOralEvalSDK;
import com.unisound.edu.oraleval.sdk.sep15.OralEvalSDKFactory;
import com.unisound.edu.oraleval.sdk.sep15.SDKError;

import java.io.*;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jiangyd on 8/b4/14.
 */
public class NewDemoActivity extends Activity implements View.OnClickListener, IOralEvalSDK.ICallback{
    private static final String TAG = "oraleval-demo";
    static final boolean USE_OFFLINE_SDK_IF_FAIL_TO_SERVER = true;

    private FileOutputStream audioFileOut;
    private FileOutputStream opusFileOut;
    private File files;
    private final String audioName="testAudio.mp3";  //音频文件名
    private final String opusName="testOpus";  //opus文件名
    private  boolean isPlay=true;     //Play按钮状态标志
    private String serviceType = "A";

    private Toast toast;
    private EditText editText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);
        toast=Toast.makeText(NewDemoActivity.this,"",Toast.LENGTH_LONG);
        editText = (EditText)findViewById(R.id.et_elast);
        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.play_b).setOnClickListener(this);
        findViewById(R.id.cancel_b).setOnClickListener(this);
        findViewById(R.id.retry_b).setOnClickListener(this);
        ((ProgressBar)findViewById(R.id.progressBar)).setMax(100);
        File filesdir=this.getFilesDir();
        if(!filesdir.exists()){
            filesdir.mkdirs();
        }
        if(USE_OFFLINE_SDK_IF_FAIL_TO_SERVER)
        {
            Log.i(TAG, "start init offline sdk");
            IOralEvalSDK.OfflineSDKError err = OralEvalSDKFactory.initOfflineSDK(NewDemoActivity.this, filesdir.getAbsolutePath());
            Log.i(TAG, "end init offline sdk");
            if (err != IOralEvalSDK.OfflineSDKError.NOERROR)
            {
                showResult("init sdk failed:" + err);
            }
        }
//        files=thisgetExternalFilesDir("yunzhisheng");
        files=new File(Environment.getExternalStorageDirectory()+"/yunzhisheng/");
        if (!files.exists()){
            files.mkdirs();
        }
        InputStream reader=null;
        try {
            reader=new FileInputStream(new File(files,"Data.txt"));
            byte [] b =new byte[reader.available()];
            reader.read(b);
            String data=new String(b);
            String a[] =data.split("@");
            for (int i=0;i<a.length;i++){
                Log.i(TAG,"加载列表："+a[i].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try{
                if(reader!=null){
                    reader.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void showToast(String txt){
        if(!TextUtils.isEmpty(txt)){
            toast.setText(txt);
            toast.show();
        }
    }

    IOralEvalSDK _oe;
    ProgressDialog _pd;

    final static JsonParser PARSER = new JsonParser();
    final static Gson GSON = new Gson();


    private void showResult(String str){
        JsonElement jo = null;
        try {
            jo = PARSER.parse(str);
        }catch(JsonParseException jpe){
            ((TextView) findViewById(R.id.textView)).setText(str);
            return;
        }
        StringWriter swr = new StringWriter();
        JsonWriter jwr = new JsonWriter(swr);
        jwr.setIndent("  ");
        GSON.toJson(jo, jwr);
        try{jwr.flush();swr.flush();}catch(Exception e){}
        ((TextView) findViewById(R.id.textView)).setText(swr.getBuffer());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(USE_OFFLINE_SDK_IF_FAIL_TO_SERVER)
            OralEvalSDKFactory.cleanupOfflineSDK(this);
    }

    public static class PresistCfg {
        public float _scoreAdjuest = 1.0f;
        public String serviceType = "A";
        public String uid = "";
        public String online_ip = "";
        public String host_ip = "";
        public int socket_timeout = 0;
        public boolean setAsyncRecognize=false;
        public boolean mp3Audio=true;
    }

    private OralEvalSDKFactory.StartConfig getCfg(String txt)
    {


        File file=new File(files,"test.wav");
        OralEvalSDKFactory.StartConfig cfg=null;
        if (file.exists())
        {
            try
            {
                cfg = new OralEvalSDKFactory.StartConfig(txt,file.getAbsolutePath());
            }
            catch (IOException e) {
                e.printStackTrace();
                showToast("Opening " + file.getAbsolutePath() + " failed");
                return null;
            }
            showToast("正在使用音频文件测评");
        }else{
            cfg = new OralEvalSDKFactory.StartConfig(txt);
            showToast("正在使用mic正常录音评测");
            cfg.setVadEnable(true);
            cfg.setVadAfterMs(5000);
            cfg.setVadBeforeMs(5000);
//            cfg.setAsyncRecognize(true);
        }

        if(USE_OFFLINE_SDK_IF_FAIL_TO_SERVER) {
            cfg.set_useOfflineWhenFailedToConnectToServer(true);
        }
        cfg.setBufferLog(true);
		//cfg.setVolumeReport(false);
        cfg.setMp3Audio(true);//use mp3 in onAudioData() callback, or pcm output.
//        cfg.setServiceType("A");
//        cfg.setScoreAdjuest(1.0f);
        InputStream is=null;
        try {
//            file = new File(files, "cfg.txt");
            is=new FileInputStream(new File(files, "cfg.txt"));
            byte [] b =new byte[is.available()];
            is.read(b);
            String data=new String(b);
            Log.i(TAG,"cfgData:"+data);
            PresistCfg pcfg = GSON.fromJson(data/*new FileReader(file)*/, PresistCfg.class);
            if(pcfg != null) {
                cfg.setScoreAdjuest(pcfg._scoreAdjuest);
                cfg.setServiceType(pcfg.serviceType);
                if(!TextUtils.isEmpty(pcfg.uid)) {
                    cfg.setUid(pcfg.uid);
                }
                if(!TextUtils.isEmpty(pcfg.online_ip)) {
                    cfg.setOnline_ip(pcfg.online_ip);
                }
                if(!TextUtils.isEmpty(pcfg.host_ip)) {
                    cfg.setHost_ip(pcfg.host_ip);
                }
                cfg.setSocket_timeout(pcfg.socket_timeout);
                cfg.setMp3Audio(pcfg.mp3Audio);
//                showToast("_scoreAdjuest:"+pcfg._scoreAdjuest+"---serviceType:"+pcfg.serviceType+"---mp3Audio:"+pcfg.mp3Audio+"---");
                Log.i(TAG, "cfg:_scoreAdjuest:" + pcfg._scoreAdjuest);
                Log.i(TAG,"cfg:serviceType:"+pcfg.serviceType);
                Log.i(TAG,"cfg:mp3Audio:"+pcfg.mp3Audio);
                cfg.setAsyncRecognize(pcfg.setAsyncRecognize);
//                Log.i(TAG,"onAsyncResult参数:"+pcfg.setAsyncRecognize);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return cfg;
    }

    @Override
    public void onClick(View view) {
        if(view.equals(findViewById(R.id.button))){
            if(_oe == null)
            {
                //_pcmBuf = new ByteArrayOutputStream();
                String txt = ((EditText) findViewById(R.id.editText)).getText().toString();
                if(TextUtils.isEmpty(txt))
                {
                    Toast.makeText(NewDemoActivity.this, "Empty Text!", Toast.LENGTH_LONG).show();
                    return;
                }
                OralEvalSDKFactory.StartConfig cfg = getCfg(txt);
                if(cfg == null)
                {
                    return;
                }
                _oe = OralEvalSDKFactory.start(this, cfg, this);
                try {
                    Method m = _oe.getClass().getMethod("getAppKey");
                    String k =(String)m.invoke(_oe);
                    k=k.substring(k.length()-6, k.length());
                    this.setTitle(k);
                }catch (Exception eee){
                    Log.e(TAG, "getting appkey", eee);
                }
//                view.setEnabled(false);
                ((Button) view).setText("停止");

                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    File audioFile=new File(files,audioName);
                    if (!audioFile.exists()){
                        try {
                            audioFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    File opusFile=new File(files,opusName);
                    if (!opusFile.exists()){
                        try {
                            opusFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    updateFile("title.txt", txt);

                }else{
                    Toast.makeText(this,"没有SD卡",Toast.LENGTH_LONG).show();
                }
            }else {
                _oe.stop();
                Log.e("============","stop");
                ((Button) findViewById(R.id.button)).setText(R.string.start);
//                _oe = null;
////                view.setEnabled(false);
//                _pd = new ProgressDialog(this);
//                _pd.setCanceledOnTouchOutside(false);
//                _pd.show();
            }
        }else if(view.equals(findViewById(R.id.retry_b))){
            retry(opusName);
        } else if(view.equals(findViewById(R.id.cancel_b))){
            ((Button) findViewById(R.id.button)).setText(R.string.start);
            if(_oe!=null){
                _oe.cancel();
            }
        } else if(view.equals(findViewById(R.id.play_b))){
            if(isPlay){
                try {
                    final MediaPlayer mp = MediaPlayer.create(this, Uri.fromFile(new File(files, audioName)));
                    mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            Log.d(TAG, "mp3 duration:" + mp.getDuration() + "ms");
                        }
                    });
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            Log.d(TAG, "final pos:" + mp.getCurrentPosition());
                            mp.release();
                            findViewById(R.id.play_b).setEnabled(true);
                        }
                    });
                    mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            mp.release();
                            findViewById(R.id.play_b).setEnabled(true);
                            return false;
                        }
                    });
                    mp.start();
                    view.setEnabled(false);
                }catch (Exception ee){
                    Log.e(TAG, "playing failed.", ee);
                }
                /*
                _at = playPcmBuf(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    isPlay=true;
                                    findViewById(R.id.play_b).setEnabled(true);
                                    _at.stop();
                                }catch (Exception e){}
                                try{
                                    _at.release();
                                }catch (Exception e){}
                            }
                        });
                    }
                });*/
            }
        }
    }

    private void retry(String file_name){
        if(_oe!=null){
            _oe.cancel();
            ((Button) findViewById(R.id.button)).setText(R.string.start);
            Toast.makeText(NewDemoActivity.this, "上次评测正在进行中，现在已经取消", Toast.LENGTH_LONG).show();
            return;
        }
        String txt = ((EditText) findViewById(R.id.editText)).getText().toString();
        if(TextUtils.isEmpty(txt))
        {
            Toast.makeText(NewDemoActivity.this, "Empty Text!", Toast.LENGTH_LONG).show();
            return;
        }
        File file=new File(files,file_name);
        Log.e("file size =",file.length() +"");
        OralEvalSDKFactory.StartConfig cfg=null;
        if (file.exists())
        {
            try
            {
                cfg = new OralEvalSDKFactory.StartConfig(txt,file.getAbsolutePath());
            }
            catch (IOException e) {
                e.printStackTrace();
                showToast("Opening " + file.getAbsolutePath() + " failed");
                return ;
            }
            showToast("正在重试");
        }else{
            showToast("没有可使用音频");
        }
        cfg.setBufferLog(true);
        cfg.setMp3Audio(true);
        cfg.setReTry(true);
        InputStream is=null;
        try {
            is=new FileInputStream(new File(files, "cfg.txt"));
            byte [] b =new byte[is.available()];
            is.read(b);
            String data=new String(b);
            Log.i(TAG,"cfgData:"+data);
            PresistCfg pcfg = GSON.fromJson(data/*new FileReader(file)*/, PresistCfg.class);
            if(pcfg != null) {
                cfg.setScoreAdjuest(pcfg._scoreAdjuest);
                cfg.setServiceType(pcfg.serviceType);
                if(!TextUtils.isEmpty(pcfg.uid)) {
                    cfg.setUid(pcfg.uid);
                }
                cfg.setMp3Audio(pcfg.mp3Audio);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(cfg == null)
        {
            return;
        }
        _oe = OralEvalSDKFactory.start(this, cfg, this);

    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    /*
     文件缓存
     */
    public void updateFile(String fileName,String data){
        File titleFile=new File(files,fileName);
        if (!titleFile.exists()){
            try {
                titleFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        OutputStream os=null;
        try {
            os=new FileOutputStream(titleFile);
            os.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (os!=null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    long audioLength=0;
    @Override
    public void onStart(IOralEvalSDK iOralEvalSDK, int audioId) {
        Log.i(TAG, "onStart(), audioId="+audioId);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.button).setEnabled(true);
            }
        });
        audioLength=0;
    }

    @Override
    public void onCancel() {
        Log.i(TAG, "onCancel()");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _oe = null;
                if (audioFileOut != null) {
                    try {
                        audioFileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    audioFileOut = null;
                }

                if (opusFileOut != null) {
                    try {
                        opusFileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    opusFileOut = null;
                }
                ((Button) findViewById(R.id.button)).setText(R.string.start);
            }
        });
    }
    @Override
    public void onError(IOralEvalSDK iOralEvalSDK, SDKError error, IOralEvalSDK.OfflineSDKError ofError) {
        Log.i(TAG,"onError");
        final SDKError err = error;
        final IOralEvalSDK.OfflineSDKError ofe = ofError;
        final String sdkLog = iOralEvalSDK.getLog();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _oe = null;
                updateFile("log.txt", sdkLog);
                ((Button) findViewById(R.id.button)).setText(R.string.start);
                showResult("Error:" + err + "\n. offline error:" + ofe);
                findViewById(R.id.button).setEnabled(true);
                if (_pd != null) {
                    _pd.dismiss();
                    _pd = null;
                }
                if (audioFileOut != null) {
                    try {
                        audioFileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    audioFileOut = null;
                }

                if (opusFileOut != null) {
                    try {
                        opusFileOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    opusFileOut = null;
                }
            }
        });
    }

    @Override
    public void onStop(IOralEvalSDK iOralEvalSDK, String s, boolean offline,String str,IOralEvalSDK.EndReason stoptype) {
        Log.i(TAG, "onStop(), offline=" + offline + ", stoptype:" + stoptype);
        Log.i(TAG, "result:" + s);
        Log.i(TAG, "url:" + str);

        final String sdkLog = iOralEvalSDK.getLog();

        final String rst = s;
        final String url=str;
        if (audioFileOut!=null){
            try {
                audioFileOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioFileOut=null;
        }

        if (opusFileOut != null) {
            try {
                opusFileOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            opusFileOut = null;
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _oe = null;
                ((Button) findViewById(R.id.button)).setText(R.string.start);
                showResult(rst);
                updateFile("result.txt", rst);
                updateFile("log.txt", sdkLog);
                findViewById(R.id.button).setEnabled(true);
                ((EditText) findViewById(R.id.et_url)).setText(url);
                // Toast.makeText(NewDemoActivity.this,"URl:"+url,Toast.LENGTH_LONG).show();
                File audioFile = new File(files, audioName);
                if (audioFile.exists()) {
                    long size = audioFile.length();
                    if (size < 20000) {
                        showToast("说话时间过短");
                    }
                }
                showToast("length:"+audioLength+"-----fileLength:"+audioFile.length());
                if (_pd != null) {
                    _pd.dismiss();
                    _pd = null;
                }
            }
        });
    }

    @Override
    public void onVolume(IOralEvalSDK who, final int value) {
        //Log.i(TAG, "Volume:" + value);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ProgressBar)findViewById(R.id.progressBar)).setProgress(value);
            }
        });
    }

    @Override
    public void onStartOralEval() {
        Log.e("StartOralEval","StartOralEval");
    }

    /**
     * NOTE: store or network-transfer PCM data is a waste for voice.
     * there will be 32K bytes every 1 second.
     */
    //ByteArrayOutputStream _pcmBuf;
    //AudioTrack _at;
    OutputStream _record;

    @Override
    public void onAudioData(IOralEvalSDK iOralEvalSDK, byte[] bytes, int offset, int len) {
        //Log.i(TAG, "got " + len + " bytes of pcm 录音中......offset:"+offset+"------len："+len);
        audioLength+=bytes.length;
        try {
            if (audioFileOut==null){
                audioFileOut=new FileOutputStream(new File(files,audioName));
            }
            audioFileOut.write(bytes,offset,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //_pcmBuf.write(bytes, offset, len);
    }

    @Override
    public void onOpusData(IOralEvalSDK iOralEvalSDK, byte[] bytes, int offset, int len) {
        try {
            if (opusFileOut==null){
                opusFileOut=new FileOutputStream(new File(files,opusName));
            }
            opusFileOut.write(bytes,offset,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAsyncResult(IOralEvalSDK who, String url){
        Log.i(TAG, "onAsyncResult url:" + url);
    }

    AudioTrack playPcmBuf(final Runnable onFinish){
//        if(_pcmBuf == null) {
//            onFinish.run();
//            return null;
//        }
//        isPlay=false;
//
//        final byte[] pcm = _pcmBuf.toByteArray();

        ByteArrayOutputStream bos = null;
        try {
            FileInputStream fis = new FileInputStream(new File(files,audioName));
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
            isPlay=true;
            findViewById(R.id.play_b).setEnabled(true);
            Toast.makeText(this,"没有找到文件",Toast.LENGTH_LONG).show();
            return null;
        }
        final byte[] pcm = bos.toByteArray();

        final int packLen = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;
        final AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC
                , 16000
                , AudioFormat.CHANNEL_OUT_MONO
                , AudioFormat.ENCODING_PCM_16BIT
                , packLen, AudioTrack.MODE_STREAM);

        at.setStereoVolume(1.0f,1.0f);
        try {
            int waitTimes = 3;
            while (at.getState() != AudioTrack.STATE_INITIALIZED && waitTimes > 0) {
                Thread.sleep(100);
//                waitTimes--;
            }
            at.play();
        }catch(Exception e){
            Log.e(TAG, "starting audio tracker", e);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int offset = 0;
                do{
                    offset += at.write(pcm, offset, offset + packLen > pcm.length ? pcm.length - offset : packLen);
                }while(offset < pcm.length);
                Log.i(TAG, "play audio pcm length:" + offset);
                onFinish.run();
            }
        }).start();
        return at;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }


    private void newDialog(){
        final CharSequence[] items = { "A", "B", "C","D","E","enstar","gzedunet","gzedunet_answer" };
        AlertDialog.Builder builder = new AlertDialog.Builder(
                NewDemoActivity.this);
        builder.setTitle("评测模式")
                .setCancelable(false)
                .setSingleChoiceItems(items,0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(NewDemoActivity.this,
                                "选择了" + items[which] + "评测模式", Toast.LENGTH_SHORT)
                                .show();
                        serviceType = items[which] + "";
                        dialog.dismiss();
                    }
                });
        AlertDialog dlg = builder.create();
        dlg.show();
    }


}
