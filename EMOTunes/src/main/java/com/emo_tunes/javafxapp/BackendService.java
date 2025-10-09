package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

public class BackendService {

    /**
     * Fetch songs by emotion asynchronously.
     *
     * @param emotion  Emotion to fetch songs for
     * @param callback Function to handle the result on success
     */
    public static void getSongsByEmotion(String emotion, Consumer<List<SongInfo>> callback) {
        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            System.out.println("User not logged in!");
            return;
        }
        int userId = currentUser.getUserId();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/recommend/songs?emotion=" + emotion + "&userId=" + userId))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        String body = response.body().trim();
                        if (body.startsWith("[")) {
                            // Backend returned an array of songs
                            List<SongInfo> songs = mapper.readValue(body, new TypeReference<List<SongInfo>>() {});
                            callback.accept(songs);
                        } else if (body.startsWith("{")) {
                            // Backend returned an error object
                            System.out.println("No songs found or backend error: " + body);
                            callback.accept(List.of()); // send empty list
                        } else {
                            System.out.println("Unexpected response: " + body);
                            callback.accept(List.of());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.accept(List.of());
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    callback.accept(List.of());
                    return null;
                });
    }

}
