package com.example.simplechat;

import java.util.ArrayList;

public class UserListMessage extends Message {

    private ArrayList<User> contents;

    public UserListMessage(User author, ArrayList<User> contents) {
        super(author, MessageType.USERLIST_DATA);
        this.contents = contents;
    }

    public ArrayList<User> getContents() {
        return contents;
    }

    @Override
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    @Override
    public String toString() {
        return timestamp.toString() + " " + contents.toString();
    }
}
