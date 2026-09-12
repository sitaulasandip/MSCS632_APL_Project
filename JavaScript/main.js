const { TodoListService, TaskStatus } = require('./todoListService');
const readline = require('readline');

const service = new TodoListService();
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function ask(question) {
  return new Promise((resolve) => {
    rl.question(question, (answer) => resolve(answer));
  });
}

function printTasks(tasks) {
  if (tasks.length === 0) {
    console.log('No tasks found.');
    return;
  }

  tasks.forEach((task) => console.log(task.toString()));
}

function seedSampleData() {
  service.addTask('Write project report', 'Draft the group report for the APL project', 'School', 'Alice');
  service.addTask('Prepare presentation', 'Build slides for the demo', 'Presentation', 'Bob');
  service.addTask('Review code', 'Inspect the Java implementation for bugs', 'Development', 'Alice');
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
  const taskId = Number.parseInt((await ask('Task ID: ')).trim(), 10);
  const statusInput = (await ask('New status (PENDING / IN_PROGRESS / COMPLETED): ')).trim().toUpperCase();

  const status = TaskStatus[statusInput];
  if (!status) {
    console.log('Invalid status. Please use PENDING, IN_PROGRESS, or COMPLETED.');
    return;
  }

  const updated = service.updateTaskStatus(taskId, status);
  console.log(`Updated task: ${updated}`);
}

async function deleteTaskFlow() {
  const taskId = Number.parseInt((await ask('Task ID: ')).trim(), 10);
  const removed = service.deleteTask(taskId);
  console.log(removed ? 'Task deleted.' : 'Task not found.');
}

async function simulateConcurrentUpdates() {
  const tasks = [
    new Promise((resolve) => setTimeout(() => {
      service.updateTaskStatus(1, TaskStatus.IN_PROGRESS);
      resolve();
    }, 20)),
    new Promise((resolve) => setTimeout(() => {
      service.updateTaskStatus(2, TaskStatus.COMPLETED);
      resolve();
    }, 10)),
    new Promise((resolve) => setTimeout(() => {
      service.updateTaskStatus(3, TaskStatus.IN_PROGRESS);
      resolve();
    }, 30))
  ];

  await Promise.all(tasks);
  console.log('Concurrent updates complete.');
  console.log(service.getSummary());
}

async function main() {
  seedSampleData();

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

    const input = (await ask('Choose an option: ')).trim();

    switch (input) {
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
        const status = TaskStatus[statusInput];
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
  }
}

main().catch((error) => {
  console.error(error.message);
  rl.close();
});
