package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class SongDetailsPageController {

    @FXML private ImageView coverImage;
    @FXML private Label songTitle;
    @FXML private Label artistName;
    @FXML private Label releaseDate;
    @FXML private Label durationLabel;
    @FXML private Button openSpotifyBtn;
    @FXML private Button openSpotifyAppBtn;
    @FXML private Button backBtn;
    @FXML private Button addToPlaylistBtn;

    private Runnable onBack;
    private SongInfo currentSong;
    private VBox resultsContainer;
    private AnchorPane songDetailsContainer;

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    public void setContainers(VBox resultsContainer, AnchorPane songDetailsContainer) {
        this.resultsContainer = resultsContainer;
        this.songDetailsContainer = songDetailsContainer;
    }

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
    }

    private String formatDuration(Integer durationMs) {
        if (durationMs == null) return "Unknown";
        int seconds = durationMs / 1000;
        int minutes = seconds / 60;
        int remaining = seconds % 60;
        return String.format("%d:%02d", minutes, remaining);
    }

    @FXML
    public void initialize() {
        backBtn.setOnAction(e -> {
            if (songDetailsContainer != null && resultsContainer != null) {
                songDetailsContainer.setVisible(false);
                resultsContainer.setVisible(true);
            } else {
                goBack();
            }
        });

        addToPlaylistBtn.setOnAction(e -> fetchUserPlaylistsAndAddSong());
    }
    private void fetchUserPlaylistsAndAddSong() {
        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            showAlert("Error", "User not logged in!");
            return;
        }

        Task<List<PlaylistInfo>> task = new Task<>() {
            @Override
            protected List<PlaylistInfo> call() throws Exception {
                String urlStr = "http://localhost:8080/playlist/user?userId=" + currentUser.getUserId();
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed to fetch playlists");
                }

                ObjectMapper mapper = new ObjectMapper();
                List<PlaylistInfo> playlists = mapper.readValue(conn.getInputStream(), new TypeReference<>() {});
                conn.disconnect();
                return playlists;
            }
        };

        task.setOnSucceeded(e -> {
            List<PlaylistInfo> playlists = task.getValue();
            if (playlists == null || playlists.isEmpty()) {
                showAlert("No Playlists", "You don't have any playlists yet!");
                return;
            }

            // Pass PlaylistInfo objects directly
            ChoiceDialog<PlaylistInfo> dialog = new ChoiceDialog<>(playlists.get(0), playlists);
            dialog.setTitle("Select Playlist");
            dialog.setHeaderText("Choose a playlist to add this song:");
            dialog.setContentText("Playlists:");

            dialog.showAndWait().ifPresent(this::addSongToPlaylistBackend);
        });

        task.setOnFailed(e -> showAlert("Error", "Failed to fetch playlists"));

        new Thread(task).start();
    }


    private void addSongToPlaylistBackend(PlaylistInfo playlist) {
        if (currentSong == null || playlist == null) return;
        System.out.println(currentSong.getSongName());

        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String urlStr = "http://localhost:8080/playlist/" + playlist.getPlaylistId()
                        + "/add-song?userId=" + currentUser.getUserId();
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonBody = new ObjectMapper().writeValueAsString(currentSong);
                try (var os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes());
                    os.flush();
                }

                int status = conn.getResponseCode();
                Platform.runLater(() -> {
                    if (status == 200) {
                        showAlert("Success", "Song added to playlist successfully!");
                    } else {
                        System.err.println("Error while adding song to playlist! :Status = "+status );
                        showAlert("Error", "Failed to add song. Please try again.");
                    }
                });

                conn.disconnect();
                return null;
            }
        };

        new Thread(task).start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (coverImage != null && coverImage.getScene() != null) {
                alert.initOwner(coverImage.getScene().getWindow());
            }
            alert.showAndWait();
        });
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
            onBack.run();
        } else {
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
