package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.layout.Pane;   // or StackPane/VBox depending on your mainContentPane
import java.io.IOException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

// If you are passing user info
import com.emo_tunes.javafxapp.UserInfo;

public class LandingPageController {

    @FXML private ImageView appLogo;
    @FXML private Label profileIcon;
    @FXML private Label profileName;
    @FXML private TextField searchField;

    @FXML private VBox happyBox;
    @FXML private VBox loveBox;
    @FXML private VBox upliftBox;
    @FXML private VBox sadBox;
    @FXML private VBox rageBox;
    @FXML private VBox container;
    @FXML private VBox insightBox;
    @FXML private Label sidebarPlaylists;

    @FXML private Label sidebarEmolists;
    @FXML private Label sidebarLogout;
    @FXML private VBox emotionPopup;
    @FXML private Label popupTitle;
    @FXML private ListView<String> popupListView;
    @FXML private Label closePopup;

    @FXML private Button searchButton;
private EmotionSongsPopupController emotionPopupController;
    @FXML
    private StackPane mainContentPane;

    private UserInfo userInfo;
    @FXML
    private Label titleLabel;
    @FXML
    private Label insightTitle;

    @FXML
    private Label insightSubtitle;// your #titleLabel from FXML

    @FXML
    private AnchorPane resultsPlaceholder;

    private final String[] texts = {
            "Welcome to Emotunes",
            "How do you feel today?",
            "Tell us your mood",
            "Discover songs for your vibe"
    };

    private final List<String> titles = List.of(
            "How are you feeling today?",
            "Pick your current mood",
            "What's your vibe right now?",
            "Feeling happy or sad?",
            "Need some uplifting tunes?"
    );

    private final List<String> subtitles = List.of(
            "Select a mood and we’ll match the perfect tunes!",
            "Let music guide your emotions.",
            "Your mood, your playlist!",
            "Find songs that resonate with your feelings.",
            "Discover tracks to lift your spirits."
    );
    private int currentIndex = 0;
    private final Random random = new Random();

    private int limit = 20;
    private int offset = 0;
    private String lastQuery = "";
    private SongResultsViewController songResultsController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String BACKEND_URL = "http://localhost:8080/search/song";


    public void initialize() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/SongResultsView.fxml"));
            Parent resultsView = loader.load();
            songResultsController = loader.getController();
            resultsPlaceholder.getChildren().add(resultsView);
            AnchorPane.setTopAnchor(resultsView, 0.0);
            AnchorPane.setBottomAnchor(resultsView, 0.0);
            AnchorPane.setLeftAnchor(resultsView, 0.0);
            AnchorPane.setRightAnchor(resultsView, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/EmotionSongsPopup.fxml"));
            VBox popup = loader.load();
            emotionPopupController = loader.getController();

            // Add to main pane
            mainContentPane.getChildren().add(popup);
            popup.setVisible(false); // hide initially
        } catch (IOException e) {
            e.printStackTrace();
        }
        sidebarPlaylists.setText("Playlists");

        // Reset to original playlist click handler
        sidebarPlaylists.setOnMouseClicked(event -> handleSidebarPlaylistsClick());

        // Load App Logo safely
        closePopup.setOnMouseClicked(e -> closePopup());

