package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import javafx.scene.control.Button;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Name your new playlist");
        dialog.setContentText("Playlist name:");

        dialog.showAndWait().ifPresent(name -> {
            createPlaylist(name, null, List.of(),true); // null cover uses default image, empty initial songs
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
        for (int i = 1; i <= 3; i++) {
            String name = "Playlist " + i;
            String coverImage = "https://thumbs.dreamstime.com/b/music-notes-heart-simple-illustration-shape-treble-clef-white-background-70248786.jpg"; // Replace with real image from backend URL
            String[] songs = {
                    "Song 1 from " + name,
                    "Song 2 from " + name,
                    "Song 3 from " + name
            };
            playlistContainer.getChildren().add(createPlaylistCard(name, coverImage, songs));
            UserInfo currentUser = SessionManager.getInstance().getUser();
            if (currentUser == null) {
                System.out.println("User not logged in!");
                return;
            }
            int userId = currentUser.getUserId();
            loadPlaylistsFromBackend(userId);
        }
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
                        for (PlaylistInfo playlist : playlists) {
                            String[] songs = playlist.getSongs()
                                    .stream()
                                    .map(song -> song.getSongName())
                                    .toArray(String[]::new);

                            playlistContainer.getChildren().add(
                                    createPlaylistCard(
                                            playlist.getPlaylistName(),
                                            playlist.getCoverUrl(),
                                            songs
                                    )
                            );
                        }
                    });
                } else {
                    System.err.println("⚠️ HTTP Error: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    /**
     * Creates a playlist in the UI and optionally persists it to the backend.
     * @param playlistName The name of the new playlist
     * @param coverUrl URL of the cover image (optional, can be null)
     * @param songs Initial list of songs (can be empty)
     * @param persistBackend If true, sends the playlist to backend
     */
    private void createPlaylist(String playlistName, String coverUrl, List<String> songs, boolean persistBackend) {
        // 1️⃣ Add playlist to UI
        String[] songsArray = songs.toArray(new String[0]);
        playlistContainer.getChildren().add(0, createPlaylistCard(
                playlistName,
                coverUrl != null ? coverUrl : "https://i.scdn.co/image/ab67616d0000b2732db7ff835f7a3d7ff7e6b6c9", // default image
                songsArray
        ));

        // 2️⃣ Persist to backend if needed
        if (persistBackend) {
            new Thread(() -> {
                try {
                    UserInfo currentUser = SessionManager.getInstance().getUser();
                    if (currentUser == null) {
                        System.out.println("User not logged in!");
                        return;
                    }

                    int userId = currentUser.getUserId();

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("name", playlistName);
                    payload.put("coverUrl", coverUrl);  // allow null
                    payload.put("songs", songs);


                    String jsonPayload = new ObjectMapper().writeValueAsString(payload);
                    String finalUrl = "http://localhost:8080/playlist/create?userId=" + userId;
                    URL url = new URL(finalUrl); // replace with your endpoint
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    conn.getOutputStream().write(jsonPayload.getBytes());
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200 || responseCode == 201) {
                        System.out.println("Playlist saved to backend: " + playlistName);
                        loadPlaylistsFromBackend(userId);
                    } else {
                        System.err.println("Failed to save playlist. HTTP code: " + responseCode);
                    }

                    conn.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }



    private HBox createPlaylistCard(String name, String imageUrl, String[] songs) {
        Image coverImage;

        try {
            if (imageUrl != null && !imageUrl.isBlank()) {
                coverImage = new Image(imageUrl, true);
            } else {
                // Fallback to default image if URL is missing
                coverImage = new Image(
                        getClass().getResource("default_cover.png").toExternalForm()
                );
            }
        } catch (Exception e) {
            System.err.println("⚠️ Invalid cover URL for playlist: " + name + " -> " + imageUrl);
            // fallback to local image if error occurs
            coverImage = new Image(
                    getClass().getResource("default_cover.png").toExternalForm()
            );
        }
        ImageView cover = new ImageView(coverImage);
        cover.setFitWidth(80);
        cover.setFitHeight(80);
        cover.setStyle("-fx-background-radius: 10;");

        Label label = new Label(name);
        label.getStyleClass().add("playlist-name");

        VBox songListBox = new VBox();
        songListBox.setSpacing(5);
        songListBox.setVisible(false);
        songListBox.setManaged(false);

        // Add existing songs (if any)
        for (String song : songs) {
            Label songLabel = new Label("♪ " + song);
            songLabel.getStyleClass().add("song-label");
            songListBox.getChildren().add(songLabel);
        }

        Button addSongBtn = new Button("+ Add Song");
        addSongBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #89f7fe, #66a6ff);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand;"
        );
        addSongBtn.setOnMouseEntered(e -> addSongBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #66a6ff, #89f7fe);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 6 14;" +
                        "-fx-cursor: hand;"
        ));

        addSongBtn.setOnMouseExited(e -> addSongBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #89f7fe, #66a6ff);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 6 14;" +
                        "-fx-cursor: hand;"
        ));


        addSongBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Song");
            dialog.setHeaderText("Add a song to " + name);
            dialog.setContentText("Song Name:");

            dialog.showAndWait().ifPresent(song -> {
                if (!song.trim().isEmpty()) {
                    Label songLabel = new Label("♪ " + song);
                    songLabel.getStyleClass().add("song-label");
                    songListBox.getChildren().add(songLabel);
                    songListBox.setVisible(true);
                    songListBox.setManaged(true);
                }
            });
        });

        VBox rightBox = new VBox(label, addSongBtn, songListBox);
        rightBox.setAlignment(Pos.CENTER_LEFT);
        rightBox.setSpacing(10);

        HBox card = new HBox(20, cover, rightBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);" +
                "-fx-background-radius: 15; -fx-cursor: hand;");
        card.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.25)));

        card.setOnMouseClicked(e -> {
            boolean visible = songListBox.isVisible();
            songListBox.setVisible(!visible);
            songListBox.setManaged(!visible);
        });

        card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color: rgba(60, 60, 60, 0.6); -fx-background-radius: 15;"));
        card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 15;"));

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
