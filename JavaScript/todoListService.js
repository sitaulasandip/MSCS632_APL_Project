const TaskStatus = Object.freeze({
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED'
});

class Task {
  constructor(id, title, description, category, assignedTo) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.category = category;
    this.assignedTo = assignedTo;
    this.createdAt = new Date().toISOString();
    this.status = TaskStatus.PENDING;
  }

  toString() {
    return `Task{id=${this.id}, title='${this.title}', description='${this.description}', category='${this.category}', assignedTo='${this.assignedTo}', status=${this.status}, createdAt=${this.createdAt}}`;
  }
}

class TodoListService {
  constructor() {
    this.tasks = new Map();
    this.idCounter = 1;
  }

  addTask(title, description, category, assignedTo) {
    if (!title || !description || !category || !assignedTo) {
      throw new Error('Title, description, category, and assigned user are required.');
    }

    const task = new Task(this.idCounter++, title, description, category, assignedTo);
    this.tasks.set(task.id, task);
    return task;
  }

  updateTaskStatus(taskId, newStatus) {
    const task = this.tasks.get(taskId);
    if (!task) {
      throw new Error(`Task not found: ${taskId}`);
    }

    task.status = newStatus;
    return task;
  }

  deleteTask(taskId) {
    return this.tasks.delete(taskId);
  }

  getAllTasks() {
    return Array.from(this.tasks.values());
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
    const total = this.tasks.size;
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
    return new Set(this.tasks.keys());
  }
}

module.exports = {
  TaskStatus,
  Task,
  TodoListService
};
