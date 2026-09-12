package com.mscs632.todolist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TodoListService {
    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public Task addTask(String title, String description, String category, String assignedTo) {
        Objects.requireNonNull(title, "Title cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        Objects.requireNonNull(assignedTo, "Assigned user cannot be null");

        int id = idCounter.getAndIncrement();
        Task task = new Task(id, title, description, category, assignedTo);
        tasks.put(id, task);
        return task;
    }

    public Task updateTaskStatus(int taskId, TaskStatus newStatus) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        task.setStatus(newStatus);
        return task;
    }

    public boolean deleteTask(int taskId) {
        return tasks.remove(taskId) != null;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> getTasksByUser(String userName) {
        return tasks.values().stream()
                .filter(task -> task.getAssignedTo().equalsIgnoreCase(userName))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByCategory(String category) {
        return tasks.values().stream()
                .filter(task -> task.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Task> searchTasks(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return getAllTasks();
        }

        return tasks.values().stream()
                .filter(task -> task.getTitle().toLowerCase().contains(normalized)
                        || task.getDescription().toLowerCase().contains(normalized)
                        || task.getCategory().toLowerCase().contains(normalized)
                        || task.getAssignedTo().toLowerCase().contains(normalized))
                .collect(Collectors.toList());
    }

    public String getSummary() {
        long total = tasks.size();
        long pending = tasks.values().stream().filter(task -> task.getStatus() == TaskStatus.PENDING).count();
        long inProgress = tasks.values().stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long completed = tasks.values().stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();

        Map<String, Long> byCategory = tasks.values().stream()
                .collect(Collectors.groupingBy(Task::getCategory, Collectors.counting()));

        StringBuilder summary = new StringBuilder();
        summary.append("Total tasks: ").append(total).append(System.lineSeparator());
        summary.append("Pending: ").append(pending).append(System.lineSeparator());
        summary.append("In Progress: ").append(inProgress).append(System.lineSeparator());
        summary.append("Completed: ").append(completed).append(System.lineSeparator());
        summary.append("Tasks by category:").append(System.lineSeparator());

        List<String> categoryEntries = new ArrayList<>();
        byCategory.forEach((category, count) -> categoryEntries.add("- " + category + ": " + count));
        Collections.sort(categoryEntries);
        categoryEntries.forEach(entry -> summary.append(entry).append(System.lineSeparator()));

        return summary.toString();
    }

    public Set<Integer> getTaskIds() {
        return tasks.keySet();
    }
}
