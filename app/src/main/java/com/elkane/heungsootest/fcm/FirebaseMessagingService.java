package com.elkane.heungsootest.fcm;

import com.elkane.heungsootest.Util;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by elkan on 2018-08-16.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Util.logcat("메세지 Title : " + remoteMessage.getData().get("title"));
        Util.logcat("메세지바디 : " + remoteMessage.getData().get("message"));
        Util.sendNotification(getApplicationContext(),remoteMessage.getData().get("title"),remoteMessage.getData().get("message"));
    }
}
