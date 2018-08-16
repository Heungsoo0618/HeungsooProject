package com.elkane.heungsootest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

/**
 * Created by elkan on 2018-08-08.
 */

public class BluetoothTestActivity extends Activity {

    Context thisContext;
    TextView txt_BluetoothLog,txt_currentStatus,txt_receivedData;
    Button btn_connect,btn_lock,btn_unlock, btn_horn_light, btn_Light ,btn_vehicle_status, btn_engine_start ,btn_engine_stop,btn_clear,btn_notification;
    ScrollView scrl_bluetooth;
    CheckBox chk_autoConnect;
    public static boolean autoConnect;
    public static BluetoothTestActivity bluetoothTestActivity;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothTestActivity = this;
        thisContext = this;
        setContentView(R.layout.bluetooth_main);
        txt_BluetoothLog = findViewById(R.id.txt_BluetoothLog);
        registerReceiver(logTextReceiver, new IntentFilter("textLog"));
        registerReceiver(bluetoothServiceReceiver, BlueLinkBLEService.makeBluetoothServiceIntentFilter());
        startBluetoothService();
        initLayout();
        boolean isOnline = Util.isInternetOnline(thisContext);
        if(!isOnline)
        {
            Toast.makeText(thisContext,"인터넷 연결이 끊겨있습니다.",Toast.LENGTH_LONG).show();
            txt_currentStatus.setText("인터넷 연결이 끊겨있습니다.");
        }
        Util.logcat("크하하하하하하하하 내가 집에서 커밋함ㅇㅁㅇㅁㅇㅇㅇㅇ");
    }

    private void initLayout()
    {
        chk_autoConnect = findViewById(R.id.chk_autoConnect);
        chk_autoConnect.setChecked(autoConnect);
        chk_autoConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                autoConnect = isChecked;
            }
        });
        scrl_bluetooth = findViewById(R.id.scrl_bluetooth);
        txt_BluetoothLog = findViewById(R.id.txt_BluetoothLog);
        txt_currentStatus = findViewById(R.id.txt_currentStatus);
        txt_receivedData = findViewById(R.id.txt_receivedData);
        btn_connect = findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(btLisener);
        btn_lock = findViewById(R.id.btn_lock);
        btn_lock.setOnClickListener(btLisener);

        btn_unlock = findViewById(R.id.btn_unlock);
        btn_unlock.setOnClickListener(btLisener);

        btn_horn_light = findViewById(R.id.btn_horn_light);
        btn_horn_light.setOnClickListener(btLisener);

        btn_Light = findViewById(R.id.btn_Light);
        btn_Light.setOnClickListener(btLisener);

        btn_vehicle_status = findViewById(R.id.btn_vehicle_status);
        btn_vehicle_status.setOnClickListener(btLisener);

        btn_engine_start = findViewById(R.id.btn_engine_start);
        btn_engine_start.setOnClickListener(btLisener);

        btn_engine_stop = findViewById(R.id.btn_engine_stop);
        btn_engine_stop.setOnClickListener(btLisener);


        btn_clear = findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(btLisener);

        btn_notification = findViewById(R.id.btn_notification);
        btn_notification.setOnClickListener(btLisener);
    }

    public void addTextLog(final String msg)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String logMessage = Util.addTimeDate(null,0,0,null) + "//"+ msg;
                String log =  txt_BluetoothLog.getText().toString();
                txt_BluetoothLog.setText(log + "\n" +logMessage);
                scrl_bluetooth.post(new Runnable()
                {
                    public void run()
                    {
                        scrl_bluetooth.fullScroll(View.FOCUS_DOWN);
                    }
                });

            }
        });

    }
    private void logcat(String msg)
    {
        Util.logcat("BluetoothTestActivity/" + msg);
        addTextLog(msg);
    }

    private View.OnClickListener btLisener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.btn_connect:
                    stopService(new Intent(thisContext, BlueLinkBLEService.class));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startBluetoothService();
                        }
                    },10000);
                    break;
                case R.id.btn_lock:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_LOCK);
                    break;
                case R.id.btn_unlock:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_UNLOCK);
                    break;
                case R.id.btn_horn_light:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_HORN_LIGHT);
                    break;
                case R.id.btn_Light:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_LIGHT);
                    break;
                case R.id.btn_vehicle_status:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_VEHICLE_STATUS);
                    break;
                case R.id.btn_engine_start:
                    Bundle bundle = new Bundle();
                    bundle.putFloat(BlueLinkBLEService.KEY_BLUELINK_TEMPERATURE,17.0f);
                    bundle.putBoolean(BlueLinkBLEService.KEY_BLUELINK_DEFROST_ENABLED,true);
                    bundle.putInt(BlueLinkBLEService.KEY_BLUELINK_IDLE_TIME,2);
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_ENGINE_START,bundle);
                    break;
                case R.id.btn_engine_stop:
                    sendBlueLinkOperation(BlueLinkBLEService.BLUELINK_OPERATION_ENGINE_STOP);
                    break;
                case R.id.btn_clear:
                    txt_BluetoothLog.setText("");
                    break;
                case R.id.btn_notification:
                    Util.sendNotification(thisContext,"타이틀","내용 메세지 입니다." + Util.addTimeDate(null,0,0,null));
                    break;
            }
        }
    };

    private BroadcastReceiver logTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String textlog = intent.getStringExtra("textlog");
            if(TextUtils.isEmpty(textlog))
                return;
            String log =  txt_BluetoothLog.getText().toString();
            txt_BluetoothLog.setText(log + "\n" +textlog);
        }
    };

    private void startBluetoothService()
    {
        Intent bluetoothServiceIntent = new Intent(thisContext,BlueLinkBLEService.class);
        bluetoothServiceIntent.setAction(BlueLinkBLEService.STARTCOMMAND_SERVICE_START);
        startService(bluetoothServiceIntent);
    }
    private void sendBlueLinkOperation(int blueLinkOperation)
    {
        sendBlueLinkOperation(blueLinkOperation,null);
    }

    private void sendBlueLinkOperation(int blueLinkOperation,Bundle extras)
    {
        Intent bluetoothServiceOperationIntent = new Intent(thisContext,BlueLinkBLEService.class);
        bluetoothServiceOperationIntent.setAction(BlueLinkBLEService.STARTCOMMAND_OPERATION);
        Bundle bundle = new Bundle();
        bundle.putInt(BlueLinkBLEService.KEY_BLUELINK_OPERATION,blueLinkOperation);
        if(extras!=null)
            bundle.putAll(extras);
        bluetoothServiceOperationIntent.putExtras(bundle);
        startService(bluetoothServiceOperationIntent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothServiceReceiver);
        stopService(new Intent(this, BlueLinkBLEService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode== BlueLinkBLEService.REQUEST_ENABLE_BT&&resultCode==RESULT_OK)
        {
            startBluetoothService();
        }
    }

    private final BroadcastReceiver bluetoothServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            txt_currentStatus.setText(action);
            logcat(action);
            if(BlueLinkBLEService.BLUETOOTH_LE_NOT_SUPPORT.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.BLUETOOTH_LE_NOT_SUPPORT");
            }
            else if(BlueLinkBLEService.BLUETOOTH_OFF.equals(action))
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BlueLinkBLEService.REQUEST_ENABLE_BT);
            }
            else if(BlueLinkBLEService.RBLSERVICE_INITIALIZED_SUCCESS.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.RBLSERVICE_INITIALIZED_SUCCESS");
            }
            else if(BlueLinkBLEService.RBLSERVICE_INITIALIZED_FAIL.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.RBLSERVICE_INITIALIZED_FAIL");
            }
            else if(BlueLinkBLEService.RBLSERVICE_DISCONNECTED.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.RBLSERVICE_DISCONNECTED");

            }
            else if(BlueLinkBLEService.SELF_DESTROY_SERVICE_TIMEOUT.equals(action))
            {
                logcat("메인액티비티에서 서비스 타임아웃 종료 브로드캐스트 받음. 12초후 다시시작함.");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startBluetoothService();
                    }
                },12000);
            }
            else if(BlueLinkBLEService.ACTION_GATT_CONNECTED.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.ACTION_GATT_CONNECTED");
            }
            if (BlueLinkBLEService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                btn_connect.setEnabled(true);
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.ACTION_GATT_DISCONNECTED");
                stopService(new Intent(thisContext, BlueLinkBLEService.class));

            }
            else if (BlueLinkBLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.ACTION_GATT_SERVICES_DISCOVERED");
                txt_currentStatus.setText("연결되었습니다.");
                btn_connect.setEnabled(false);
            }
            else if (BlueLinkBLEService.ACTION_GATT_RSSI.equals(action))
            {
                txt_currentStatus.setText("연결되었습니다.");
//                logcat("메인에서 브로드캐스트 받음, BluetoothService.ACTION_GATT_RSSI");
            }
            else if(BlueLinkBLEService.ACTION_AUTOMATION.equals(action))
            {
                txt_currentStatus.setText("연결되었습니다.");
                Bundle bundle = intent.getExtras();
                byte[] rx = bundle.getByteArray(RBLService.EXTRA_DATA);
                logcat("automation 브로드캐스트 수신, automationSettingsEnabled : " +RBLService.automationSettingsEnabled);
                if(rx[1]==0x02&&rx[2]==0x01)
                {
                    if(rx[3]==0x01||rx[3]==0x00)
                    {

//                        automationSettingsEnabled=true;
                        //HomeScreen.toasttobeDisplayed("Short/Medium");
                    }
                    else
                    {
//                        automationSettingsEnabled=false;
                        //HomeScreen.toasttobeDisplayed("Long");
                    }
//                    mDistMsgCounter++;
                }
            }
            else if(BlueLinkBLEService.OPERATION_ACK_SUCCESS.equals(action))
            {
                String message = BlueLinkBLEService.operationNameMap.get(BlueLinkBLEService.currentOperation) + " 명령 전송 성공";
                txt_currentStatus.setText(message);
            }
            else if(BlueLinkBLEService.OPERATION_ACK_FAIL.equals(action))
            {
                String message = BlueLinkBLEService.operationNameMap.get(BlueLinkBLEService.currentOperation) + " 명령 전송 실패";
                txt_currentStatus.setText(message);
            }
            else if(BlueLinkBLEService.UPDATE_VEHICLE_STATUS.equals(action))
            {
                logcat("----------------차량 상태 업데이트----------------------------------");
                logcat("sClimateCtrl : " + (RBLService.sClimateCtrl==1?"ON":"OFF"));
                logcat("sEngineOn : " + (RBLService.sEngineOn==1?"ON":"OFF"));
                logcat("sDoorOn : " +  (RBLService.sDoorOn==1?"OPEN":"CLOSE"));
                logcat("sTempValue : " + RBLService.sTempValue);
                if(BlueLinkBLEService.mTempMap!=null)
                {
                    if((RBLService.sTempValue>=0x06)&&(RBLService.sTempValue<=0x1A))
                    {
                        logcat("설정온도 : " +String.valueOf(getKeyFromValue(BlueLinkBLEService.mTempMap, RBLService.sTempValue)) + " " + "\u00b0" + "C" );
                    }
                    else
                    {
                        logcat("설정온도 : " +String.valueOf(RBLService.sTempValue) );
                    }
                }
                logcat("sDefrost : "+ (RBLService.sDefrost==1?"ON":"OFF"));
                logcat("frontLeft : "+ (RBLService.frontLeft==1?"OPEN":"CLOSE"));
                logcat("frontRight : " + (RBLService.frontRight==1?"OPEN":"CLOSE"));
                logcat("rearLeft : " + (RBLService.rearLeft==1?"OPEN":"CLOSE"));
                logcat("rearRight : " + (RBLService.rearRight==1?"OPEN":"CLOSE"));
                logcat("trunk : " + (RBLService.trunk==1?"OPEN":"CLOSE"));
                logcat("----------------차량 상태 업데이트 끝----------------------------------");
            }
            else if (BlueLinkBLEService.ACTION_DATA_AVAILABLE.equals(action))
            {
                txt_currentStatus.setText("연결되었습니다.");
                Bundle bundle = intent.getExtras();
                final byte[] rx = bundle.getByteArray(RBLService.EXTRA_DATA);
//                logcat("ACTION_DATA_AVAILABLE 브로드캐스트 수신, rx : " + new String(rx));
                txt_receivedData.setText(new String(rx));
            }
        }
    };

    public static  Object getKeyFromValue(Map hm, byte value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

    private void updateVehicleStatus()
    {
        Util.logcat("updateVehicleStatus!!");
    }

}
