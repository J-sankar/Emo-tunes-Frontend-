package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.control.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.emo_tunes.javafxapp.UserInfo;


public class PlaylistPageController {

    @FXML
    private VBox playlistContainer;

    @FXML
    private Button createPlaylistBtn;

    @FXML
    private void handleCreatePlaylist() {
        // Step 1: Ask for playlist name
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Create Playlist");
        nameDialog.setHeaderText("Name your new playlist");
        nameDialog.setContentText("Playlist name:");

        nameDialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return; // skip empty names

            // Step 2: Optionally ask for cover URL
            TextInputDialog coverDialog = new TextInputDialog();
            coverDialog.setTitle("Playlist Cover (optional)");
            coverDialog.setHeaderText("Enter a URL for the playlist cover image (or leave blank for default)");
            coverDialog.setContentText("Cover URL:");

            coverDialog.showAndWait().ifPresent(coverUrl -> {
                if (coverUrl.isBlank()) coverUrl = null;

                // Step 3: Create PlaylistInfo object
                PlaylistInfo newPlaylist = new PlaylistInfo();
                newPlaylist.setPlaylistName(name);
                newPlaylist.setCoverUrl(coverUrl);
                newPlaylist.setSongs(new ArrayList<>()); // empty initial songs

                // Step 4: Add to UI immediately
                playlistContainer.getChildren().add(0, createPlaylistCard(newPlaylist));

                // Step 5: Persist to backend in a thread
                String finalCoverUrl = coverUrl;
                new Thread(() -> {
                    try {
                        UserInfo currentUser = SessionManager.getInstance().getUser();
                        if (currentUser == null) return;

                        int userId = currentUser.getUserId();
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("name", name);
                        payload.put("coverUrl", finalCoverUrl);
                        payload.put("songs", newPlaylist.getSongs());

                        String jsonPayload = new ObjectMapper().writeValueAsString(payload);
                        URL url = new URL("http://localhost:8080/playlist/create?userId=" + userId);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.getOutputStream().write(jsonPayload.getBytes());

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200 || responseCode == 201) {
                            System.out.println("Playlist saved: " + name);
                            loadPlaylistsFromBackend(userId); // reload updated playlists
                        } else {
                            System.err.println("Failed to save playlist. HTTP code: " + responseCode);
                        }
                        conn.disconnect();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            });
        });
    }



//FOR CALLING BACKEND API TO PERSIST CREATED PLAYLIST
//    @FXML
//    private void handleCreatePlaylist() {
//        TextInputDialog dialog = new TextInputDialog();
//        dialog.setTitle("Create Playlist");
//        dialog.setHeaderText("Name your new playlist");
//        dialog.setContentText("Playlist name:");
//
//        dialog.showAndWait().ifPresent(name -> {
//            try {
//                // Example REST API call
//                HttpClient client = HttpClient.newHttpClient();
//                HttpRequest request = HttpRequest.newBuilder()
//                        .uri(URI.create("http://localhost:8080/api/playlists"))
//                        .header("Content-Type", "application/json")
//                        .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + name + "\"}"))
//                        .build();
//
//                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
//                        .thenAccept(response -> System.out.println("Saved: " + response.body()));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }


    // Map of playlists and their songs (example data)
    private final Map<String, List<String>> playlistsToSongs = new HashMap<>();

    // Track currently expanded card to collapse when needed
    private VBox expandedCard = null;

    @FXML
    public void initialize() {
        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            System.out.println("User not logged in!");
            return;
        }

