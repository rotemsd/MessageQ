package com.saado.rotem.messageq;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersAdapter extends ArrayAdapter<User> {

    private Context mContext;
    private List<User> mUsers;
    private LayoutInflater mInflater;
    private String mCurrentUserEmail, mCurrentUserId;
    private Long mCurrentUserCreatedAt;
    private static Map<String, User> mUserMapByUid;
    private int colors[] = {Color.GREEN, Color.CYAN, Color.WHITE, Color.MAGENTA};

    public UsersAdapter(Activity activity, List<User> users) {
        super(activity, R.layout.user_row, R.id.tvDisplayName, users);
//        mContext = context;
        mUsers = users;
        mUserMapByUid = new HashMap<>();
        mInflater = activity.getLayoutInflater();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.user_row, null);
            holder = new ViewHolder();
            holder.displayName = (TextView) convertView.findViewById(R.id.tvDisplayName);
            holder.userImage = (ImageView) convertView.findViewById(R.id.userImage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.displayName.setText(mUsers.get(position).getDisplayName());
        final String photoUrl = mUsers.get(position).getPhotoUrl();
        if (photoUrl.equals(User.NO_URI)) {
            holder.userImage.setImageResource(R.drawable.default_avatar);
            holder.userImage.setColorFilter(colors[AppHelper.generateRandomColor()], PorterDuff.Mode.OVERLAY);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = loadImageFromURL(photoUrl);
                    holder.userImage.post(new Runnable() {
                        @Override
                        public void run() {
                            holder.userImage.setImageBitmap(bitmap);
                        }
                    });
                }
            }).start();
        }
        return convertView;
    }

    public void addUser(User user) {
        mUsers.add(user);
        mUserMapByUid.put(user.getRecipientId(), user);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mUsers == null ? 0 : mUsers.size();
    }

    public void clearList() {
        mUsers.clear();
    }

    public void changeUser(int index, User user) {
        mUsers.set(index, user);
        notifyDataSetChanged();
    }

    public void setCurrentUserInfo(String userUid, String email, long createdAt) {

        mCurrentUserId = userUid;
        mCurrentUserEmail = email;
        mCurrentUserCreatedAt = createdAt;
    }

    public User getUser(int index) {

        return mUsers.get(index);
    }

    public static User getUserByUid(String uid) {

        return mUserMapByUid.get(uid);
    }

    private Bitmap loadImageFromURL(String url) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private class ViewHolder {
        TextView displayName;
        ImageView userImage;
    }
}
