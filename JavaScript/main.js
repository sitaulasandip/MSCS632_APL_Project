const { TodoListService, TaskStatus } = require('./todoListService');
const readline = require('readline');

let service;
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Async iteration retains piped lines as well as interactive terminal input.
const lines = rl[Symbol.asyncIterator]();
const EOF = Symbol('EOF');
async function ask(question) {
  process.stdout.write(question);
  const result = await lines.next();
  if (result.done) throw EOF;
  return result.value;
}
function parseId(value) {
  if (!/^[+-]?\d+$/.test(value.trim())) throw new Error('Task ID must be an integer');
  const id = Number(value);
  if (!Number.isInteger(id) || id < -2147483648 || id > 2147483647) throw new Error('Invalid task ID');
  return id;
}
function printUsers() {
  const users = service.getUsers();
  if (!users.length) return console.log('No users registered. Choose option 12 to add a user.');
  console.log(`Registered users (${users.length}):`);
  users.forEach(user => console.log(`- ${user.name}`));
}

function printTasks(tasks) {
  if (tasks.length === 0) {
    console.log('No tasks found.');
    return;
  }

  tasks.forEach((task) => console.log(task.toString()));
}

async function addTaskFlow() {
  const title = (await ask('Title: ')).trim();
  const description = (await ask('Description: ')).trim();
  const category = (await ask('Category: ')).trim();
  const assignedTo = (await ask('Assigned to: ')).trim();

  const task = service.addTask(title, description, category, assignedTo);
  console.log(`Added task: ${task.id}`);
}

async function updateStatusFlow() {
  const taskId = parseId(await ask('Task ID: '));
  const statusInput = (await ask('New status (PENDING / IN_PROGRESS / COMPLETED): ')).trim().toUpperCase();

  const status = Object.values(TaskStatus).includes(statusInput) ? statusInput : null;
  if (!status) {
    console.log('Invalid status. Please use PENDING, IN_PROGRESS, or COMPLETED.');
    return;
  }

  const updated = service.updateTaskStatus(taskId, status);
  console.log(`Updated task: ${updated}`);
}

async function deleteTaskFlow() {
  const taskId = parseId(await ask('Task ID: '));
  const removed = service.deleteTask(taskId);
  console.log(removed ? 'Task deleted.' : 'Task not found.');
}

async function simulateConcurrentUpdates() {
  const task = service.getAllTasks()[0];
  if (!task) return console.log('Add a task first. The demo updates the first task from three asynchronous operations.');
  console.log('3 asynchronous operations share one service on one event-loop thread.');
  console.log('Each operation and CSV save completes before the next callback runs.');
  const results = await Promise.allSettled(Object.values(TaskStatus).map(status =>
    new Promise((resolve, reject) => setImmediate(() => {
      try { resolve(service.updateTaskStatus(task.id, status)); } catch (error) { reject(error); }
    }))));
  for (const result of results) {
    console.log(result.status === 'fulfilled' ? `Operation saved: ${result.value}` : `Concurrent update failed: ${result.reason.message}`);
  }
  console.log('Concurrent operations finished; last serialized update wins.');
  console.log(service.getSummary());
}

async function main() {
  service = new TodoListService(process.argv[2] || 'data/todo.csv');
  console.log('Changes are saved automatically. Add users before assigning tasks.');

  while (true) {
    console.log('\n=== Collaborative To-Do List (JavaScript) ===');
    console.log('1. Add task');
    console.log('2. Update task status');
    console.log('3. Delete task');
    console.log('4. View all tasks');
    console.log('5. Filter by user');
    console.log('6. Filter by category');
    console.log('7. Filter by status');
    console.log('8. Search tasks');
    console.log('9. Show summary');
    console.log('10. Simulate concurrent updates');
    console.log('11. Exit');
    console.log('12. Add user');
    console.log('13. View users');

    try {
    const input = (await ask('Choose an option: ')).trim();

    switch (input) {
      case '12':
        console.log(`Added user: ${service.addUser(await ask('User name: '))}`);
        break;
      case '13':
        printUsers();
        break;
      case '1':
        await addTaskFlow();
        break;
      case '2':
        await updateStatusFlow();
        break;
      case '3':
        await deleteTaskFlow();
        break;
      case '4':
        printTasks(service.getAllTasks());
        break;
      case '5': {
        const userName = (await ask('Enter user name: ')).trim();
        printTasks(service.getTasksByUser(userName));
        break;
      }
      case '6': {
        const category = (await ask('Enter category: ')).trim();
        printTasks(service.getTasksByCategory(category));
        break;
      }
      case '7': {
        const statusInput = (await ask('Enter status (PENDING / IN_PROGRESS / COMPLETED): ')).trim().toUpperCase();
        const status = Object.values(TaskStatus).includes(statusInput) ? statusInput : null;
        if (!status) {
          console.log('Invalid status. Please use PENDING, IN_PROGRESS, or COMPLETED.');
        } else {
          printTasks(service.getTasksByStatus(status));
        }
        break;
      }
      case '8': {
        const keyword = (await ask('Enter keyword: ')).trim();
        printTasks(service.searchTasks(keyword));
        break;
      }
      case '9':
        console.log(service.getSummary());
        break;
      case '10':
        await simulateConcurrentUpdates();
        break;
      case '11':
        console.log('Exiting JavaScript to-do list app.');
        rl.close();
        return;
      default:
        console.log('Invalid option. Try again.');
    }
    } catch (error) {
      if (error === EOF) return;
      console.log(`Error: ${error.message}`);
    }
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
}).finally(() => rl.close());
