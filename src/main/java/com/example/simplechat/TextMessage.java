package com.example.simplechat;

public class TextMessage extends Message {

    private String contents;

    public TextMessage(User author, MessageType type, String contents) {
        super(author, type);
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return timestamp.toString() + " " + author.nickname() + ": " + contents;
    }
}
