package com.mscs632.todolist;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Dependency-free behavioral tests. Each run uses a temporary storage directory. */
public class TodoListServiceTest {
    private static final int MAX_WORKERS = 8;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected validation error");
    }
    private static void together(String scenario, List<Callable<Void>> actions) throws Exception {
        int workers = Math.min(MAX_WORKERS, actions.size());
        System.out.printf("%n%s: %d operations, %d worker threads.%n", scenario, actions.size(), workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        // Release workers together to create contention. The service lock then serializes access.
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> results = new ArrayList<>();
        try {
            for (Callable<Void> action : actions) results.add(pool.submit(() -> { start.await(); return action.call(); }));
            start.countDown();
            // Reading every Future ensures worker exceptions fail the test instead of being hidden.
            for (Future<Void> result : results) result.get(30, TimeUnit.SECONDS);
            System.out.printf("Finished: %d/%d operations returned without unexpected errors.%n", results.size(), actions.size());
        } finally { pool.shutdownNow(); }
    }
    public static void main(String[] args) throws Exception {
        TodoListUnitTest.main(args);
        Path dir = Files.createTempDirectory("todo-tests-");
        try {
            Path csv = dir.resolve("todo.csv");
            TodoListService service = new TodoListService(csv);
            service.addUser("Alice"); service.addUser("Bob");
            rejects(() -> service.addUser(" ALICE "));
            rejects(() -> service.addUser(" "));
            rejects(() -> service.addTask(" ", "", "School", "Alice"));
            rejects(() -> service.addTask("Title", "", "School", "Nobody"));
            String title = "Report, \"draft\"";
            String description = "First line\r\nSecond line: café 😀";
            Task original = service.addTask(title, description, "School", "alice");
            service.updateTaskStatus(original.getId(), TaskStatus.COMPLETED);
            check(original.getStatus() == TaskStatus.PENDING, "Task snapshots must be immutable");
            TodoListService loaded = new TodoListService(csv);
            Task restored = loaded.getAllTasks().get(0);
            check(restored.getTitle().equals(title) && restored.getDescription().equals(description), "CSV escaping round trip");
            check(restored.getCreatedAt().equals(original.getCreatedAt()), "Timestamp round trip");
            check(restored.getStatus() == TaskStatus.COMPLETED && loaded.getUsers().size() == 2, "Status and users restored");
            check(loaded.getTasksByUser("ALICE").size() == 1, "User view");
            check(loaded.getTasksByCategory("school").size() == 1, "Category view");
            System.out.println("\n=== Concurrency verification ===");
            System.out.println("Registered users: " + service.getUsers().size() + " " + service.getUsers());
            System.out.println("Users are task assignees; worker threads simulate operations, not separate logged-in users.");
            System.out.println("All workers share ONE TodoListService and ONE temporary CSV file.");
            System.out.println("Protection: synchronized service methods serialize access and CSV saves;");
            System.out.println("immutable Task snapshots prevent changes outside the service lock.");
            List<Callable<Void>> actions = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                final int n = i;
                actions.add(() -> { service.addTask("Task " + n, "", "Work", n % 2 == 0 ? "Alice" : "Bob"); return null; });
            }
            together("Concurrent additions", actions);
            check(service.getTaskIds().size() == 81, "Concurrent additions must retain unique IDs");
            check(new TodoListService(csv).getAllTasks().size() == 81, "Concurrent additions persisted");
            check(service.getTasksByUser("Alice").size() == 41 && service.getTasksByUser("Bob").size() == 40,
                    "Concurrent additions retain the correct assignees");
            System.out.println("PASS: 80 additions + 1 existing task = 81 unique tasks in memory and CSV.");
            System.out.println("Assignments verified: Alice 41, Bob 40. No additions lost in this run.");
            actions.clear();
            for (int i = 0; i < 30; i++) {
                TaskStatus status = TaskStatus.values()[i % 3];
                actions.add(() -> { service.updateTaskStatus(original.getId(), status); return null; });
            }
            together("Competing updates to the same task", actions);
            check(new TodoListService(csv).getAllTasks().get(0).getStatus() == service.getAllTasks().get(0).getStatus(), "Concurrent status save agrees with memory");
            System.out.println("PASS: all 30 updates completed; final status in CSV matches memory: "
                    + service.getAllTasks().get(0).getStatus());
            System.out.println("The last update to obtain the lock wins; its status may differ between runs.");
            java.util.concurrent.atomic.AtomicReference<String> updateOutcome = new java.util.concurrent.atomic.AtomicReference<>();
            together("Update/delete conflict", List.of(
                () -> { check(service.deleteTask(original.getId()), "Concurrent deletion succeeds"); return null; },
                () -> {
                    try {
                        service.updateTaskStatus(original.getId(), TaskStatus.COMPLETED);
                        updateOutcome.set("Update succeeded before deletion.");
                    }
                    catch (IllegalArgumentException expected) {
                        check(expected.getMessage().startsWith("Task not found"), "Expected missing task");
                        updateOutcome.set("Update was rejected because deletion happened first (expected).");
                    }
                    return null;
                }));
            check(!new TodoListService(csv).getTaskIds().contains(original.getId()), "Update must not resurrect deleted task");
            check(!service.getTaskIds().contains(original.getId()), "Deleted task absent from memory");
            System.out.println(updateOutcome.get());
            System.out.println("PASS: task absent from memory and CSV; update did not resurrect it.");
            System.out.println("These checks verify the tested schedules within one service instance, not every possible ordering or multiple processes.");
            int largest = Collections.max(service.getTaskIds());
            service.deleteTask(largest);
            check(new TodoListService(csv).addTask("Next", "", "Work", "Bob").getId() > largest, "IDs survive deletion and restart");
            Path bad = dir.resolve("bad.csv");
            Files.writeString(bad, "invalid data");
            rejects(() -> new TodoListService(bad));
            check(Files.readString(bad).equals("invalid data"), "Invalid storage must not be overwritten");
            Path blocked = dir.resolve("blocked");
            TodoListService failing = new TodoListService(blocked.resolve("todo.csv"));
            Files.writeString(blocked, "not a directory");
            try { failing.addUser("Alice"); throw new AssertionError("Expected save failure"); }
            catch (java.io.UncheckedIOException expected) { check(failing.getUsers().isEmpty(), "Failed save must not change memory"); }
            System.out.println("PASS: validation, CSV round trips, views, concurrent additions/updates/deletion, ID continuity, malformed storage, save rollback");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
