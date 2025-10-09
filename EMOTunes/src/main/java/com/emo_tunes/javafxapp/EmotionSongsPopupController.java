package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    @FXML private Button backButton, nextButton, closeButton;

    private int limit = 20;         // number of songs per page
    private int offset = 0;
    private String currentEmotion;
    private List<SongInfo> allSongs;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BACKEND_URL = "http://localhost:8080/recommend/songs";


    @FXML
    public void initialize() {
        closeButton.setOnAction(e -> closePopup());
        nextButton.setOnAction(e -> showNextPage());
        backButton.setOnAction(e -> showPreviousPage());

        backButton.setDisable(true);  // initially no previous page
        nextButton.setDisable(true);  // until songs are loaded
    }



    /**
     * Show the popup and fetch songs for a given emotion.
     */
    public void showPopup(String emotion) {
        currentEmotion = emotion;
        songsContainer.getChildren().clear();
        popupTitle.setText("Fetching songs for " + emotion + "...");
        popupRoot.setVisible(true);
        popupRoot.setOpacity(1);

        closeButton.setDisable(true);  // disable during fetch
        fetchSongs(emotion);
    }


    private VBox mainContentPane;

    public void setEmotionsContainer(VBox container) {
        this.mainContentPane = container;
    }

    /**
     * Close the popup with a fade animation.
     */
    public void closePopup() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), popupRoot);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            popupRoot.setVisible(false);
            if (mainContentPane != null) {
                mainContentPane.setVisible(true);
            }
        });
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
        }
        int userId = currentUser.getUserId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                final String finalUrl = BACKEND_URL + "?emotion=" + emo + "&userId=" + userId;
                HttpURLConnection conn = (HttpURLConnection) new URL(finalUrl).openConnection();
                conn.setRequestMethod("GET");
                backButton.setDisable(true);
                nextButton.setDisable(true);
                int status = conn.getResponseCode();
                InputStream is = status == 200 ? conn.getInputStream() : conn.getErrorStream();

                // Parse backend response as JSON
                var node = OBJECT_MAPPER.readTree(is);

                if (status == 200) {
                    // Successful response: list of songs
                    allSongs = OBJECT_MAPPER.convertValue(node, new TypeReference<List<SongInfo>>() {});
                    // Show the songs on UI thread

                } else {
                    // Error response: has a "message" field
                    String errorMsg = node.has("message") ? node.get("message").asText() : "Unknown error";
                    allSongs = null;
                    showErrorMessage(errorMsg); // Display the message in the UI
                }
                Platform.runLater(() -> {
                    closeButton.setDisable(false);  // enable after fetch
                    showPage();  // or showErrorMessage() if error
                });
                return null;
            }
        };

        new Thread(task).start();

    }


    private void showErrorMessage(String msg) {
        Platform.runLater(() -> {
            songsContainer.getChildren().clear();
            Label errorLabel = new Label(msg);
            errorLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 16px; -fx-font-weight: bold;");
            errorLabel.setWrapText(true);
            errorLabel.setMaxWidth(songsContainer.getWidth());
            songsContainer.getChildren().add(errorLabel);

            backButton.setDisable(true);
            nextButton.setDisable(true);
        });
    }


    /**
     * Populate songs inside the VBox.
     */
    private void populateSongs(List<SongInfo> songs, String emotion) {
        Platform.runLater(() -> {
            songsContainer.getChildren().clear();
            popupTitle.setText("Songs for " + emotion.toLowerCase() + "...");

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
        card.setPrefWidth(400);

        // Song cover
        ImageView coverView = new ImageView();
        coverView.setFitWidth(60);
        coverView.setFitHeight(60);
        coverView.setPreserveRatio(true);

        // Default placeholder image if song cover not available
        String coverURL = song.getCoverURL();
        try {
            Image cover;
            if (coverURL != null && !coverURL.isEmpty()) {
                cover = new Image(coverURL, true);
            } else {
                // Use a local placeholder image from resources
                cover = new Image(getClass().getResource("/com/emo_tunes/javafxapp/images/placeholder.png").toExternalForm());
            }
            coverView.setImage(cover);
        } catch (Exception e) {
            System.out.println("Cover not loaded for: " + song.getSongName());
        }

        // Song title & artist
        VBox infoBox = new VBox(5);
        Label titleLabel = new Label(song.getSongName() != null ? song.getSongName() : "Unknown Title");
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16;");

        Label artistLabel = new Label(song.getArtistName() != null ? song.getArtistName() : "Unknown Artist");
        artistLabel.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 12;");

        infoBox.getChildren().addAll(titleLabel, artistLabel);
        card.getChildren().addAll(coverView, infoBox);

        return card;
    }

    private void showPage() {
        Platform.runLater(() -> {  // ensure UI updates happen on the JavaFX thread
            songsContainer.getChildren().clear();
            popupTitle.setText("Songs for " + currentEmotion);

            if (allSongs == null || allSongs.isEmpty()) {
                Label noSongs = new Label("No songs found for \"" + currentEmotion + "\".");
                noSongs.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 16px; -fx-font-weight: bold;");
                noSongs.setWrapText(true);
                noSongs.setMaxWidth(songsContainer.getWidth());

                songsContainer.getChildren().add(noSongs);

                backButton.setDisable(true);
                nextButton.setDisable(true);
                return;
            }

            int end = Math.min(offset + limit, allSongs.size());
            List<SongInfo> page = allSongs.subList(offset, end);

            for (SongInfo song : page) {
                HBox card = createSongCard(song);
                songsContainer.getChildren().add(card);
            }

            backButton.setDisable(offset == 0);
            nextButton.setDisable(end >= allSongs.size());
        });
    }


    private void showNextPage() {
        if (offset + limit < allSongs.size()) {
            offset += limit;
            showPage();
        }
    }

    private void showPreviousPage() {
        if (offset - limit >= 0) {
            offset -= limit;
        } else {
            offset = 0;
        }
        showPage();
    }


}
