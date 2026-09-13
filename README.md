# MSCS632 Advanced Programming Languages Project

## Purpose

This project implements the same collaborative command-line to-do list in **Java** and **JavaScript (Node.js)**. It compares how the languages express similar functionality, with particular attention to data structures, memory management, concurrency, and error handling.

Both applications support registered users, task assignment, categories, status tracking, search, summaries, and CSV persistence. Their user-facing features are aligned, while their implementations demonstrate language-specific approaches. Users are task assignees; the applications do not provide login sessions or ownership-based permissions.

## Project structure

```text
MSCS632_APL_Project/
├── README.md                         # Shared guide and language comparison
├── Java/
│   ├── README.md                     # Java implementation details
│   ├── src/main/java/com/mscs632/todolist/
│   │   ├── Main.java                 # CLI and concurrency demo
│   │   ├── User.java                 # Immutable user record
│   │   ├── Task.java                 # Immutable task class
│   │   ├── TaskStatus.java           # Status enum
│   │   ├── TodoListService.java      # Shared task/user operations
│   │   └── CsvStorage.java           # CSV loading and saving
│   └── src/test/java/com/mscs632/todolist/
│       ├── TodoListUnitTest.java
│       └── TodoListServiceTest.java
└── JavaScript/
    ├── README.md                     # JavaScript implementation details
    ├── package.json                  # App and test commands
    ├── main.js                       # CLI and concurrency demo
    ├── models.js                     # User, Task, and TaskStatus
    ├── todoListService.js            # Shared task/user operations
    ├── csvStorage.js                 # CSV loading and saving
    └── test/todo.test.js              # Unit and integration tests
```

Each language creates its own `data/todo.csv` when run from its application directory. Java compilation creates `Java/out/`. Generated data and build output are ignored by Git.

## Prerequisites

- Java: JDK 17 or later, with `java` and `javac` on PATH.
- JavaScript: Node.js 18 or later and npm.
- A terminal; no GUI, database, or external libraries are required.

Check installed tools:

```sh
java -version
javac -version
node --version
npm --version
```

## Run the applications

**The following instructions start from the project root**, the directory containing both `Java` and `JavaScript`. Folder names are case-sensitive on some systems. Quote paths containing spaces.

### Java — macOS/Linux

```sh
cd Java
javac -d out src/main/java/com/mscs632/todolist/*.java
java -cp out com.mscs632.todolist.Main
```

Recompile after source changes, and restart any already-running app to use the new code.

### Java — Windows PowerShell

```powershell
cd Java
$javaSources = Get-ChildItem src/main/java/com/mscs632/todolist -Filter '*.java' | Select-Object -ExpandProperty FullName
javac -d out $javaSources
java -cp out com.mscs632.todolist.Main
```

### JavaScript — all platforms

From the project root:

```sh
cd JavaScript
npm start
```

No `npm install` is needed because the project has no external package dependencies. JavaScript source changes take effect on the next app launch.

**If your terminal is already inside `Java`, use `cd ../JavaScript`** to switch applications. From `JavaScript`, use `cd ../Java` to switch back. Running `cd JavaScript` from inside `Java` looks for a nonexistent nested directory. A missing npm `start` script usually means the terminal is outside this project's `JavaScript` folder.

## Use either application

A new data store starts empty. Choose **12** to add a user, **13** to confirm the user exists, then **1** to create a task assigned to that user.

| Option | Action |
|---|---|
| 1 | Add a task with title, description, category, and assigned user |
| 2 | Update a task's status using its ID |
| 3 | Delete a task using its ID |
| 4 | View all tasks and their IDs |
| 5 | Filter tasks by assigned user |
| 6 | Filter tasks by category |
| 7 | Filter tasks by status |
| 8 | Search titles, descriptions, categories, and assigned usernames |
| 9 | View totals by status and category |
| 10 | Run competing updates on the first existing task |
| 11 | Exit |
| 12 | Register a user |
| 13 | View registered users |

Statuses are `PENDING`, `IN_PROGRESS`, and `COMPLETED`. New tasks start pending. Titles, categories, and usernames must not be blank; descriptions may be empty. Usernames are unique ignoring case, and assignments must reference a registered user. Invalid CLI input produces feedback and returns to the menu.

