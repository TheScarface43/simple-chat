package com.example.simplechat;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client implements Runnable {
    
    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean isRunning;
    private ChatController chatController;
    private String nickname;
    private String ip;
    private int port;

    public Client(ChatController chatController, String nickname, String ip, int port, Socket socket) {
        this.chatController = chatController;
        this.nickname = nickname;
        this.ip = ip;
        this.port = port;
        this.client = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());
            isRunning = true;

            sendMessage("/name " + nickname);

            MessageType type;
            while(isRunning) {
                type = (MessageType) in.readObject();
                switch(type) {
                    case CHAT:
                        String inMessage = in.readUTF();
                        chatController.receiveMessage(inMessage);
                        break;
                    case USERLIST_DATA:
                        ArrayList<User> listOfUsers = (ArrayList<User>) in.readObject();
                        chatController.updateUserList(listOfUsers);
                        break;
                    case NAME_CHANGE:
                        nickname = in.readUTF();
                        chatController.updateWindowTitle(nickname);
                }
            }
        } catch (IOException e) {
            disconnect();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String message) {
        try {
            if(message.startsWith("/")) {
                if(verifyCommand(message)) {    //Check if it's a command that needs to be handled by server, client or both
                    out.writeObject(MessageType.COMMAND);
                } else {
                    return;
                }
            } else {
                out.writeObject(MessageType.CHAT);
            }
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            disconnect();
        }
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

    public String getNickname() {
        return nickname;
    }
}
