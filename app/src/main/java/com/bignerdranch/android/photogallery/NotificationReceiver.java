package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Received result " + intent.getAction());
        if(getResultCode() != Activity.RESULT_OK){
            return;
        }
        int requestCode = intent.getIntExtra(PollService.REQUEST_CODE,0);
        Notification notification = (Notification) intent.getParcelableExtra(PollService.NOTIFICATION);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

        managerCompat.notify(requestCode,notification);

    }
}
