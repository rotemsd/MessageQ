package com.saado.rotem.messageq;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// This class represent the chat's UI
public class ChatActivity extends AppCompatActivity implements ChildEventListener {

    // Constants and static members for intent
    public static final String RECIPIENT_DISPLAY_NAME = "display_name";
    public static final String CURRENT_USER_ID = "current_user_id";
    public static final String RECIPIENT_ID = "recipient_id";
    public static final String UNIQUE_CHAT_ID = "unique_chat_id";
    private static final String TAG = ChatActivity.class.getSimpleName();

    // Private class members
    private static String mUniqueChatId;
    private static boolean mIsActive;
    private String mRecipientDisplayName, mRecipientId, mCurrentUserId;
    private DatabaseReference mChatDatabaseReference;
    private ListView mChatListView;
    private EditText mMessageText;
    private ChildEventListener mChatListener;
    private ChatAdapter mChatAdapter;
    private TimePicker mTimePicker;
    private boolean mIsTimeCondition, mIsLocationCondition;
    private LinearLayout mTimeConditionSection;
    private RelativeLayout mLocationConditionSection;
    private ProgressDialog mLoadingDialog;
    private Context mContext;
    private EditText mEtAddress;
    private AlertDialog.Builder mBuilder;
    private double mLatitude, mLongitude;
    private String mAddressResult;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Add back button in action bar
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get view reference
        mMessageText = (EditText) findViewById(R.id.etMessageText);
        mChatListView = (ListView) findViewById(R.id.chatListView);
        mEtAddress = (EditText) findViewById(R.id.etAddress);
        mLocationConditionSection = (RelativeLayout) findViewById(R.id.locationConditionSection);
        mTimeConditionSection = (LinearLayout) findViewById(R.id.timeConditionSection);
        mTimePicker = (TimePicker) findViewById(R.id.timePicker);

        // Set 24 hours time
        mTimePicker.setIs24HourView(true);

        // Initialize the chat adapter and set the list view for this adapter
        mChatAdapter = new ChatAdapter(this, new ArrayList<SingleMessage>());
        mChatListView.setAdapter(mChatAdapter);
        // Remove Message lined devider
        mChatListView.setDivider(null);

        getValuesFromIntent();
        //set the name of the user we are chatting with
        setTitle(mRecipientDisplayName);

        // Default message conditions is without time or location
        mIsTimeCondition = false;
        mIsLocationCondition = false;

        // Get Firebase reference to for the chat's unique id
        mChatDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("chats").child(mUniqueChatId);

        //initialize members
        mAddressResult = null;
        mBuilder = null;
        mContext = this;
        mLatitude = mLongitude = -1;

