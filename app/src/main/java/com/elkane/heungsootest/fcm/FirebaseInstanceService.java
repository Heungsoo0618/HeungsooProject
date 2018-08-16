package com.elkane.heungsootest.fcm;

import com.elkane.heungsootest.Util;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by elkan on 2018-08-16.
 */

public class FirebaseInstanceService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        try
        {
            String token = FirebaseInstanceId.getInstance().getToken();
//            FirebaseMessaging.getInstance().subscribeToTopic("kfcinicis/topics/all");
            Util.logcat("FirebaseInstanceIDService onTokenRefresh , token : " + token);
//            saveToken(token);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }
}
