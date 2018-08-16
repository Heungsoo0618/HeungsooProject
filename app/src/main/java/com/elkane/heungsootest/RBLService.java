/*
 * ***************************************************************************************
 *  ** Hyundai Motor India Engineering Pvt Ltd
 *  ** Electronics Engineering Dept-1., - Software Development Team
 *  ** Do Not Copy Without Prior Permission
 *  *****************************************************************************************
 *  ** Project Name: ESDS Development
 *  ** Target: Proof Of Concept (NU Head Unit)
 *  ** File Name: RBLService.java
 *  ** @Author: Sivaram Boina
 *  ** @Co-Author: Sai Sriram Madhiraju
 *  ** Completion Date: 29-12-2017
 *  ***************************************************************************************
 */

package com.elkane.heungsootest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

//import hyundai.esds.fragments.VehicleStatusFragment;
//import static hyundai.esds.ControlPagerActivity.sControlPagerActivity;
//import static hyundai.esds.ResponseAlertClass.sResponseLayoutAlertDialog;
//import static hyundai.esds.fragments.VehicleControlFragment.clickedGridPosition;
//import static hyundai.esds.fragments.VehicleControlFragment.textString;
/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class RBLService extends Service {
	private final static String TAG = "aaaaa";

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;

	public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_GATT_RSSI = "ACTION_GATT_RSSI";
	public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "EXTRA_DATA";

	public final static UUID UUID_BLE_SHIELD_TX = UUID.fromString(RBLGattAttributes.BLE_SHIELD_TX);
	public final static UUID UUID_BLE_SHIELD_RX = UUID.fromString(RBLGattAttributes.BLE_SHIELD_RX);
	public final static UUID UUID_BLE_SHIELD_SERVICE = UUID.fromString(RBLGattAttributes.BLE_SHIELD_SERVICE);

	public static boolean automationSettingsEnabled=false;

    private boolean isConnected = false;

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
            Util.logcat("mGattCallback status : " + status);
            //
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_RSSI, rssi);
			} else {
				Log.w(TAG, "onReadRemoteRssi received: " + status);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Util.logcat("SERVICES DEISCOVERED STATUS: "+status);
            isConnected = true;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Util.logcat("SERVICES ARE: "+gatt.getServices().size());
				for(BluetoothGattService s:gatt.getServices())
				{
                    Util.logcat("UUID of Individual Services: "+s.getUuid().toString());
//					if(s.getUuid().toString().equals(RBLGattAttributes.BLE_SHIELD_SERVICE))
//					{
						for(BluetoothGattCharacteristic abc:s.getCharacteristics())
						{
                            Util.logcat("UUID of Individual Characteristics: "+abc.getUuid().toString()+" And properties: "+ Arrays.toString(abc.getValue()) + " , stringvalue : " +abc.getStringValue(0));


						}
//					}
				}
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
                Util.logcat("서비스의 onCharacteristicRead 호출됨");
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Util.logcat("서비스의 onCharacteristicChanged 호출됨");
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
	};
	public static long mDistMsgCounter=3;
	public static byte sEngineOn= (byte) 0xFF;
	public static byte sDoorOn= (byte) 0xFF;
	public static byte sDefrost= (byte) 0xFF;
	public static byte sClimateCtrl= (byte) 0xFF;
	public static byte sTempValue=(byte) 0xFF;
	public static int frontLeft=0xFF;
    public static int frontRight=0xFF;
    public static int rearLeft=0xFF;
    public static int rearRight=0xFF;
    public static int trunk=0xFF;




	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, int rssi) {
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
		sendBroadcast(intent);
	}

	int[] doorStatusArray;
	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
        Util.logcat("블루투스로 정보 받음. " + characteristic.getUuid() + "/" + characteristic.getStringValue(0) + "/" + new String(characteristic.getValue()));
		// This is special handling for the Heart Rate Measurement profile. Data
		// parsing is
		// carried out as per profile specifications:
		// http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
		if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid()))
		{
			final byte[] rx = characteristic.getValue();
            Util.heungsooShowDataLog("넘어온데이터",rx);
			intent.putExtra(EXTRA_DATA, rx);
			if(rx[1]==0x02&&rx[2]==0x01)
			{
                //아마도 자동차에 접근해서 접속성공하면 자동으로 자동차가 쏴주는 데이터에 자동화 세팅여부가 포함되어 있는것 같다..
				if(rx[3]==0x01||rx[3]==0x00){
                    //00 02 01 01
					automationSettingsEnabled=true;
					//HomeScreen.toasttobeDisplayed("Short/Medium");
				}
				else {
                    //00 02 01 02++(2보다 크다면)
					automationSettingsEnabled=false;
					//HomeScreen.toasttobeDisplayed("Long");
				}
//                Util.logcat("automation : " +automationSettingsEnabled);
                //setaction 대신 extras 값으로 구분하면 좋을듯. 중복호출되기 때문.
			    intent.setAction("automation");
				sendBroadcast(intent);
				mDistMsgCounter++;
			}

			//원래 handler 로 감쌌던 걸 밖으로 뺌. UI핸들링 뺐기 때문.
            {
                if(rx[1]==0)
                {
                    //아무동작도 안하는듯. 명령 수행 애크널러지도 안넣네..
                    intent.putExtra(BlueLinkBLEService.KEY_BLUELINK_OPERATION_ACK, BlueLinkBLEService.ACKNOWLEDGE_NONE);
                }
                else if(rx[1]==1)
                {
                    //HomeScreen.toasttobeDisplayed("Response: " + Arrays.toString(rx));
                    //조흥수 : 임시로 주석처리. UI핸들링은 서비스단에서 하지말자..
//						if(sResponseLayoutAlertDialog!=null) {
//							 if(sResponseLayoutAlertDialog.isShowing()){
//								sResponseLayoutAlertDialog.dismiss();
//							}
//						}
                    if(rx[3]==0)
                    {
                        //00 01 00 00
                        intent.putExtra(BlueLinkBLEService.KEY_BLUELINK_OPERATION_ACK, BlueLinkBLEService.ACKNOWLEDGE_FAIL);
//							ResponseAlertClass.showResponseDialog(sControlPagerActivity, getApplicationContext(), textString[clickedGridPosition].replace("\n", " ") + " request is failed", "Ok", false);
                    }
                    else
                    {
                        //00 01 00 01++
                        intent.putExtra(BlueLinkBLEService.KEY_BLUELINK_OPERATION_ACK, BlueLinkBLEService.ACKNOWLEDGE_SUCCESS);
//							ResponseAlertClass.showResponseDialog(sControlPagerActivity, getApplicationContext(), textString[clickedGridPosition].replace("\n", " ") + " request is success", "Ok", false);
                        if(rx.length>10)
                        {
                            //00 01 00 01 00 00 01 01 01 12 01 00 08
                            sClimateCtrl = rx[0x06];
                            sEngineOn = rx[0x07];
                            sDoorOn = rx[0x08];
                            sTempValue = rx[0x09];
                            sDefrost = rx[0x0A];
                            doorStatusArray=getBinary(rx[0x0C]);
                            //2진수 : 10110 : 트렁크 열림1, 앞왼쪽 닫힘0,앞오른쪽 열림1,뒤왼쪽 열림1,뒤오른쪽 닫힘0
                            trunk=doorStatusArray[3];
                            frontLeft=doorStatusArray[4];
                            frontRight=doorStatusArray[5];
                            rearLeft=doorStatusArray[6];
                            rearRight=doorStatusArray[7];

                            Log.d("Vehicle","Temp Value:"+rx[0x09]);
                            intent.putExtra(BlueLinkBLEService.KEY_VEHICLE_STATUS_UPDATE,true);
                        }
                    }
                }
            }
		}
        intent.setAction(action);
		sendBroadcast(intent);
	}

    private int[] getBinary(byte b) {
        int binaryArray[] = new int[8];
        Util.logcat("input : " + b);
        byte receivedByte = (byte) (b & 0xFF);
        Util.logcat(b  + "& 0xFF : " + receivedByte);
        //7 6 5 4 3 2 1 0 순으로 넣기
        for (int i = 7; i >= 0; i--) {
            binaryArray[i] = receivedByte & 1;
            Util.logcat("현재 receivedByte :  " + receivedByte + " , binaryArray["+i+"] (receivedByte & 1) :  "  + binaryArray[i]);
            receivedByte = (byte) (receivedByte >> 1);
        }
        return binaryArray;
    }

    public class LocalBinder extends Binder {
		public RBLService getService() {
			return RBLService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
        disconnect();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                close();
            }
        },1000);
