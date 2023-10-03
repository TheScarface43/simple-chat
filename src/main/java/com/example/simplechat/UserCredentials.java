package com.example.simplechat;

import java.net.Socket;

public final class UserCredentials {

    private static UserCredentials INSTANCE;
    private String nickname;
    private String ipAddress;
    private int port;
    private boolean host;
    private Socket clientSocket;
    private Server server;

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
    void setHost(boolean host) {
        this.host = host;
    }
    void setClientSocket(Socket socket) {
        this.clientSocket = socket;
    }
    void setServer(Server server) {
        this.server = server;
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
    boolean isHost() {
        return host;
    }
    Socket getClientSocket() {
        return clientSocket;
    }
    Server getServer() {
        return server;
    }
}
