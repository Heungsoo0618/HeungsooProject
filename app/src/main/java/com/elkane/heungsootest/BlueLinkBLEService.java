package com.elkane.heungsootest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.elkane.heungsootest.BluetoothTestActivity.autoConnect;
import static com.elkane.heungsootest.BluetoothTestActivity.bluetoothTestActivity;
import static com.elkane.heungsootest.RBLService.automationSettingsEnabled;
import static com.elkane.heungsootest.RBLService.mDistMsgCounter;



/**
 * Created by elkan on 2018-08-08.
 */

public class BlueLinkBLEService extends Service implements SensorEventListener, StepListener{

    public BluetoothAdapter mBluetoothAdapter;
    HashMap<String,String> deviceHashMap;
    private RBLService mBluetoothLeService;
    public static Map<Float,Byte> mTempMap=new HashMap<>();

    private SensorManager sensorManager;
    private Sensor accel;
    private StepDetector simpleStepDetector;
    public static List<BluetoothDevice> sDevices = new ArrayList<BluetoothDevice>();
    private boolean sDeviceFound=false;
    private boolean serviceConnected=false;
    public static String connectionStatusString=null;


    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private String mDeviceAddress;
    Intent gattServiceIntent;

    int mAutoCount=0;
    private long mUnlockCounter=0,mWelcomeLightCounter=0;
    private boolean mDistLockMsgSent=false;
    private int numSteps=0;

    private Timer checkConnectionTimer;
    private TimerTask checkConnectionTask;



    boolean isBindService;

    public final String BLUETOOTH_DEVICE_NAME = "LGIT";

    //서비스 커맨드
    public static final String STARTCOMMAND_SERVICE_START = "STARTCOMMAND_SERVICE_START"; //서비스 시작
    public static final String STARTCOMMAND_OPERATION = "STARTCOMMAND_OPERATION"; //서비스 조작


    //블루투스 관련 브로드캐스트
    public static final String BLUETOOTH_LE_NOT_SUPPORT = "BLUETOOTH_LE_NOT_SUPPORT"; //BLE 지원안함
    public static final String BLUETOOTH_OFF = "BLUETOOTH_OFF"; //폰의 블루투스 기능이 꺼져 있음
    public static final String RBLSERVICE_INITIALIZED_SUCCESS = "RBLSERVICE_INITIALIZED_SUCCESS"; //RBLService 초기화 성공
    public static final String RBLSERVICE_INITIALIZED_FAIL = "RBLSERVICE_INITIALIZED_FAIL"; //RBLService 초기화 실패
    public static final String RBLSERVICE_DISCONNECTED = "RBLSERVICE_DISCONNECTED"; //RBLService 접속종료
    public static final String SELF_DESTROY_SERVICE_TIMEOUT = "SELF_DESTROY_SERVICE_TIMEOUT"; //타임아웃되어 서비스 셀프 종료
    //RBLService 에서 넘어오는 브로드캐스트를 바이패싱해줌
    public final static String ACTION_GATT_CONNECTED = "BLUETOOTHSERVICE_ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "BLUETOOTHSERVICE_ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "BLUETOOTHSERVICE_ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_RSSI = "BLUETOOTHSERVICE_ACTION_GATT_RSSI";
    public final static String ACTION_DATA_AVAILABLE = "BLUETOOTHSERVICE_ACTION_DATA_AVAILABLE";
    public final static String ACTION_AUTOMATION = "BLUETOOTHSERVICE_automation";

    //명령 조작 관련 브로드캐스트
    public final static String OPERATION_ACK_SUCCESS = "OPERATION_ACK_SUCCESS";
    public final static String OPERATION_ACK_FAIL = "OPERATION_ACK_FAIL";
    public final static String UPDATE_VEHICLE_STATUS = "UPDATE_VEHICLE_STATUS";



    //블루투스 켜기 REQUEST CODE
    public static final int REQUEST_ENABLE_BT=10030;

    //블루링크 조작 명령 관련 상수 선언
    public static final int BLUELINK_OPERATION_LOCK = 1;
    public static final int BLUELINK_OPERATION_UNLOCK = 2;
    public static final int BLUELINK_OPERATION_HORN_LIGHT = 3;
    public static final int BLUELINK_OPERATION_LIGHT = 4;
    public static final int BLUELINK_OPERATION_VEHICLE_STATUS = 5;
    public static final int BLUELINK_OPERATION_ENGINE_START = 6;
    public static final int BLUELINK_OPERATION_ENGINE_STOP = 7;

