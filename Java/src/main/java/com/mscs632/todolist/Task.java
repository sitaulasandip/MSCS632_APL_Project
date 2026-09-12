package com.mscs632.todolist;

import java.time.LocalDateTime;

public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final String category;
    private final String assignedTo;
    private final LocalDateTime createdAt;
    private TaskStatus status;

    public Task(int id, String title, String description, String category, String assignedTo) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.assignedTo = assignedTo;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
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

    public void setStatus(TaskStatus status) {
        this.status = status;
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
