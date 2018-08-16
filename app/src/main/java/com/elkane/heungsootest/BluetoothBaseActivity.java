package com.elkane.heungsootest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.elkane.heungsootest.RBLService.automationSettingsEnabled;
import static com.elkane.heungsootest.RBLService.mDistMsgCounter;
import static com.elkane.heungsootest.ResponseAlertClass.sResponseLayoutAlertDialog;

/**
 * Created by elkan on 2018-08-07.
 */

public class BluetoothBaseActivity extends Activity implements SensorEventListener, StepListener
{
    //Declaring memeber fields
    private int numOfPages=6;
    private final static String TAG = BluetoothBaseActivity.class.getSimpleName();
    private RBLService mBluetoothLeService;
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();
    private String mDeviceAddress;
    static int mAutoCount=0;
    public static Map<Float,Byte> mTempMap=new HashMap<>();
    Intent gattServiceIntent;

    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner heungsooLEScanner;
    public static final int REQUEST_ENABLE_BT=1004;
    public Dialog mDialog;
    public static final long SCAN_PERIOD = 2000;
    public static List<BluetoothDevice> sDevices = new ArrayList<BluetoothDevice>();
    HashMap<String,String> deviceHashMap;


    private static boolean sDeviceFound=false;
    public static boolean serviceConnected=false;
    public static String connectionStatusString=null;

    public static float mTemperatureValue=(float)17.0;
    public static boolean mDefrostEnabled=false;

    private SensorManager sensorManager;
    private Sensor accel;
    private StepDetector simpleStepDetector;
    private int numSteps=0;

    //조흥수 : 샘플에서는 각각의 fragment에서 세팅된 값이었음.
    public String instruction="";
    public int mIdleTimeValue=2;
    public boolean autoEngineStartEnabled,autoDoorUnlockEnabled,welcomeLightEnabled,autodoorLockEnabled;

    private static BluetoothBaseActivity thisInstance;
    public static boolean isBluetoothBaseActivityActive;

