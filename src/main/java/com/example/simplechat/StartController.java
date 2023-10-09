package com.example.simplechat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.*;
import java.util.ResourceBundle;

import org.apache.commons.validator.routines.InetAddressValidator;

import static com.example.simplechat.StartMessageType.ERROR;
import static com.example.simplechat.StartMessageType.INFO;

public class StartController implements Initializable {

    @FXML
    private TextField textField_nickname;
    @FXML
    private TextField textField_ipAddress;
    @FXML
    private TextField textField_port;
    @FXML
    private Label label_warning;
    @FXML
    private Label label_version;
    @FXML
    private ColorPicker colorPicker_start;
    private String nickname;
    private String ip;
    private int port;
    private boolean isTryingToConnect = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        label_version.setText("v" + SimpleChat.VERSION);
    }

    @FXML
    private void onHostButtonClick() {
        if(isTryingToConnect) {
            return;
        }

        isTryingToConnect = true;
        new Thread(() -> {
            if(validate(true))
            {
                Server server = tryHost();
                if (server != null) {
                    startChat(server);
                }
            }
            isTryingToConnect = false;
        }).start();
    }
    @FXML
    private void onJoinButtonClick() {
        if(isTryingToConnect) {
            return;
        }

        isTryingToConnect = true;
        new Thread(() -> {
            if(validate(false))
            {
                Socket socket = tryClient();
                if (socket != null) {
                    startChat(socket);
                }
            }
            isTryingToConnect = false;
        }).start();
    }

    private boolean validate(boolean isHost) {
        nickname = textField_nickname.getText();
        ip = textField_ipAddress.getText();
        String portString = textField_port.getText();

        StartMessageType type = ERROR;

        if(portString.isEmpty()) {
            port = 7063;
        } else {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                port = 0;
            }
        }

        if(nickname.length() > 32) {
            displayMessage("Nicknames cannot be longer than\n32 characters.", type);
            return false;
        }
        if(nickname.length() < 3) {
            displayMessage("Nicknames have to be at least\n3 characters long.", type);
            return false;
        }
        if(nickname.contains(" ")) {
            displayMessage("Nicknames cannot contain whitespace.", type);
            return false;
        }
        if(!ip.isBlank() && !ip.equals("localhost")) {
            if(!InetAddressValidator.getInstance().isValidInet4Address(ip)) {
                displayMessage("This is not a valid IPv4 address.", type);
                return false;
            }
        }
        if(port < 1024 || port > 65353) {
            displayMessage("Please enter a valid port number\n(1024 - 65353).", type);
            return false;
        }
        if(!isHost && ip.isBlank()) {
            displayMessage("Please specify an IP address.", type);
            return false;
        }
        return true;
    }

    private void displayMessage(String warningMessage, StartMessageType type) {
        String style;

        switch(type) {
            case INFO -> style = "label-start-info";
            case ERROR -> style = "label-start-error";
            default -> style = "label-start-error";
        }

        Platform.runLater(() -> {
            label_warning.setText(warningMessage);
            label_warning.getStyleClass().clear();
            label_warning.getStyleClass().add(style);
            label_warning.setVisible(true);
        });
    }

    private Socket tryClient() {
        displayMessage("Connecting...", INFO);

        Socket socket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(ip, port);

        try {
            socket.connect(socketAddress, 15000);
        } catch (IOException e) {
            e.printStackTrace();
            displayMessage("Unable to connect\nto " + ip + ":" + port + ".", ERROR);
            return null;
        }

        return socket;
    }

    private Server tryHost() {
        displayMessage("Attempting to host...", INFO);
        ServerSocket serverSocket;

        try {
            if (ip.isBlank()) {
                ip = "localhost";
                serverSocket = new ServerSocket(port);
            } else {
                InetAddress addr = InetAddress.getByName(ip);
                serverSocket = new ServerSocket(port, 50, addr);
            }
        } catch (IOException e) {
            displayMessage("Unable to host at " + ip + ":" + port + ".\nMaybe try a different port/IP?", ERROR);
            return null;
        }

        Server server = new Server(serverSocket);
        Thread serverThread = new Thread(server);
        serverThread.start();
        return server;
    }

    private void startChat(Server server) {
        Socket clientSocket = tryClient();
        startChat(clientSocket, server);
    }
    private void startChat(Socket clientSocket) {
        startChat(clientSocket, null);
    }
    private void startChat(Socket clientSocket, Server server) {
        UserCredentials userCredentials = UserCredentials.getInstance();
        userCredentials.setNickname(nickname);
        userCredentials.setIpAddress(ip);
        userCredentials.setPort(port);
        userCredentials.setClientSocket(clientSocket);
        if (server != null) {
            userCredentials.setServer(server);
        }
        userCredentials.setColor(colorPicker_start.getValue());

        try {
            SimpleChat.setRoot("chat-view");
        } catch (IOException e) {
            displayMessage("Error - could not start the chat window. :/", ERROR);
            try {
                if(server != null) {
                    server.shutdown();
                }
                clientSocket.close();
            } catch (IOException ex) {
                //Ignore
            }
        } finally {
            isTryingToConnect = false;
        }
    }
}
