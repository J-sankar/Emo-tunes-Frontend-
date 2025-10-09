package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

// If you are passing user info
import com.emo_tunes.javafxapp.UserInfo;

public class LandingPageController {

    /* ---- FXML fields ---- */
    @FXML private ImageView appLogo;
    @FXML private Label profileIcon, profileName, titleLabel, insightTitle, insightSubtitle;
    @FXML private TextField searchField;
    @FXML private VBox happyBox, loveBox, upliftBox, sadBox, rageBox, container, insightBox;
    @FXML private StackPane mainContentPane;
    @FXML private AnchorPane resultsPlaceholder;
    @FXML private Label sidebarPlaylists, sidebarEmolists, sidebarLogout;
    @FXML private Button searchButton;
    @FXML private Label popupTitle;
    @FXML private ListView<String> popupListView;
    @FXML private Label closePopup;
    @FXML private VBox emotionPopup;          // kept only for the legacy popup// already from FXML
    @FXML private VBox songsContainer; // add this in FXML popup VBox for song cards

    @FXML private VBox emotionsContainer;

    /* ---- Runtime helpers ---- */
    private EmotionSongsPopupController emotionPopupController;
    private SongResultsViewController songResultsController;
    private UserInfo userInfo;

    private static final Random RANDOM = new Random();
    private static final String[] TEXTS = {
            "Welcome to Emotunes",
            "How do you feel today?",
            "Tell us your mood",
            "Discover songs for your vibe"
    };

    private static final List<String> TITLES  = List.of(
            "How are you feeling today?",
            "Pick your current mood",
            "What's your vibe right now?",
            "Feeling happy or sad?",
            "Need some uplifting tunes?"
    );
    private static final List<String> SUBTITLES = List.of(
            "Select a mood and we’ll match the perfect tunes!",
            "Let music guide your emotions.",
            "Your mood, your playlist!",
            "Find songs that resonate with your feelings.",
            "Discover tracks to lift your spirits."
    );

