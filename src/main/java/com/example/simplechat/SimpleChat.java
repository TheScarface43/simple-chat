package com.example.simplechat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SimpleChat extends Application {

    public static Stage stage;
    public static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        SimpleChat.stage = stage;
        scene = new Scene(loadFXML("start-view"));
        stage.setTitle("Simple Chat");
        stage.setScene(scene);
        String cssPath = SimpleChat.class.getResource("style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        SimpleChat.stage.sizeToScene();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleChat.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}