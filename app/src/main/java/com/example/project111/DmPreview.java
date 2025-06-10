package com.example.project111;

public class DmPreview {
    private String userId;
    private String username;
    private String profileUrl;
    private String lastMessage;
    private long timestamp;

    public DmPreview() {}

    public DmPreview(String userId, String username, String profileUrl, String lastMessage, long timestamp) {
        this.userId = userId;
        this.username = username;
        this.profileUrl = profileUrl;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getProfileUrl() { return profileUrl; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
}