        //handler for dismiss the loading dialog
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mLoadingDialog.dismiss();
                mIsLocationCondition = true;
            }
        };

    }

    // Initialize members from Intent that we get from the MainActivity
    private void getValuesFromIntent() {
        Intent intent = getIntent();
        mRecipientDisplayName = intent.getStringExtra(RECIPIENT_DISPLAY_NAME);
        mRecipientId = intent.getStringExtra(RECIPIENT_ID);
        mCurrentUserId = intent.getStringExtra(CURRENT_USER_ID);
        mUniqueChatId = intent.getStringExtra(UNIQUE_CHAT_ID);
    }

    @Override
    //Override onStart to add listener to Firebase
    protected void onStart() {
        super.onStart();
        hideSoftKeyboard();
        mIsActive = true;
        mChatListener = mChatDatabaseReference.addChildEventListener(this);
    }

    @Override
    // Remove the firebase listener and clear the list
    protected void onStop() {
        super.onStop();
        mIsActive = false;
        if (mChatListener != null) {
            mChatDatabaseReference.removeEventListener(mChatListener);
        }
        mChatAdapter.clearList();

    }

    // Implement the "Send" message button
    public void sendMessage(View view) {
        // Get the String from the EditText
        String message = mMessageText.getText().toString();
        // Check if message isn't empty
        if (!message.isEmpty()) {
            // Create new message object
            SingleMessage singleMessage = new SingleMessage(message, mCurrentUserId, mRecipientId, new Date().getTime(), true);

            // If we have a time conditioned message-> add the conditions to the SingleMessage object.
            if(mIsTimeCondition)
            {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, mTimePicker.getCurrentHour());
                calendar.set(Calendar.MINUTE, mTimePicker.getCurrentMinute());

                singleMessage.setTimeCondition(calendar.getTimeInMillis());
            }
            // If we have a location conditioned message-> add the conditions to the SingleMessage object.
            if(mIsLocationCondition)
            {
                singleMessage.setLocationCondition(mLongitude, mLatitude);
            }

            // Push the message to the DB
            mChatDatabaseReference.push().setValue(singleMessage);
            mMessageText.setText("");
            mTimeConditionSection.setVisibility(View.GONE);
            mLocationConditionSection.setVisibility(View.GONE);
            mIsTimeCondition = false;
            mIsLocationCondition = false;
            hideSoftKeyboard();
        }
    }

    // If i cancel the time conditons
    public void timeConditionCanceled(View view) {

        mTimeConditionSection.setVisibility(View.GONE);
        mLocationConditionSection.setVisibility(View.GONE);
        mIsLocationCondition = false;
        mIsTimeCondition = false;
    }


    // Implement the "find" message button
    public void findCoordinates(View view) {

        //hide keyboard
        hideSoftKeyboard();

        //show loading dialog
        mLoadingDialog = ProgressDialog.show(this, "fetching address...", "loading");

        //create new thread for getFromLocationName function
        new Thread() {
            @Override
            public void run() {

                //init Geocoder object
                Geocoder geocoder = new Geocoder(mContext);
                //list for address results
                final List<Address> addresses;
                //get the input address from user
                String streetAddress = mEtAddress.getText().toString();
                //check empty value
                if (streetAddress.equals("")) {
                    showToastOnMainUIThread("Address field is empty");
                } else {
                    try {
                        //get addresses results (max results = 5)
                        addresses = geocoder.getFromLocationName(streetAddress, 5);

                        //if list is empty
                        if (addresses.size() == 0) {

                            showToastOnMainUIThread("Address field is empty");

                        }
                        //if there is only one address
                        else if (addresses.size() == 1) {
                            mAddressResult = addresses.get(0).getAddressLine(0) +
                                    (addresses.get(0).getAddressLine(1) == null ? "" : ", " + addresses.get(0).getAddressLine(1)) +
                                    (addresses.get(0).getAddressLine(2) == null ? "" : ", " + addresses.get(0).getAddressLine(2));
                            mLongitude = addresses.get(0).getLongitude();
                            mLatitude = addresses.get(0).getLatitude();
                            updateUITextViews();

                        }
                        //if there more than 1 address
                        else {

                            //create a list of addresses items for the dialog
                            List<String> addressList = new ArrayList<>();
                            for (Address ad : addresses) {
                                String item = ad.getAddressLine(0) +
                                        (ad.getAddressLine(1) == null ? "" : ", " + ad.getAddressLine(1)) +
                                        (ad.getAddressLine(2) == null ? "" : ", " + ad.getAddressLine(2));
                                addressList.add(item);
                            }

                            //convert the list to string array
                            String[] items = new String[addressList.size()];
                            items = addressList.toArray(items);

                            //define the dialog
                            mBuilder = new AlertDialog.Builder(mContext);
                            mBuilder.setTitle("Select specific address");
                            mBuilder.setItems(items, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int item) {

                                    //pick the user choice
                                    mAddressResult = addresses.get(item).getAddressLine(0) +
                                            (addresses.get(item).getAddressLine(1) == null ? "" : ", " + addresses.get(item).getAddressLine(1)) +
                                            (addresses.get(item).getAddressLine(2) == null ? "" : ", " + addresses.get(item).getAddressLine(2));
                                    mLongitude = addresses.get(item).getLongitude();
                                    mLatitude = addresses.get(item).getLatitude();
                                    updateUITextViews();

                                }
                            });
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //show the dialog
                                    if (mBuilder != null) {
                                        AlertDialog alert = mBuilder.create();
                                        alert.show();
                                    }
                                }
                            });

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        showToastOnMainUIThread("Can't find street");
                    }
                }
                //send message to handler to dismiss the loading dialog
                mHandler.sendEmptyMessage(0);
            }
        }.start();
    }

    //function for showing Toast message on the UI thread (called for another thread)
    public void showToastOnMainUIThread(final String toastMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //function for updating the text views on the UI thread (called for another thread)
    public void updateUITextViews() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEtAddress.setText(mAddressResult);
            }
        });
    }

    // Hide the keyboard
    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    // Create the chat menu
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    // When selecting items on chat menu
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id)
        {
            case R.id.locationCondition:
                mLocationConditionSection.setVisibility(View.VISIBLE);
                return true;

            case R.id.timeCondition:
                mTimeConditionSection.setVisibility(View.VISIBLE);
                mIsTimeCondition = true;
                return true;

            //about the programmers :)
            case R.id.action_about:
                Toast.makeText(this, "Created By\nRotem Saado & Elya Bar-On", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    // Firebase function listener when a new message is created
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if (dataSnapshot.exists()) {
            // Get the message values to a new SingleMessage object
            SingleMessage newMessage = dataSnapshot.getValue(SingleMessage.class);
            mChatDatabaseReference.child(dataSnapshot.getKey()).child("isChildAdded").setValue(true);
            // Define the message to a sender or recipient
            if (newMessage.getSender().equals(mCurrentUserId)) {
                newMessage.setRecipientOrSender(ChatAdapter.SENDER);
            } else {
                newMessage.setRecipientOrSender(ChatAdapter.RECIPIENT);
            }
            // Add the message to the adapter
            mChatAdapter.addMessage(newMessage);
            // Show the last message in the list
            mChatListView.setSelection(mChatAdapter.getCount() - 1);
        }

    }

    @Override
    // Firebase function listener when a  message is changed
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        if (dataSnapshot.exists()) {
            // Get the message values to a new SingleMessage object
            SingleMessage newMessage = dataSnapshot.getValue(SingleMessage.class);
            // Check if the child is already added
            if(!newMessage.getIsChildAdded() ) {
                // Define the message to a sender or recipient
                if (newMessage.getSender().equals(mCurrentUserId)) {
                    newMessage.setRecipientOrSender(ChatAdapter.SENDER);
                    int index = mChatAdapter.getMessageIndex(newMessage);
                    View view = mChatListView.getChildAt(index - mChatListView.getFirstVisiblePosition());
                    if(view == null)
                        return;
                    // Change the color of message to black
                    TextView textMessage = (TextView) view.findViewById(R.id.tvMessageSender);
                    textMessage.setTextColor(Color.BLACK);
                    // Show the last message in the list
                    mChatListView.setSelection(mChatAdapter.getCount() - 1);
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
    // Show if the activity in active or not for notifications indication
    public static boolean isActive(String uniqueChatId) {
        if (mIsActive && mUniqueChatId.equals(uniqueChatId))
            return true;
        return false;
    }
}
