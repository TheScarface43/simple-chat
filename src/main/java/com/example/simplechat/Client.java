package com.example.simplechat;

import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;

import static com.example.simplechat.MessageType.HELLO;

public class Client implements Runnable {
    
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isRunning;
    private ChatController chatController;
    private String ip;
    private int port;
    private User user;

    public Client(ChatController chatController, String nickname, String ip, int port, Socket socket, String color) {
        this.chatController = chatController;
        //this.nickname = nickname;
        this.ip = ip;
        this.port = port;
        this.client = socket;
        this.user = new User(nickname, RoleType.REGULAR, color);
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());
            isRunning = true;

            sendMessage();

            MessageType type;
            Message inMessage;
            while(isRunning) {
                type = (MessageType) in.readObject();
                inMessage = (Message) in.readObject();
                switch(type) {
                    case CHAT, SERVER:
                        chatController.receiveMessage((TextMessage) inMessage);
                        break;
                    case USERLIST_DATA:
                        chatController.updateUserList((UserListMessage) inMessage);
                        break;
                    case ASSIGN_USER:
                        assignUser((AssignMessage) inMessage);
                        chatController.updateWindowTitle(user.getNickname());
                        break;
                }
            }
        } catch (IOException e) {
            disconnect();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(TextMessage message) {
        if(message.isEmpty()) {
            return;
        }

        if (message.getType() == MessageType.COMMAND) {
            if(!verifyCommand(message.getContents())) {  //Check if it's a command that needs to be handled by server
                return;                                  //If not - don't send anything
            }
        }

        try {
            out.writeObject(message.getType());
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            disconnect();
        }
    }
    public void sendMessage(String string) {
        if(string.isBlank()) {
            return;
        }

        MessageType type;

        if(string.startsWith("/")) {
            type = MessageType.COMMAND;
        } else {
            type = MessageType.CHAT;
        }

        TextMessage message = new TextMessage(user, type, string);
        sendMessage(message);
    }
    public void sendMessage() {
        TextMessage message = new TextMessage(user, HELLO, "Hello!");
        sendMessage(message);
    }

    private boolean verifyCommand(String command) {
        switch(command) {   //These are "local" commands to be executed by the client - do not bother server with those!
            case "/quit":
            case "/q":
                chatController.closeChatWindow();
                return false;
            case "/disconnect":
            case "/dc":
                disconnect();
                return false;
            case "/clear":
            case "/cls":
                chatController.clearChatWindow();
                return false;
        }
        return true;
    }

    private void assignUser(AssignMessage message) {
        user = message.getContents();
    }

    public synchronized void disconnect() {
        if(isRunning) {
            chatController.disableChat();
            isRunning = false;
            try {
                in.close();
                out.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
