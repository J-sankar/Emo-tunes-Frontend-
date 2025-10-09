package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class EmotionSongsPopupController {

    @FXML private VBox popupRoot;
    @FXML private VBox songsContainer;
    @FXML private Label popupTitle;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BACKEND_URL = "http://localhost:8080/recommend/songs";





    /**
     * Show the popup and fetch songs for a given emotion.
     */
    public void showPopup(String emotion) {
        popupRoot.setVisible(true);
        popupRoot.setOpacity(1);
        popupTitle.setText("Fetching songs for " + emotion + "...");

        fetchSongs(emotion);
    }

    /**
     * Close the popup with a fade animation.
     */
    public void closePopup() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), popupRoot);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> popupRoot.setVisible(false));
        ft.play();
    }

    /**
     * Fetch songs from backend based on emotion.
     */
    private void fetchSongs(String emotion) {
        String emo = emotion.toLowerCase();
        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            System.out.println("User not logged in!");
            return;
            // use user info as needed
        }
        int userId = currentUser.getUserId();
        Task<List<SongInfo>> task = new Task<>() {
            @Override
            protected List<SongInfo> call() throws Exception {
                final String finalUrl = BACKEND_URL + "?emotion=" + emo + "&userId=" + userId;

                HttpURLConnection conn = (HttpURLConnection) new URL(finalUrl).openConnection();
                conn.setRequestMethod("GET");
                try (InputStream is = conn.getInputStream()) {
                    return OBJECT_MAPPER.readValue(is, new TypeReference<>() {});
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<SongInfo> songs = task.getValue();
            populateSongs(songs, emotion);
        });

        task.setOnFailed(e -> e.getSource().getException().printStackTrace());

        new Thread(task).start();
    }

    /**
     * Populate songs inside the VBox.
     */
    private void populateSongs(List<SongInfo> songs, String emotion) {
        Platform.runLater(() -> {
            songsContainer.getChildren().clear();
            popupTitle.setText("Songs for " + emotion);

            if (songs.isEmpty()) {
                Label noSongs = new Label("No songs found.");
                noSongs.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 16px;");
                songsContainer.getChildren().add(noSongs);
                return;
            }

            for (SongInfo song : songs) {
                HBox card = createSongCard(song);

            }
        });
    }

    /**
     * Create a single song card HBox.
     */
    private HBox createSongCard(SongInfo song) {
        HBox card = new HBox(10);
        card.setStyle("-fx-background-color: #2E2E3E; -fx-padding: 10; -fx-background-radius: 10;");

        // Song cover
        ImageView coverView = new ImageView();
        coverView.setFitWidth(60);
        coverView.setFitHeight(60);
        coverView.setPreserveRatio(true);

        try {
            if (song.getCoverURL() != null && !song.getCoverURL().isEmpty()) {
                Image cover = new Image(song.getCoverURL(), true);
                coverView.setImage(cover);
            }
        } catch (Exception e) {
            System.out.println("Cover not loaded for: " + song.getSongName());
        }

        // Song title & artist
        VBox infoBox = new VBox(5);
        Label titleLabel = new Label(song.getSongName());
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16;");

        Label artistLabel = new Label(song.getArtistName());
        artistLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12;");

        infoBox.getChildren().addAll(titleLabel, artistLabel);

        card.getChildren().addAll(coverView, infoBox);

        songsContainer.getChildren().add(card);

        return card;
    }

}
