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

// This class is adapter class, it shows the list of the Users in the MainActivity.
public class UsersAdapter extends ArrayAdapter<User> {

    // private members
    private Context mContext;
    private List<User> mUsers;
    private LayoutInflater mInflater;
    private String mCurrentUserEmail, mCurrentUserId;
    private Long mCurrentUserCreatedAt;
    private static Map<String, User> mUserMapByUid;
    // An array of random colors for user picture
    private int colors[] = {Color.GREEN, Color.CYAN, Color.WHITE, Color.MAGENTA};

    // Constructor.
    public UsersAdapter(Activity activity, List<User> users) {
        super(activity, R.layout.user_row, R.id.tvDisplayName, users);
        mUsers = users;
        mUserMapByUid = new HashMap<>();
        mInflater = activity.getLayoutInflater();
    }

    @NonNull
    @Override
    // Creates the view of each User, and returns the view.
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        // creates the view, if we don't have any yet.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.user_row, null);
            holder = new ViewHolder();
            holder.displayName = (TextView) convertView.findViewById(R.id.tvDisplayName);
            holder.userImage = (ImageView) convertView.findViewById(R.id.userImage);
            convertView.setTag(holder);
        } else
        // Otherwise just get the reference
        {
            holder = (ViewHolder) convertView.getTag();
        }
        // Set the values by the correct user
        holder.displayName.setText(mUsers.get(position).getDisplayName());
        final String photoUrl = mUsers.get(position).getPhotoUrl();
        // If we don't have a picture
        if (photoUrl.equals(User.NO_URI)) {
            holder.userImage.setImageResource(R.drawable.default_avatar);
            holder.userImage.setColorFilter(colors[AppHelper.generateRandomColor()], PorterDuff.Mode.OVERLAY);
        } else {
            // Get the Image url
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
        // Return the final row view
        return convertView;
    }
    // Add a new User to the list and update the change.
    public void addUser(User user) {
        mUsers.add(user);
        mUserMapByUid.put(user.getRecipientId(), user);
        notifyDataSetChanged();
    }

    @Override
    // Returns the mUsers list size
    public int getCount() {
        return mUsers == null ? 0 : mUsers.size();
    }

    // Clears the list
    public void clearList() {
        mUsers.clear();
    }

    // Changes the user
    public void changeUser(int index, User user) {
        mUsers.set(index, user);
        notifyDataSetChanged();
    }
    // Set the current user that running this App
    public void setCurrentUserInfo(String userUid, String email, long createdAt) {

        mCurrentUserId = userUid;
        mCurrentUserEmail = email;
        mCurrentUserCreatedAt = createdAt;
    }
    // Return user by index
    public User getUser(int index) {

        return mUsers.get(index);
    }
    // Get the current user's id
    public static User getUserByUid(String uid) {

        return mUserMapByUid.get(uid);
    }
    // Download the image from URL
    private Bitmap loadImageFromURL(String url) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
    // Class that holds the views of the row.
    private class ViewHolder {
        TextView displayName;
        ImageView userImage;
    }
}
