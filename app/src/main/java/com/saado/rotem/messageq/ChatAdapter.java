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

public class ChatAdapter extends ArrayAdapter<SingleMessage> {

    public static final boolean SENDER = true;
    public static final boolean RECIPIENT = false;

    private List<SingleMessage> mMessages;
    private LayoutInflater mInflater;

    public ChatAdapter(Activity activity, List<SingleMessage> messages) {
        super(activity, R.layout.single_message, R.id.tvMessageSender, messages);
        mMessages = messages;
        mInflater = activity.getLayoutInflater();
    }

    @Nullable
    @Override
    public SingleMessage getItem(int position) {
        return mMessages.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.single_message, null);
            holder = new ViewHolder();
            holder.messageSender = (TextView) convertView.findViewById(R.id.tvMessageSender);
            holder.messageRecipient = (TextView) convertView.findViewById(R.id.tvMessageRecipient);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        SingleMessage singleMessage = getItem(position);
        if (singleMessage.getIsSender())
        {
            if (singleMessage.getIsTimed())
                holder.messageSender.setTextColor(Color.RED);
            holder.messageSender.setVisibility(View.VISIBLE);
            holder.messageRecipient.setVisibility(View.GONE);
            holder.messageSender.setText(singleMessage.getMessage());
        } else {

            holder.messageSender.setVisibility(View.GONE);
            Log.d("rotem", "" + singleMessage.getIsTimed());
            Log.d("rotem", "" + new Date().getTime());
            Log.d("rotem", "" + singleMessage.getTimeToShow());
            if (singleMessage.getIsTimed()) {
                holder.messageRecipient.setVisibility(View.GONE);
            } else {

                holder.messageRecipient.setText(singleMessage.getMessage());
                holder.messageRecipient.setVisibility(View.VISIBLE);
            }

        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mMessages == null ? 0 : mMessages.size();
    }

    public void clearList() {
        mMessages.clear();
    }

    public void addMessage(SingleMessage message) {
        mMessages.add(message);
        notifyDataSetChanged();
    }

    public int getMessageIndex(SingleMessage msg)
    {
        return mMessages.indexOf(msg);
    }


    private class ViewHolder {
        TextView messageSender, messageRecipient;
    }

}
