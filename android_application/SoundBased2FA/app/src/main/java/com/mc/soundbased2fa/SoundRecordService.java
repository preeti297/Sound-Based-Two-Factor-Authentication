package com.mc.soundbased2fa;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class SoundRecordService extends IntentService implements MediaRecorder.OnInfoListener{

    private static final String TAG = SoundRecordService.class.getSimpleName();

    public static final String ACTION_RECORD = "com.mc.soundbased2fa.action.start_recording";
    public static final String ACTION_SEND_FILE = "com.mc.soundbased2fa.action.send_file";

    private static final String CHANNEL_ID = "SOUND_2FA_FOREGROUND_CHANNEL_ID";
    private static final String SHARED_PREF_NAME = "com.mc.soundbased2fa.PREFERENCE_FILE_KEY";
    private static final String SHARED_PREF_AUDIO_FILE_KEY = "com.mc.soundbased2fa.AUDIO_FILE_KEY";

    private MediaRecorder recorder = null;
    private static String fileName = null;
//    private CountDownLatch signal = new CountDownLatch(1);

    public SoundRecordService() {
        super("HelloIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();
            if(ACTION_RECORD.equalsIgnoreCase(action)){
                createNotificationChannel();
                Intent notificationIntent = new Intent(SoundRecordService.this, MainActivity.class);
                PendingIntent pendingIntent =
                        PendingIntent.getActivity(SoundRecordService.this, 0, notificationIntent, 0);

                Notification notification =
                        new Notification.Builder(SoundRecordService.this, CHANNEL_ID)
                                .setContentTitle(getText(R.string.notification_title))
                                .setContentText(getText(R.string.notification_message))
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentIntent(pendingIntent)
                                .build();

                startForeground(12345, notification);
                startRecording();
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    // Restore interrupt status.
                    Thread.currentThread().interrupt();
                }
                recorder.stop();
                recorder.release();
            }else if(ACTION_SEND_FILE.equalsIgnoreCase(action)){
                sendFileToServer();
            }
        }
    }

    private void startRecording() {
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordmobile.mp4";

        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_AUDIO_FILE_KEY, fileName);
        editor.apply();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setMaxDuration(5000);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        recorder.setOnInfoListener(SoundRecordService.this);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        recorder.start();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            Log.d(TAG,"Maximum Duration Reached " + getMimeType(fileName));
            mr.stop();
            mr.release();
        }
    }

    private void sendFileToServer(){
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(30,
                TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).readTimeout(30,
                TimeUnit.SECONDS).build();

        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        fileName = sharedPref.getString(SHARED_PREF_AUDIO_FILE_KEY, "");

        Log.i(TAG, "Filename from sharedpref: " + fileName);
        File audioFile = new File(fileName);
        MultipartBody.Builder mMultipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(MediaType.parse(getMimeType(fileName)),
                        audioFile));

        RequestBody mRequestBody = mMultipartBody.build();
        Request request = new Request.Builder()
                .url("http://192.168.43.202:8080/perform2fa").post(mRequestBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseString = response.body().string();
            Log.i(TAG, "sendFileToServer: " + responseString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}
