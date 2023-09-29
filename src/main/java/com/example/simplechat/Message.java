package com.example.simplechat;

import java.io.Serializable;
import java.time.LocalTime;

public abstract class Message implements Serializable {
    protected LocalTime timestamp;
    protected User author;
    protected MessageType type;

    protected Message (User author, MessageType type) {
        this.timestamp = LocalTime.now();
        this.author = author;
        this.type = type;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }
    public User getAuthor() {
        return author;
    }
    public MessageType getType() {
        return type;
    }
}
