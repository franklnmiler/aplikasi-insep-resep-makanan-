package com.example.project111;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private Context context;
    private List<Recipe> recipeList;
    private boolean isFromHistoryPage;
    private String currentUsername;
    private OnItemClickListener listener;

    // Constructor utama
    public RecipeAdapter(Context context, List<Recipe> recipeList, boolean isFromHistoryPage) {
        this.context = context;
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.isFromHistoryPage = isFromHistoryPage;

        SharedPreferences sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        currentUsername = sharedPreferences.getString("username", "Unknown User");
    }

    // Constructor tambahan supaya bisa dipanggil hanya dengan context dan list
    public RecipeAdapter(Context context, List<Recipe> recipeList) {
        this(context, recipeList, false); // default isFromHistoryPage = false
    }

    // Interface klik item
    public interface OnItemClickListener {
        void onItemClick(Recipe recipe);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // Update list dan refresh tampilan
    public void updateList(List<Recipe> newList) {
        recipeList.clear();
        if (newList != null) {
            recipeList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        holder.txtName.setText(recipe.getName());
        holder.txtDesc.setText(recipe.getDescription());
        holder.txtCategory.setText("Kategori: " + recipe.getCategory());
        holder.txtAuthor.setText(recipe.getAuthor());

        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.imgRecipe);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(recipe);
            }
        });

        if (isFromHistoryPage && recipe.getAuthor() != null && recipe.getAuthor().equals(currentUsername)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (recipe.getAuthor().equals(currentUsername)) {
                Intent intent = new Intent(context, EditRecipeActivity.class);
                intent.putExtra("recipe", recipe);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Kamu tidak bisa edit resep orang lain", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (recipe.getAuthor().equals(currentUsername)) {
                new AlertDialog.Builder(context)
                        .setTitle("Hapus Resep")
                        .setMessage("Yakin mau hapus resep ini?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            FirebaseDatabase.getInstance().getReference("recipes")
                                    .child(recipe.getId())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Resep berhasil dihapus", Toast.LENGTH_SHORT).show();
                                        recipeList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, recipeList.size());
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            }
        });

        // Animasi item
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.item_animation_fade_scale));
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRecipe;
        TextView txtName, txtDesc, txtCategory, txtAuthor;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRecipe = itemView.findViewById(R.id.img_recipe);
            txtName = itemView.findViewById(R.id.txt_name);
            txtDesc = itemView.findViewById(R.id.txt_desc);
            txtCategory = itemView.findViewById(R.id.txt_category);
            txtAuthor = itemView.findViewById(R.id.tv_author);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
