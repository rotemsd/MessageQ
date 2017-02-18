package com.saado.rotem.messageq;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ChatActivity extends AppCompatActivity implements ChildEventListener {

    public static final String RECIPIENT_DISPLAY_NAME = "display_name";
    public static final String CURRENT_USER_ID = "current_user_id";
    public static final String RECIPIENT_ID = "recipient_id";
    public static final String UNIQUE_CHAT_ID = "unique_chat_id";
    private static final String TAG = ChatActivity.class.getSimpleName();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMessageText = (EditText) findViewById(R.id.etMessageText);
        mChatListView = (ListView) findViewById(R.id.chatListView);
        mTimeConditionSection = (LinearLayout) findViewById(R.id.timeConditionSection);
        mTimePicker = (TimePicker) findViewById(R.id.timePicker);

        mTimePicker.setIs24HourView(true);

        mChatAdapter = new ChatAdapter(this, new ArrayList<SingleMessage>());
        mChatListView.setAdapter(mChatAdapter);
        mChatListView.setDivider(null);

        getValuesFromIntent();
        setTitle(mRecipientDisplayName);

        mIsTimeCondition = false;
        mIsLocationCondition = false;

        mChatDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("chats").child(mUniqueChatId);

    }

    private void getValuesFromIntent() {

        Intent intent = getIntent();
        mRecipientDisplayName = intent.getStringExtra(RECIPIENT_DISPLAY_NAME);
        mRecipientId = intent.getStringExtra(RECIPIENT_ID);
        mCurrentUserId = intent.getStringExtra(CURRENT_USER_ID);
        mUniqueChatId = intent.getStringExtra(UNIQUE_CHAT_ID);
    }

    protected void onStart() {
        super.onStart();
        hideSoftKeyboard();
        mIsActive = true;
        mChatListener = mChatDatabaseReference.addChildEventListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsActive = false;
        if (mChatListener != null) {
            mChatDatabaseReference.removeEventListener(mChatListener);
        }
        mChatAdapter.clearList();

    }


    public void sendMessage(View view) {

        String message = mMessageText.getText().toString();

        if (!message.isEmpty()) {

            SingleMessage singleMessage = new SingleMessage(message, mCurrentUserId, mRecipientId, new Date().getTime(), true);
            if(mIsTimeCondition)
            {
                Calendar calendar = Calendar.getInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    calendar.set(Calendar.HOUR_OF_DAY, mTimePicker.getHour());
                    calendar.set(Calendar.MINUTE, mTimePicker.getMinute());
                }
                singleMessage.setTimeCondition(calendar.getTimeInMillis());
            }
            mChatDatabaseReference.push().setValue(singleMessage);
            mMessageText.setText("");
            mTimeConditionSection.setVisibility(View.GONE);
            mIsTimeCondition = false;
        }
    }


    public void timeConditionCanceled(View view) {

        mTimeConditionSection.setVisibility(View.GONE);
        mIsTimeCondition = false;
    }

    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_attach) {
            return true;
        }
        else if(item.getItemId() == R.id.timeCondition)
        {
            Calendar c = Calendar.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTimePicker.setHour(c.get(Calendar.HOUR));
                mTimePicker.setMinute(c.get(Calendar.MINUTE) + 1);
            }
            mTimeConditionSection.setVisibility(View.VISIBLE);
            mIsTimeCondition = true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

        if (dataSnapshot.exists()) {
            Log.d("rotem", "onChildAdded");
            SingleMessage newMessage = dataSnapshot.getValue(SingleMessage.class);
            mChatDatabaseReference.child(dataSnapshot.getKey()).child("isChildAdded").setValue(true);
            if (newMessage.getSender().equals(mCurrentUserId)) {
                newMessage.setRecipientOrSender(ChatAdapter.SENDER);
            } else {
                newMessage.setRecipientOrSender(ChatAdapter.RECIPIENT);
            }
            mChatAdapter.addMessage(newMessage);
            mChatListView.setSelection(mChatAdapter.getCount() - 1);
        }

    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        Log.d("rotem", "onChildChanged");

        if (dataSnapshot.exists()) {
            SingleMessage newMessage = dataSnapshot.getValue(SingleMessage.class);
            if(!newMessage.getIsChildAdded() ) {
                if (newMessage.getSender().equals(mCurrentUserId)) {
                    newMessage.setRecipientOrSender(ChatAdapter.SENDER);
                    int index = mChatAdapter.getMessageIndex(newMessage);
                    Log.d("rotem", "index: " + index);
                    View view = mChatListView.getChildAt(index - mChatListView.getFirstVisiblePosition());
                    if(view == null)
                        return;
                    TextView textMessage = (TextView) view.findViewById(R.id.tvMessageSender);
                    textMessage.setTextColor(Color.BLACK);
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

    public static boolean isActive(String uniqueChatId) {
        if (mIsActive && mUniqueChatId.equals(uniqueChatId))
            return true;
        return false;
    }
}
