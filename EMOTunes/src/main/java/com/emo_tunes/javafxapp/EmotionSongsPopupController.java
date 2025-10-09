package com.emo_tunes.javafxapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class EmotionSongsPopupController {

    @FXML
    private VBox popupRoot;
    @FXML
    private Label titleLabel;
    @FXML
    private ListView<String> songsListView;

    public VBox getRoot() {
        return popupRoot;
    }

    public void setSongs(String emotion, List<SongInfo> songs) {
        titleLabel.setText("Songs for " + emotion);
        songsListView.getItems().setAll(
                songs.stream().map(SongInfo::getSongName).toList()
        );
    }



}
