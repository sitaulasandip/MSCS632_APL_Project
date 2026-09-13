package com.mscs632.todolist;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static TodoListService service;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            service = new TodoListService(java.nio.file.Path.of(args.length == 0 ? "data/todo.csv" : args[0]));
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Changes are saved automatically. Add users before assigning tasks.");

        while (true) {
            System.out.println("\n=== Collaborative To-Do List (Java) ===");
            System.out.println("1. Add task");
            System.out.println("2. Update task status");
            System.out.println("3. Delete task");
            System.out.println("4. View all tasks");
            System.out.println("5. Filter by user");
            System.out.println("6. Filter by category");
            System.out.println("7. Filter by status");
            System.out.println("8. Search tasks");
            System.out.println("9. Show summary");
            System.out.println("10. Simulate concurrent updates");
            System.out.println("11. Exit");
            System.out.println("12. Add user");
            System.out.println("13. View users");
            System.out.print("Choose an option: ");
            if (!scanner.hasNextLine()) return;

            String input = scanner.nextLine().trim();

            try {
            switch (input) {
                case "12":
                    System.out.print("User name: ");
                    System.out.println("Added user: " + service.addUser(scanner.nextLine()));
                    break;
                case "13":
                    printUsers(service.getUsers());
                    break;
                case "1":
                    addTaskFlow();
                    break;
                case "2":
                    updateStatusFlow();
                    break;
                case "3":
                    deleteTaskFlow();
                    break;
                case "4":
                    printTasks(service.getAllTasks());
                    break;
                case "5":
                    System.out.print("Enter user name: ");
                    printTasks(service.getTasksByUser(scanner.nextLine().trim()));
                    break;
                case "6":
                    System.out.print("Enter category: ");
                    printTasks(service.getTasksByCategory(scanner.nextLine().trim()));
                    break;
                case "7":
                    System.out.print("Enter status (PENDING / IN_PROGRESS / COMPLETED): ");
                    printTasks(service.getTasksByStatus(TaskStatus.valueOf(scanner.nextLine().trim().toUpperCase())));
                    break;
                case "8":
                    System.out.print("Enter keyword: ");
                    printTasks(service.searchTasks(scanner.nextLine().trim()));
                    break;
                case "9":
                    System.out.println(service.getSummary());
                    break;
                case "10":
                    simulateConcurrentUpdates();
                    break;
                case "11":
                    System.out.println("Exiting Java to-do list app.");
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
            } catch (IllegalArgumentException | java.io.UncheckedIOException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (java.util.NoSuchElementException e) {
                return;
            }
        }
    }

    private static void addTaskFlow() {
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        System.out.print("Category: ");
        String category = scanner.nextLine().trim();
        System.out.print("Assigned to: ");
        String assignedTo = scanner.nextLine().trim();

        Task task = service.addTask(title, description, category, assignedTo);
        System.out.println("Added task: " + task.getId());
    }

    private static void updateStatusFlow() {
        System.out.print("Task ID: ");
        int taskId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("New status (PENDING / IN_PROGRESS / COMPLETED): ");
        TaskStatus status = TaskStatus.valueOf(scanner.nextLine().trim().toUpperCase());

        Task updated = service.updateTaskStatus(taskId, status);
        System.out.println("Updated task: " + updated);
    }

    private static void deleteTaskFlow() {
        System.out.print("Task ID: ");
        int taskId = Integer.parseInt(scanner.nextLine().trim());
        boolean removed = service.deleteTask(taskId);
        System.out.println(removed ? "Task deleted." : "Task not found.");
    }

    private static void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users registered. Choose option 12 to add a user.");
            return;
        }
        System.out.println("Registered users (" + users.size() + "):");
        for (User user : users) {
            System.out.println("- " + user.name());
        }
    }

    private static void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        for (Task task : tasks) {
            System.out.println(task);
        }
    }

    private static void simulateConcurrentUpdates() {
        List<Task> tasks = service.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("Add a task first. The demo updates the first task from three worker threads.");
            return;
        }
        int id = tasks.get(0).getId();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<Task>> results = new java.util.ArrayList<>();
        for (TaskStatus status : TaskStatus.values()) {
            results.add(pool.submit(() -> { start.await(); return service.updateTaskStatus(id, status); }));
        }
        start.countDown();
        pool.shutdown();
        try {
            for (var result : results) System.out.println("Worker saved: " + result.get());
            System.out.println("Concurrent updates complete; last serialized update wins.");
            System.out.println(service.getSummary());
        } catch (java.util.concurrent.ExecutionException e) {
            System.out.println("Concurrent update failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Concurrent update simulation interrupted.");
        } finally {
            pool.shutdownNow();
        }
    }
}
