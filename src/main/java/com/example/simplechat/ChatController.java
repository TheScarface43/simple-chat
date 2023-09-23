package com.example.simplechat;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.WindowEvent;

import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static com.example.simplechat.SimpleChat.stage;

public class ChatController implements Initializable {
    @FXML
    private VBox vBox_messages;
    @FXML
    private VBox vBox_userList;
    @FXML
    private TextField textField_message;
    @FXML
    private Button button_sendMessage;

    private Server server;
    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UserCredentials userCredentials = UserCredentials.getInstance();
        String nickname = userCredentials.getNickname();
        String ip = userCredentials.getIpAddress();
        int port = userCredentials.getPort();
        Socket socket = userCredentials.getClientSocket();
        server = userCredentials.getServer();

        client = new Client(this, nickname, ip, port, socket);
        Thread clientThread = new Thread(client);
        clientThread.start();

        SimpleChat.stage.setOnCloseRequest(shutdownEverything);
    }

    private EventHandler<WindowEvent> shutdownEverything = event -> {
        if(server != null) {
            server.shutdown();
        } else {
            client.disconnect();
        }
    };

    @FXML
    private void onSendButtonClick() {
        sendMessage();
    }
    @FXML
    private void onMessageTextFieldAction() {
        sendMessage();
    }

    private void sendMessage() {
        String messageToSend = textField_message.getText();
        textField_message.clear();
        if(messageToSend.isBlank()) {
            return;
        }
        client.sendMessage(messageToSend);
    }

    public void receiveMessage(String receivedMessage) {
        HBox messageContainer = createNewHBoxContainer(receivedMessage);
        messageContainer.getStyleClass().add("text-chat");
        messageContainer.getStyleClass().add("message-chat");
        Platform.runLater(() -> vBox_messages.getChildren().add(messageContainer));
    }

    public void updateUserList(ArrayList<String> listOfUsernames) {
        Platform.runLater(() -> vBox_userList.getChildren().clear());
        for (String user : listOfUsernames) {
            HBox usernameContainer = createNewHBoxContainer(user);
            usernameContainer.getStyleClass().add("text-chat");
            usernameContainer.getStyleClass().add("userlist-entry-chat");
            Platform.runLater(() -> vBox_userList.getChildren().add(usernameContainer));
        }
    }

    private HBox createNewHBoxContainer(String contents) {
        HBox container = new HBox();
        Text text = new Text(contents);
        TextFlow textFlow = new TextFlow(text);
        container.getChildren().add(textFlow);
        return container;
    }

    public void disableChat() {
        receiveMessage("Disconnected.");
        textField_message.setDisable(true);
        button_sendMessage.setDisable(true);
        if(server != null) {
            server.shutdown();
        }
    }

    public void closeChatWindow() {
        Platform.runLater(() -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
    }
}