package com.example.lucas.opencvdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.lucas.opencvdemo.Bluetooth.ServerOrCilent;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class chatActivity extends Activity implements OnItemClickListener ,OnClickListener,CameraBridgeViewBase.CvCameraViewListener2 {
    /** Called when the activity is first created. */

    //private ListView mListView;
    //private ArrayList<deviceListItem>list;
    private Button sendButton;
    private Button disconnectButton;
    private EditText editMsgView;
    //deviceListAdapter mAdapter;
    Context mContext;
    private static final String TAG = "ChatActivity";

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
    private readThread mreadThread = null;;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    static final double MIN_CONTOUR_AREA = 100;

    private Mat _rgbaImage;

    private JavaCameraView _opencvCameraView;

    volatile double _contourArea = 7;
    volatile Point _centerPoint = new Point(-1, -1);
    Point _screenCenterCoordinates = new Point(-1, -1);
    int _countOutOfFrame = 0;

    Mat _hsvMat;
    Mat _processedMat;
    Mat _dilatedMat;
    Scalar _lowerThreshold;
    Scalar _upperThreshold;
    final List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

    SharedPreferences _sharedPreferences;
    private boolean _showContourEnable = true;
    OrderController orderController;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        mContext = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        // PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        //_lowerThreshold = new Scalar(0,150,100); // red
        //_upperThreshold = new Scalar(10,255,255);
      //  _lowerThreshold = new Scalar(100,145,90); //blue
      //  _upperThreshold = new Scalar(179,255,255);
         _lowerThreshold = new Scalar(60, 100, 30); // Green
        _upperThreshold = new Scalar(130, 255, 255);
        //_lowerThreshold = new Scalar(10, 150, 150); // Orange
        //_upperThreshold = new Scalar(24, 255, 255);
        // _showContourEnable = _sharedPreferences.getBoolean("contour", false);

        _opencvCameraView = (JavaCameraView) findViewById(R.id.aav_activity_surface_view);
        _opencvCameraView.setCvCameraViewListener(this);

        _opencvCameraView.setMaxFrameSize(1280,720); // (176, 144); //(320, 240); <-Callback buffer is too small for these resolutions.
        _countOutOfFrame = 0;
    }




    @Override
    public synchronized void onPause() {
        super.onPause();
        if (_opencvCameraView != null)
            _opencvCameraView.disableView();
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(Bluetooth.isOpen)
        {
            Toast.makeText(mContext, "连接已经打开，可以通信。如果要再建立连接，请先断开！", Toast.LENGTH_SHORT).show();
            return;
        }
        if(Bluetooth.serviceOrCilent==ServerOrCilent.CILENT)
        {
            String address = Bluetooth.BlueToothAddress;
            if(!address.equals("null"))
            {
                device = mBluetoothAdapter.getRemoteDevice(address);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                Bluetooth.isOpen = true;
            }
            else
            {
                Toast.makeText(mContext, "address is null !", Toast.LENGTH_SHORT).show();
            }
        }
        else if(Bluetooth.serviceOrCilent==ServerOrCilent.SERVICE)
        {
       //     startServerThread = new ServerThread();
         //   startServerThread.start();
         //   Bluetooth.isOpen = true;
        }
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        hideNavigationBar();
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
            Imgproc.GaussianBlur(_hsvMat,_hsvMat,new Size(7,7), 2, 2);
            Core.inRange(_hsvMat, _lowerThreshold, _upperThreshold, _processedMat);
            Imgproc.erode(_processedMat, _dilatedMat, new Mat());
            Imgproc.findContours(_dilatedMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            MatOfPoint2f points = new MatOfPoint2f();
          //  Utils.matToBitmap();
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
                }
            }
            contours.clear();
        }
        System.out.println(_contourArea);
        System.out.println(_screenCenterCoordinates);
        System.out.println(_centerPoint);
        return _rgbaImage;
    }
    private class SendThread extends Thread{
        public void run(){
            String order="s";
            String order_old="s";
            try{
                while (true){
                    Thread.sleep(60);
                    if(socket!=null){
                            order=orderController.updateOrder(_contourArea, _screenCenterCoordinates, _centerPoint);
                            if(order!=order_old) {
                                if(order=="u"&&order_old=="d"){
                                    sendMessageHandle("s");
                                }
                                if(order=="d"&&order_old=="u"){
                                    sendMessageHandle("s");
                                }
                                sendMessageHandle(order);
                                order_old=order;
                            }
                           //Log.d("Send", "is send!!!!");
                           System.out.println("连接成功");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //开启客户端
    private class clientThread extends Thread {
        public void run() {
            try {
                //创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket = device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                //连接
                Message msg2 = new Message();
                msg2.obj = "请稍候，正在连接服务器:"+Bluetooth.BlueToothAddress;
                msg2.what = 0;
                //LinkDetectedHandler.sendMessage(msg2);

                socket.connect();

                Message msg = new Message();
                msg.obj = "已经连接上服务端！可以发送信息。";
                msg.what = 0;
                //LinkDetectedHandler.sendMessage(msg);
                //启动接受数据
            //    mreadThread = new readThread();
                orderController = new OrderController();
                new Thread(new SendThread()).start();
                //    mreadThread.start();
            }
            catch (IOException e)
            {
                Log.e("connect", "", e);
                Message msg = new Message();
                msg.obj = "连接服务端异常！断开连接重新试一试。";
                msg.what = 0;
                //LinkDetectedHandler.sendMessage(msg);
            }
        }
    };

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
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
    /* 停止服务器 */
    private void shutdownServer() {
        new Thread() {
            public void run() {
                if(startServerThread != null)
                {
                    startServerThread.interrupt();
                    startServerThread = null;
                }
                if(mreadThread != null)
                {
                    mreadThread.interrupt();
                    mreadThread = null;
                }
                try {
                    if(socket != null)
                    {
                        socket.close();
                        socket = null;
                    }
                    if (mserverSocket != null)
                    {
                        mserverSocket.close();/* 关闭服务器 */
                        mserverSocket = null;
                    }
                } catch (IOException e) {
                    Log.e("server", "mserverSocket.close()", e);
                }
            };
        }.start();
    }
    /* 停止客户端连接 */
    private void shutdownClient() {
        new Thread() {
            public void run() {
                if(clientConnectThread!=null)
                {
                    clientConnectThread.interrupt();
                    clientConnectThread= null;
                }
                if(mreadThread != null)
                {
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
            };
        }.start();
    }
    //发送数据
    private void sendMessageHandle(String msg)
    {
        if (socket == null)
        {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            System.out.print("连接成功！！！！！！");
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
                    if( (bytes = mmInStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (_opencvCameraView != null)
            _opencvCameraView.disableView();
        if (Bluetooth.serviceOrCilent == ServerOrCilent.CILENT)
        {
            shutdownClient();
        }
        else if (Bluetooth.serviceOrCilent == ServerOrCilent.SERVICE)
        {
            shutdownServer();
        }
        Bluetooth.isOpen = false;
        Bluetooth.serviceOrCilent = ServerOrCilent.NONE;
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