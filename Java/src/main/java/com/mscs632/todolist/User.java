package com.mscs632.todolist;

/** A named user; names are unique ignoring case. */
public record User(String name) {
    public User {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("User name is required");
        name = name.trim();
    }
    @Override public String toString() { return name; }
}
