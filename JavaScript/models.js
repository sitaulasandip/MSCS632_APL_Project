'use strict';
const TaskStatus = Object.freeze({ PENDING: 'PENDING', IN_PROGRESS: 'IN_PROGRESS', COMPLETED: 'COMPLETED' });
function required(value, field) {
  if (typeof value !== 'string' || !value.trim()) throw new Error(`${field} is required`);
  return value.trim();
}
class User {
  constructor(name) { this.name = required(name, 'User name'); Object.freeze(this); }
  toString() { return this.name; }
}
// Match Java LocalDateTime: local wall-clock time without a timezone suffix.
function localTimestamp() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().replace(/Z$/, '');
}
class Task {
  constructor(id, title, description, category, assignedTo, createdAt = localTimestamp(), status = TaskStatus.PENDING) {
    if (!Number.isInteger(id) || id < 1 || id >= 2147483647) throw new Error('Invalid task ID');
    if (typeof description !== 'string') throw new Error('Description is required');
    if (!Object.values(TaskStatus).includes(status)) throw new Error('Invalid status');
    if (typeof createdAt !== 'string' || !/^\d{4}-\d\d-\d\dT\d\d:\d\d(?::\d\d(?:\.\d{1,9})?)?$/.test(createdAt) || !Number.isFinite(Date.parse(createdAt))) throw new Error('Invalid creation time');
    Object.assign(this, { id, title: required(title, 'Title'), description, category: required(category, 'Category'), assignedTo: required(assignedTo, 'Assigned user'), createdAt, status });
    Object.freeze(this);
  }
  withStatus(status) { return new Task(this.id, this.title, this.description, this.category, this.assignedTo, this.createdAt, status); }
  toString() { return `Task{id=${this.id}, title='${this.title}', description='${this.description}', category='${this.category}', assignedTo='${this.assignedTo}', status=${this.status}, createdAt=${this.createdAt}}`; }
}
module.exports = { TaskStatus, Task, User };
