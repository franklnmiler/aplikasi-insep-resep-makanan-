package com.example.project111;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.*;

import java.util.HashMap;

public class CommentManager {

    private final DatabaseReference commentRef;
    private final Context context;
    private final SharedPreferences sharedPreferences;

    public static final String TAG = "CommentManager";

    public CommentManager(Context context, String recipeId) {
        this.context = context;
        this.commentRef = FirebaseDatabase.getInstance().getReference("comments").child(recipeId);
        this.sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
    }

    public String getUsername() {
        return sharedPreferences.getString("username", "Unknown User");
    }

    // Method to load comments from Firebase
    public void loadComments(ValueEventListener listener) {
        commentRef.addValueEventListener(listener);
    }

    // Method to add a comment
    public void addComment(String commentText, OnCompleteListener listener) {
        String commentId = commentRef.push().getKey();
        if (commentId != null) {
            HashMap<String, Object> commentData = new HashMap<>();
            commentData.put("userName", getUsername());
            commentData.put("text", commentText);
            commentData.put("timestamp", System.currentTimeMillis());

            commentRef.child(commentId).setValue(commentData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            listener.onComplete();
                        } else {
                            listener.onError(task.getException());
                        }
                    });
        } else {
            listener.onError(new Exception("Failed to generate comment ID"));
        }
    }

    // Method to add a reply to a comment
    public void addReply(String commentId, String replyText, OnCompleteListener listener) {
        String replyId = commentRef.child(commentId).child("replies").push().getKey();
        if (replyId != null) {
            HashMap<String, Object> replyData = new HashMap<>();
            replyData.put("userName", getUsername());
            replyData.put("text", replyText);
            replyData.put("timestamp", System.currentTimeMillis());

            commentRef.child(commentId).child("replies").child(replyId).setValue(replyData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            listener.onComplete();
                        } else {
                            listener.onError(task.getException());
                        }
                    });
        } else {
            listener.onError(new Exception("Failed to generate reply ID"));
        }
    }

    // Method to add a sub-reply to a reply
    public void addSubReply(String commentId, String replyId, String subReplyText, OnCompleteListener listener) {
        String subReplyId = commentRef.child(commentId).child("replies").child(replyId).child("subReplies").push().getKey();
        if (subReplyId != null) {
            HashMap<String, Object> subReplyData = new HashMap<>();
            subReplyData.put("userName", getUsername());
            subReplyData.put("text", subReplyText);
            subReplyData.put("timestamp", System.currentTimeMillis());

            commentRef.child(commentId).child("replies").child(replyId).child("subReplies").child(subReplyId).setValue(subReplyData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            listener.onComplete();
                        } else {
                            listener.onError(task.getException());
                        }
                    });
        } else {
            listener.onError(new Exception("Failed to generate sub-reply ID"));
        }
    }

    // Method to delete a comment
    public void deleteCommentById(String commentId, OnDeleteListener listener) {
        commentRef.child(commentId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onDeleted();
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Method to delete a reply
    public void deleteReply(String commentId, String replyId, OnDeleteListener listener) {
        commentRef.child(commentId).child("replies").child(replyId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listener.onDeleted();
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Interface for completion listener
    public interface OnCompleteListener {
        void onComplete();

        void onError(Exception e);
    }

    // Interface for delete listener
    public interface OnDeleteListener {
        void onDeleted();

        void onError(Exception e);
    }
}
