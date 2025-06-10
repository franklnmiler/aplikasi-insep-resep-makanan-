package com.example.project111;

public class UserProfile {
    public String username;
    public String bio;
    public String imageUrl;

    public UserProfile() {
        // Needed for Firebase
    }

    public UserProfile(String username, String bio, String imageUrl) {
        this.username = username;
        this.bio = bio;
        this.imageUrl = imageUrl;
    }
}
