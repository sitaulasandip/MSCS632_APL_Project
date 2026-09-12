package com.mscs632.todolist;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Isolated service tests: each case starts with a fresh in-memory service. */
public class TodoListUnitTest {
    private static int passed;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName(), error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static TodoListService service() {
        TodoListService service = new TodoListService();
        service.addUser("Alice"); service.addUser("Bob");
        return service;
    }
    private static Task add(TodoListService service, String title, String category, String user) {
        return service.addTask(title, "Description for " + title, category, user);
    }
    private static Set<Integer> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).collect(Collectors.toSet());
    }
    private static void run(String name, Runnable test) {
        try { test.run(); passed++; System.out.println("PASS: " + name); }
        catch (Throwable error) { throw new AssertionError("FAIL: " + name, error); }
    }
    public static void main(String[] args) {
        passed = 0;
        run("empty service", () -> {
            TodoListService s = new TodoListService();
            check(s.getUsers().isEmpty() && s.getAllTasks().isEmpty() && s.getTaskIds().isEmpty(), "Empty records");
            check(s.searchTasks("missing").isEmpty() && s.getTasksByUser("Alice").isEmpty(), "Empty queries");
            check(s.getTasksByCategory("Work").isEmpty() && s.getTasksByStatus(TaskStatus.PENDING).isEmpty(), "Empty filters");
            check(s.getSummary().equals(String.join(System.lineSeparator(), "Total tasks: 0", "Pending: 0", "In Progress: 0", "Completed: 0", "Tasks by category:", "")), "Empty summary");
        });
        run("user registration and duplicate validation", () -> {
            TodoListService s = new TodoListService();
            check(s.addUser(" Alice ").name().equals("Alice"), "Trimmed username");
            expect(IllegalArgumentException.class, () -> s.addUser("aLiCe"));
            expect(IllegalArgumentException.class, () -> s.addUser(" "));
            expect(IllegalArgumentException.class, () -> s.addUser(null));
            check(s.getUsers().equals(List.of(new User("Alice"))), "Rejected users must not be added");
        });
        run("task creation and assignment", () -> {
            TodoListService s = service();
            Task t = s.addTask(" Report ", "", " School ", " ALICE ");
            check(t.getTitle().equals("Report") && t.getCategory().equals("School"), "Trimmed details");
            check(t.getDescription().isEmpty() && t.getAssignedTo().equals("Alice"), "Optional description and canonical user");
            check(t.getStatus() == TaskStatus.PENDING && t.getCreatedAt() != null, "Initial state");
            Task second = add(s, "Slides", "Work", "Bob");
            check(t.getId() != second.getId() && s.getAllTasks().size() == 2, "Unique IDs and retained tasks");
        });
        run("invalid task fields leave storage unchanged", () -> {
            TodoListService s = service();
            for (String invalid : new String[] {null, "", "  "}) {
                expect(IllegalArgumentException.class, () -> s.addTask(invalid, "", "Work", "Alice"));
                expect(IllegalArgumentException.class, () -> s.addTask("Title", "", invalid, "Alice"));
                expect(IllegalArgumentException.class, () -> s.addTask("Title", "", "Work", invalid));
            }
            expect(IllegalArgumentException.class, () -> add(s, "Title", "Work", "Unknown"));
            expect(NullPointerException.class, () -> s.addTask("Title", null, "Work", "Alice"));
            check(s.getAllTasks().isEmpty(), "Invalid tasks not stored");
        });
        run("all status transitions and immutable snapshots", () -> {
            TodoListService s = service(); Task original = add(s, "Report", "School", "Alice");
            for (TaskStatus status : List.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.PENDING)) {
                Task updated = s.updateTaskStatus(original.getId(), status);
                check(updated.getStatus() == status && s.getAllTasks().get(0).getStatus() == status, "Status saved");
                check(updated.getTitle().equals(original.getTitle()) && updated.getCreatedAt().equals(original.getCreatedAt()), "Details preserved");
            }
            s.updateTaskStatus(original.getId(), TaskStatus.COMPLETED);
            check(original.getStatus() == TaskStatus.PENDING, "Original snapshot unchanged");
            expect(IllegalArgumentException.class, () -> s.updateTaskStatus(original.getId(), null));
            expect(IllegalArgumentException.class, () -> s.updateTaskStatus(999, TaskStatus.COMPLETED));
            check(s.getAllTasks().get(0).getStatus() == TaskStatus.COMPLETED, "Invalid update preserves status");
        });
        run("deletion and missing task behavior", () -> {
            TodoListService s = service(); Task a = add(s, "A", "Work", "Alice"), b = add(s, "B", "Work", "Bob");
            check(s.deleteTask(a.getId()), "Existing task deleted");
            check(!s.deleteTask(a.getId()) && !s.deleteTask(-1), "Missing task returns false");
            check(ids(s.getAllTasks()).equals(Set.of(b.getId())), "Other task retained");
            expect(IllegalArgumentException.class, () -> s.updateTaskStatus(a.getId(), TaskStatus.PENDING));
            check(s.getUsers().size() == 2, "Deleting task retains users");
        });
        run("user category and status filters", () -> {
            TodoListService s = service();
            Task a = add(s, "A", "School", "Alice"), b = add(s, "B", "Work", "Alice"), c = add(s, "C", "School", "Bob");
            s.updateTaskStatus(b.getId(), TaskStatus.IN_PROGRESS); s.updateTaskStatus(c.getId(), TaskStatus.COMPLETED);
            check(ids(s.getTasksByUser("aLiCe")).equals(Set.of(a.getId(), b.getId())), "User isolation");
            check(ids(s.getTasksByCategory("sChOoL")).equals(Set.of(a.getId(), c.getId())), "Category matching");
            check(ids(s.getTasksByStatus(TaskStatus.PENDING)).equals(Set.of(a.getId())), "Pending filter");
            check(ids(s.getTasksByStatus(TaskStatus.IN_PROGRESS)).equals(Set.of(b.getId())), "In progress filter");
            check(ids(s.getTasksByStatus(TaskStatus.COMPLETED)).equals(Set.of(c.getId())), "Completed filter");
            check(s.getTasksByUser("Unknown").isEmpty() && s.getTasksByCategory("Unknown").isEmpty(), "No match");
        });
        run("search across every supported field", () -> {
            TodoListService s = service();
            Task a = s.addTask("Report", "Unique notes", "School", "Alice");
            add(s, "Slides", "Work", "Bob");
            for (String query : List.of("  REPORT  ", "unique", "school", "ALICE"))
                check(ids(s.searchTasks(query)).equals(Set.of(a.getId())), "Search: " + query);
            check(s.searchTasks("absent").isEmpty(), "No search match");
            for (String query : new String[] {null, "", "  "}) check(s.searchTasks(query).size() == 2, "Empty search returns all");
        });
        run("summary counts after status changes and deletion", () -> {
            TodoListService s = service();
            add(s, "A", "School", "Alice"); Task b = add(s, "B", "Work", "Bob"), c = add(s, "C", "School", "Bob");
            s.updateTaskStatus(b.getId(), TaskStatus.IN_PROGRESS); s.updateTaskStatus(c.getId(), TaskStatus.COMPLETED);
            check(s.getSummary().equals(String.join(System.lineSeparator(), "Total tasks: 3", "Pending: 1", "In Progress: 1", "Completed: 1", "Tasks by category:", "- School: 2", "- Work: 1", "")), "Mixed summary");
            s.deleteTask(b.getId());
            check(s.getSummary().contains("Total tasks: 2") && s.getSummary().contains("In Progress: 0") && !s.getSummary().contains("- Work:"), "Summary after deletion");
        });
        run("returned collections cannot mutate service storage", () -> {
            TodoListService s = service(); Task t = add(s, "Report", "School", "Alice");
            s.getAllTasks().clear(); s.getTasksByUser("Alice").clear(); s.getTasksByCategory("School").clear();
            s.getTasksByStatus(TaskStatus.PENDING).clear(); s.searchTasks("Report").clear();
            expect(UnsupportedOperationException.class, () -> s.getUsers().clear());
            expect(UnsupportedOperationException.class, () -> s.getTaskIds().clear());
            check(s.getTaskIds().equals(Set.of(t.getId())) && s.getUsers().size() == 2, "Internal records retained");
        });
        System.out.println("Unit tests passed: " + passed);
    }
}
