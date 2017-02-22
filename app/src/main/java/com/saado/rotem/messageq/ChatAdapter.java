package com.saado.rotem.messageq;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

// This class is adapter class, it shows the list of the messages in chat activity.
public class ChatAdapter extends ArrayAdapter<SingleMessage> {

    // Static members to define the sender and recipient.
    public static final boolean SENDER = true;
    public static final boolean RECIPIENT = false;

    // private members
    private List<SingleMessage> mMessages;
    private LayoutInflater mInflater;

    // Constructor.
    public ChatAdapter(Activity activity, List<SingleMessage> messages) {
        super(activity, R.layout.single_message, R.id.tvMessageSender, messages);
        mMessages = messages;
        mInflater = activity.getLayoutInflater();
    }

    @Nullable
    @Override
    // Returns the message by position in mMessage List.
    public SingleMessage getItem(int position) {
        return mMessages.get(position);
    }

    @NonNull
    @Override
    // Creates the view of each message, and returns the view.
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        // creates the view, if we don't have any yet.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.single_message, null);
            holder = new ViewHolder();
            holder.messageSender = (TextView) convertView.findViewById(R.id.tvMessageSender);
            holder.messageRecipient = (TextView) convertView.findViewById(R.id.tvMessageRecipient);
            convertView.setTag(holder);
        }
        // Otherwise just get the reference
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the correct message
        SingleMessage singleMessage = getItem(position);
        // If I sent the message,
        if (singleMessage.getIsSender())
        {
            // If it's conditioned message, message should be displayed in red color.
            if (singleMessage.getIsTimed() || singleMessage.getIsLocation())
                holder.messageSender.setTextColor(Color.RED);
            holder.messageSender.setVisibility(View.VISIBLE);
            holder.messageRecipient.setVisibility(View.GONE);
            holder.messageSender.setText(singleMessage.getMessage());
        } else {
            // I am the recipient
            holder.messageSender.setVisibility(View.GONE);
            // If the message is conditioned message, i shouldn't get it.
            if (singleMessage.getIsTimed() || singleMessage.getIsLocation()) {
                holder.messageRecipient.setVisibility(View.GONE);
            } else {
                // It's a regular message, I should see it.
                holder.messageRecipient.setText(singleMessage.getMessage());
                holder.messageRecipient.setVisibility(View.VISIBLE);
            }

        }
        // return the final row view
        return convertView;
    }

    @Override
    // Returns the mMessages list size
    public int getCount() {
        return mMessages == null ? 0 : mMessages.size();
    }

    // Clears the mMessages list
    public void clearList() {
        mMessages.clear();
    }

    // Add a new message to the list and update the change.
    public void addMessage(SingleMessage message) {
        mMessages.add(message);
        notifyDataSetChanged();
    }

    // Returns the index of the message
    public int getMessageIndex(SingleMessage msg)
    {
        return mMessages.indexOf(msg);
    }

    // Class that holds the views of the row.
    private class ViewHolder {
        TextView messageSender, messageRecipient;
    }

}