    public final String BLUETOOTH_DEVICE_NAME = "LGIT";
//    public final String BLUETOOTH_DEVICE_NAME = "heungsoo";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
//        registerReceiver(heungsooReceiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //Initializing member fields
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        //intializing bluetooth parameters
        //조흥수 : 현재 디바이스가 블루투스 로우 에너지를 지원하는지 확인한다.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("Ble not supported");
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP)
            heungsooLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothAdapter == null) {
            toast("Ble not supported");
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            logcat("블루투스가 꺼져있음");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        deviceHashMap=new HashMap<String,String>();
        scanLeDevice();
//        showRoundProcessDialog(ControlPagerActivity.this, R.layout.loading_process_dialog_anim);

        load_mTempMapValues();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT&&resultCode==RESULT_OK)
        {
            scanLeDevice();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        thisInstance = this;
        isBluetoothBaseActivityActive = true;
    }
    public static BluetoothBaseActivity getInstance()
    {
        return thisInstance;
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        mDefrostEnabled=getSharedPreferences("Automation Settings", Context.MODE_PRIVATE).getBoolean("Defrost",false);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        registerReceiver(broadcast_reciever, new IntentFilter("finish_activity"));
        registerReceiver(broadcast_reciever, new IntentFilter("automation"));
        sensorManager.registerListener(this,accel, SensorManager.SENSOR_DELAY_FASTEST);


//        mScanRequestButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(mScanRequestBleDeviceLayout.getVisibility()==View.VISIBLE)
//                {
//                    showLayout(false);
//                }
//                if(mBluetoothAdapter!=null && mLeScanCallback!=null) {
//                    scanLeDevice();
////                    showRoundProcessDialog(ControlPagerActivity.this, R.layout.loading_process_dialog_anim);
//                }
//            }
//        });

        //attempts for connecting the BLE unit(Here: LGIT)
        if(mBluetoothLeService!=null) {
            logcat("onResume, mBluetoothLeService.connect(mDeviceAddress)");
            mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isBluetoothBaseActivityActive = false;

//        unregisterReceiver(mGattUpdateReceiver);
//        unregisterReceiver(broadcast_reciever);
        sDeviceFound=false;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if(sResponseLayoutAlertDialog!=null && sResponseLayoutAlertDialog.isShowing())
        {
            sResponseLayoutAlertDialog.dismiss();
        }
    }

    public void updateVehicleStatus()
    {
        //서비스에서 호출했는데 이것들을 브로드캐스팅으로 획일화 시키면 될것 같음..
        logcat("서비스에서 차량 상태값 업데이트 호출함...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(mBluetoothLeService!=null) {
//
//            mBluetoothLeService.disconnect();
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mBluetoothLeService.close();
//                }
//            },1000);
//        }
//        unregisterReceiver(heungsooReceiver);
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(broadcast_reciever);
        serviceConnected=false;
        connectionStatusString=null;
    }

    //mLeScanCallBack method for searching the BLE device(here LGIT)
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//
//                        }
//                    });
//                }
//            }).start();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (device != null) {
                            if (sDevices.indexOf(device) == -1)
                                sDevices.add(device);
                            deviceHashMap.put(device.getName(), device.getAddress());
                            //  Log.d(getClass().getSimpleName(),"Scan Record: "+Arrays.toString(scanRecord));
                            if(!TextUtils.isEmpty(device.getName()))
                                logcat(device.getName() + " , address : " + device.getAddress());
                            if (BLUETOOTH_DEVICE_NAME.equals(device.getName())) {
                                sDeviceFound=true;
                                logcat(BLUETOOTH_DEVICE_NAME + " 발견, getName : " + device.getName() + " , address : " + device.getAddress());
//                                ((TextView)findViewById(R.id.txt_step)).setText(device.getName() + " : " + device.getAddress());
                                logcat("Rssi Value:  "+rssi);
                                startProcessing();
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                if (mDialog != null && mDialog.isShowing()) {
                                    mDialog.dismiss();
                                }

//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                startProcessing();
//                            }
//                        },2500);


                            } else {
                                sDeviceFound=false;
                            }

                        }else{
                            logcat("device is null");
                        }
                    }catch(Exception e)
                    {
                        errorLogcat("블루투스 스캔 에러",e);
                    }
                }
            });




        }
    };

    private void startProcessing() {
        mDeviceAddress=deviceHashMap.get(BLUETOOTH_DEVICE_NAME);
        gattServiceIntent = new Intent(this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private BroadcastReceiver heungsooReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
//                Utils.heungsooShowDataLog("블루투스 발견함",intent);
                BluetoothDevice searchedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                logcat("발견한 블루투스 : " + searchedDevice.getName() + "/"+ searchedDevice.getAddress());
                if(mDialog!=null)
                    mDialog.dismiss();
            }
        }
    };

    /**
     * method for intiating search for BLE device(Here: LGIT)
     * After, 2 Second and 500 milliseconds , it will automatically stop searching for the BLE device(Reason: Scan method consumes lot of mobile battery)
     * After that, user can again initiate by clicking try again button
     *
     */
    private void scanLeDevice()
    {
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//        for(BluetoothDevice device : pairedDevices){
//            Utils.logcat("페어링된 디바이스 name : " + device.getName() + "/" + device.getAddress());
//        }
//        mBluetoothAdapter.startDiscovery();

//        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP)
//            heungsooLEScanner.startScan()
        logcat("scan LE Device!");
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                if(mDialog!=null)
                    mDialog.dismiss();
                if(sDeviceFound)
                {
                    //show layout
                    showLayout(false);

                }else{
                    //hide layout
                    showLayout(true);
                }
            }
        },2500);
    }

    //Method to display bottom request layout
    private void showLayout(boolean check) {
        if(check && !serviceConnected) {
//            mScanRequestBleDeviceLayout.startAnimation(slide_up);
//            mScanRequestBleDeviceLayout.setVisibility(View.VISIBLE);
        }else{
//            mScanRequestBleDeviceLayout.setVisibility(View.INVISIBLE);
        }
    }

    //Intilaizing Service Connection for Connecting to BLE unit
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            logcat("블루투스 이니셜라이즹");
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            toast("Service Disconnected");
            logcat("Service Disconnected");


        }
    };


    //Initialing mGattUpdateReceiver for registering the changes of RBL service connection
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                toast("GATT Service Disconnected");
                serviceConnected=false;
                connectionStatusString="red";
                mBluetoothLeService.close();
                mBluetoothLeService.connect(mDeviceAddress);
//                mBluetoothLeService.disconnect();
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mBluetoothLeService.close();
////                        mBluetoothLeService.connect(mDeviceAddress);
////                        logcat("연결 재시도...");
//                    }
//                },2000);

            } else if(RBLService.ACTION_GATT_CONNECTED.equals(action)){
                toast("GATT Service Connected");
                serviceConnected=false;
                connectionStatusString="orange";
                logcat("RBLService.ACTION_GATT_CONNECTED");
            }
            else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattService(mBluetoothLeService.getSupportedGattService());
                logcat("RBLService.ACTION_GATT_SERVICES_DISCOVERED");
                //hide the scan request layout if it is visible even after service is discovered
