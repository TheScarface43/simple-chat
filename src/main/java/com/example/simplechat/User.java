package com.example.simplechat;

import java.io.Serializable;

public class User implements Serializable {
    private String nickname;
    private RoleType role;
    private String color;

    public User(String nickname, RoleType role, String color) {
        this.nickname = nickname;
        this.role = role;
        this.color = color;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public RoleType getRole() {
        return role;
    }
    public void setRole(RoleType role) {
        this.role = role;
    }

    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        if(!color.startsWith("#")) {
            color = "#" + color;
        }
        this.color = color;
    }
}
