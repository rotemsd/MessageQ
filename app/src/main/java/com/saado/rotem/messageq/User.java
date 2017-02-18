package com.saado.rotem.messageq;


import android.net.Uri;

import com.google.firebase.database.Exclude;

public class User {

    public static final String NO_URI = "no_uri";
    private String displayName;
    private String email;
    private String connection;
    private long createdAt;
    private String photoUrl;
    private String mRecipientId;

    public User() {}

    public User(String displayName, String email, String connection, long createdAt, String photoUrl) {
        this.displayName = displayName;
        this.email = email;
        this.connection = connection;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
    }


    public String createUniqueChatId(String recipientUserEmail, long recipientCreatedAt) {

        String uniqueChatId = "";
        if (recipientCreatedAt > getCreatedAt()) {
            uniqueChatId = cleanEmailAddress(recipientUserEmail) + "_" + cleanEmailAddress(getUserEmail());
        } else {

            uniqueChatId = cleanEmailAddress(getUserEmail()) + "_" + cleanEmailAddress(recipientUserEmail);
        }
        return uniqueChatId;
    }

    private String cleanEmailAddress(String email) {
        return email.replace(".", "_");
    }

    public long getCreatedAt() {
        return createdAt;
    }

    private String getUserEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getConnection() {
        return connection;
    }

    @Exclude
    public String getRecipientId() {
        return mRecipientId;
    }

    public void setRecipientId(String recipientId) {
        this.mRecipientId = recipientId;
    }
}