'use strict';
const { TaskStatus, Task, User } = require('./models');
const storage = require('./csvStorage');
const path = require('node:path');

class TodoListService {
  #tasks = new Map();
  #users = new Map();
  #nextId = 1;
  #file;
  constructor(file = null) {
    this.#file = file === null ? null : path.resolve(file);
    if (this.#file) {
      const state = storage.load(this.#file);
      this.#tasks = state.tasks; this.#users = state.users; this.#nextId = state.nextId;
    }
  }
  // No awaits: each operation and its save finish before another event-loop callback runs.
  // Publish new state only after the snapshot has been saved successfully.
  #commit(tasks, users, nextId) {
    if (this.#file) storage.save(this.#file, tasks, users, nextId);
    this.#tasks = tasks; this.#users = users; this.#nextId = nextId;
  }
  addUser(name) {
    const user = new User(name), key = user.name.toLowerCase();
    if (this.#users.has(key)) throw new Error(`User already exists: ${name}`);
    const users = new Map(this.#users); users.set(key, user);
    this.#commit(this.#tasks, users, this.#nextId);
    return user;
  }
  getUsers() { return [...this.#users.values()].sort((a, b) => a.name.localeCompare(b.name)); }
  addTask(title, description, category, assignedTo) {
    const user = this.#users.get(typeof assignedTo === 'string' ? assignedTo.trim().toLowerCase() : '');
    if (!user) throw new Error('Unknown user. Add the user first.');
    if (this.#nextId >= 2147483647) throw new Error('Task IDs exhausted');
    const task = new Task(this.#nextId, title, description, category, user.name);
    const tasks = new Map(this.#tasks); tasks.set(task.id, task);
    this.#commit(tasks, this.#users, this.#nextId + 1);
    return task;
  }
  updateTaskStatus(taskId, newStatus) {
    const task = this.#tasks.get(taskId);
    if (!task) throw new Error(`Task not found: ${taskId}`);
    const updated = task.withStatus(newStatus), tasks = new Map(this.#tasks);
    tasks.set(taskId, updated); this.#commit(tasks, this.#users, this.#nextId);
    return updated;
  }
  deleteTask(taskId) {
    if (!this.#tasks.has(taskId)) return false;
    const tasks = new Map(this.#tasks); tasks.delete(taskId);
    this.#commit(tasks, this.#users, this.#nextId); return true;
  }

  getAllTasks() {
    return Array.from(this.#tasks.values()).sort((a, b) => a.id - b.id);
  }

  getTasksByUser(userName) {
    const normalizedUser = (userName || '').trim().toLowerCase();
    return this.getAllTasks().filter((task) => task.assignedTo.toLowerCase() === normalizedUser);
  }

  getTasksByCategory(category) {
    const normalizedCategory = (category || '').trim().toLowerCase();
    return this.getAllTasks().filter((task) => task.category.toLowerCase() === normalizedCategory);
  }

  getTasksByStatus(status) {
    return this.getAllTasks().filter((task) => task.status === status);
  }

  searchTasks(keyword) {
    const normalized = (keyword || '').trim().toLowerCase();
    if (!normalized) {
      return this.getAllTasks();
    }

    return this.getAllTasks().filter((task) => {
      return [task.title, task.description, task.category, task.assignedTo]
        .some((field) => field.toLowerCase().includes(normalized));
    });
  }

  getSummary() {
    const total = this.#tasks.size;
    const pending = this.getTasksByStatus(TaskStatus.PENDING).length;
    const inProgress = this.getTasksByStatus(TaskStatus.IN_PROGRESS).length;
    const completed = this.getTasksByStatus(TaskStatus.COMPLETED).length;

    const byCategory = new Map();
    this.getAllTasks().forEach((task) => {
      byCategory.set(task.category, (byCategory.get(task.category) || 0) + 1);
    });

    const lines = [
      `Total tasks: ${total}`,
      `Pending: ${pending}`,
      `In Progress: ${inProgress}`,
      `Completed: ${completed}`,
      'Tasks by category:'
    ];

    Array.from(byCategory.entries())
      .sort(([categoryA], [categoryB]) => categoryA.localeCompare(categoryB))
      .forEach(([category, count]) => {
        lines.push(`- ${category}: ${count}`);
      });

    return lines.join('\n');
  }

  getTaskIds() {
    return new Set(this.#tasks.keys());
  }
}

module.exports = {
  TaskStatus,
  Task,
  User,
  TodoListService
};
