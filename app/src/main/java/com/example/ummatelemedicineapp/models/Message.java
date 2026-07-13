package com.example.ummatelemedicineapp.models;

public class Message {
    private String otherUserId;
    private String senderName;
    private String lastMessage;
    private String time;
    private int profileImage;
    private int unreadCount;

    public Message(String otherUserId, String senderName, String lastMessage, String time, int profileImage, int unreadCount) {
        this.otherUserId = otherUserId;
        this.senderName = senderName;
        this.lastMessage = lastMessage;
        this.time = time;
        this.profileImage = profileImage;
        this.unreadCount = unreadCount;
    }

    public String getOtherUserId() { return otherUserId; }
    public String getSenderName() { return senderName; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getProfileImage() { return profileImage; }
    public int getUnreadCount() { return unreadCount; }
}