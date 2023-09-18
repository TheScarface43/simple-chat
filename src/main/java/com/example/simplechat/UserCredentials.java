package com.example.simplechat;

public final class UserCredentials {

    private static UserCredentials INSTANCE;
    private String nickname;
    private String ipAddress;
    private int port;
    private boolean host;

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
}