Option 10 **changes and saves the selected task's status**. Use disposable tasks when demonstrating it.

## Language comparison

| Area | Java implementation | JavaScript implementation | Practical comparison |
|---|---|---|---|
| Type system | Declared types, generics, and a `TaskStatus` enum | Dynamic types, explicit runtime checks, and a frozen status object | Java catches many type errors at compilation; JavaScript validates values during execution. Both still need input validation. |
| Object modeling | `Task` class, `User` record, private fields, and service methods | Classes, frozen model instances, and private `#` service fields | Both separate domain objects, business operations, storage, and UI. |
| Task/user storage | `TreeMap<Integer, Task>` and `TreeMap<String, User>` | `Map` instances keyed by numeric IDs and normalized usernames | Java's tree maps maintain key order; JavaScript explicitly sorts returned records where needed. |
| Queries | Streams, predicates, and collectors | Array `filter`, `some`, and other array operations | Both express filtering and search as collection transformations. |
| Snapshot protection | Final task fields and immutable user records; copied result collections | `Object.freeze` on models and copied arrays/sets | Callers cannot mutate stored model values or change service maps through query results. |
| Memory management | JVM garbage collection | Node.js JavaScript engine garbage collection | Neither implementation manually frees objects. Unreachable replaced tasks and temporary collections become eligible for collection. |
| Concurrency | Executor worker threads, a start latch, and synchronized service methods | Promises and `setImmediate` callbacks on one event-loop thread | Java demonstrates thread contention; JavaScript demonstrates asynchronous operations scheduled on a shared event loop. |
| Consistency | The service monitor serializes reads, mutations, and saves | Synchronous methods and saves finish without yielding to another callback | Both prevent overlapping changes within one shared service instance. |
| Error handling | Exceptions such as `IllegalArgumentException`, `UncheckedIOException`, and worker failures exposed through `Future.get()` | Thrown `Error` objects, `try/catch`, Promise rejection, and settled results | Both report validation/storage failures and inspect background operation results. |
| Persistence | Java NIO temporary file followed by atomic replacement | Node filesystem temporary file followed by rename | Both save a full snapshot before publishing new in-memory state. |
| Running | Compile with `javac`, then run JVM bytecode with `java` | Launch source using Node.js through `npm start` | Java has an explicit project compilation step; JavaScript has no separate build command here. |

### Memory and storage tradeoffs

Both services retain all users and tasks in memory. An update creates a new immutable task and copies the relevant map; queries also allocate result collections. These choices simplify consistency and prevent external mutation, but allocate more temporary objects than in-place updates.

Each successful mutation writes a full CSV snapshot. This is suitable for a small demonstration, but cost grows with the data set. Java holds the service lock during saving, so other workers wait. JavaScript uses synchronous file operations, so saving blocks the event loop. No benchmark in this repository establishes that either version is faster or uses less memory.

### What concurrency means here

Both versions allow multiple simulated operations to access **one shared service**. Registered user counts and execution-thread counts are different concepts.

| Scenario | Java | JavaScript |
|---|---|---|
| Menu demo | 3 worker threads update the same task | 3 asynchronous callbacks update the same task on 1 event-loop thread |
| Addition test | 80 additions using up to 8 worker threads | 80 scheduled asynchronous additions on 1 event-loop thread |
| User records in the concurrency test | Alice and Bob | Alice and Bob |
| Competing status test | 30 updates using up to 8 worker threads | 30 scheduled updates on the event loop |
| Update/delete conflict | Competing update and delete; either ordering is accepted | Both valid callback orderings are exercised explicitly |
| Final status rule | Last serialized update wins; worker ordering can vary | Last serialized callback update wins |

Java's synchronized methods establish mutual exclusion and visibility between threads. JavaScript does not use a thread lock: its service methods contain no `await`, and callbacks execute one at a time. Introducing asynchronous saving into those methods would require additional coordination; the present guarantee depends on their synchronous structure.

An update after deletion fails with a missing-task error. An update before deletion succeeds and is subsequently removed. Tests check that deleted tasks do not reappear and that saved data agrees with memory.