//                if(mScanRequestBleDeviceLayout!=null && mScanRequestBleDeviceLayout.getVisibility()==View.VISIBLE)
//                {
//                    mScanRequestBleDeviceLayout.setVisibility(View.INVISIBLE);
//                }
                serviceConnected=true;
                connectionStatusString="blue";
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                //do nothing
            }
        }
    };

    private long mUnlockCounter=0,mWelcomeLightCounter=0;
    private boolean mDistLockMsgSent=false;

    //Initializing broadcast receiver for registering the click of all control buttons form variious screen with action("finish_activity","automation")
    BroadcastReceiver broadcast_reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            try {
                String action = intent.getAction();
                final byte[] mSendMessage = new byte[20];
                for (int i = 0; i < mSendMessage.length; i++) {
                    mSendMessage[i] = 0;
                }

                final BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);

                if (action.equals("finish_activity")) {
                    // DO WHATEVER YOU WANT.
                    String trueorfalse = intent.getExtras().getString("resultfrombraodcast");
                    if (trueorfalse.equals("true"))
                    {
                        //조흥수: 핀번호 통과하여 실제 작업 수행
                        if (instruction.equals("Lock"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x01;
                        }
                        else if (instruction.equals("Unlock"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x00;
                        }
                        else if (instruction.equals("Horn \nLight"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x04;
                        }
                        else if (instruction.equals("Light"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x05;
                        }
                        else if (instruction.equals("Vehicle \nStatus"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x07;
                        }
                        else if (instruction.equals("Engine \nStart"))
                        {
                            mSendMessage[2] = 0x07;
                            mSendMessage[3] = 0x02;
                            mSendMessage[4] = 0x01;

                            mSendMessage[5] = (byte) (mDefrostEnabled?0x01:0x00);
                            mSendMessage[6] = mTempMap.get(mTemperatureValue);
                            mSendMessage[7] = 0x00;
                            mSendMessage[8] = (byte) mIdleTimeValue;
                            if (mIdleTimeValue == 10) {
                                mSendMessage[8] = 0x0A;
                            }

                            mSendMessage[9] = 0x00;
                        }
                        else if (instruction.equals("Engine \nStop"))
                        {
                            mSendMessage[2] = 0x01;
                            mSendMessage[3] = 0x03;
                        }
                        else
                        {
                            //do nothing
                        }


                        //reverseByte(mSendMessage);
                        if (serviceConnected)
                        {
                            //블루투스로 명령 전송
                            logcat("블루투스로 명령 전송, characteristic : " + characteristic);
                            characteristic.setValue(mSendMessage);
                            mBluetoothLeService.writeCharacteristic(characteristic);
                            toast(instruction.replace("\n", " ") + " request sent");
                        } else {
                            System.out.println("Not connected ");
                        }

                    } else {
                        //do nothing
                    }

                }

                if (action.equals("automation"))
                {

                    Handler handler = new Handler();
//                if(mAutoCount<1) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);
                            byte[] autoSendEngineMsg = {0x00, 0x00, 0x07, 0x02, 0x02, 0x01, 0x0A, 0x00, 0x0A, 0x00};
                            byte[] autoDoorUnlockMsg = {0x00, 0x00, 0x01, 0x00};
                            byte[] autoDoorLockMsg = {0x00, 0x00, 0x01, 0x01};
                            byte[] welcomeLightMsg = {0x00, 0x00, 0x01, 0x06};
                            if (automationSettingsEnabled)
                            {
                                mAutoCount++;
                                mDistLockMsgSent = false;
                                if (mAutoCount == 1) {


/*                                if (autoEngineStartEnabled) {
                                    characteristic.setValue(autoSendEngineMsg);
                                    mBluetoothLeService.writeCharacteristic(characteristic);
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }*/

                                    if ((mDistMsgCounter - mUnlockCounter > 3)
                                            && (sResponseLayoutAlertDialog != null) && (!sResponseLayoutAlertDialog.isShowing())
                                            && numSteps > 2) {
                                        if (autoDoorUnlockEnabled) {
                                            characteristic.setValue(autoDoorUnlockMsg);
                                            mBluetoothLeService.writeCharacteristic(characteristic);
                                            mUnlockCounter = mDistMsgCounter;
//                                            clickedGridPosition = 8;
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            if (!welcomeLightEnabled) {
                                                numSteps = 0;
                                            }
                                        }
                                    }
                                    if ((mDistMsgCounter - mWelcomeLightCounter > 3 && numSteps > 2)) {
                                        if (sResponseLayoutAlertDialog == null)
                                        {
                                            if (welcomeLightEnabled) {
                                                characteristic.setValue(welcomeLightMsg);
                                                mBluetoothLeService.writeCharacteristic(characteristic);
                                                numSteps = 0;
                                                mWelcomeLightCounter = mDistMsgCounter;
//                                                clickedGridPosition = 7;
                                            }
                                        }
//                                        else if (!sResponseLayoutAlertDialog.isShowing())
                                        {
//                                            if (welcomeLightEnabled)
//                                            {
//                                                characteristic.setValue(welcomeLightMsg);
//                                                mBluetoothLeService.writeCharacteristic(characteristic);
//                                                numSteps = 0;
//                                                mWelcomeLightCounter = mDistMsgCounter;
//                                                clickedGridPosition = 7;
//                                            }
                                        }
                                    }
                                }
                            } else {
                                mAutoCount = 0;

                                if ((!mDistLockMsgSent)
                                        && (sResponseLayoutAlertDialog != null) && (!sResponseLayoutAlertDialog.isShowing())
                                        && numSteps > 2) {
                                    if (autodoorLockEnabled)
                                    {
                                        characteristic.setValue(autoDoorLockMsg);
                                        mBluetoothLeService.writeCharacteristic(characteristic);
                                        numSteps = 0;
//                                        clickedGridPosition = 6;
                                    }
                                    mDistLockMsgSent = true;
                                }
                            }

                        }

                    }, 1500);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    };




    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     */
    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
        map.put(characteristic.getUuid(), characteristic);

        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }


    //Adding actions to the mGattUpdateReceiver
    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);

        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }


    //Showing round progress bar to wait for the nearest ble devices to be detected
    public void showRoundProcessDialog(Context mContext, int layout) {
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_HOME
                        || keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true;
                }
                return false;
            }
        };

        mDialog = new AlertDialog.Builder(mContext).create();
        mDialog.setOnKeyListener(keyListener);
        mDialog.show();
        // 娉ㄦ��姝ゅ��瑕���惧��show涔���� ������浼���ュ��甯�
        mDialog.setContentView(layout);
    }

    //loading mTempMapValues Hashmap
    public void load_mTempMapValues() {
        mTempMap.put((float)17.0,(byte) 0x06);
        mTempMap.put((float)17.5,(byte) 0x07);
        mTempMap.put((float)18.0,(byte) 0x08);
        mTempMap.put((float)18.5,(byte) 0x09);
        mTempMap.put((float)19.0,(byte) 0x0A);
        mTempMap.put((float)19.5,(byte) 0x0B);
        mTempMap.put((float)20.0,(byte) 0x0C);
        mTempMap.put((float)20.5,(byte) 0x0D);
        mTempMap.put((float)21.0,(byte) 0x0E);
        mTempMap.put((float)21.5,(byte) 0x0F);
        mTempMap.put((float)22.0,(byte) 0x10);
        mTempMap.put((float)22.5,(byte) 0x11);
        mTempMap.put((float)23.0,(byte) 0x12);
        mTempMap.put((float)23.5,(byte) 0x13);
        mTempMap.put((float)24.0,(byte) 0x14);
        mTempMap.put((float)24.5,(byte) 0x15);
        mTempMap.put((float)25.0,(byte) 0x16);
        mTempMap.put((float)25.5,(byte) 0x17);
        mTempMap.put((float)26.0,(byte) 0x18);
        mTempMap.put((float)26.5,(byte) 0x19);
        mTempMap.put((float)27.0,(byte) 0x1A);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    @Override
    public void step(long timeNs) {
        numSteps++;
//        ((TextView)findViewById(R.id.txt_step)).setText("만보기 : " + numSteps);
    }

    public void refreshBlueTooth(View view)
    {
        scanLeDevice();
    }

    public void toast(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    public void logcat(String msg)
    {
        Log.d("heungsoo",msg);
    }

    public void errorLogcat(String msg,Exception e)
    {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        logcat( msg +"\n"+writer.toString());
    }
}
