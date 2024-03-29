package com.example.simplechat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SimpleChat extends Application {

    public static Stage stage;
    public static Scene scene;
    public static final String VERSION = "1.0.0";

    @Override
    public void start(Stage stage) throws IOException {
        SimpleChat.stage = stage;
        scene = new Scene(loadFXML("start-view"));
        stage.setTitle("Simple Chat");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        Platform.runLater(() -> SimpleChat.stage.sizeToScene());
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleChat.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}