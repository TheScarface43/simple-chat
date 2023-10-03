package com.example.simplechat;

import javafx.scene.paint.Color;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
    private final User serverUser;

    public Server(ServerSocket serverSocket) {
        connections = new ArrayList<>();
        this.serverSocket = serverSocket;
        running = true;
        this.serverUser = new User("SERVER", RoleType.SERVER, "#DDDDDD");
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

    public void broadcastMessage(Message message) {
        for(ConnectionHandler ch : connections) {
            ch.sendMessage(message);
        }
    }
    public void broadcastMessage(String string) {
        Message message = new TextMessage(serverUser, MessageType.SERVER, string);
        broadcastMessage(message);
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
            listOfUsers.add(ch.user);
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
        private User user;
        private boolean firstNicknameChange;

        public ConnectionHandler(Socket client) {
            this(client, REGULAR);
        }
        public ConnectionHandler(Socket client, RoleType role) {
            this.client = client;
            this.user = new User("User", role, "#DDDDDD");
            this.firstNicknameChange = true;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(client.getOutputStream());
                in = new ObjectInputStream(client.getInputStream());

                Message message;
                MessageType type;
                while(!client.isClosed()) {
                    type = (MessageType) in.readObject();
                    message = (Message) in.readObject();
                    if(!validateReceivedMessage(message, type)) {
                        message = null;
                        continue;
                    }
                    switch (type) {
                        case COMMAND -> processCommand((TextMessage) message);
                        case CHAT -> broadcastMessage(message);
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

        private boolean validateReceivedMessage(Message message, MessageType type) {
            if(message.getType() != type) {
                System.out.println("Client " + client.getRemoteSocketAddress() + " sent the wrong type of message (implied - " + type + ", actual - " + message.getType() + ")!");
                return false;
            }
            if(message == null || message.isEmpty()) {
                System.out.println("Client " + client.getRemoteSocketAddress() + " sent an empty message (type " + message.getType() + ").");
                return false;
            }
            if(message instanceof TextMessage) {
                if(((TextMessage) message).getContents().length() > MESSAGE_CHAR_LIMIT) {
                    System.out.println("Client " + client.getRemoteSocketAddress() + " sent a message exceeding the set character limit (" + MESSAGE_CHAR_LIMIT + ").");
                    return false;
                }
            }
            return true;
        }

        private void sendMessage(Message message) {
            try {
                out.writeObject(message.getType());
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }
        private void sendMessage(String string) {
            Message message = new TextMessage(serverUser, MessageType.SERVER, string);
            sendMessage(message);
        }

        private void sendUserList(ArrayList<User> userList) {
            Message message = new UserListMessage(serverUser, userList);
            sendMessage(message);
        }

        private void sendNameChange(String name) {
            Message message = new TextMessage(serverUser, MessageType.NAME_CHANGE, name);
            sendMessage(message);
        }

        private void sendAssignUser(User user) {
            Message message = new AssignMessage(serverUser, user);
            sendMessage(message);
        }

        private void processCommand(TextMessage message) {
            String contents = message.getContents();
            String[] command = contents.split(" ");
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
                user.setNickname(newNickname);
            }

            if(newNickname.length() < 3) {
                sendMessage("Nicknames have to be at least 3 characters long.");
                return;
            }
            if(newNickname.length() > 32) {
                sendMessage("Nicknames cannot be longer than 32 characters.");
                return;
            }

            if(firstNicknameChange) {
                firstNicknameChange = false;
                broadcastMessage(newNickname + " has joined!");
            } else {
                sendMessage("Nickname has been changed to " + newNickname + ".");
            }

            sendNameChange(newNickname);
            sendAssignUser(user);
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
                    broadcastMessage(user.getNickname() + " has left.");
                    updateUserList();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
