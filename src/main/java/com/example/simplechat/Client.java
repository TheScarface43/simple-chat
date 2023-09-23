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
                out.writeObject(MessageType.COMMAND);
            } else {
                out.writeObject(MessageType.CHAT);
            }
            out.writeUTF(message);
            out.flush();
            switch(message) { //Special commands that should be executed locally, regardless of server's response
                case "/quit":
                case "/q":
                    chatController.closeChatWindow();
                    break;
                case "/disconnect":
                case "/dc":
                    disconnect();
                    break;
            }
        } catch (IOException e) {
            disconnect();
        }
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
