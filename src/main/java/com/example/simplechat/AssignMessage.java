package com.example.simplechat;

public class AssignMessage extends Message {

    private User contents;
    public AssignMessage(User author, User contents) {
        super(author, MessageType.ASSIGN_USER);
        this.contents = contents;
    }

    public User getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return timestamp + " User object [" + contents.nickname() + ", " + contents.role() + "]";
    }
}
