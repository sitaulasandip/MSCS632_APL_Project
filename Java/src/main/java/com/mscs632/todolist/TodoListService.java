package com.mscs632.todolist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TodoListService {
    private Map<Integer, Task> tasks = new java.util.TreeMap<>();
    private Map<String, User> users = new java.util.TreeMap<>();
    private int nextId = 1;
    private final java.nio.file.Path storage;

    /** In-memory service, useful for isolated tests. */
    public TodoListService() { storage = null; }

    public TodoListService(java.nio.file.Path storage) {
        this.storage = storage.toAbsolutePath();
        CsvStorage.State state = CsvStorage.load(this.storage);
        tasks = state.tasks();
        users = state.users();
        nextId = state.nextId();
    }

    private void commit(Map<Integer, Task> newTasks, Map<String, User> newUsers, int newNextId) {
        if (storage != null) CsvStorage.save(storage, newTasks, newUsers, newNextId);
        tasks = newTasks;
        users = newUsers;
        nextId = newNextId;
    }

    public synchronized User addUser(String name) {
        User user = new User(name);
        String key = user.name().toLowerCase(java.util.Locale.ROOT);
        if (users.containsKey(key)) throw new IllegalArgumentException("User already exists: " + name);
        Map<String, User> copy = new java.util.TreeMap<>(users);
        copy.put(key, user);
        commit(tasks, copy, nextId);
        return user;
    }

    public synchronized List<User> getUsers() { return List.copyOf(users.values()); }

    public synchronized Task addTask(String title, String description, String category, String assignedTo) {
        if (assignedTo == null) throw new IllegalArgumentException("Assigned user is required");
        User user = users.get(assignedTo.trim().toLowerCase(java.util.Locale.ROOT));
        if (user == null) throw new IllegalArgumentException("Unknown user. Add the user first.");
        if (nextId == Integer.MAX_VALUE) throw new IllegalStateException("Task IDs exhausted");
        Task task = new Task(nextId, title, description, category, user.name());
        Map<Integer, Task> copy = new java.util.TreeMap<>(tasks);
        copy.put(nextId, task);
        commit(copy, users, nextId + 1);
        return task;
    }

    public synchronized Task updateTaskStatus(int taskId, TaskStatus newStatus) {
        Task task = tasks.get(taskId);
        if (task == null) throw new IllegalArgumentException("Task not found: " + taskId);
        Task updated = task.withStatus(newStatus);
        Map<Integer, Task> copy = new java.util.TreeMap<>(tasks);
        copy.put(taskId, updated);
        commit(copy, users, nextId);
        return updated;
    }

    public synchronized boolean deleteTask(int taskId) {
        if (!tasks.containsKey(taskId)) return false;
        Map<Integer, Task> copy = new java.util.TreeMap<>(tasks);
        copy.remove(taskId);
        commit(copy, users, nextId);
        return true;
    }

    public synchronized List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public synchronized List<Task> getTasksByUser(String userName) {
        return tasks.values().stream()
                .filter(task -> task.getAssignedTo().equalsIgnoreCase(userName))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> getTasksByCategory(String category) {
        return tasks.values().stream()
                .filter(task -> task.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public synchronized List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized List<Task> searchTasks(String keyword) {
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

    public synchronized String getSummary() {
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

    public synchronized Set<Integer> getTaskIds() {
        return Set.copyOf(tasks.keySet());
    }
}
