package com.example.project111;

import java.util.Map;

public class Reply {
    public String userName;
    public String text;
    public long timestamp;
    public Map<String, Reply> subReplies;  // Sub-balasan untuk reply

    // ID disimpan terpisah, tidak ikut di Firebase (hanya untuk lokal)
    public String replyId;

    // Constructor kosong dibutuhkan untuk Firebase deserialization
    public Reply() {}

    // Constructor lengkap
    public Reply(String userName, String text, long timestamp, Map<String, Reply> subReplies) {
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
        this.subReplies = subReplies;
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

    public Map<String, Reply> getSubReplies() {
        return subReplies;
    }

    public void setSubReplies(Map<String, Reply> subReplies) {
        this.subReplies = subReplies;
    }

    public String getReplyId() {
        return replyId;
    }

    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }
}
