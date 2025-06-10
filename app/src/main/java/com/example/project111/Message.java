package com.example.project111;

public class Message {
    private String senderId;
    private String receiverId;
    private String text;
    private long timestamp;

    // Untuk soft‐delete
    private String type;            // "text" atau "deletion"
    private String deletedMessageId;

    public Message() {
        // Firebase needs empty constructor
    }

    // Constructor untuk pesan baru
    public Message(String senderId, String receiverId, String text, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.type = "text";
    }

    // Getters & Setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDeletedMessageId() { return deletedMessageId; }
    public void setDeletedMessageId(String deletedMessageId) { this.deletedMessageId = deletedMessageId; }
}
