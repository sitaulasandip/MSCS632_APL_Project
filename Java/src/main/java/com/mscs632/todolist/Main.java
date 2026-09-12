package com.mscs632.todolist;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final TodoListService service = new TodoListService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        seedSampleData();

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
            System.out.print("Choose an option: ");

            String input = scanner.nextLine().trim();

            switch (input) {
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
        }
    }

    private static void seedSampleData() {
        service.addTask("Write project report", "Draft the group report for the APL project", "School", "Alice");
        service.addTask("Prepare presentation", "Build slides for the demo", "Presentation", "Bob");
        service.addTask("Review code", "Inspect the Java implementation for bugs", "Development", "Alice");
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
        ExecutorService pool = Executors.newFixedThreadPool(3);

        pool.submit(() -> service.updateTaskStatus(1, TaskStatus.IN_PROGRESS));
        pool.submit(() -> service.updateTaskStatus(2, TaskStatus.COMPLETED));
        pool.submit(() -> service.updateTaskStatus(3, TaskStatus.IN_PROGRESS));

        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Concurrent update simulation interrupted.");
        }

        System.out.println("Concurrent updates complete.");
        System.out.println(service.getSummary());
    }
}
