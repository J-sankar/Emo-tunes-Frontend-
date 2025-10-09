module com.emo_tunes.javafxapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires javafx.graphics;

    opens com.emo_tunes.javafxapp to javafx.fxml;
    exports com.emo_tunes.javafxapp;
}
