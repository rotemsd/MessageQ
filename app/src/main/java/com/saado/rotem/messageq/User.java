package com.saado.rotem.messageq;


import android.net.Uri;

import com.google.firebase.database.Exclude;
// This class will represent user entity in MessageQ app
public class User {

    // For User without a picture
    public static final String NO_URI = "no_uri";

    // Private members
    private String displayName;
    private String email;
    private String connection;
    private long createdAt;
    private String photoUrl;
    private String mRecipientId;

    public User() {}

    /**
     * User constractor
     * @param displayName
     * @param email
     * @param connection
     * @param createdAt
     * @param photoUrl - url with the picture of the user
     */
    public User(String displayName, String email, String connection, long createdAt, String photoUrl) {
        this.displayName = displayName;
        this.email = email;
        this.connection = connection;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
    }

    /**
     * Create uniqe chat window id, so two users will get the same chat window,
     * no matter who will open the this conversation first
     * the id will use both email address, while the first email will be the email of the oldest user account.
     * @param recipientUserEmail
     * @param recipientCreatedAt
     * @return
     */
    public String createUniqueChatId(String recipientUserEmail, long recipientCreatedAt) {

        String uniqueChatId = "";
        if (recipientCreatedAt > getCreatedAt()) {
            uniqueChatId = cleanEmailAddress(recipientUserEmail) + "_" + cleanEmailAddress(getUserEmail());
        } else {

            uniqueChatId = cleanEmailAddress(getUserEmail()) + "_" + cleanEmailAddress(recipientUserEmail);
        }
        return uniqueChatId;
    }
    /**
     * this method will replace '.' in email accounts with '_' so it could be saved to DB.
     * @param email
     * @return
     */
    private String cleanEmailAddress(String email) {
        return email.replace(".", "_");
    }

    public long getCreatedAt() {
        return createdAt;
    }
    // Getters and Setters
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