package com.emo_tunes.javafxapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.awt.Desktop;
import java.net.URI;

public class SongDetailsPageController {

    @FXML private ImageView coverImage;
    @FXML private Label songTitle;
    @FXML private Label artistName;
    @FXML private Label releaseDate;
    @FXML private Label durationLabel;
    @FXML private Button openSpotifyBtn;
    @FXML private Button openSpotifyAppBtn;
    @FXML private Button backBtn;

    private Runnable onBack; // callback to return to the same playlist

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    private SongInfo currentSong;

    public void setSong(SongInfo song) {
        this.currentSong = song;

        songTitle.setText(song.getSongName());
        artistName.setText("Artist: " + song.getArtistName());
        releaseDate.setText("Released: " + song.getReleaseDate());
        durationLabel.setText("Duration: " + formatDuration(song.getDuration()));

        if (song.getCoverURL() != null && !song.getCoverURL().isEmpty()) {
            coverImage.setImage(new Image(song.getCoverURL(), true));
        } else {
            coverImage.setImage(new Image(getClass().getResource("/com/emo_tunes/javafxapp/default_cover.png").toExternalForm()));
        }

        openSpotifyBtn.setOnAction(e -> openInBrowser(song.getSongURL()));
        openSpotifyAppBtn.setOnAction(e -> openInSpotifyApp(song.getSongURL()));

        backBtn.setOnAction(e -> {
            if (onBack != null) {
                onBack.run();
            } else {
                goBack(); // fallback
            }
        });

    }

    private String formatDuration(Integer durationMs) {
        if (durationMs == null) return "Unknown";
        int seconds = durationMs / 1000;
        int minutes = seconds / 60;
        int remaining = seconds % 60;
        return String.format("%d:%02d", minutes, remaining);
    }

    private void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openInSpotifyApp(String url) {
        try {
            if (url != null && url.contains("spotify.com")) {
                String uri = url.replace("https://open.spotify.com/", "spotify:");
                Desktop.getDesktop().browse(new URI(uri));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void goBack() {
        if (onBack != null) {
            onBack.run(); // return to previous playlist
        } else {
            // fallback: reload EmoListPage.fxml
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/EmoListPage.fxml"));
                Parent root = loader.load();
                backBtn.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
