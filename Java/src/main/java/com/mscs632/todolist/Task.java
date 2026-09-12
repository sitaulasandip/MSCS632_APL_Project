package com.mscs632.todolist;

import java.time.LocalDateTime;

public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final String category;
    private final String assignedTo;
    private final LocalDateTime createdAt;
    private final TaskStatus status;

    public Task(int id, String title, String description, String category, String assignedTo) {
        this(id, title, description, category, assignedTo, LocalDateTime.now(), TaskStatus.PENDING);
    }

    public Task(int id, String title, String description, String category, String assignedTo,
                LocalDateTime createdAt, TaskStatus status) {
        if (id <= 0) throw new IllegalArgumentException("Task ID must be positive");
        this.id = id;
        this.title = required(title, "Title");
        this.description = java.util.Objects.requireNonNull(description, "Description is required");
        this.category = required(category, "Category");
        this.assignedTo = required(assignedTo, "Assigned user");
        this.createdAt = java.util.Objects.requireNonNull(createdAt);
        this.status = java.util.Objects.requireNonNull(status);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    Task withStatus(TaskStatus status) {
        if (status == null) throw new IllegalArgumentException("Status is required");
        return new Task(id, title, description, category, assignedTo, createdAt, status);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