        searchField.setOnAction(e -> fetchSongs(true));
        searchButton.setOnAction(e -> fetchSongs(true));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), titleLabel);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), titleLabel);

        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            fadeOut.play();
            fadeOut.setOnFinished(evt -> {
                currentIndex = (currentIndex + 1) % texts.length;
                titleLabel.setText(texts[currentIndex]);
                fadeIn.play();
            });
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        animateInsightTextSmooth();
        try {
            Image logo = new Image(getClass().getResource("/com/emo_tunes/javafxapp/logo.png").toExternalForm());
            appLogo.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found.");
        }

        // Animate sidebar labels
        addSidebarHover(sidebarPlaylists);
        addSidebarHover(sidebarEmolists);
        addSidebarHover(sidebarLogout);

        // Animate title and insight text
        animateTitleText();
        animateInsightTextSmooth();

        // Pagination buttons
        // Back button
        songResultsController.backButton.setOnAction(e -> {
            if (offset >= limit) {
                offset -= limit;
                fetchSongs(false); // fetch previous page
            } else {
                // Edge case: first page, go back to emotion view
                resultsPlaceholder.setVisible(false);
                resultsPlaceholder.setOpacity(0);
                container.setVisible(true);
            }
        });

        songResultsController.nextButton.setOnAction(e -> {
            offset += limit;
            fetchSongs(false);
        });
    }
    // ✅ Fetch songs with optional reset for new search
    private void fetchSongs(boolean resetOffset) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        if (resetOffset) offset = 0;
        lastQuery = query;

        UserInfo currentUser = SessionManager.getInstance().getUser();
        if (currentUser == null) {
            System.out.println("User not logged in!");
            return;
        }
        int userId = currentUser.getUserId();

        Task<List<SongInfo>> fetchTask = new Task<>() {
            @Override
            protected List<SongInfo> call() throws Exception {
                String apiUrl = BACKEND_URL + "?query=" + query.replace(" ", "%20") +
                        "&userId=" + userId + "&limit=" + limit + "&offset=" + offset;
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                try (InputStream inputStream = conn.getInputStream()) {
                    return objectMapper.readValue(inputStream, new TypeReference<>() {});
                }
            }
        };

        fetchTask.setOnSucceeded(e -> {
            List<SongInfo> songs = fetchTask.getValue();
            songResultsController.clearResults();
            for (SongInfo song : songs) songResultsController.addSongCard(song);

            // Hide the emotions container
            container.setVisible(false);

            // Show results placeholder with fade-in
            resultsPlaceholder.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), resultsPlaceholder);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });


        fetchTask.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        new Thread(fetchTask).start();
    }

    private void fetchSongsWithLastQuery() {
        if (!lastQuery.isEmpty()) {
            searchField.setText(lastQuery);
            fetchSongs(false);
        }
    }
    private void showEmotionsAgain() {
        container.setVisible(true);
        resultsPlaceholder.setVisible(false);
        resultsPlaceholder.setOpacity(0);
    }
    private void animateTitleText() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), titleLabel);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), titleLabel);

        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            fadeOut.play();
            fadeOut.setOnFinished(evt -> {
                currentIndex = (currentIndex + 1) % texts.length;
                titleLabel.setText(texts[currentIndex]);
                fadeIn.play();
            });
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void animateInsightTextSmooth() {
        FadeTransition fadeOutTitle = new FadeTransition(Duration.seconds(0.5), insightTitle);
        FadeTransition fadeOutSubtitle = new FadeTransition(Duration.seconds(0.5), insightSubtitle);

        fadeOutTitle.setFromValue(1.0);
        fadeOutTitle.setToValue(0.0);
        fadeOutSubtitle.setFromValue(1.0);
        fadeOutSubtitle.setToValue(0.0);

        fadeOutTitle.setOnFinished(event -> {
            int titleIndex = random.nextInt(titles.size());
            int subtitleIndex = random.nextInt(subtitles.size());

            insightTitle.setText(titles.get(titleIndex));
            insightSubtitle.setText(subtitles.get(subtitleIndex));

            insightSubtitle.setWrapText(true);
            insightTitle.setWrapText(true);

            // Force layout update
            insightBox.applyCss();
            insightBox.layout();

            // Fade-in new text
            FadeTransition fadeInTitle = new FadeTransition(Duration.seconds(0.5), insightTitle);
            FadeTransition fadeInSubtitle = new FadeTransition(Duration.seconds(0.5), insightSubtitle);
            fadeInTitle.setFromValue(0.0);
            fadeInTitle.setToValue(1.0);
            fadeInSubtitle.setFromValue(0.0);
            fadeInSubtitle.setToValue(1.0);

            fadeInTitle.play();
            fadeInSubtitle.play();

            // Pause before next change
            PauseTransition pause = new PauseTransition(Duration.seconds(4));
            pause.setOnFinished(e -> animateInsightTextSmooth());
            pause.play();
        });

        fadeOutTitle.play();
        fadeOutSubtitle.play();
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        if(userInfo != null && userInfo.getUsername() != null) {
            String firstName = userInfo.getUsername().split(" ")[0];
            profileName.setText(firstName);
            profileIcon.setText(firstName.substring(0,1).toUpperCase());
        } else {
            profileName.setText("User");
            profileIcon.setText("U");
        }
    }

    // Hover animation for emotion boxes
    public void handleHoverEnter(MouseEvent event) {
        VBox box = (VBox) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), box);
        st.setToX(1.1);
        st.setToY(1.1);
        st.play();
    }

    public void handleHoverExit(MouseEvent event) {
        VBox box = (VBox) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), box);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
    // Hover effect for sidebar labels
    private void addSidebarHover(Label label) {
        label.setOnMouseEntered(e -> label.setStyle("-fx-text-fill: #FFD700; -fx-font-family: 'Segoe UI'; -fx-font-size: 18px;"));
        label.setOnMouseExited(e -> label.setStyle("-fx-text-fill: WHITE; -fx-font-family: 'Segoe UI'; -fx-font-size: 18px;"));
    }

    @FXML
    private void handleSidebarEmoListsClick() {

        try {
            // Load the EmoListPage FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EmoListPage.fxml"));
            Parent emoListPage = loader.load();

            // Optional: pass user info to controller
            EmoListPageController controller = loader.getController();
            controller.setUserInfo(userInfo); // if you have a userInfo field

            // Replace main content with EmoListPage
            mainContentPane.getChildren().setAll(emoListPage);

            // Change sidebar label to Home
            sidebarEmolists.setText("Home");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Fixed emotion popup


    @FXML
    private void handleEmotionClick(MouseEvent event) {
        VBox clickedBox = (VBox) event.getSource();
        String emotion = ((Label) clickedBox.getChildren().get(0)).getText();

        BackendService.getSongsByEmotion(emotion, songs -> {
            Platform.runLater(() -> {
                if (emotionPopupController != null) {
                    emotionPopupController.setSongs(emotion, songs);

                    // Show popup with fade-in
                    VBox popupRoot = emotionPopupController.getRoot();
                    popupRoot.setVisible(true);
                    popupRoot.toFront();

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), popupRoot);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                }
            });
        });
    }


    @FXML
    private void handleSidebarPlaylistsClick() {
        if (sidebarPlaylists.getText().equals("Playlists")) {
            // Load PlaylistPage.fxml
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/PlaylistPage.fxml"));
                Parent playlistView = loader.load();

                PlaylistPageController controller = loader.getController();


                mainContentPane.getChildren().setAll(playlistView);

                // Change sidebar label to "Home"
                sidebarPlaylists.setText("Home");

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Reload LandingPage.fxml to simulate "Home"
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/LandingPage.fxml"));
                Parent landingPage = loader.load();

                // Get controller and pass user info
                LandingPageController controller = loader.getController();
                controller.setUserInfo(userInfo);

                Scene scene = sidebarPlaylists.getScene(); // get current scene
                ((VBox) sidebarPlaylists.getParent()).getScene().setRoot(landingPage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showEmotionPopup(String emotion, List<String> songs) {
        popupTitle.setText("Songs for " + emotion);
        popupListView.getItems().setAll(songs);

        // Make popup visible and bring to front
        emotionPopup.setVisible(true);
        emotionPopup.toFront();

        // Fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), emotionPopup);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    @FXML
    private void closePopup() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), emotionPopup);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> emotionPopup.setVisible(false));
        fadeOut.play();
    }


}
