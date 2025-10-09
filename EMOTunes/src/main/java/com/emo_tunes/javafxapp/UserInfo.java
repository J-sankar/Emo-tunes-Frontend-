package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class UserInfo {
    private Integer userId;          // Backend user ID
    private String username;         // Username
    private String email;            // Email

    // Default constructor (needed for Jackson)
    public UserInfo() {}

    public UserInfo(Integer userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    // Getters and setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Custom callback interface for async results
    public interface PlaylistCallback {
        void onSuccess(List<PlaylistInfo> playlists);
        void onError(Exception e);
    }

    // Fetch playlists asynchronously
    public static List<PlaylistInfo> getPlaylists(Integer userId) {
        try {
            String urlStr = "http://localhost:8080/playlist/user?userId=" + userId;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status == 200) {
                InputStream is = conn.getInputStream();
                ObjectMapper mapper = new ObjectMapper();
                List<PlaylistInfo> playlists = mapper.readValue(is, new TypeReference<List<PlaylistInfo>>() {});
                is.close();
                return playlists;
            } else {
                throw new RuntimeException("Failed to fetch playlists, status: " + status);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // return empty list on error
        }
    }

}
