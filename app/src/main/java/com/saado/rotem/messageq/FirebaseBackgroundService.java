package com.saado.rotem.messageq;

import android.*;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FirebaseBackgroundService extends Service implements LocationListener {

    private DatabaseReference mChatDatabaseReference = FirebaseDatabase.getInstance()
            .getReference().child("chats");
    private ChildEventListener mPostListener;
    private String mCurrentUserId;
    private List<SingleMessage> mTimeMessageList;
    private List<SingleMessage> mLocationMessageList;
    private LocationManager mLocationManager;
    private Location mLocation;

    //private constants for location manager params
    private static final int MIN_TIME_FOR_UPDATE = 1024;
    private static final int MIN_DIS_FOR_UPDATE = 1;
    private static final int DISTANCE = 80;

    public FirebaseBackgroundService()
    {
        mTimeMessageList = new ArrayList<>();
        mLocationMessageList = new ArrayList<>();
        mLocation = null;
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.d("rotem", "mTimeMessageList thread is running  "  + mTimeMessageList.size());
                SingleMessage message = null;
                for(SingleMessage msg : mTimeMessageList)
                {
                    Log.d("rotem", "" + msg.getMessage());
                    Log.d("rotem", "My time: "  + new Date().getTime());
                    Log.d("rotem", "Message time: "  + msg.getTimeToShow());
                    if(new Date().getTime() >= msg.getTimeToShow())
                    {
                        message = msg;
                        msg.getDatabaseReference().child("isTimed").setValue(false);
                        msg.getDatabaseReference().child("isChildAdded").setValue(false);
                        if(!ChatActivity.isActive(msg.getUniqueChatId()))
                            postNotification(msg, msg.getUniqueChatId());
                        break;

                    }
                }
                mTimeMessageList.remove(message);
                Log.d("rotem", "mLocationMessageList thread is running  "  + mLocationMessageList.size());
                for(SingleMessage msg : mLocationMessageList)
                {
                    if (mLocation != null)
                    {
                        float[] distance = new float[1];
                        //get the distance from the target location
                        Location.distanceBetween(msg.getLatitude(), msg.getLongitude(), mLocation.getLatitude(),
                                mLocation.getLongitude(), distance);
                        if(distance[0] < DISTANCE)
                        {
                            message = msg;
                            msg.getDatabaseReference().child("isLocation").setValue(false);
                            msg.getDatabaseReference().child("isChildAdded").setValue(false);
                            if(!ChatActivity.isActive(msg.getUniqueChatId()))
                                postNotification(msg, msg.getUniqueChatId());
                        }
                    }
                }
                mLocationMessageList.remove(message);

            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mCurrentUserId = intent.getStringExtra("UID");
        Toast.makeText(this, "start service",Toast.LENGTH_SHORT).show();
        //assign a request to location
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME_FOR_UPDATE, MIN_DIS_FOR_UPDATE, this);
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPostListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                getRelevantMessages(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                getRelevantMessages(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mChatDatabaseReference.addChildEventListener(mPostListener);
    }

    private void getRelevantMessages(DataSnapshot dataSnapshot)
    {
        if(dataSnapshot.exists()){

            for(DataSnapshot data : dataSnapshot.getChildren()) {
                SingleMessage message = data.getValue(SingleMessage.class);
                Log.d("rotem", "current user id: " + message.getRecipient().equals(mCurrentUserId));
                Log.d("rotem", "message.getIsNew(): " + message.getIsNew());
                Log.d("rotem", "message.getIsChildAdded(): " + message.getIsChildAdded());
                if (message.getRecipient().equals(mCurrentUserId) && message.getIsNew() && !message.getIsChildAdded()) {
                    String uniqueChatId = dataSnapshot.getKey();
                    message.setDatabaseReference(mChatDatabaseReference.child(uniqueChatId).child(data.getKey()));
                    message.setUniqueChatId(uniqueChatId);
                    Log.d("rotem", "before setting isNew to false");
                    message.getDatabaseReference().child("isNew").setValue(false);
                    Log.d("rotem", "after setting isNew to false");
                    if(message.getIsTimed()) {
                        mTimeMessageList.add(message);
                    }
                    else if(message.getIsLocation())
                    {
                        mLocationMessageList.add(message);
                    }
                    else
                    {
                        if(!ChatActivity.isActive(uniqueChatId))
                            postNotification(message, uniqueChatId);
                    }
                }
            }
        }
    }


    private void postNotification(SingleMessage message, String uniqueChatId) {
        User sender = UsersAdapter.getUserByUid(message.getSender());
        if(sender != null ) {

            Intent resultIntent = new Intent(this, ChatActivity.class);
            resultIntent.putExtra(ChatActivity.RECIPIENT_DISPLAY_NAME, sender.getDisplayName());
            resultIntent.putExtra(ChatActivity.CURRENT_USER_ID, mCurrentUserId);
            resultIntent.putExtra(ChatActivity.RECIPIENT_ID, message.getRecipient());
            resultIntent.putExtra(ChatActivity.UNIQUE_CHAT_ID, uniqueChatId);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //create Notification bar
            Log.d("rotem", "build notification");
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setSmallIcon(R.drawable.logo)
                            .setContentIntent(resultPendingIntent)
                            .setContentTitle("From: " + sender.getDisplayName())
                            .setAutoCancel(true)
                            .setContentText(message.getMessage());
            mNotificationManager.notify(1, mBuilder.build());
        }
    }

    //called when location was changed
    @Override
    public void onLocationChanged(Location location) {

        mLocation = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
    }

    //remove request to location
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.removeUpdates(this);
    }
}
