package com.example.homeautomationapp;

// Message.java
public class Message {
    private String text;
    private boolean isUser1;

    public Message(String text, boolean isUser1) {
        this.text = text;
        this.isUser1 = isUser1;
    }

    public String getText() {
        return text;
    }

    public boolean isUser1() {
        return isUser1;
    }
}

