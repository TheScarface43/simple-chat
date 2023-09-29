package com.example.simplechat;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.WindowEvent;

import java.net.Socket;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import static com.example.simplechat.MessageType.SERVER;
import static com.example.simplechat.RoleType.HOST;
import static com.example.simplechat.SimpleChat.stage;

public class ChatController implements Initializable {
    @FXML
    private VBox vBox_messages;
    @FXML
    private ScrollPane scrollPane_messages;
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

        //chat auto-scrolling
        vBox_messages.heightProperty().addListener((observableValue, oldValue, newValue) -> scrollPane_messages.setVvalue((Double) newValue));
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
    @FXML
    private void onClearButtonClick() {
        clearChatWindow();
    }

    private void sendMessage() {
        String messageToSend = textField_message.getText();

        if(messageToSend.length() > 5000) {
            receiveMessage("Messages cannot be longer than 5000 characters.", SERVER);
            return;
        }

        textField_message.clear();

        if(messageToSend.isBlank()) {
            return;
        }

        client.sendMessage(messageToSend);
    }

    public void receiveMessage(TextMessage message) {
        HBox messageContainer = createNewHBoxContainer(message);
        Platform.runLater(() -> vBox_messages.getChildren().add(messageContainer));
    }

    public void updateUserList(ArrayList<User> listOfUsers) {
        Platform.runLater(() -> vBox_userList.getChildren().clear());
        for (User user : listOfUsers) {
            HBox usernameContainer = createNewHBoxContainer(user.nickname());
            usernameContainer.getStyleClass().add("text-chat");
            usernameContainer.getStyleClass().add("userlist-entry-chat");
            if(user.role().equals(HOST)) {
                usernameContainer.getStyleClass().add("userlist-entry-chat-host");
            }
            Platform.runLater(() -> vBox_userList.getChildren().add(usernameContainer));
        }
    }

    public void updateWindowTitle(String newTitle) {
        Platform.runLater(() -> SimpleChat.stage.setTitle("Simple Chat - " + newTitle));
    }

    private HBox createNewHBoxContainer(TextMessage message) {
        HBox container = new HBox();
        TextFlow textFlow = new TextFlow();

        if(message.getType() == MessageType.CHAT) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

            String timestampString = message.getTimestamp().format(dtf);
            String authorString = message.getAuthor().nickname();

            Text timestampText = new Text(timestampString);
            Text authorText = new Text(authorString);

            timestampText.getStyleClass().add("time-chat");
            authorText.getStyleClass().add("author-chat");

            textFlow.getChildren().add(timestampText);
            textFlow.getChildren().add(authorText);
        }

        String contentsString = message.getContents();
        Text contentsText = new Text(contentsString);
        contentsText.getStyleClass().add("content-chat");
        textFlow.getChildren().add(contentsText);

        container.getStyleClass().add("text-chat");
        container.getStyleClass().add("message-chat");

        container.getChildren().add(textFlow);

        if(message.getType() == MessageType.SERVER) {
            container.getStyleClass().add("server-chat");
        }

        return container;
    }

    public void clearChatWindow() {
        Platform.runLater(() -> vBox_messages.getChildren().clear());
    }

    public void disableChat() {
        receiveMessage("Disconnected.", SERVER);
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