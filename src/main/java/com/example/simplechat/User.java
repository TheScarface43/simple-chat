package com.example.simplechat;

import java.io.Serializable;

public record User(String nickname, RoleType role) implements Serializable {
}