        int userId = currentUser.getUserId();
        loadPlaylistsFromBackend(userId);
    }



    private void loadPlaylistsFromBackend(int userId) {
        new Thread(() -> {
            try {
                String urlString = "http://localhost:8080/playlist/user?userId=" + userId;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    ObjectMapper mapper = new ObjectMapper();
                    List<PlaylistInfo> playlists = mapper.readValue(
                            response.toString(),
                            new TypeReference<List<PlaylistInfo>>() {}
                    );

                    javafx.application.Platform.runLater(() -> {
                        playlistContainer.getChildren().clear();

                        if (playlists.isEmpty()) {
                            Label noPlaylistsLabel = new Label("No playlists yet — create one to get started!");
                            noPlaylistsLabel.setStyle(
                                    "-fx-text-fill: white; " +
                                            "-fx-font-size: 16px; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-opacity: 0.8;"
                            );
                            playlistContainer.getChildren().add(noPlaylistsLabel);
                        } else {
                            for (PlaylistInfo playlist : playlists) {
                                String[] songs = playlist.getSongs()
                                        .stream()
                                        .map(song -> song.getSongName())
                                        .toArray(String[]::new);

                                playlistContainer.getChildren().add(
                                        createPlaylistCard(
                                                playlist
                                        )
                                );
                            }
                        }
                    });
                } else {
                    System.err.println("⚠️ HTTP Error: " + responseCode);
                    javafx.application.Platform.runLater(() -> {
                        playlistContainer.getChildren().clear();
                        Label errorLabel = new Label("No Playlists available.");
                        errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
                        playlistContainer.getChildren().add(errorLabel);
                    });
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    playlistContainer.getChildren().clear();
                    Label errorLabel = new Label("Error loading playlists. Please check your connection.");
                    errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
                    playlistContainer.getChildren().add(errorLabel);
                });
            }
        }).start();
    }
    @FXML
    private void handleAddSong() {

    }


    private HBox createPlaylistCard(PlaylistInfo playlist) {
        String name = playlist.getPlaylistName();
        String imageUrl = playlist.getCoverUrl();
        List<SongInfo> songInfos = playlist.getSongs();
        String[] songs = songInfos.stream()
                .map(SongInfo::getSongName)
                .toArray(String[]::new);

        // Image setup
        Image coverImage;
        try {
            if (imageUrl != null && !imageUrl.isBlank()) {
                coverImage = new Image(imageUrl, true);
            } else {
                coverImage = new Image(getClass().getResource("default_cover.png").toExternalForm());
            }
        } catch (Exception e) {
            coverImage = new Image(getClass().getResource("default_cover.png").toExternalForm());
        }
        ImageView cover = new ImageView(coverImage);
        cover.setFitWidth(80);
        cover.setFitHeight(80);
        cover.setStyle("-fx-background-radius: 10;");

        // Playlist label
        Label label = new Label(name);
        label.getStyleClass().add("playlist-name");

        // Song list VBox (expandable)
        VBox songListBox = new VBox();
        songListBox.setSpacing(5);
        songListBox.setVisible(false);
        songListBox.setManaged(false);
        for (String song : songs) {
            Label songLabel = new Label("♪ " + song);
            songLabel.getStyleClass().add("song-label");
            songListBox.getChildren().add(songLabel);
        }

        // Add Song button
        Button addSongBtn = new Button("+ Add Song");
        addSongBtn.getStyleClass().add("create-playlist-btn"); // use same style
        addSongBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/LandingPage.fxml"));
                Parent landingRoot = loader.load();
                LandingPageController landingController = loader.getController();
                landingController.focusOnSearchBar();

                // Replace current scene root
                Stage stage = (Stage) addSongBtn.getScene().getWindow();
                stage.getScene().setRoot(landingRoot);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        VBox rightBox = new VBox(label, addSongBtn, songListBox);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setSpacing(10);

        // Playlist card HBox
        HBox card = new HBox(20, cover, rightBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 15; -fx-cursor: hand;");
        card.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.25)));

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.6); -fx-background-radius: 15;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 15;"));

        // Allow clicks on labels/images to pass to HBox
        cover.setMouseTransparent(true);
        label.setMouseTransparent(true);

        // Double-click to open playlist details in same window
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.isPrimaryButtonDown()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/PlaylistDetailsPage.fxml"));
                    Parent detailsRoot = loader.load();

                    PlaylistDetailsController controller = loader.getController();
                    controller.setPlaylistData(playlist);

                    // Replace current scene root with playlist details
                    Stage stage = (Stage) card.getScene().getWindow();
                    stage.getScene().setRoot(detailsRoot);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return card;
    }



    private void expandCard(VBox card, VBox songsBox) {
        // Show songs
        songsBox.setVisible(true);
        songsBox.setManaged(true);

        // Animate height increase for smooth effect
        Timeline timeline = new Timeline();
        double startHeight = card.getHeight();
        double endHeight = startHeight + songsBox.getChildren().size() * 24 + 20; // approx song height + padding

        KeyValue kv = new KeyValue(card.minHeightProperty(), endHeight);
        KeyFrame kf = new KeyFrame(Duration.millis(250), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

        // Change background to lighter on expand
        card.setStyle(
                "-fx-background-color: rgba(50,50,50,0.85);" +
                        "-fx-background-radius: 15;" +
                        "-fx-cursor: hand;"
        );
    }

    private void collapseCard(VBox card) {
        // Hide songs
        if (card.getChildren().size() < 2) return;
        VBox songsBox = (VBox) card.getChildren().get(1);
        songsBox.setVisible(false);
        songsBox.setManaged(false);

        // Animate height decrease
        Timeline timeline = new Timeline();
        double startHeight = card.getHeight();
        double endHeight = 110;  // approx height of collapsed card (adjust if needed)

        KeyValue kv = new KeyValue(card.minHeightProperty(), endHeight);
        KeyFrame kf = new KeyFrame(Duration.millis(250), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();

        // Restore background style
        card.setStyle(
                "-fx-background-color: rgba(0,0,0,0.6);" +
                        "-fx-background-radius: 15;" +
                        "-fx-cursor: hand;"
        );
    }
}
