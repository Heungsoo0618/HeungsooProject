/*
 * ***************************************************************************************
 *  ** Hyundai Motor India Engineering Pvt Ltd
 *  ** Electronics Engineering Dept-1., - Software Development Team
 *  ** Do Not Copy Without Prior Permission
 *  *****************************************************************************************
 *  ** Project Name: ESDS Development
 *  ** Target: Proof Of Concept (NU Head Unit)
 *  ** File Name: RBLGattAttributes.java
 *  ** @Author: Sivaram Boina
 *  ** @Co-Author: Sai Sriram Madhiraju
 *  ** Completion Date: 29-12-2017
 *  ***************************************************************************************
 */

package com.elkane.heungsootest;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class RBLGattAttributes {
	private static HashMap<String, String> attributes = new HashMap<String, String>();
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
//	public static String BLE_SHIELD_TX = "713d0003-503e-4c75-ba94-3148f18d941e";
//	public static String BLE_SHIELD_TX = "00005502-d102-11e1-9b23-000240198212";


//	public static String BLE_SHIELD_RX = "00005501-d102-11e1-9b23-000240198212";
//	public static String BLE_SHIELD_RX = "713d0002-503e-4c75-ba94-3148f18d941e";
//	public static String BLE_SHIELD_SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e";
//public static String BLE_SHIELD_SERVICE = "00005500-d102-11e1-9b23-000240198212";


    public static String BLE_SHIELD_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String BLE_SHIELD_RX = "0000fff4-0000-1000-8000-00805f9b34fb";
    public static String BLE_SHIELD_TX = "0000fff4-0000-1000-8000-00805f9b34fb";


	static {
		// RBL Services.
		attributes.put("0000fff0-0000-1000-8000-00805f9b34fb","BLE Shield Service");
		// RBL Characteristics.
		attributes.put(BLE_SHIELD_TX, "BLE Shield TX");
		attributes.put(BLE_SHIELD_RX, "BLE Shield RX");
	}

	public static String lookup(String uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}
