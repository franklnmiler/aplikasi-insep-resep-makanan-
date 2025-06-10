package com.example.project111;

import java.io.Serializable;
import java.util.List;

public class Recipe implements Serializable {

    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private List<String> ingredients;
    private List<String> steps;
    private String videoUrl;
    private String author;
    private long timestamp;

    // Constructor kosong (dibutuhkan untuk Firebase)
    public Recipe() {
    }

    // Constructor lengkap
    public Recipe(String id, String name, String description, String imageUrl, String category,
                  List<String> ingredients, List<String> steps, String videoUrl,
                  String author, long timestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.ingredients = ingredients;
        this.steps = steps;
        this.videoUrl = videoUrl;
        this.author = author;
        this.timestamp = timestamp;
    }

    // Getter dan Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
