package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SearchController {

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private FlowPane resultsContainer;

    private static final String BACKEND_URL = "http://localhost:8080/search/song"; // replace with your backend URL

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // Trigger search when pressing Enter
        searchField.setOnAction(e -> fetchSongs());
        // Trigger search when clicking search button
        searchButton.setOnAction(e -> fetchSongs());
    }

    private void fetchSongs() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        // Get current logged-in user
        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            System.out.println("User not logged in!");
            return;
        }
        Integer userId = currentUser.getUserId();

        new Thread(() -> {
            try {
                String apiUrl = BACKEND_URL +
                        "?query=" + query.replace(" ", "%20") +
                        "&userId=" + userId +
                        "&limit=20&offset=0";

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                try (InputStream inputStream = conn.getInputStream()) {
                    // Deserialize JSON into List<SongInfo>
                    List<SongInfo> songs = objectMapper.readValue(inputStream, new TypeReference<List<SongInfo>>() {});

                    // Update UI
                    Platform.runLater(() -> {
                        resultsContainer.getChildren().clear();
                        for (SongInfo song : songs) {
                            resultsContainer.getChildren().add(createSongCard(song));
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox createSongCard(SongInfo song) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
                -fx-background-color: #2b2b2b;
                -fx-padding: 10;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                """);

        // Song cover image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        if (song.getCoverURL() != null && !song.getCoverURL().isEmpty()) {
            imageView.setImage(new Image(song.getCoverURL(), true));
        }

        // Song title
        Label titleLabel = new Label(song.getSongName());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Artist name
        Label artistLabel = new Label(song.getArtistName());
        artistLabel.setStyle("-fx-text-fill: #aaaaaa;");

        card.getChildren().addAll(imageView, titleLabel, artistLabel);
        return card;
    }
}
