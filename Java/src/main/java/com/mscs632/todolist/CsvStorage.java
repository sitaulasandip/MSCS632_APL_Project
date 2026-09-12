package com.mscs632.todolist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** One CSV snapshot keeps user records, tasks and the ID counter consistent. */
final class CsvStorage {
    private static final List<String> HEADER = List.of("type", "id", "title", "description", "category", "user", "createdAt", "status");
    record State(Map<Integer, Task> tasks, Map<String, User> users, int nextId) {}

    static State load(Path path) {
        Map<Integer, Task> tasks = new TreeMap<>();
        Map<String, User> users = new TreeMap<>();
        if (Files.notExists(path)) return new State(tasks, users, 1);
        try {
            List<List<String>> rows = parse(Files.readString(path, StandardCharsets.UTF_8));
            if (rows.isEmpty() || !rows.remove(0).equals(HEADER)) throw new IllegalArgumentException("Invalid CSV header");
            int nextId = 0;
            for (List<String> r : rows) {
                if (r.size() != 8) throw new IllegalArgumentException("Each CSV row must have 8 columns");
                switch (r.get(0)) {
                    case "META" -> {
                        if (nextId != 0) throw new IllegalArgumentException("Duplicate metadata");
                        nextId = Integer.parseInt(r.get(1));
                        if (nextId < 1) throw new IllegalArgumentException("Invalid next ID");
                    }
                    case "USER" -> {
                        User user = new User(r.get(5));
                        if (users.putIfAbsent(user.name().toLowerCase(Locale.ROOT), user) != null)
                            throw new IllegalArgumentException("Duplicate user");
                    }
                    case "TASK" -> {
                        Task task = new Task(Integer.parseInt(r.get(1)), r.get(2), r.get(3), r.get(4), r.get(5),
                                LocalDateTime.parse(r.get(6)), TaskStatus.valueOf(r.get(7)));
                        if (tasks.putIfAbsent(task.getId(), task) != null) throw new IllegalArgumentException("Duplicate task ID");
                    }
                    default -> throw new IllegalArgumentException("Unknown CSV record type");
                }
            }
            if (nextId == 0 || tasks.keySet().stream().anyMatch(id -> id <= 0))
                throw new IllegalArgumentException("Missing or invalid metadata");
            for (Task task : tasks.values()) {
                if (task.getId() >= nextId) throw new IllegalArgumentException("Invalid next ID");
                if (!users.containsKey(task.getAssignedTo().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Task refers to an unknown user");
            }
            return new State(tasks, users, nextId);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read CSV: " + path, e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot load CSV " + path + ": " + e.getMessage(), e);
        }
    }

    static void save(Path path, Map<Integer, Task> tasks, Map<String, User> users, int nextId) {
        StringBuilder out = new StringBuilder();
        row(out, HEADER);
        row(out, List.of("META", "" + nextId, "", "", "", "", "", ""));
        for (User user : users.values()) row(out, List.of("USER", "", "", "", "", user.name(), "", ""));
        for (Task t : tasks.values()) row(out, List.of("TASK", "" + t.getId(), t.getTitle(), t.getDescription(),
                t.getCategory(), t.getAssignedTo(), t.getCreatedAt().toString(), t.getStatus().name()));
        Path temp = null;
        try {
            Files.createDirectories(path.getParent());
            temp = Files.createTempFile(path.getParent(), "todo-", ".tmp");
            Files.writeString(temp, out, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save CSV; change was not applied: " + path, e);
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private static void row(StringBuilder out, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append('"').append(cells.get(i).replace("\"", "\"\"")).append('"');
        }
        out.append("\r\n");
    }

    /** Handles quoted commas, escaped quotes and multiline fields; rejects malformed input. */
    private static List<List<String>> parse(String text) {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false, closed = false, started = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else cell.append(c);
            } else if (c == ',' || c == '\r' || c == '\n') {
                row.add(cell.toString()); cell.setLength(0); closed = false; started = false;
                if (c != ',') {
                    rows.add(row); row = new ArrayList<>();
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                }
            } else if (c == '"' && !started && !closed) {
                quoted = true; started = true;
            } else {
                if (closed || c == '"') throw new IllegalArgumentException("Malformed CSV quoting");
                cell.append(c); started = true;
            }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        if (started || closed || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }
}
