# Collaborative To-Do List — Java

Requires JDK 17 or later; no external dependencies.

From the `Java` directory:

```sh
javac -d out src/main/java/com/mscs632/todolist/*.java
java -cp out com.mscs632.todolist.Main
```

On PowerShell, compile using:

```powershell
$files = Get-ChildItem src/main/java/com/mscs632/todolist -Filter '*.java' | Select-Object -ExpandProperty FullName
javac -d out $files
java -cp out com.mscs632.todolist.Main
```

Use option 12 to register users, then option 1 to add and assign tasks. Option 13 lists users. Existing menu options support deleting, updating status, filtering by user/category/status, searching, and summaries. Invalid input returns to the menu. A new store starts empty.

## Design

- `Main`: CLI input and output, validation feedback, and a thread-based concurrency demonstration.
- `User`: immutable named user. Usernames are unique ignoring case; task assignments must refer to a registered user.
- `Task`: immutable task with ID, title, description, category, assigned username, creation time, and status. The username links the task to its `User` record.
- `TaskStatus`: pending, in progress, and completed states.
- `TodoListService`: shared users and tasks, queries, validation, and synchronized mutations. Returned tasks are immutable snapshots. A successful status update replaces the stored task.
- `CsvStorage`: UTF-8 CSV reading, validation, and atomic snapshot replacement.

This demonstrates encapsulation, separation of responsibilities, immutable objects, and threads. Users represent task assignees; there is no authentication or ownership-based access restriction.

## CSV persistence

The default path is `data/todo.csv`, relative to the working directory. An optional first argument selects a different file:

```sh
java -cp out com.mscs632.todolist.Main /path/to/todo.csv
```

Every successful mutation saves automatically. One CSV contains USER, TASK, and META rows under the columns `type,id,title,description,category,user,createdAt,status`. META preserves the next task ID even after tasks are deleted. This single snapshot avoids inconsistencies between separate user and task files.

Commas, quotes, multiline descriptions, and Unicode are supported. Open/import the file in Excel as UTF-8 CSV; import text columns as Text to preserve values and avoid interpreting task text as spreadsheet formulas. Close the app before editing the file. Preserve the header, record types, IDs, and valid user references; Excel exports must remain UTF-8 CSV. Invalid storage stops startup without overwriting it.

Saves write a temporary file beside the CSV and atomically replace the previous snapshot. If saving fails, the service retains its previous in-memory state and reports an error. Storage must support atomic replacement; there is no non-atomic fallback.

## Concurrency and limits

One service instance is shared by worker threads. Its synchronized methods serialize reads, mutations, and file saves. Concurrent updates to the same task use last-serialized-update-wins behavior. An update ordered after deletion fails; an update ordered before deletion succeeds and is then removed. Queries see consistent snapshots.

Menu option 10 starts three worker threads together to update the first existing task to different statuses. It uses actual task IDs, waits for results, and reports worker failures. The final status is intentionally nondeterministic, and the changes are persisted.

This supports simulated concurrent users within one process. Do not run multiple service instances/processes or Excel writers against the same CSV: there is no cross-process locking or shared live cache. Full snapshots and a single lock suit a small course project; a database/server would be more appropriate for independent concurrent clients.

## Automated verification

From `Java`:

```sh
javac -d out src/main/java/com/mscs632/todolist/*.java src/test/java/com/mscs632/todolist/*.java
java -cp out com.mscs632.todolist.TodoListServiceTest
```

The dependency-free test runner uses temporary files and throws on failure. It checks input validation, CSV escaping and reload, timestamps/status/users, user/category views, 80 concurrent additions, competing status updates, update/delete conflicts, ID continuity, malformed storage rejection, and rollback after a failed save. Worker exceptions and timeouts fail the run.

Group assignments, a dated project timeline, and the design for the JavaScript implementation should be documented separately to meet the full planning rubric.

### Run unit tests separately

```sh
java -cp out com.mscs632.todolist.TodoListUnitTest
```

Compile with the automated verification command above first. `TodoListUnitTest` reports named cases for empty storage, user registration, task creation/validation, status transitions, deletion, all filters, search, summaries, and collection isolation. Each case uses a fresh in-memory service. `TodoListServiceTest` runs these unit tests first, then the existing CSV and concurrency integration checks. A failure exits with an exception and a nonzero status. These tests do not replace a later manual check of the interactive CLI or Excel import/export.
