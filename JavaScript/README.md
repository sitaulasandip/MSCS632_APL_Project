# Collaborative To-Do List — JavaScript

Requires Node.js 18 or later. No external packages are needed.

From the `JavaScript` directory:

```sh
npm start
npm test
```

The test command runs all named cases and prints concurrency explanations. A failure returns a nonzero exit code. Tests use temporary files and do not modify application data.

## Feature parity with Java

Both applications offer the same numbered menu: add task (1), update status (2), delete (3), view tasks (4), filter by user/category/status (5–7), search (8), summary (9), concurrency demo (10), exit (11), add user (12), and view users (13).

A new store starts empty. Register users before assigning tasks. Names are unique ignoring case. Blank titles/categories/users and invalid statuses are rejected; descriptions can be empty. Invalid CLI input returns to the menu. Tasks and users are immutable snapshots. Status updates retain the original creation time.

## Structure

- `models.js`: immutable `User`, `Task`, and `TaskStatus`; field validation.
- `todoListService.js`: private maps, user registration, task operations, filtering, search, summaries, and save-before-publish mutations.
- `csvStorage.js`: CSV parsing, validation, and atomic snapshot replacement using a temporary file in the same directory.
- `main.js`: CLI, input recovery, user display, and asynchronous update demonstration.
- `test/todo.test.js`: unit, persistence, concurrency, and CLI integration tests.

## Storage and Excel

Changes automatically save to `data/todo.csv`, relative to the working directory. Select a custom file with:

```sh
node main.js /path/to/todo.csv
```

The schema matches Java: `type,id,title,description,category,user,createdAt,status`, with USER, TASK, and META records. META preserves the next task ID after deletion. Both programs can read and write the same CSV sequentially. Creation times use local wall-clock timestamps without a timezone suffix, matching Java's LocalDateTime.

CSV supports quotes, commas, multiline descriptions, and Unicode. Import into Excel as UTF-8, with text columns treated as Text to retain values and avoid interpreting task content as formulas. Close the app before editing/exporting CSV. Preserve the schema and valid IDs/references. Malformed storage stops startup without overwriting it. Failed saves leave in-memory state unchanged.

## Concurrency: same behavior, different language mechanism

Java uses worker threads and synchronized service methods. JavaScript schedules asynchronous operations using `setImmediate` and Promises on ONE event-loop thread. Every service operation and file save is synchronous with no `await`, so another callback cannot interrupt it. Frozen objects and private maps prevent callers from bypassing the service. This demonstrates asynchronous concurrency, not parallel worker-thread execution.

Option 10 schedules three competing status updates to the first existing task, collects successes/failures, and persists them. The last serialized update wins. The tests explain the two user records (Alice/Bob), one event-loop thread, 80 additions, 30 competing updates, and both update/delete orderings. Users are assignees, not authenticated sessions. Tests check unique IDs, assignments, full CSV/memory agreement, and absence of deleted tasks.

Both designs are scoped to one shared service instance. Do not use separate app instances or Excel writers against the same file simultaneously. Synchronous full-snapshot writes block the JavaScript event loop; this is acceptable for the small course demo, but independent clients and large data sets would need a different storage/access design.

## Verification scope

Tests cover empty storage, users, task validation, status transitions, deletion, all filters, search, summaries, collection isolation, CSV round trips, ID continuity, failed saves, malformed input, concurrent operations, and CLI add/view users, error recovery, demo, and restart. They provide evidence for tested scenarios rather than proof of every possible schedule. Manual Excel import/export can be checked separately.

Group roles and dated milestones must still be documented separately for the planning rubric.
