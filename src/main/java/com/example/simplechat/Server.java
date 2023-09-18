package com.example.simplechat;

import javafx.application.Platform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.simplechat.SimpleChat.stage;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private static Boolean running;

    public Server(ServerSocket serverSocket) {
        connections = new ArrayList<>();
        this.serverSocket = serverSocket;
        running = true;
    }

    @Override
    public void run() {
        try {
            pool = Executors.newCachedThreadPool();
            System.out.println("Waiting for connections...");
            while(running) {
                Socket client = serverSocket.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(client);
                connections.add(connectionHandler);
                pool.execute(connectionHandler);
                System.out.println("Connection from " + client.getRemoteSocketAddress().toString());
            }
        } catch (IOException e) {
            if(running) {
                shutdown();
            }
        }
    }

    public void broadcastMessage(String nickname, String message) {
        broadcastMessage(nickname + ": " + message);
    }
    public void broadcastMessage(String message) {
        if(message.isBlank()) {
            return;
        }
        for(ConnectionHandler ch : connections) {
            ch.sendMessage(message);
        }
    }

    public void disconnectEveryone() {
        for(Iterator<ConnectionHandler> it = connections.iterator(); it.hasNext();) {
            ConnectionHandler ch = it.next();
            it.remove();
            ch.disconnect();
        }
    }

    public void shutdown() {
        if(!serverSocket.isClosed()) {
            System.out.println("Shutting down...");
            try {
                running = false;
                serverSocket.close();
                disconnectEveryone();

                pool.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("...Finished shutting down.");
        }
    }

    public ArrayList<String> getListOfUsernames() {
        ArrayList<String> listOfUsernames = new ArrayList<>();

        for(ConnectionHandler ch : connections) {
            listOfUsernames.add(ch.nickname);
        }

        return listOfUsernames;
    }

    private void updateUserList() {
        ArrayList<String> listOfUsernames = getListOfUsernames();

        for (ConnectionHandler ch : connections) {
            ch.sendUserList(listOfUsernames);
        }
    }
    
    class ConnectionHandler implements Runnable {

        private Socket client;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String nickname;
        private boolean firstNicknameChange;

        public ConnectionHandler(Socket client) {
            this.client = client;
            this.nickname = "User";
            this.firstNicknameChange = true;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(client.getOutputStream());
                in = new ObjectInputStream(client.getInputStream());

                String message;
                MessageType type;
                while(!client.isClosed()) {
                    type = (MessageType) in.readObject();
                    message = in.readUTF();
                    if(message == null) {
                        System.out.println("Client " + client.getRemoteSocketAddress() + " sent an empty message (type " + type + ").");
                        continue;
                    }
                    switch (type) {
                        case COMMAND -> processCommand(message);
                        case CHAT -> broadcastMessage(nickname, message);
                        default -> {
                            System.out.println("Client " + client.getRemoteSocketAddress() + " attempted an invalid server request (message type " + type + ").");
                            message = null;
                        }
                    }
                }
            } catch (IOException e) {
                disconnect();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private void sendMessage(String message) {
            try {
                out.writeObject(MessageType.CHAT);
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void sendUserList(ArrayList<String> userList) {
            try {
                out.writeObject(MessageType.USERLIST_DATA);
                out.writeObject(userList);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void processCommand(String message) {
            String[] command = message.split(" ");
            switch (command[0]) {
                case "/name" -> {
                    if (checkCommandArguments(command, 1)) {
                        changeNickname(command[1]);
                    }
                }
                case "/quit", "/q", "/disconnect", "/dc" -> disconnect();
                default -> sendMessage("Invalid command.");
            }
        }

        private boolean checkCommandArguments(String[] command, int arg) {
            int expected = arg + 1;
            if(command.length > expected) {
                sendMessage("Too many arguments for this command. (expected - " + arg + ").");
                return false;
            }
            if(command.length < expected) {
                sendMessage("Too few arguments for this command. (expected - " + arg + ").");
                return false;
            }
            return true;
        }

        private void changeNickname(String newNickname) {
            int len = newNickname.length();
            if(len >= 3 && len <= 32) {
                nickname = newNickname;
            }
            if(firstNicknameChange) {
                firstNicknameChange = false;
                broadcastMessage(nickname + " has joined!");
            }

            if(newNickname.length() < 3) {
                sendMessage("Nicknames have to be at least 3 characters long.");
                return;
            }
            if(newNickname.length() > 32) {
                sendMessage("Nicknames cannot be longer than 32 characters.");
                return;
            }

            updateUserList();
        }

        private void disconnect() {
            try {
                in.close();
                out.close();
                if(!client.isClosed()) {
                    client.close();
                }
                connections.remove(this);
                if(running) {
                    broadcastMessage(nickname + " has left.");
                    updateUserList();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
