package com.example.project111;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View v, int position);
    }

    private final List<Message> messages;
    private final List<String> keys;        // parallel list of Firebase pushKeys
    private final String currentUserId;
    private OnItemLongClickListener longClickListener;

    public ChatAdapter(List<Message> messages, List<String> keys, String currentUserId) {
        this.messages      = messages;
        this.keys          = keys;
        this.currentUserId = currentUserId;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        this.longClickListener = l;
    }

    public String getKeyAt(int pos) {
        return keys.get(pos);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int pos) {
        Message m = messages.get(pos);

        // Display text or placeholder “Message Deleted”
        if ("deletion".equals(m.getType())) {
            holder.tvText.setText("Pesan dihapus");
            holder.tvText.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.tvText.setText(m.getText());
            holder.tvText.setTypeface(null, Typeface.NORMAL);
        }

        // Display time
        String time = DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(new Date(m.getTimestamp()));
        holder.tvTime.setText(time);

        // Left/Right alignment
        boolean isMine = currentUserId.equals(m.getSenderId());
        holder.container.setGravity(isMine ? Gravity.END : Gravity.START);

        // Long‐press to delete message
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(v, pos);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        LinearLayout container;   // Use LinearLayout so it has setGravity()

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText    = itemView.findViewById(R.id.messageTextView);
            tvTime    = itemView.findViewById(R.id.timeTextView);
            container = itemView.findViewById(R.id.chatContainer);
        }
    }
}
