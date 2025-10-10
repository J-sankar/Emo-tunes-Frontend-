package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class PlaylistDetailsController {

    @FXML
    private VBox songsContainer;

    @FXML
    private Label playlistName;

    @FXML
    private Label songCount;

    @FXML
    private ImageView coverImage;

    private PlaylistInfo playlist;

    /**
     * Fetches the full playlist by ID from backend.
     */
    public void loadPlaylistData(int playlistId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8080/playlist/" + playlistId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    ObjectMapper mapper = new ObjectMapper();
                    PlaylistInfo fullPlaylist = mapper.readValue(response.toString(), PlaylistInfo.class);

                    Platform.runLater(() -> setPlaylistData(fullPlaylist));
                } else {
                    System.err.println("Failed to fetch playlist: HTTP " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Populates UI with playlist info and songs
     */
    public void setPlaylistData(PlaylistInfo playlistInfo) {
        playlist = playlistInfo;
        List<SongInfo> songs = playlist.getSongs();
        // Set playlist info
        playlistName.setText(playlistInfo.getPlaylistName());
        songCount.setText(songs.size() + " songs");

        // Set cover image
        if (playlistInfo.getCoverUrl() != null && !playlistInfo.getCoverUrl().isEmpty()) {
            try {
                coverImage.setImage(new Image(playlistInfo.getCoverUrl(), true));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Populate songs
        songsContainer.getChildren().clear();
        for (SongInfo song : songs) {
            songsContainer.getChildren().add(createSongCard(song));
        }
    }

    /**
     * Creates a UI card for a song
     */
    private HBox createSongCard(SongInfo song) {
        Label nameLabel = new Label("♪ " + song.getSongName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox card = new HBox(nameLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setSpacing(10);
        card.setStyle("-fx-background-color: linear-gradient(to right, #e0eafc, #cfdef3); -fx-background-radius: 10;");
        card.setEffect(new DropShadow(5, Color.rgb(0, 0, 0, 0.25)));

        return card;
    }

    /**
     * Handles back button
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/PlaylistPage.fxml"));
            Parent playlistRoot = loader.load();

            Stage stage = (Stage) songsContainer.getScene().getWindow();
            stage.getScene().setRoot(playlistRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
