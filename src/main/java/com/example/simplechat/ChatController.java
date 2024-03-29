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

import static com.example.simplechat.RoleType.*;
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
    private User localUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UserCredentials userCredentials = UserCredentials.getInstance();
        String nickname = userCredentials.getNickname();
        String ip = userCredentials.getIpAddress();
        int port = userCredentials.getPort();
        Socket socket = userCredentials.getClientSocket();
        server = userCredentials.getServer();
        String color = userCredentials.getColor();

        client = new Client(this, nickname, ip, port, socket, color);
        Thread clientThread = new Thread(client);
        clientThread.start();

        localUser = new User("LOCAL", LOCAL, "#DDDDDD");

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
            receiveMessage("Messages cannot be longer than 5000 characters.");
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
    public void receiveMessage(String string) {
        TextMessage message = new TextMessage(localUser, MessageType.LOCAL, string);
        receiveMessage(message);
    }

    public void updateUserList(UserListMessage userListMessage) {
        if(userListMessage.author.getRole() != RoleType.SERVER) {
            return;
        }

        ArrayList<User> listOfUsers = userListMessage.getContents();

        Platform.runLater(() -> vBox_userList.getChildren().clear());
        for (User user : listOfUsers) {
            HBox usernameContainer = createNewHBoxContainer(user);
            usernameContainer.getStyleClass().add("text-chat");
            usernameContainer.getStyleClass().add("userlist-entry-chat");
            if(user.getRole().equals(HOST)) {
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

            String timestampString = message.getTimestamp().format(dtf) + " ";
            String authorString = message.getAuthor().getNickname() + ": ";

            Text timestampText = new Text(timestampString);
            Text authorText = new Text(authorString);

            timestampText.getStyleClass().add("time-chat");
            authorText.getStyleClass().add("author-chat");

            authorText.setStyle("-fx-fill: " + message.getAuthor().getColor());

            textFlow.getChildren().add(timestampText);
            textFlow.getChildren().add(authorText);
        }

        String contentsString = message.getContents();
        Text contentsText = new Text(contentsString);
        contentsText.getStyleClass().add("content-chat");
        contentsText.setStyle("-fx-fill: " + message.getAuthor().getColor());
        textFlow.getChildren().add(contentsText);

        container.getStyleClass().add("text-chat");
        container.getStyleClass().add("message-chat");

        container.getChildren().add(textFlow);

        if(message.getType() == MessageType.SERVER) {
            container.getStyleClass().add("server-chat");
        }
        if(message.getType() == MessageType.LOCAL) {
            container.getStyleClass().add("local-chat");
        }

        return container;
    }
    private HBox createNewHBoxContainer(User user) {
        Text text = new Text(user.getNickname());
        text.setStyle("-fx-fill: " + user.getColor());
        TextFlow textFlow = new TextFlow(text);
        HBox container = new HBox(textFlow);

        return container;
    }

    public void clearChatWindow() {
        Platform.runLater(() -> vBox_messages.getChildren().clear());
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