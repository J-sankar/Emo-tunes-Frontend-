package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class MusicAppLoginController {

    @FXML
    private Button spotifyButton;

    @FXML
    private ImageView appLogo;

    private final String backendLoginUrl = "http://localhost:8080/spotify/login";
    private final String callbackUrl = "http://127.0.0.1:8080/callback";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResource("/com/emo_tunes/javafxapp/logo.png").toExternalForm());
            appLogo.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found!");
        }

//       spotifyButton.setOnAction(e -> openSpotifyLogin());
      UserInfo userInfo = new UserInfo(1,"Tester","tester@gmail.com");
      spotifyButton.setOnAction(e -> navigateToLanding(userInfo));
      SessionManager.getInstance().setUser(userInfo);

    }

    private void openSpotifyLogin() {
        Stage webStage = new Stage();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        webEngine.load(backendLoginUrl);

        // Listen for callback URL
        webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc.startsWith(callbackUrl)) {
                webEngine.getLoadWorker().stateProperty().addListener((o, oldState, newState) -> {
                    switch (newState) {
                        case SUCCEEDED -> {
                            try {
                                // Now document.body.innerText is JSON
                                String json = (String) webEngine.executeScript("document.body.innerText");
                                UserInfo userInfo = objectMapper.readValue(json, UserInfo.class);
                                SessionManager.getInstance().setUser(userInfo);
                                webStage.close();
                                Platform.runLater(() -> navigateToLanding(userInfo));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            }
        });


        Scene scene = new Scene(webView, 800, 600);
        webStage.setScene(scene);
        webStage.setTitle("Spotify Login");
        webStage.show();
    }



    @FXML
    private void navigateToLanding(UserInfo userInfo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emo_tunes/javafxapp/LandingPage.fxml"));
            BorderPane root = loader.load();

            LandingPageController controller = loader.getController();
            controller.setUserInfo(userInfo);  // pass user info

            Stage stage = (Stage) spotifyButton.getScene().getWindow();
            Scene scene = new Scene(root, 700, 500);
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
