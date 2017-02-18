package com.saado.rotem.messageq;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

// class represent single message chat
public class SingleMessage {

    // basic member message
    private String message;
    private String sender;
    private String recipient;
    private long createdAt;
    private boolean isNew;
    private boolean mIsSender;

    // members for showing on specific time
    private boolean isTimed;
    private long timeToShow;

    // members for showing on specific place
    private boolean isPlaced;
    private double latitude, longitude;

    //Firebase ref
    private DatabaseReference dbRef;
    private String uniqueChatId;
    private boolean isChildAdded;

    public SingleMessage() {}

    // constructor
    public SingleMessage(String message, String sender, String recipient, long createdAt, boolean isNew) {
        this.message = message;
        this.recipient = recipient;
        this.sender = sender;
        this.createdAt = createdAt;
        this.isNew = isNew;
        this.isTimed = false;
        this.timeToShow = 0;
        this.isChildAdded = false;
    }

    public void setTimeCondition(long timeCondition)
    {
        this.isTimed = true;
        this.timeToShow = timeCondition;
    }

    // getters
    public String getMessage() {
        return message;
    }

    public String getRecipient(){
        return recipient;
    }

    public String getSender(){
        return sender;
    }

    public long getCreatedAt(){
        return createdAt;
    }

    public boolean getIsNew(){
        return isNew;
    }

    public boolean getIsTimed() {
        return isTimed;
    }

    public long getTimeToShow() {
        return timeToShow;
    }

    public void setIsChildAdded(boolean isChildAdded)
    {
        this.isChildAdded = isChildAdded;
    }

    public boolean getIsChildAdded()
    {
        return this.isChildAdded;
    }


    // not including in FireBase database
    @Exclude
    public void setRecipientOrSender(boolean isSender) {
        mIsSender = isSender;
    }

    @Exclude
    public boolean getIsSender() {
        return mIsSender;
    }

    @Exclude
    public void setDatabaseReference(DatabaseReference databaseReference)
    {
        this.dbRef = databaseReference;
    }
    @Exclude
    public DatabaseReference getDatabaseReference()
    {
        return this.dbRef;
    }

    @Exclude
    public void setUniqueChatId(String uniqueChatId)
    {
        this.uniqueChatId = uniqueChatId;
    }
    @Exclude
    public String getUniqueChatId()
    {
        return this.uniqueChatId;
    }

    @Exclude
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof SingleMessage)) return false;
        SingleMessage msg = (SingleMessage) obj;
        return msg.getCreatedAt() == this.getCreatedAt();
    }
}
