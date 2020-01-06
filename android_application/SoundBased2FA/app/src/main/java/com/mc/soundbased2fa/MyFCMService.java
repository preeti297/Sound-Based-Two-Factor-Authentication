package com.mc.soundbased2fa;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFCMService extends FirebaseMessagingService {
    private static final String TAG = MyFCMService.class.getSimpleName();
    public MyFCMService() {

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String value = data.get("record");
        Log.d(TAG, "onMessageReceived: " + value);
        Intent intent = new Intent(this, SoundRecordService.class);

        if("start_recording".equalsIgnoreCase(value)) {
            intent.setAction(SoundRecordService.ACTION_RECORD);
            startForegroundService(intent);
        }else if("send_file".equalsIgnoreCase(value)){
            intent.setAction(SoundRecordService.ACTION_SEND_FILE);
            startService(intent);
        }
    }

    @Override
    public void onNewToken(String s) {
        Log.d(TAG, "FCM Token " + s);
        super.onNewToken(s);
    }
}