    public static final String KEY_BLUELINK_OPERATION = "KEY_BLUELINK_OPERATION";
    public static final String KEY_BLUELINK_IDLE_TIME = "KEY_BLUELINK_IDLE_TIME";
    public static final String KEY_BLUELINK_TEMPERATURE = "KEY_BLUELINK_TEMPERATURE";
    public static final String KEY_BLUELINK_DEFROST_ENABLED = "KEY_BLUELINK_DEFROST_ENABLED";

    //ACTION_DATA_AVAILABLE 브로드캐스트 송신할 때 차량상태값 변경 플래그를 같이 넣어줄 때 이 키를 사용
    public static final String KEY_VEHICLE_STATUS_UPDATE = "KEY_VEHICLE_STATUS_UPDATE"; //차량 상태값 변경

    //블루링크 명령 호출에 대한 ack값을 같이 넣어준다.
    public static final String KEY_BLUELINK_OPERATION_ACK = "KEY_BLUELINK_OPERATION_ACK"; //명령 수행 결과
    //명령 수행결과 값 acknowledge
    public static final int ACKNOWLEDGE_NONE = 0;
    public static final int ACKNOWLEDGE_FAIL = 1;
    public static final int ACKNOWLEDGE_SUCCESS = 2;
    public static HashMap<Integer,String> operationNameMap = new HashMap<>();
    public static int currentOperation = 0;
    /**
     * 블루투스 연결 관련한 브로드캐스트 필터 생성
     * @return IntentFilter
     */
    public static IntentFilter makeBluetoothServiceIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLUETOOTH_LE_NOT_SUPPORT);
        intentFilter.addAction(BLUETOOTH_OFF);
        intentFilter.addAction(SELF_DESTROY_SERVICE_TIMEOUT);
        intentFilter.addAction(RBLSERVICE_INITIALIZED_SUCCESS);
        intentFilter.addAction(RBLSERVICE_INITIALIZED_FAIL);
        intentFilter.addAction(RBLSERVICE_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_GATT_RSSI);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ACTION_AUTOMATION);
        intentFilter.addAction(OPERATION_ACK_SUCCESS);
        intentFilter.addAction(OPERATION_ACK_FAIL);
        intentFilter.addAction(UPDATE_VEHICLE_STATUS);
        return intentFilter;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    private void initialize()
    {
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,accel, SensorManager.SENSOR_DELAY_FASTEST);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        deviceHashMap=new HashMap<>();
        load_mTempMapValues();
        load_OperationNameMap();
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            logcat("블루투스서비스 onStartCommand!!, action : " + action);
            //블루투스 서비스 시작
            if(STARTCOMMAND_SERVICE_START.equals(action))
            {
                //조흥수 : 현재 디바이스가 블루투스 로우 에너지를 지원하는지 확인한다.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    sendBroadcastBluetoothService(BLUETOOTH_LE_NOT_SUPPORT,null);
                    stopSelf();
                    return START_NOT_STICKY;
                }

                final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter == null) {
                    sendBroadcastBluetoothService(BLUETOOTH_LE_NOT_SUPPORT,null);
                    stopSelf();
                    return START_NOT_STICKY;
                }

                if (!mBluetoothAdapter.isEnabled())
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //2초 기다렸다가 다시 체크
                            if (!mBluetoothAdapter.isEnabled()) {
                                sendBroadcastBluetoothService(BLUETOOTH_OFF, null);
                                stopSelf();
                            }
                            else
                                scanLeDevice();
                        }
                    },2000);

                    return START_NOT_STICKY;
                }
                scanLeDevice();
            }
            //블루링크 조작 명령
            else if(STARTCOMMAND_OPERATION.equals(action))
            {
                invokeBlueLinkOperation(bundle);
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * 블루링크 엔진 조작 수행
     * @param bundle
     */
    private void invokeBlueLinkOperation(Bundle bundle)
    {
        if(bundle==null)
            return;
        int blueLinkOperation = bundle.getInt(KEY_BLUELINK_OPERATION);
        //현재 명령을 저장
        currentOperation = blueLinkOperation;
        final byte[] mSendMessage = new byte[20];
        for (int i = 0; i < mSendMessage.length; i++) {
            mSendMessage[i] = 0;
        }
        final BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);

        switch (blueLinkOperation)
        {
            case BLUELINK_OPERATION_LOCK:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x01;
                break;
            case BLUELINK_OPERATION_UNLOCK:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x00;
                break;
            case BLUELINK_OPERATION_HORN_LIGHT:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x04;
                break;
            case BLUELINK_OPERATION_LIGHT:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x05;
                break;
            case BLUELINK_OPERATION_VEHICLE_STATUS:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x07;
                break;
            case BLUELINK_OPERATION_ENGINE_START:
                float mTemperatureValue = bundle.getFloat(KEY_BLUELINK_TEMPERATURE);
                boolean mDefrostEnabled = bundle.getBoolean(KEY_BLUELINK_DEFROST_ENABLED);
                mSendMessage[2] = 0x07;
                mSendMessage[3] = 0x02;
                mSendMessage[4] = 0x01;

                mSendMessage[5] = (byte) (mDefrostEnabled?0x01:0x00);
                mSendMessage[6] = mTempMap.get(mTemperatureValue);
                mSendMessage[7] = 0x00;
                int mIdleTimeValue = bundle.getInt(KEY_BLUELINK_IDLE_TIME);
                mSendMessage[8] = (byte) mIdleTimeValue;
                if (mIdleTimeValue == 10) {
                    mSendMessage[8] = 0x0A;
                }
                mSendMessage[9] = 0x00;
                break;
            case BLUELINK_OPERATION_ENGINE_STOP:
                mSendMessage[2] = 0x01;
                mSendMessage[3] = 0x03;
                break;
            default:
                break;
        }
        if(serviceConnected)
        {
//            logcat("블루투스로 명령 전송, characteristic : " + characteristic);
            characteristic.setValue(mSendMessage);
            writeCharacteristic(characteristic);
        }
        else
            logcat("블루투스 연결이 되어있지않아 전송못함.");

    }

    private void writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mBluetoothLeService.writeCharacteristic(characteristic);
        //명령전송 후 30초 후 currentOperation 리셋
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    currentOperation = 0;
//                }
//            },30000);
    }


    /**
     * 블루투스 스캔 시작
     */
    private void scanLeDevice()
    {
        logcat("scan LE Device!");
        sDeviceFound=false;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                if(sDeviceFound)
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                else
//                    logcat("디바이스가 발견되지 않아 계속 찾기");


            }
        },2500);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord)
        {
            try
            {
                if (device != null)
                {
                    if (sDevices.indexOf(device) == -1)
                        sDevices.add(device);
                    deviceHashMap.put(device.getName(), device.getAddress());
                    //  Log.d(getClass().getSimpleName(),"Scan Record: "+Arrays.toString(scanRecord));
                    if(!TextUtils.isEmpty(device.getName()))
                        logcat(device.getName() + " , address : " + device.getAddress());
                    if (BLUETOOTH_DEVICE_NAME.equals(device.getName())&&!sDeviceFound)
                    {
                        sDeviceFound=true;
//                        logcat(BLUETOOTH_DEVICE_NAME + " 디바이스 발견, getName : " + device.getName() + " , address : " + device.getAddress());
//                        logcat("Rssi Value:  "+rssi);
                        startProcessing();
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
//                    else
//                    {
//                        sDeviceFound=false;
//                    }

                }
                else
                {
                    logcat("device is null");
                }
            }catch(Exception e)
            {
                Util.errorLogcat("블루투스 스캔 에러",e);
            }
        }
    };

    private void startProcessing() {
        mDeviceAddress=deviceHashMap.get(BLUETOOTH_DEVICE_NAME);
        gattServiceIntent = new Intent(this, RBLService.class);
        isBindService = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                logcat("Unable to initialize Bluetooth");
                sendBroadcastBluetoothService(RBLSERVICE_INITIALIZED_FAIL,null);
//                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            //생략..
//            sendBroadcastBluetoothService(RBLSERVICE_INITIALIZED_SUCCESS,null);
            if(mBluetoothLeService.connect(mDeviceAddress))
            {
                logcat("접속중.......(자동접속 : "+autoConnect+")");
                if(!autoConnect)
                    return;
                //타이머 시작함
                checkConnectionTimer = new Timer();
                checkConnectionTask = new TimerTask() {
                    @Override
                    public void run() {
                        if(!serviceConnected)
                        {
                            logcat("10초가 지나도 블루투스가 안붙어서 서비스 종료후 재시작함.");
                            stopSelf();
                            sendBroadcastBluetoothService(SELF_DESTROY_SERVICE_TIMEOUT,null);
                        }
                        else
                        {
                            this.cancel();
                            checkConnectionTimer.cancel();
                            checkConnectionTimer= null;
                        }
                    }
                };
                checkConnectionTimer.schedule(checkConnectionTask,10000);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            sendBroadcastBluetoothService(RBLSERVICE_DISCONNECTED,null);
            logcat("Service Disconnected");
        }
    };
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction("automation");
        return intentFilter;
    }
    //Initialing mGattUpdateReceiver for registering the changes of RBL service connection
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final Bundle bundle = intent.getExtras();
            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                serviceConnected = false;
                sendBroadcastBluetoothService(ACTION_GATT_DISCONNECTED,bundle);
                //연결되었다가 끊어진 경우 타임아웃의 경우처럼 재시작
                if(checkConnectionTimer==null&&autoConnect)
                {
                    logcat("연결되었다가 끊어진 경우 타임아웃의 경우처럼 재시작");
                    sendBroadcastBluetoothService(SELF_DESTROY_SERVICE_TIMEOUT,null);
                }
            } else if(RBLService.ACTION_GATT_CONNECTED.equals(action)){
                sendBroadcastBluetoothService(ACTION_GATT_CONNECTED,bundle);
            }
            else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattService(mBluetoothLeService.getSupportedGattService());
                sendBroadcastBluetoothService(ACTION_GATT_SERVICES_DISCOVERED,bundle);
                logcat("RBLService.ACTION_GATT_SERVICES_DISCOVERED");
                serviceConnected=true;
                connectionStatusString="blue";
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                sendBroadcastBluetoothService(ACTION_DATA_AVAILABLE,bundle);
                //RBLService 에서 ack 받았는지 여부를 확인해서 바로 팝업 때려버리고 차량상태 UI 업데이트 하고 하는데 이것을 주석처리하고 여기로 가져온다.
                //UI업데이트 하는 부분만 여기로 가져온다. 변수는 RBLService의 것을 사용함.
                operationForActionDataAvailable(bundle);
            }
            else if("automation".equals(action))
            {
                logcat("블루투스서비스에서 automation 브로드캐스트함.");
                sendBroadcastBluetoothService(ACTION_AUTOMATION,bundle);
                operationForAutomation();
            }
        }
    };

    private void operationForActionDataAvailable(Bundle bundle)
    {
        final boolean isVehicleStatusUpdate = bundle.getBoolean(KEY_VEHICLE_STATUS_UPDATE,false);
        final int acknowledge = bundle.getInt(KEY_BLUELINK_OPERATION_ACK,ACKNOWLEDGE_NONE);
        if(isVehicleStatusUpdate)
            sendBroadcastBluetoothService(UPDATE_VEHICLE_STATUS,null);
        switch (acknowledge)
        {
            case ACKNOWLEDGE_FAIL:
                sendBroadcastBluetoothService(OPERATION_ACK_FAIL,null);
                break;
            case ACKNOWLEDGE_SUCCESS:
                sendBroadcastBluetoothService(OPERATION_ACK_SUCCESS,null);
                break;
        }

    }



    public boolean autoDoorUnlockEnabled = true;
    public boolean welcomeLightEnabled = true;
    public boolean autodoorLockEnabled = true;
    int clickedGridPosition;
    private void operationForAutomation()
    {
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);
                byte[] autoSendEngineMsg = {0x00, 0x00, 0x07, 0x02, 0x02, 0x01, 0x0A, 0x00, 0x0A, 0x00};
                byte[] autoDoorUnlockMsg = {0x00, 0x00, 0x01, 0x00};
                byte[] autoDoorLockMsg = {0x00, 0x00, 0x01, 0x01};
                byte[] welcomeLightMsg = {0x00, 0x00, 0x01, 0x06};
                //이 로직을 수행하기에 앞서 RBLService 에서 브로드캐스트 때리기 직전 mDistMsgCounter++; 수행한다. 초기값은 3 그러므로 현재 4
                //각 동작별 변수에 이 값을 바인딩 하고 mDistMsgCounter 와의 차이가 3이상일때만 동작하도록 하였네..
                //각 동작 수행 후 오토메이션 브로드캐스트가 3회 이상 더 들어와야 동작하도록 되어있다. 자동차가 여러번 쏴주나? 첫 1회 실행 후 3회 automation 호출해도 무시, 그다음 동작.
                //자동차의 오토메이션 세팅이 켜져있을시 //00 02 01 01 , 끈상태로 설정을 원한다면 00 02 01 02++(2이상)
                logcat("automationSettingsEnabled : " + automationSettingsEnabled + " , numSteps : " + numSteps + " , mDistMsgCounter : " + mDistMsgCounter +  " , autoDoorUnlockEnabled : " + autoDoorUnlockEnabled);
                if (automationSettingsEnabled)
                {
                    mAutoCount++; //0->1 된다.
                    mDistLockMsgSent = false;
                    logcat("mAutoCount : " + mAutoCount + " , mUnlockCounter : " + mUnlockCounter);
                    if (mAutoCount == 1) //첫1회만 수행될 수밖에 없네..0으로 만드는건 자동차에서 automationSettingsEnabled 가 false로 떨어질 때 뿐이다.
                    {

                        //앱 키고 차에 접근해서 붙은뒤 오토메이션 설정이 되어있는 차일경우 처음 실행시 무조건 진입
                        if ((mDistMsgCounter - mUnlockCounter > 3) //4-0 이므로 3보다 크다.
                                //팝업다이얼로그가 꺼져 있을경우...근데 이걸왜 체크함? 일단주석처리
                                //블루링크 명령 수행후 콜백이 오면 이 다이얼로그가 켜지니 그걸 체크한듯 싶다. 다이얼로그 꺼져있을 때 수행
//                                && (ResponseAlertClass.sResponseLayoutAlertDialog != null) && (!ResponseAlertClass.sResponseLayoutAlertDialog.isShowing())
                                && numSteps > 2) //두걸음 이상걸었을때?
                        {
                            //자동 문 열기가 활성화되어있다면
                            if (autoDoorUnlockEnabled)
                            {
                                //자동으로 문 열음
                                characteristic.setValue(autoDoorUnlockMsg);
                                logcat("Automation : 자동으로 문 열음" );
                                writeCharacteristic(characteristic);
                                mUnlockCounter = mDistMsgCounter; //언락 카운터는 4로 바인딩
                                currentOperation = BLUELINK_OPERATION_UNLOCK;
                                clickedGridPosition = 8;
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (!welcomeLightEnabled) {
                                    //웰컴라이트 기능이 꺼져있다면 스텝은 0
                                    //근데 이거 필요음슴 어차피 welcomeLightEnabled 밑에서 체크하는디?
                                    numSteps = 0;
                                }
                            }
                        }
                        //웰컴라이트 기능이 꺼져있다면 이 if문에 들어올 조건 numSteps > 2 가 못되므로 패스된다.
                        if ((mDistMsgCounter - mWelcomeLightCounter > 3 && numSteps > 2)) //4-0이므로 3보다 크다. 진입
                        {
                            if (welcomeLightEnabled)
                            {
                                //웰컴 라이트 킴
                                logcat("Automation : 웰컴 라이트 킴 " + ResponseAlertClass.sResponseLayoutAlertDialog );
                                characteristic.setValue(welcomeLightMsg);
                                writeCharacteristic(characteristic);
                                numSteps = 0; //걸음 0이 된다.
                                mWelcomeLightCounter = mDistMsgCounter; //웰컴라이트 카운터는 4가 된다.
                                currentOperation = BLUELINK_OPERATION_LIGHT;
                                clickedGridPosition = 7;
                            }
                            //기존로직 주석처리
//                            if (ResponseAlertClass.sResponseLayoutAlertDialog == null)
//                            {
//                                if (welcomeLightEnabled) {
//                                    //웰컴 라이트 킴
//                                    logcat("Automation : 웰컴 라이트 킴 " + ResponseAlertClass.sResponseLayoutAlertDialog );
//                                    characteristic.setValue(welcomeLightMsg);
//                                    mBluetoothLeService.writeCharacteristic(characteristic);
//                                    numSteps = 0;
//                                    mWelcomeLightCounter = mDistMsgCounter;
//                                    clickedGridPosition = 7;
//                                }
//                            } else if (!ResponseAlertClass.sResponseLayoutAlertDialog.isShowing()) {
//                                if (welcomeLightEnabled) {
//                                    characteristic.setValue(welcomeLightMsg);
//                                    logcat("Automation : 웰컴 라이트 킴 " + ResponseAlertClass.sResponseLayoutAlertDialog );
//                                    mBluetoothLeService.writeCharacteristic(characteristic);
//                                    numSteps = 0;
//                                    mWelcomeLightCounter = mDistMsgCounter;
//                                    clickedGridPosition = 7;
//                                }
//                            }
                        }
                    }
                }
                //자동차의 오토메이션 세팅값이 OFF라고 차가 알려줬을 경우 && 2보 이상 움직였을 경우
                else
                {
                    mAutoCount = 0;
                    //mDistLockMsgSent 은 초기값이 false이므로 무조건 진입하며 2보이상 움직였을 경우 동작
                    //mDistLockMsgSent 은 서비스 실행후 첫 1회만 수행하라는 스위칭 플래그이다.
                    if ((!mDistLockMsgSent)
//                            && (ResponseAlertClass.sResponseLayoutAlertDialog != null) && (!ResponseAlertClass.sResponseLayoutAlertDialog.isShowing())
                            && numSteps > 2)
                    {
                        //자동 문잠금이 세팅되어 있다면
                        if (autodoorLockEnabled)
                        {
                            //자동으로 문 잠그도록함
                            characteristic.setValue(autoDoorLockMsg);
                            logcat("Automation : 자동으로 문 잠그도록함 ");
                            writeCharacteristic(characteristic);
                            numSteps = 0;
                            currentOperation = BLUELINK_OPERATION_LOCK;
                            clickedGridPosition = 6;
                        }
                        mDistLockMsgSent = true;
                    }
                }

            }

        }, 1500);
    }

    private void getGattService(BluetoothGattService gattService)
    {
        if (gattService == null)
            return;
        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
        map.put(characteristic.getUuid(), characteristic);
        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Util.logcat("블루투스서비스 onDestroy!!");
        if(mServiceConnection!=null&&isBindService)
            unbindService(mServiceConnection);
        if(checkConnectionTimer!=null) {
            checkConnectionTimer.cancel();
            checkConnectionTimer = null;
        }
        if(checkConnectionTask!=null)
        {
            checkConnectionTask.cancel();
            checkConnectionTask = null;
        }
    }
    private void logcat(String msg)
    {
        Util.logcat("service/" + msg);
        bluetoothTestActivity.addTextLog("service/" + msg);
    }

    public void sendBroadcastBluetoothService(String action,Bundle bundle)
    {
        Intent intent = new Intent(action);
        if(bundle!=null) {
            intent.putExtras(bundle);
        }
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void load_OperationNameMap()
    {
        operationNameMap = new HashMap<>();
        operationNameMap.put(0,"NONE");
        operationNameMap.put(BLUELINK_OPERATION_LOCK,"문잠금");
        operationNameMap.put(BLUELINK_OPERATION_UNLOCK,"문열기");
        operationNameMap.put(BLUELINK_OPERATION_HORN_LIGHT,"혼 라이트");
        operationNameMap.put(BLUELINK_OPERATION_LIGHT,"라이트 켜기");
        operationNameMap.put(BLUELINK_OPERATION_VEHICLE_STATUS,"차량상태보기");
        operationNameMap.put(BLUELINK_OPERATION_ENGINE_START,"시동걸기");
        operationNameMap.put(BLUELINK_OPERATION_ENGINE_STOP,"시동끄기");
    }


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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void step(long timeNs) {
        numSteps++;
    }
}
