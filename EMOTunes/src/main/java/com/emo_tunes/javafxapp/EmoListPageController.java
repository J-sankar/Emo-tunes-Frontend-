package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmoListPageController {

    @FXML
    private VBox emolistContainer;

    @FXML
    private Button createEmoListBtn;

    /** Fetch EmoLists for currently logged-in user */
    public void fetchEmoListsForCurrentUser() {
        UserInfo user = SessionManager.getInstance().getUser();
        if (user != null) {
            loadEmoListsFromBackend(user.getUserId());
        }
    }

    @FXML
    private void handleCreateEmoList() {
        Dialog<PlaylistInfo> dialog = new Dialog<>();
        dialog.setTitle("Create EmoList");
        dialog.setHeaderText("Enter details for your new EmoList");

        // ✅ Set a dialog pane with standard button area
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label nameLabel = new Label("EmoList Name:");
        TextField nameField = new TextField();

        Label emotionLabel = new Label("Emotion:");
        TextField emotionField = new TextField();

        content.getChildren().addAll(nameLabel, nameField, emotionLabel, emotionField);
        dialogPane.setContent(content);

        // ✅ Access the OK button (renaming it to "Create")
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Create");
        okButton.setDisable(true);

        // Enable only when name is filled
        nameField.textProperty().addListener((obs, oldVal, newVal) ->
                okButton.setDisable(newVal.trim().isEmpty())
        );

        // ✅ Convert result into PlaylistInfo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                PlaylistInfo emoList = new PlaylistInfo();
                emoList.setPlaylistName(nameField.getText().trim());
                emoList.setEmotion(emotionField.getText().trim());
                return emoList;
            }
            return null;
        });

        // ✅ Handle the result
        dialog.showAndWait().ifPresent(newEmoList -> {
            try {
                createEmoList(newEmoList); // your backend call
                showAlert(Alert.AlertType.INFORMATION, "Success", "EmoList created successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to create EmoList.");
            }
        });
    }

    // Optional helper for alerts
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    /** Add EmoList to UI and persist to backend */
    private void createEmoList(PlaylistInfo emoList) {
        // Ensure songs list is not null
        if (emoList.getSongs() == null) {
            emoList.setSongs(new ArrayList<>());
        }

        // Add UI card
        emolistContainer.getChildren().add(0, createEmoListCard(emoList));

        new Thread(() -> {
            try {
                UserInfo currentUser = SessionManager.getInstance().getUser();
                if (currentUser == null) return;

                int userId = currentUser.getUserId();

                Map<String, Object> payload = new HashMap<>();
                payload.put("name", emoList.getPlaylistName());
                payload.put("emotion", emoList.getEmotion());
                payload.put("songs", emoList.getSongs()); // safe now

                String jsonPayload = new ObjectMapper().writeValueAsString(payload);

                String urlStr = "http://localhost:8080/playlist/create?userId=" + userId;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.getOutputStream().write(jsonPayload.getBytes());

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    System.out.println("EmoList saved: " + emoList.getPlaylistName());
                    loadEmoListsFromBackend(userId);
                } else {
                    System.err.println("Failed to save EmoList. HTTP code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /** Load EmoLists from backend for a user */
    private void loadEmoListsFromBackend(int userId) {
        new Thread(() -> {
            try {
                String urlStr = "http://localhost:8080/playlist/user/emolist?userId=" + userId;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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
                    List<PlaylistInfo> lists = mapper.readValue(response.toString(), new TypeReference<List<PlaylistInfo>>() {});

                    javafx.application.Platform.runLater(() -> {
                        emolistContainer.getChildren().clear();

                        if (lists.isEmpty()) {
                            Label noListsLabel = new Label("No emotion playlists yet — create one to get started!");
                            noListsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: normal;");
                            emolistContainer.getChildren().add(noListsLabel);
                        } else {
                            for (PlaylistInfo list : lists) {
                                emolistContainer.getChildren().add(createEmoListCard(list));
                            }
                        }
                    });
                } else {
                    System.err.println("⚠️ HTTP Error: " + responseCode);
                    javafx.application.Platform.runLater(() -> {
                        emolistContainer.getChildren().clear();
                        Label errorLabel = new Label("No Emolists available.");
                        errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");
                        emolistContainer.getChildren().add(errorLabel);
                    });
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    emolistContainer.getChildren().clear();
                    Label errorLabel = new Label("Error loading emotion playlists. Please check your connection.");
                    emolistContainer.getChildren().add(errorLabel);
                });
            }
        }).start();
    }


    /** Create UI card for an EmoList */
    private HBox createEmoListCard(PlaylistInfo emoList) {
        Image cover = new Image("https://i.scdn.co/image/ab67616d0000b2732db7ff835f7a3d7ff7e6b6c9", true);
        ImageView coverView = new ImageView(cover);
        coverView.setFitWidth(80);
        coverView.setFitHeight(80);

        Label nameLabel = new Label(emoList.getPlaylistName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");

        Label emotionLabel = new Label("Mood: " + emoList.getEmotion());
        emotionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ddd;");

        VBox songsBox = new VBox(5);
        songsBox.setVisible(false);
        songsBox.setManaged(false);

        for (SongInfo song : emoList.getSongs()) {
            Label songLabel = new Label("♪ " + song.getSongName());
            songLabel.setStyle("-fx-text-fill: white;");
            songsBox.getChildren().add(songLabel);
        }

        Button addSongBtn = new Button("+ Add Song");
        addSongBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #89f7fe, #66a6ff);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 6 14; -fx-cursor: hand;"
        );

// Hover effect
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

// ✅ Main action (redirect to LandingPage + focus search bar)
        addSongBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/LandingPage.fxml"));
                Parent landingRoot = loader.load();

                LandingPageController landingController = loader.getController();
                landingController.focusOnSearchBar(); // highlight + focus

                Stage currentStage = (Stage) addSongBtn.getScene().getWindow();
                currentStage.getScene().setRoot(landingRoot);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        VBox rightBox = new VBox(nameLabel, emotionLabel, addSongBtn, songsBox);
        rightBox.setSpacing(5);
        rightBox.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(15, coverView, rightBox);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-background-radius: 15;");
        card.setEffect(new DropShadow(8, Color.BLACK));

        card.setOnMouseClicked(e -> {
            boolean visible = songsBox.isVisible();
            songsBox.setVisible(!visible);
            songsBox.setManaged(!visible);
        });

        return card;
    }

    /** EmoList model (no Color field) */


         // for Jackson



}