//		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null	&& address.equals(mBluetoothDeviceAddress)&& mBluetoothGatt != null)
		{
			Log.d(TAG,
					"Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				return true;
			} else {
				return false;
			}

		}

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;

		return true;
	}
//    BluetoothDevice device=null;

    private void heungsooDisconnect()
    {
        if(isConnected)
            return;
        Util.logcat("타임아웃...");
        disconnect();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isConnected)
                    return;
                close();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        connect(mBluetoothDeviceAddress);
                    }
                },5000);

            }
        },2000);
    }

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Util.logcat("블루투스 disconnect : BluetoothAdapter not initialized");
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
        Util.logcat("블루투스 disconnect...");
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
            Util.logcat("블루투스 disconnect : mBluetoothGatt 가 널이다...예외상황");
			return;
		}
        Util.logcat("블루투스 disconnect : mBluetoothGatt close");
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readCharacteristic(characteristic);
		Util.logcat("SRIRAM REC CHAR VALUE: "+characteristic.getValue());
	}

	public void readRssi() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readRemoteRssi();
	}

	public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid()))
		{
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID
							.fromString(RBLGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Util.logcat("SRIRAM ENABLE ARRAY: "+ Arrays.toString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
			mBluetoothGatt.writeDescriptor(descriptor);
	    }
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public BluetoothGattService getSupportedGattService() {
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getService(UUID_BLE_SHIELD_SERVICE);
	}
}
