package com.example.simplechat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.simplechat.RoleType.*;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private static Boolean running;
    private static Boolean firstConnection = true;
    private static final int MESSAGE_CHAR_LIMIT = 5000;

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
                ConnectionHandler connectionHandler;
                if(firstConnection) {
                    connectionHandler = new ConnectionHandler(client, HOST);
                    firstConnection = false;
                } else {
                    connectionHandler = new ConnectionHandler(client);
                }
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

    public void broadcastServerInfo(String info) {
        if(info.isBlank()) {
            return;
        }
        for(ConnectionHandler ch : connections) {
            ch.sendServerInfo(info);
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

    public ArrayList<User> getListOfUsers() {
        ArrayList<User> listOfUsers = new ArrayList<>();

        for(ConnectionHandler ch : connections) {
            User user = new User(ch.nickname, ch.role);
            listOfUsers.add(user);
        }

        return listOfUsers;
    }

    private void updateUserList() {
        ArrayList<User> listOfUsers = getListOfUsers();

        for (ConnectionHandler ch : connections) {
            ch.sendUserList(listOfUsers);
        }
    }
    
    class ConnectionHandler implements Runnable {

        private Socket client;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String nickname;
        private final RoleType role;
        private boolean firstNicknameChange;

        public ConnectionHandler(Socket client) {
            this(client, REGULAR);
        }
        public ConnectionHandler(Socket client, RoleType role) {
            this.client = client;
            this.nickname = "User";
            this.role = role;
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
                    if(!validateReceivedMessage(message, type)) {
                        message = "";
                        continue;
                    }
                    switch (type) {
                        case COMMAND -> processCommand(message);
                        case CHAT -> broadcastMessage(nickname, message);
                        default -> {
                            System.out.println("Client " + client.getRemoteSocketAddress() + " attempted an invalid server request (message type " + type + ").");
                            message = "";
                        }
                    }
                }
            } catch (IOException e) {
                disconnect();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean validateReceivedMessage(String message, MessageType type) {
            if(message == null || message.isBlank()) {
                System.out.println("Client " + client.getRemoteSocketAddress() + " sent an empty message (type " + type + ").");
                return false;
            }
            if(message.length() > MESSAGE_CHAR_LIMIT) {
                System.out.println("Client " + client.getRemoteSocketAddress() + " sent a message exceeding the set character limit (" + MESSAGE_CHAR_LIMIT + ").");
                return false;
            }
            return true;
        }

        private void sendMessage(String message) {
            String time = getTimestamp();
            String messageWithTimestamp = time + " " + message;
            try {
                out.writeObject(MessageType.CHAT);
                out.writeUTF(messageWithTimestamp);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void sendServerInfo(String info) {
            try {
                out.writeObject(MessageType.SERVER);
                out.writeUTF(info);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void sendUserList(ArrayList<User> userList) {
            try {
                out.writeObject(MessageType.USERLIST_DATA);
                out.writeObject(userList);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private void sendNameChange(String name) {
            try {
                out.writeObject(MessageType.NAME_CHANGE);
                out.writeUTF(name);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        private String getTimestamp() {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return time.format(formatter);
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
                default -> sendServerInfo("Invalid command.");
            }
        }

        private boolean checkCommandArguments(String[] command, int arg) {
            int expected = arg + 1;
            if(command.length > expected) {
                sendServerInfo("Too many arguments for this command. (expected - " + arg + ").");
                return false;
            }
            if(command.length < expected) {
                sendServerInfo("Too few arguments for this command. (expected - " + arg + ").");
                return false;
            }
            return true;
        }

        private void changeNickname(String newNickname) {
            int len = newNickname.length();
            if(len >= 3 && len <= 32) {
                nickname = newNickname;
            }

            if(newNickname.length() < 3) {
                sendServerInfo("Nicknames have to be at least 3 characters long.");
                return;
            }
            if(newNickname.length() > 32) {
                sendServerInfo("Nicknames cannot be longer than 32 characters.");
                return;
            }

            if(firstNicknameChange) {
                firstNicknameChange = false;
                broadcastServerInfo(nickname + " has joined!");
            } else {
                sendServerInfo("Nickname has been changed to " + nickname + ".");
            }

            sendNameChange(nickname);
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
                    broadcastServerInfo(nickname + " has left.");
                    updateUserList();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
