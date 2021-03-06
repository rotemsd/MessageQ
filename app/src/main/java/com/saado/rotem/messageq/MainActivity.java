package com.saado.rotem.messageq;

import android.app.ActivityManager;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
// This class represent the chat's UI
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // Private class members
    private static String TAG = MainActivity.class.getSimpleName();
    private static final String SERVICE_NAME = "com.saado.rotem.messageq.FirebaseBackgroundService";
    private ListView mUserListView;
    private String mCurrentUserId;
    private User mCurrentUser;
    private List<String> mUsersKeyList;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mUsersDatabaseReference;
    private ChildEventListener mChildEventListener;
    private UsersAdapter mUsersAdapter;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get view reference
        mUserListView = (ListView) findViewById(R.id.usersListView);
        mUsersAdapter = new UsersAdapter(this, new ArrayList<User>());
        // Initialize the UserAdapter and set the list view for this adapter
        mUserListView.setAdapter(mUsersAdapter);
        mUserListView.setOnItemClickListener(this);


        mAuth = FirebaseAuth.getInstance();
        // Get Firebase reference to for the users unique id
        mUsersDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users");
        mUsersKeyList = new ArrayList<String>();

        Transition mFadeTransition = new Fade();
    }

    // Check if the user if authenticated
    private void setAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    mCurrentUserId = user.getUid();
                    mChildEventListener = getChildEventListener();
                    mUsersDatabaseReference.limitToFirst(50).addChildEventListener(mChildEventListener);
                    startBackgroundService();
                } else {
                    // User is signed out
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    // LoginActivity is a New Task
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // The old task when coming back to this activity should be cleared so we cannot come back to it.
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        };
    }
    // Starts the service in the background
    private void startBackgroundService() {

        if(!isServiceRunning()) {
            Intent serviceIntent = new Intent(MainActivity.this, FirebaseBackgroundService.class);
            serviceIntent.putExtra("UID", mCurrentUserId);
            startService(serviceIntent);
        }
    }

    //function to check if the service running in background
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SERVICE_NAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // Stops the service in the background
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(MainActivity.this, FirebaseBackgroundService.class);
        stopService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    // When selecting items on MainActivity menu
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            stopBackgroundService();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    // Set a listener
    public void onStart() {
        super.onStart();
        setAuthListener();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUsersAdapter.clearList();
    }

    @Override
    // Stop the listener
    public void onStop() {
        super.onStop();
        mUsersKeyList.clear();

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        if (mChildEventListener != null) {
            mUsersDatabaseReference.removeEventListener(mChildEventListener);
        }
    }
    // Add Listener on the user in the Firebase DB
    private ChildEventListener getChildEventListener() {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.exists()) {

                    String userUid = dataSnapshot.getKey();
                    // If i'ts not me show the user
                    if (dataSnapshot.getKey().equals(mCurrentUserId)) {
                        mCurrentUser = dataSnapshot.getValue(User.class);
                        mUsersAdapter.setCurrentUserInfo(userUid, mCurrentUser.getEmail(), mCurrentUser.getCreatedAt());
                    } else {
                        User contact = dataSnapshot.getValue(User.class);
                        contact.setRecipientId(userUid);
                        mUsersKeyList.add(userUid);
                        mUsersAdapter.addUser(contact);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.exists()) {
                    String userUid = dataSnapshot.getKey();
                    if (!userUid.equals(mCurrentUserId)) {

                        User user = dataSnapshot.getValue(User.class);

                        int index = mUsersKeyList.indexOf(userUid);
                        if (index > -1) {
                            mUsersAdapter.changeUser(index, user);
                        }
                    }

                }


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


    }

    @Override
    // When clicked on the user, open a chat with the user
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        User recipientUser = mUsersAdapter.getUser(i);
        String uniqueChatId = mCurrentUser.createUniqueChatId(recipientUser.getEmail(), recipientUser.getCreatedAt());

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.RECIPIENT_DISPLAY_NAME, recipientUser.getDisplayName());
        intent.putExtra(ChatActivity.CURRENT_USER_ID, mCurrentUserId);
        intent.putExtra(ChatActivity.RECIPIENT_ID, recipientUser.getRecipientId());
        intent.putExtra(ChatActivity.UNIQUE_CHAT_ID, uniqueChatId);
        startActivity(intent);

    }
}
