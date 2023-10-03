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
    public boolean isEmpty() {
        return contents.isBlank();
    }

    @Override
    public String toString() {
        return timestamp.toString() + " " + author.getNickname() + ": " + contents;
    }
}