**Neither application supports simultaneous independent processes writing the same CSV.** There is no cross-process lock, shared live cache, or multi-client server. JavaScript does not use worker threads. Passing tests verifies the exercised scenarios, not every possible execution schedule.

## Challenges and Design Decisions

The implementations address the following design challenges:

| Challenge | Decision and tradeoff |
|---|---|
| Keeping both languages functionally aligned | Use the same menu, task statuses, user-assignment rules, and CSV schema while retaining language-specific implementations. |
| Coordinating competing task changes | Java synchronizes shared service operations; JavaScript completes each operation and save within one event-loop callback. Both serialize changes within one service instance. |
| Preventing accidental changes outside the service | Use immutable task/user models and copied query collections. This protects stored state but allocates additional objects. |
| Keeping saved data consistent with memory | Save a replacement CSV snapshot before publishing the new in-memory state. Failed saves leave the previous state intact; full-file writes add overhead as data grows. |
| Handling invalid input and CSV formatting | Validate user input and stored records, handle quoted/multiline CSV fields, and report errors. Invalid CLI input returns to the menu; invalid storage stops startup without overwriting the file. |

These choices suit a small CLI demonstration. Simultaneous independent clients would require additional storage coordination or a server/database design.

## CSV persistence and Excel

Both use the same UTF-8 CSV columns:

```csv
type,id,title,description,category,user,createdAt,status
```

- `USER` rows preserve registered users, including users without tasks.
- `TASK` rows preserve task fields, status, and creation time.
- `META` preserves the next ID so deleted IDs are not reused after restart.

Quoted commas, escaped quotes, multiline descriptions, and Unicode are supported. Creation times use local wall-clock timestamps without a timezone suffix. On startup the selected file is loaded; every successful mutation saves automatically. Invalid storage stops startup without overwriting it. A failed save leaves the previous in-memory state intact.

Default files, when using the run commands above:

- Java: `Java/data/todo.csv`
- JavaScript: `JavaScript/data/todo.csv`

These are separate data stores. To compare the implementations using one file, pass the same absolute path and run them **one at a time**:

```sh
# From Java/
java -cp out com.mscs632.todolist.Main "/absolute/path/to/shared-todo.csv"
```

```sh
# From JavaScript/, after the Java app has exited
node main.js "/absolute/path/to/shared-todo.csv"
```

Sequential Java → JavaScript → Java CSV exchange has been checked. This does not imply identical handling of every malformed external input or every locale-specific string.

For Excel, close the app and import the CSV as UTF-8. Import text columns as Text to preserve content and avoid formula interpretation. Preserve the header, row types, IDs, and valid user references when exporting back to CSV. Excel import/export remains a separate manual verification step.

## Run automated tests

### Java — macOS/Linux

From `Java/`:

```sh
javac -d out src/main/java/com/mscs632/todolist/*.java src/test/java/com/mscs632/todolist/*.java
java -cp out com.mscs632.todolist.TodoListServiceTest
```

This runs the 10 named unit-test cases followed by persistence and concurrency checks. To run only the unit cases after compilation:

```sh
java -cp out com.mscs632.todolist.TodoListUnitTest
```

### Java — Windows PowerShell

From `Java/`:

```powershell
$javaSources = Get-ChildItem src/main/java/com/mscs632/todolist, src/test/java/com/mscs632/todolist -Filter '*.java' | Select-Object -ExpandProperty FullName
javac -d out $javaSources
java -cp out com.mscs632.todolist.TodoListServiceTest
```

### JavaScript

From `JavaScript/`:

```sh
npm test
```

This runs 13 named tests, including CLI integration checks. Both suites cover task operations, users, validation, filtering, search, summaries, immutable snapshots, persistence, and concurrency. Temporary test files keep checks separate from application data. Failures return a nonzero exit status; concurrency output explains users, operations, execution mechanisms, and checked outcomes.

For a manual demonstration, register Alice and Bob, assign each a task, exercise filters and status updates, run option 10, then exit and restart to confirm persistence.

## Further documentation

See [Java details](Java/README.md) and [JavaScript details](JavaScript/README.md) for implementation-specific notes. Group-member responsibilities and a dated milestone timeline must be supplied separately to complete the planning rubric; this guide documents the implemented design and comparison.
