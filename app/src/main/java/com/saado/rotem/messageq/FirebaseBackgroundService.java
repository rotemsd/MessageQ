package com.saado.rotem.messageq;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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

public class FirebaseBackgroundService extends Service {

    private DatabaseReference mChatDatabaseReference = FirebaseDatabase.getInstance()
            .getReference().child("chats");
    private ChildEventListener mPostListener;
    private String mCurrentUserId;
    private List<SingleMessage> mMessageList;

    public FirebaseBackgroundService()
    {
        mMessageList = new ArrayList<>();
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.d("rotem", "thread is running  "  + mMessageList.size());
                for(SingleMessage msg : mMessageList)
                {
                    if(new Date().getTime() >= msg.getTimeToShow())
                    {
                        msg.getDatabaseReference().child("isTimed").setValue(false);
                        msg.getDatabaseReference().child("isChildAdded").setValue(false);
                        if(!ChatActivity.isActive(msg.getUniqueChatId()))
                            postNotification(msg, msg.getUniqueChatId());
                        mMessageList.remove(msg);
                    }
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mCurrentUserId = intent.getStringExtra("UID");
        Toast.makeText(this, "start service",Toast.LENGTH_SHORT).show();
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
                if (message.getRecipient().equals(mCurrentUserId) && message.getIsNew()) {
                    String uniqueChatId = dataSnapshot.getKey();
                    message.setDatabaseReference(mChatDatabaseReference.child(uniqueChatId).child(data.getKey()));
                    message.setUniqueChatId(uniqueChatId);
                    message.getDatabaseReference().child("isNew").setValue(false);
                    if(message.getIsTimed()) {
                        mMessageList.add(message);
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
}
