package com.example.simplechat;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import org.apache.commons.validator.Validator;
import org.apache.commons.validator.routines.InetAddressValidator;

public class StartController {

    @FXML
    private TextField textField_nickname;
    @FXML
    private TextField textField_ipAddress;
    @FXML
    private TextField textField_port;
    @FXML
    private Label label_warning;
    private String nickname;
    private String ip;
    private int port;

    @FXML
    private void onHostButtonClick() throws IOException {
        if(validate(true)) {
            startChat(true);
        } else {
            label_warning.setVisible(true);
        }
    }
    @FXML
    private void onJoinButtonClick() throws IOException {
        if(validate(false)) {
            startChat(false);
        } else {
            label_warning.setVisible(true);
        }
    }

    private boolean validate(boolean isHost) {
        nickname = textField_nickname.getText();
        ip = textField_ipAddress.getText();
        String portString = textField_port.getText();
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
            label_warning.setText("Nicknames cannot be longer than\n32 characters.");
            return false;
        }
        if(nickname.length() < 3) {
            label_warning.setText("Nicknames have to be at least\n3 characters long.");
            return false;
        }
        if(nickname.contains(" ")) {
            label_warning.setText("Nicknames cannot contain whitespace.");
            return false;
        }
        if(!ip.isBlank() && !ip.equals("localhost")) {
            if(!InetAddressValidator.getInstance().isValidInet4Address(ip)) {
                label_warning.setText("This is not a valid IPv4 address.");
                return false;
            }
        }
        if(port < 1024 || port > 65353) {
            label_warning.setText("Please enter a valid port number\n(1024 - 65353).");
            return false;
        }
        if(!isHost && ip.isBlank()) {
            label_warning.setText("Please specify an IP address.");
            return false;
        }
        return true;
    }

    private void startChat(boolean host) throws IOException {
        UserCredentials userCredentials = UserCredentials.getInstance();
        userCredentials.setNickname(nickname);
        userCredentials.setIpAddress(ip);
        userCredentials.setPort(port);
        userCredentials.setHost(host);

        SimpleChat.setRoot("chat-view");
    }
}