    private int currentIndex = 0;
    private int limit   = 20;
    private int offset  = 0;
    private String lastQuery = "";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BACKEND_URL = "http://localhost:8080/search/song";
    @FXML
    public void initialize() {
        loadResultsView();
        loadEmotionPopup();         // first time setup

        sidebarPlaylists.setText("Playlists");
        sidebarPlaylists.setOnMouseClicked(e -> handleSidebarPlaylistsClick());


        searchField.setOnAction(e -> fetchSongs(true));
        searchButton.setOnAction(e -> fetchSongs(true));

        setupTitleAnimation();
        loadAppLogo();
        setupSidebarHover();
        animateInsightTextSmooth();

        /* Pagination handlers */
        songResultsController.backButton.setOnAction(e -> handleBackButton());
        songResultsController.nextButton.setOnAction(e -> handleNextButton());
    }
    /* -------------  View loaders ------------- */
    private void loadResultsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/emo_tunes/javafxapp/SongResultsView.fxml"));
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
    }

    private void loadEmotionPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/emo_tunes/javafxapp/EmotionSongsPopup.fxml"));
            VBox popupRoot = loader.load();
            emotionPopupController = loader.getController();

            mainContentPane.getChildren().add(popupRoot);
            popupRoot.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ----------  App logo  ---------- */
    private void loadAppLogo() {
        try {
            Image logo = new Image(
                    getClass().getResource("/com/emo_tunes/javafxapp/logo.png").toExternalForm());
            appLogo.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found.");
        }
    }

    /* ----------  Sidebar hover  ---------- */
    private void setupSidebarHover() {
        addSidebarHover(sidebarPlaylists);
        addSidebarHover(sidebarEmolists);
        addSidebarHover(sidebarLogout);
    }
    @FXML
    private void handleEmoListButton() {

    }

    @FXML
    private void handleEmotionClick(MouseEvent event) {
        VBox clickedBox = (VBox) event.getSource();
        String emotion = "";

        if (clickedBox == happyBox) emotion = "Happy";
        else if (clickedBox == loveBox) emotion = "Love";
        else if (clickedBox == upliftBox) emotion = "Uplift";
        else if (clickedBox == sadBox) emotion = "Sad";
        else if (clickedBox == rageBox) emotion = "Rage";

        if (!emotion.isEmpty() && emotionPopupController != null) {
            emotionPopupController.showPopup(emotion);
        }
    }
    private void addSidebarHover(Label label) {
        label.setOnMouseEntered(e ->
                label.setStyle("-fx-text-fill:#FFD700; -fx-font-family:'Segoe UI'; -fx-font-size:18px;"));
        label.setOnMouseExited(e ->
                label.setStyle("-fx-text-fill:WHITE; -fx-font-family:'Segoe UI'; -fx-font-size:18px;"));
    }
    /* ----------  Title animation  ---------- */
    private void setupTitleAnimation() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), titleLabel);
        FadeTransition fadeIn  = new FadeTransition(Duration.millis(500), titleLabel);

        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    fadeOut.play();
                    fadeOut.setOnFinished(ev -> {
                        currentIndex = (currentIndex + 1) % TEXTS.length;
                        titleLabel.setText(TEXTS[currentIndex]);
                        fadeIn.play();
                    });
                }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /* ----------  Insight text  ---------- */
    private void animateInsightTextSmooth() {
        FadeTransition fadeOutTitle = new FadeTransition(Duration.seconds(0.5), insightTitle);
        FadeTransition fadeOutSubtitle = new FadeTransition(Duration.seconds(0.5), insightSubtitle);

        fadeOutTitle.setFromValue(1.0);
        fadeOutTitle.setToValue(0.0);
        fadeOutSubtitle.setFromValue(1.0);
        fadeOutSubtitle.setToValue(0.0);

        fadeOutTitle.setOnFinished(ev -> {
            int titleIdx  = RANDOM.nextInt(TITLES.size());
            int subtitleIdx = RANDOM.nextInt(SUBTITLES.size());

            insightTitle.setText(TITLES.get(titleIdx));
            insightSubtitle.setText(SUBTITLES.get(subtitleIdx));

            insightSubtitle.setWrapText(true);
            insightTitle.setWrapText(true);
            insightBox.applyCss();          // force layout
            insightBox.layout();

            FadeTransition fadeInTitle  = new FadeTransition(Duration.seconds(0.5), insightTitle);
            FadeTransition fadeInSubtitle = new FadeTransition(Duration.seconds(0.5), insightSubtitle);
            fadeInTitle.setFromValue(0.0);
            fadeInTitle.setToValue(1.0);
            fadeInSubtitle.setFromValue(0.0);
            fadeInSubtitle.setToValue(1.0);

            fadeInTitle.play();
            fadeInSubtitle.play();

            PauseTransition pause = new PauseTransition(Duration.seconds(4));
            pause.setOnFinished(e -> animateInsightTextSmooth());
            pause.play();
        });

        fadeOutTitle.play();
        fadeOutSubtitle.play();
    }
    /* ----------  Emotion pop‑up  ---------- */
    private void closeEmotionPopup() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), emotionPopup);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> emotionPopup.setVisible(false));
        ft.play();

        // Show main emotions container again
        container.setVisible(true);
    }

    // When opening the popup
    private void openEmotionPopup(String emotion) {
        container.setVisible(false);       // hide main emotions
        emotionPopup.setVisible(true);
        emotionPopup.setOpacity(0);

        FadeTransition ft = new FadeTransition(Duration.millis(300), emotionPopup);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        // You can populate your popup list here
    }


    public void showEmotionsPage() {
        emotionsContainer.setVisible(true);
    }

    /* ----------  Hover effects  ---------- */
    @FXML
    private void handleHoverEnter(MouseEvent event) {
        Node node = (Node) event.getSource();          // `VBox` is a subclass of `Node`
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);

        st.setToX(1.1);
        st.setToY(1.1);
        st.play();
    }

    @FXML
    private void handleHoverExit(MouseEvent event) {
        Node node = (Node) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);

        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }


    @FXML
    private void handleSidebarPlaylistsClick() {
        if (sidebarPlaylists.getText().equals("Playlists")) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/emo_tunes/javafxapp/PlaylistPage.fxml"));
                Parent playlistView = loader.load();

                mainContentPane.getChildren().setAll(playlistView);
                sidebarPlaylists.setText("Home");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/emo_tunes/javafxapp/LandingPage.fxml"));
                Parent landingPage = loader.load();

                LandingPageController controller = loader.getController();
                controller.setUserInfo(userInfo);

                ((VBox) sidebarPlaylists.getParent()).getScene().setRoot(landingPage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSidebarEmoListsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("EmoListPage.fxml"));
            Parent root = loader.load();

            // Get the controller instance
            EmoListPageController controller = loader.getController();

            // Trigger fetching
            controller.fetchEmoListsForCurrentUser();

            // Replace main content (or use StackPane, etc.)
            mainContentPane.getChildren().setAll(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setUserInfo(UserInfo user) {
        this.userInfo = user;
        if (user != null && user.getUsername() != null) {
            String firstName = user.getUsername().split(" ")[0];
            profileName.setText(firstName);
            profileIcon.setText(firstName.substring(0, 1).toUpperCase());
        } else {
            profileName.setText("User");
            profileIcon.setText("U");
        }
    }
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

        Task<List<SongInfo>> task = new Task<>() {
            @Override
            protected List<SongInfo> call() throws Exception {
                String url = BACKEND_URL + "?query=" + query.replace(" ", "%20")
                        + "&userId=" + userId + "&limit=" + limit + "&offset=" + offset;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                try (InputStream is = conn.getInputStream()) {
                    return OBJECT_MAPPER.readValue(is, new TypeReference<>() {});
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<SongInfo> songs = task.getValue();
            songResultsController.clearResults();
            songs.forEach(songResultsController::addSongCard);

            container.setVisible(false);
            resultsPlaceholder.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(400), resultsPlaceholder);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });

        task.setOnFailed(e -> e.getSource().getException().printStackTrace());
        new Thread(task).start();
    }

    private void handleBackButton() {
        if (offset >= limit) {
            offset -= limit;
            fetchSongs(false);
        } else {
            resultsPlaceholder.setVisible(false);
            resultsPlaceholder.setOpacity(0);
            container.setVisible(true);
        }
    }

    private void handleNextButton() {
        offset += limit;
        fetchSongs(false);
    }
}
