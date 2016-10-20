package com.example.lucas.opencvdemo;

/**
 * Created by Lucas on 2016/9/4.
 */
import com.example.lucas.opencvdemo.Bluetooth.ServerOrCilent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class chatActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,OnItemClickListener ,OnClickListener {
    /**
     * Called when the activity is first created.
     */

    //private ListView mListView;
    //private ArrayList<deviceListItem>list;
    //deviceListAdapter mAdapter;
    Context mContext;

    /* 一些常量，代表服务器的名称 */
    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

    private BluetoothServerSocket mserverSocket = null;
    private ServerThread startServerThread = null;
    private clientThread clientConnectThread = null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private readThread mreadThread = null;
    ;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final String _TAG = "AAVActivity";

    static final double MIN_CONTOUR_AREA = 100;

    private Mat _rgbaImage;

    private JavaCameraView _opencvCameraView;
    private ActuatorController _mainController;

    volatile double _contourArea = 7;
    volatile Point _centerPoint = new Point(-1, -1);    //圆球中间点
    Point _screenCenterCoordinates = new Point(-1, -1);   //屏幕中间点
    int _countOutOfFrame = 0;

    Mat _hsvMat;
    Mat _processedMat;
    Mat _dilatedMat;
    Scalar _lowerThreshold;
    Scalar _upperThreshold;
    final List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

    SharedPreferences _sharedPreferences;
    static int _trackingColor = 0;
    GestureDetector _gestureDetector;
    private boolean _showContourEnable = true;

    // See Static Initialization of OpenCV (http://tinyurl.com/zof437m)
    //
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("ERROR", "Unable to load OpenCV");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    _opencvCameraView.enableView();
                    _hsvMat = new Mat();
                    _processedMat = new Mat();
                    _dilatedMat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("onCreate");
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        mContext = this;
        // PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _trackingColor = Integer.parseInt(_sharedPreferences.getString(getString(R.string.color_key), "0"));

        if (_trackingColor == 0) {
            _lowerThreshold = new Scalar(60, 100, 30); // Green
            _upperThreshold = new Scalar(130, 255, 255);
        } else if (_trackingColor == 1) {
            _lowerThreshold = new Scalar(160, 50, 90); // Purple
            _upperThreshold = new Scalar(255, 255, 255);
        } else if (_trackingColor == 2) {
            _lowerThreshold = new Scalar(1, 50, 150); // Orange
            _upperThreshold = new Scalar(60, 255, 255);
        }
        // _showContourEnable = _sharedPreferences.getBoolean("contour", false);

        _opencvCameraView = (JavaCameraView) findViewById(R.id.aav_activity_surface_view);
        _opencvCameraView.setCvCameraViewListener(this);

        _opencvCameraView.setMaxFrameSize(352, 288); // (176, 144); //(320, 240); <-Callback buffer is too small for these resolutions.
        _mainController = new ActuatorController();
        _countOutOfFrame = 0;


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        _showContourEnable = _sharedPreferences.getBoolean("contour", false);
        _trackingColor = Integer.parseInt(_sharedPreferences.getString(getString(R.string.color_key), "0"));

        switch (_trackingColor) {
            case 0: // Green
                _lowerThreshold.set(new double[]{60, 100, 30, 0});
                _upperThreshold.set(new double[]{130, 255, 255, 0});
                break;
            case 1: // Purple
                _lowerThreshold.set(new double[]{160, 50, 90});
                _upperThreshold.set(new double[]{255, 255, 255, 0});
                break;
            case 2: // Orange
                _lowerThreshold.set(new double[]{1, 50, 150});
                _upperThreshold.set(new double[]{60, 255, 255, 0});
                break;
            default:
                _lowerThreshold.set(new double[]{60, 100, 30, 0});
                _upperThreshold.set(new double[]{130, 255, 255, 0});
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback)) {
        // Log.e(_TAG, "Cannot connect to OpenCV Manager");
        // }
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        hideNavigationBar();
        if (Bluetooth.isOpen) {
            Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
            String address = Bluetooth.BlueToothAddress;
            if (!address.equals("null")) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                Bluetooth.isOpen = true;
            } else {
                Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
            }
        } else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {
            startServerThread = new ServerThread();
            startServerThread.start();
            Bluetooth.isOpen = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_opencvCameraView != null)
            _opencvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (_opencvCameraView != null)
            _opencvCameraView.disableView();

        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT) {
            shutdownClient();
        } else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE) {
            shutdownServer();
        }
        Bluetooth.isOpen = false;
        Bluetooth.serviceOrCilent = ServerOrCilent.NONE;

    }

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        _rgbaImage = new Mat(height, width, CvType.CV_8UC4);
        _screenCenterCoordinates.x = _rgbaImage.size().width / 2;
        _screenCenterCoordinates.y = _rgbaImage.size().height / 2;
    }

    @Override
    public void onCameraViewStopped() {
        _mainController.reset();
        _rgbaImage.release();
        _centerPoint.x = -1;
        _centerPoint.y = -1;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.out.println("onCameraFrame");
        synchronized (inputFrame) {

            _rgbaImage = inputFrame.rgba();
            if (android.os.Build.MODEL.equalsIgnoreCase("Nexus 5X")) {
                Core.flip(_rgbaImage, _rgbaImage, -1);
            }

            double current_contour;

            // In contrast to the C++ interface, Android API captures images in the RGBA format.
            // Also, in HSV space, only the hue determines which color it is. Saturation determines
            // how 'white' the color is, and Value determines how 'dark' the color is.
            Imgproc.cvtColor(_rgbaImage, _hsvMat, Imgproc.COLOR_RGB2HSV_FULL);
            Core.inRange(_hsvMat, _lowerThreshold, _upperThreshold, _processedMat);
            Imgproc.erode(_processedMat, _dilatedMat, new Mat());
            Imgproc.findContours(_dilatedMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            MatOfPoint2f points = new MatOfPoint2f();
            _contourArea = 7;
            for (int i = 0, n = contours.size(); i < n; i++) {
                current_contour = Imgproc.contourArea(contours.get(i));
                if (current_contour > _contourArea) {
                    _contourArea = current_contour;
                    contours.get(i).convertTo(points, CvType.CV_32FC2); // contours.get(x) is a single MatOfPoint, but to use minEnclosingCircle we need to pass a MatOfPoint2f so we need to do a
                    // conversion
                }
            }
            if (!points.empty() && _contourArea > MIN_CONTOUR_AREA) {
                Imgproc.minEnclosingCircle(points, _centerPoint, null);
                // Core.circle(_rgbaImage, _centerPoint, 3, new Scalar(255, 0, 0), Core.FILLED);
                if (_showContourEnable) {
                    Core.circle(_rgbaImage, _centerPoint, (int) Math.round(Math.sqrt(_contourArea / Math.PI)), new Scalar(255, 0, 0), 3, 8, 0);// Core.FILLED);
                    Core.line(_rgbaImage, _centerPoint, _screenCenterCoordinates, new Scalar(255, 0, 0));
                    System.out.println(_centerPoint);
                    System.out.println(_screenCenterCoordinates);
                }
            }
            contours.clear();
        }
        return _rgbaImage;
    }



	/*
	private Handler LinkDetectedHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
			if(msg.what==1)
			{
				list.add(new deviceListItem((String)msg.obj, true));
			}
			else
			{
				list.add(new deviceListItem((String)msg.obj, false));
			}
			mAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
		}

	};
	*/


    //发送PWM值

        //开启客户端
        private class clientThread extends Thread {
            public void run() {
                try {
                    //创建一个Socket连接：只需要服务器在注册时的UUID号
                    // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    //连接
                    Message msg2 = new Message();
                    msg2.obj = "请稍候，正在连接服务器:" + Bluetooth.BlueToothAddress;
                    msg2.what = 0;
                    //LinkDetectedHandler.sendMessage(msg2);

                    socket.connect();

                    Message msg = new Message();
                    msg.obj = "已经连接上服务端！可以发送信息。";
                    msg.what = 0;
                    //LinkDetectedHandler.sendMessage(msg);
                    //启动接受数据
                    mreadThread = new readThread();
                    mreadThread.start();
                } catch (IOException e) {
                    Log.e("connect", "", e);
                    Message msg = new Message();
                    msg.obj = "连接服务端异常！断开连接重新试一试。";
                    msg.what = 0;
                    //LinkDetectedHandler.sendMessage(msg);
                }
            }
        }

        ;

        //开启服务器
        private class ServerThread extends Thread {
            public void run() {

                try {
				/* 创建一个蓝牙服务器
				 * 参数分别：服务器名称、UUID	 */
                    mserverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM,
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                    Log.d("server", "wait cilent connect...");

                    Message msg = new Message();
                    msg.obj = "请稍候，正在等待客户端的连接...";
                    msg.what = 0;
                    //LinkDetectedHandler.sendMessage(msg);

				/* 接受客户端的连接请求 */
                    socket = mserverSocket.accept();
                    Log.d("server", "accept success !");

                    Message msg2 = new Message();
                    String info = "客户端已经连接上！可以发送信息。";
                    msg2.obj = info;
                    msg.what = 0;
                    //LinkDetectedHandler.sendMessage(msg2);
                    //启动接受数据
                    mreadThread = new readThread();
                    mreadThread.start();
                    sendMessageHandle("r");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        ;

        /* 停止服务器 */
        private void shutdownServer() {
            new Thread() {
                public void run() {
                    if (startServerThread != null) {
                        startServerThread.interrupt();
                        startServerThread = null;
                    }
                    if (mreadThread != null) {
                        mreadThread.interrupt();
                        mreadThread = null;
                    }
                    try {
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                        if (mserverSocket != null) {
                            mserverSocket.close();/* 关闭服务器 */
                            mserverSocket = null;
                        }
                    } catch (IOException e) {
                        Log.e("server", "mserverSocket.close()", e);
                    }
                }

                ;
            }.start();
        }

        /* 停止客户端连接 */
        private void shutdownClient() {
            new Thread() {
                public void run() {
                    if (clientConnectThread != null) {
                        clientConnectThread.interrupt();
                        clientConnectThread = null;
                    }
                    if (mreadThread != null) {
                        mreadThread.interrupt();
                        mreadThread = null;
                    }
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        socket = null;
                    }
                }

                ;
            }.start();
        }

        //发送数据
        private void sendMessageHandle(String msg) {
            if (socket == null) {
                Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //list.add(new deviceListItem(msg, false));
            //mAdapter.notifyDataSetChanged();
            //mListView.setSelection(list.size() - 1);
        }

        //读取数据
        private class readThread extends Thread {
            public void run() {

                byte[] buffer = new byte[1024];
                int bytes;
                InputStream mmInStream = null;

                try {
                    mmInStream = socket.getInputStream();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                while (true) {
                    try {
                        // Read from the InputStream
                        if ((bytes = mmInStream.read(buffer)) > 0) {
                            byte[] buf_data = new byte[bytes];
                            for (int i = 0; i < bytes; i++) {
                                buf_data[i] = buffer[i];
                            }
                            String s = new String(buf_data);
                            Message msg = new Message();
                            msg.obj = s;
                            msg.what = 1;
                            //LinkDetectedHandler.sendMessage(msg);
                        }
                    } catch (IOException e) {
                        try {
                            mmInStream.close();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        public class SiriListItem {
            String message;
            boolean isSiri;

            public SiriListItem(String msg, boolean siri) {
                message = msg;
                isSiri = siri;
            }
        }

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
        }

        public class deviceListItem {
            String message;
            boolean isSiri;

            public deviceListItem(String msg, boolean siri) {
                message = msg;
                isSiri = siri;
            }
        }
}