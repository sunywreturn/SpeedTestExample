package com.sun.speedtestexample;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private SpeedDisplayView sdv;
    private Button bt_test;

    private ConnectionClassManager mConnectionClassManager;
    private DeviceBandwidthSampler mDeviceBandwidthSampler;
    private ConnectionChangedListener mListener;
    private String mURL = "http://downloadtest.kdatacenter.com/10MB";
    private ConnectionQuality mConnectionClass = ConnectionQuality.UNKNOWN;
    private int mTries = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConnectionClassManager.remove(mListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionClassManager.register(mListener);
    }

    private void init() {
        sdv = findViewById(R.id.sdv);
        bt_test = findViewById(R.id.bt_test);
        setTestStatusView(Constant.StartTest);
        mConnectionClassManager = ConnectionClassManager.getInstance();
        mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();
        mListener = new ConnectionChangedListener();
    }

    public void testspeed(View view) {
        //重置此带宽管理器实例的带宽平均值
        mConnectionClassManager.reset();
        new DownloadFile().execute(mURL);
    }

    private class ConnectionChangedListener
            implements ConnectionClassManager.ConnectionClassStateChangeListener {
        @Override
        public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
            mConnectionClass = bandwidthState;
        }
    }

    private class DownloadFile extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            mDeviceBandwidthSampler.startSampling();
            setTestStatusView(Constant.Testing);
        }

        @Override
        protected Void doInBackground(String... url) {
            String imageURL = url[0];
            try {
                URLConnection connection = new URL(imageURL).openConnection();
                connection.setUseCaches(false);  //不使用缓存
                connection.connect();
                InputStream input = null;
                try {
                    input = connection.getInputStream();
                    byte[] buffer = new byte[1024];
                    while (true){
                        int read = input.read(buffer);
                        if (read == -1) {
                            break;
                        }
                        //当前下载速度
                        final float speed = mConnectionClassManager.getDownloadKBitsPerSecond() == -1?0.00f:formatNum(mConnectionClassManager.getDownloadKBitsPerSecond()/1024);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sdv.setSpeedValue(speed);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e("MainActivity", "Error while downloading file.");
                }finally {
                    if(input != null){
                        input.close();
                    }
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Error while downloading file.");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mDeviceBandwidthSampler.stopSampling();
            // Retry for up to 10 times until we find a ConnectionClass.
            if (mConnectionClass == ConnectionQuality.UNKNOWN && mTries < 10) {
                mTries++;
                new DownloadFile().execute(mURL);
            }
            if (!mDeviceBandwidthSampler.isSampling()) {
                setTestStatusView(Constant.RetryTest);
            }
            sdv.reset();
        }
    }

    private void setTestStatusView(int status) {
        if(status == Constant.StartTest){
            bt_test.setText(getString(R.string.start_speed_test));
            bt_test.setEnabled(true);
        }else if(status == Constant.Testing){
            bt_test.setText(getString(R.string.speed_testing));
            bt_test.setEnabled(false);
        }else if(status == Constant.RetryTest){
            bt_test.setText(getString(R.string.retry_speed_test));
            bt_test.setEnabled(true);
        }
    }

    public static float formatNum(double num){
        DecimalFormat format = new DecimalFormat("#.##");
        return Float.valueOf(format.format(num));
    }
}
