package com.example.project111;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList;
    private final CommentManager commentManager;

    public CommentAdapter(Context context, List<Comment> commentList, CommentManager commentManager) {
        this.context = context;
        this.commentList = commentList;
        this.commentManager = commentManager;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvUsername.setText(comment.userName);
        holder.tvCommentText.setText(comment.text);
        holder.tvCommentTime.setText(formatTime(comment.timestamp));

        // Reply button functionality for main comment
        holder.btnReply.setOnClickListener(v -> showReplyDialog(comment.commentId, null, comment.userName));

        // Long press to show delete confirmation for comment
        holder.itemView.setOnLongClickListener(v -> {
            if (isUserAllowedToDelete(comment.userName)) {
                showDeleteConfirmationDialog(comment.commentId, null);  // Delete comment
            } else {
                Toast.makeText(context, "Anda hanya bisa menghapus komentar milik Anda.", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Hide all replies initially
        holder.layoutReplies.setVisibility(View.GONE);  // Hide the replies by default

        // Add the "Lihat Balasan" button
        holder.btnViewReplies.setOnClickListener(v -> {
            // Toggle visibility of the replies
            if (holder.layoutReplies.getVisibility() == View.GONE) {
                holder.layoutReplies.setVisibility(View.VISIBLE);  // Show replies
                holder.btnViewReplies.setText("Sembunyikan Balasan");  // Change button text to "Hide Replies"
            } else {
                holder.layoutReplies.setVisibility(View.GONE);  // Hide replies
                holder.btnViewReplies.setText("Lihat Balasan");  // Change button text to "Show Replies"
            }
        });

        // Displaying replies (and sub-replies)
        holder.layoutReplies.removeAllViews();
        if (comment.replies != null && !comment.replies.isEmpty()) {
            for (Map.Entry<String, Reply> entry : comment.replies.entrySet()) {
                Reply reply = entry.getValue();

                View replyView = LayoutInflater.from(context).inflate(R.layout.item_reply, holder.layoutReplies, false);
                TextView tvReplyUser = replyView.findViewById(R.id.tv_reply_user);
                TextView tvReplyText = replyView.findViewById(R.id.tv_reply_text);
                TextView tvReplyTime = replyView.findViewById(R.id.tv_reply_time);

                // Set the hierarchical username (show only two levels: `Irsyad > Faisal`)
                tvReplyUser.setText(formatReplyUser(comment.userName, reply.userName));
                tvReplyText.setText(reply.text);
                tvReplyTime.setText(formatTime(reply.timestamp));

                // Long press to show delete confirmation for reply
                replyView.setOnLongClickListener(v -> {
                    if (isUserAllowedToDelete(reply.userName)) {
                        showDeleteConfirmationDialog(comment.commentId, entry.getKey());  // Pass replyId here
                    } else {
                        Toast.makeText(context, "Anda hanya bisa menghapus balasan milik Anda.", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });

                // Sub-reply button for replies
                Button btnSubReply = replyView.findViewById(R.id.btn_sub_reply);
                btnSubReply.setOnClickListener(v -> {
                    // Format the parent reply username in the dialog header, passing the current comment and reply user names
                    String parentReplyUsername = comment.userName + " > " + reply.userName;
                    showReplyDialog(comment.commentId, entry.getKey(), parentReplyUsername);  // Passing the parent usernames
                });

                // Sub-replies to replies
                LinearLayout layoutSubReplies = replyView.findViewById(R.id.layout_sub_replies);
                if (reply.subReplies != null && !reply.subReplies.isEmpty()) {
                    for (Map.Entry<String, Reply> subEntry : reply.subReplies.entrySet()) {
                        Reply subReply = subEntry.getValue();
                        View subReplyView = LayoutInflater.from(context).inflate(R.layout.item_sub_reply, layoutSubReplies, false);
                        TextView tvSubReplyUser = subReplyView.findViewById(R.id.tv_sub_reply_user);
                        TextView tvSubReplyText = subReplyView.findViewById(R.id.tv_sub_reply_text);
                        TextView tvSubReplyTime = subReplyView.findViewById(R.id.tv_sub_reply_time);

                        // Set the hierarchical username (show only two levels: `Dhean > Irsyad`)
                        tvSubReplyUser.setText(formatReplyUser(reply.userName, subReply.userName));  // Display parent > reply format
                        tvSubReplyText.setText(subReply.text);
                        tvSubReplyTime.setText(formatTime(subReply.timestamp));

                        layoutSubReplies.addView(subReplyView);
                    }
                }

                holder.layoutReplies.addView(replyView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvCommentText, tvCommentTime;
        Button btnReply, btnViewReplies;
        LinearLayout layoutReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvCommentTime = itemView.findViewById(R.id.tv_comment_time);
            btnReply = itemView.findViewById(R.id.btn_reply);
            btnViewReplies = itemView.findViewById(R.id.btn_view_replies);  // Add button for viewing replies
            layoutReplies = itemView.findViewById(R.id.layout_replies);
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatReplyUser(String parentUser, String replyUser) {
        return parentUser + " > " + replyUser;
    }

    private void showReplyDialog(String commentId, String replyId, String parentUsername) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_reply, null);
        EditText etReply = view.findViewById(R.id.et_reply_text);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Set dialog title based on whether it's a reply or sub-reply
        builder.setTitle(replyId == null ? "Balas Komentar" : "Balas Balasan (Untuk " + parentUsername + ")")
                .setView(view)
                .setPositiveButton("Kirim", (dialog, which) -> {
                    String replyText = etReply.getText().toString().trim();
                    if (replyText.isEmpty()) {
                        Toast.makeText(context, "Balasan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    } else {
                        if (replyId == null) {
                            // Add reply to comment
                            commentManager.addReply(commentId, replyText, new CommentManager.OnCompleteListener() {
                                @Override
                                public void onComplete() {
                                    Toast.makeText(context, "Balasan berhasil dikirim", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(context, "Gagal mengirim balasan", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Add sub-reply to reply
                            commentManager.addSubReply(commentId, replyId, replyText, new CommentManager.OnCompleteListener() {
                                @Override
                                public void onComplete() {
                                    Toast.makeText(context, "Sub-balasan berhasil dikirim", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(context, "Gagal mengirim sub-balasan", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteConfirmationDialog(String commentId, String replyId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus komentar ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    if (replyId == null) {
                        commentManager.deleteCommentById(commentId, new CommentManager.OnDeleteListener() {
                            @Override
                            public void onDeleted() {
                                Toast.makeText(context, "Komentar berhasil dihapus", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(context, "Gagal menghapus komentar", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        commentManager.deleteReply(commentId, replyId, new CommentManager.OnDeleteListener() {
                            @Override
                            public void onDeleted() {
                                Toast.makeText(context, "Balasan berhasil dihapus", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(context, "Gagal menghapus balasan", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private boolean isUserAllowedToDelete(String ownerUsername) {
        return ownerUsername.equals(commentManager.getUsername());
    }
}
