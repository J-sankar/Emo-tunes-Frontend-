package com.emo_tunes.javafxapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.geometry.Pos;

public class SongResultsViewController {

    @FXML private VBox resultsContainer;
    @FXML public Button backButton;
    @FXML public Button nextButton;
    //    To clear the results
    public void clearResults() {
        resultsContainer.getChildren().clear();
    }
    //    Adding Song card to the Scroll pane
    public void addSongCard(SongInfo song) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-padding: 10;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            """);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        if (song.getCoverURL() != null && !song.getCoverURL().isEmpty()) {
            imageView.setImage(new Image(song.getCoverURL(), true));
        }

        Label title = new Label(song.getSongName());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label artist = new Label(song.getArtistName());
        artist.setStyle("-fx-text-fill: #aaaaaa;");

        card.getChildren().addAll(imageView, title, artist);
        resultsContainer.getChildren().add(card);
    }
}