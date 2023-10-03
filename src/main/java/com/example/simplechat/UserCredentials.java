package com.example.simplechat;

import javafx.scene.paint.Color;

import java.net.Socket;

public final class UserCredentials {

    private static UserCredentials INSTANCE;
    private String nickname;
    private String ipAddress;
    private int port;
    private Socket clientSocket;
    private Server server;
    private String color;

    private UserCredentials() {}

    public static UserCredentials getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new UserCredentials();
        }
        return INSTANCE;
    }

    void setNickname(String nickname) {
        if(nickname.length() > 32 || nickname.length() < 3) {
            nickname = "";
        }
        this.nickname = nickname;
    }
    void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    void setPort(int port) {
        this.port = port;
    }
    void setClientSocket(Socket socket) {
        this.clientSocket = socket;
    }
    void setServer(Server server) {
        this.server = server;
    }
    void setColor(Color color) {
        this.color = color.toString();
    }
    void setColor(String color) {
        this.color = color;
    }

    String getNickname() {
        return nickname;
    }
    String getIpAddress() {
        return ipAddress;
    }
    int getPort() {
        return port;
    }
    Socket getClientSocket() {
        return clientSocket;
    }
    Server getServer() {
        return server;
    }
    String getColor() {
        return color;
    }
}
