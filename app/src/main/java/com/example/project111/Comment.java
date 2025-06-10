package com.example.project111;

import java.util.Map;

public class Comment {
    public String userName;
    public String text;
    public long timestamp;
    public Map<String, Reply> replies;  // Balasan untuk komentar

    // ID disimpan terpisah, tidak ikut di Firebase (hanya untuk lokal)
    public String commentId;

    // Constructor kosong dibutuhkan untuk Firebase deserialization
    public Comment() {}

    // Constructor lengkap
    public Comment(String userName, String text, long timestamp, Map<String, Reply> replies) {
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
        this.replies = replies;
    }

    // Getter dan Setter
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Reply> getReplies() {
        return replies;
    }

    public void setReplies(Map<String, Reply> replies) {
        this.replies = replies;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }
}
