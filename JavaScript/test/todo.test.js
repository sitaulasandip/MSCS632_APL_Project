'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { TodoListService, TaskStatus: S } = require('../todoListService');
function service(file) { const s = new TodoListService(file); s.addUser('Alice'); s.addUser('Bob'); return s; }
const temporaryDirectories = [];
process.on('exit', () => temporaryDirectories.forEach(dir => fs.rmSync(dir, { recursive: true, force: true })));
function temp() { const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'todo-js-')); temporaryDirectories.push(dir); return dir; }
const add = (s, title = 'Report', category = 'School', user = 'Alice') => s.addTask(title, '', category, user);
test('empty storage and summary', () => {
  const s = new TodoListService();
  assert.deepEqual(s.getUsers(), []); assert.deepEqual(s.getAllTasks(), []);
  assert.equal(s.getSummary(), 'Total tasks: 0\nPending: 0\nIn Progress: 0\nCompleted: 0\nTasks by category:');
});
test('users, normalization and validation', () => {
  const s = new TodoListService(); assert.equal(s.addUser(' Alice ').name, 'Alice');
  for (const name of ['ALICE', '', ' ', null]) assert.throws(() => s.addUser(name));
  assert.equal(s.getUsers().length, 1);
});
test('task creation and validation', () => {
  const s = service();
  for (const value of ['', ' ', null]) {
    assert.throws(() => add(s, value)); assert.throws(() => add(s, 'Title', value)); assert.throws(() => add(s, 'Title', 'Work', value));
  }
  assert.throws(() => add(s, 'Title', 'Work', 'Nobody'));
  assert.throws(() => s.addTask('Title', null, 'Work', 'Alice'));
  assert.equal(s.getAllTasks().length, 0);
  const t = add(s, ' Report ', ' School ', ' ALICE ');
  assert.equal(t.title, 'Report'); assert.equal(t.assignedTo, 'Alice'); assert.equal(t.status, S.PENDING);
  assert.notEqual(add(s).id, t.id);
});
test('status transitions, immutable snapshots and invalid updates', () => {
  const s = service(), t = add(s);
  for (const status of Object.values(S)) {
    const updated = s.updateTaskStatus(t.id, status);
    assert.equal(updated.status, status); assert.equal(updated.createdAt, t.createdAt);
  }
  assert.equal(t.status, S.PENDING); assert.throws(() => { t.status = S.COMPLETED; });
  assert.throws(() => s.updateTaskStatus(t.id, null)); assert.throws(() => s.updateTaskStatus(999, S.PENDING));
});
test('deletion preserves other tasks and users', () => {
  const s = service(), a = add(s), b = add(s);
  assert.equal(s.deleteTask(a.id), true); assert.equal(s.deleteTask(a.id), false);
  assert.deepEqual(s.getTaskIds(), new Set([b.id])); assert.equal(s.getUsers().length, 2);
  assert.throws(() => s.updateTaskStatus(a.id, S.COMPLETED));
});
test('user category and every status filter', () => {
  const s = service(), a = add(s), b = add(s, 'Slides', 'Work', 'Bob');
  s.updateTaskStatus(b.id, S.COMPLETED);
  assert.deepEqual(s.getTasksByUser('ALICE').map(t => t.id), [a.id]);
  assert.deepEqual(s.getTasksByCategory('school').map(t => t.id), [a.id]);
  assert.equal(s.getTasksByStatus(S.PENDING).length, 1); assert.equal(s.getTasksByStatus(S.COMPLETED).length, 1);
  assert.equal(s.getTasksByStatus(S.IN_PROGRESS).length, 0); assert.deepEqual(s.getTasksByUser('Nobody'), []);
});
test('search across all fields and empty queries', () => {
  const s = service(); s.addTask('Report', 'Unique notes', 'School', 'Alice'); add(s, 'Slides', 'Work', 'Bob');
  for (const query of [' REPORT ', 'unique', 'SCHOOL', 'alice']) assert.equal(s.searchTasks(query).length, 1);
  for (const query of [null, '', ' ']) assert.equal(s.searchTasks(query).length, 2);
  assert.deepEqual(s.searchTasks('absent'), []);
});
test('summary reflects updates and deletion', () => {
  const s = service(); add(s); const b = add(s, 'B', 'Work'), c = add(s);
  s.updateTaskStatus(b.id, S.IN_PROGRESS); s.updateTaskStatus(c.id, S.COMPLETED);
  assert.equal(s.getSummary(), 'Total tasks: 3\nPending: 1\nIn Progress: 1\nCompleted: 1\nTasks by category:\n- School: 2\n- Work: 1');
  s.deleteTask(b.id); assert.match(s.getSummary(), /In Progress: 0/); assert.doesNotMatch(s.getSummary(), /- Work:/);
});
test('returned collections cannot change stored records', () => {
  const s = service(); add(s); s.getUsers().pop(); s.getAllTasks().pop(); s.getTaskIds().clear();
  assert.equal(s.getUsers().length, 2); assert.equal(s.getAllTasks().length, 1);
  assert.throws(() => { s.getUsers()[0].name = 'Changed'; });
});
test('CSV round trip, ID continuity and empty store after deletions', t => {
  const file = path.join(temp(t), 'todo.csv'), s = service(file);
  const task = s.addTask('Report, "draft"', 'Line 1\r\nLine 2 café 😀', 'School', 'Alice');
  s.updateTaskStatus(task.id, S.COMPLETED);
  const loaded = new TodoListService(file);
  assert.deepEqual(loaded.getAllTasks(), s.getAllTasks()); assert.deepEqual(loaded.getUsers(), s.getUsers());
  loaded.deleteTask(task.id); const restarted = new TodoListService(file);
  assert.equal(restarted.getAllTasks().length, 0); assert.ok(add(restarted).id > task.id);
});
test('malformed CSV rejected without overwrite and failed saves roll back', t => {
  const dir = temp(t), file = path.join(dir, 'bad.csv'); fs.writeFileSync(file, 'invalid');
  assert.throws(() => new TodoListService(file)); assert.equal(fs.readFileSync(file, 'utf8'), 'invalid');
  const blocked = path.join(dir, 'blocked'), s = new TodoListService(path.join(blocked, 'todo.csv'));
  fs.writeFileSync(blocked, 'file'); assert.throws(() => s.addUser('Alice')); assert.deepEqual(s.getUsers(), []);
});
// Each setImmediate schedules an independent callback. There are no worker threads;
// synchronous service methods and saves cannot interleave on this event loop.
async function together(actions) {
  await Promise.all(actions.map(action => new Promise((resolve, reject) => setImmediate(() => {
    try { resolve(action()); } catch (error) { reject(error); }
  }))));
}
test('concurrent async operations retain data and serialize conflicts', async t => {
  const file = path.join(temp(t), 'todo.csv'), s = service(file), original = add(s);
  console.log('Concurrency: 2 registered users [Alice, Bob]; 1 event-loop thread; no worker threads.');
  console.log('Synchronous operations and CSV saves complete together; frozen tasks prevent outside mutation.');
  await together(Array.from({ length: 80 }, (_, i) => () => add(s, `Task ${i}`, 'Work', i % 2 ? 'Bob' : 'Alice')));
  assert.equal(s.getTaskIds().size, 81); assert.equal(s.getTasksByUser('Alice').length, 41); assert.equal(s.getTasksByUser('Bob').length, 40);
  assert.deepEqual(new TodoListService(file).getAllTasks(), s.getAllTasks());
  console.log('PASS: 80/80 additions; 81 unique tasks including original; Alice 41, Bob 40; CSV matches memory.');
  await together(Array.from({ length: 30 }, (_, i) => () => s.updateTaskStatus(original.id, Object.values(S)[i % 3])));
  assert.deepEqual(new TodoListService(file).getAllTasks(), s.getAllTasks());
  console.log('PASS: 30/30 competing updates; final CSV status matches memory; last serialized update wins.');
  // Exercise both valid orderings explicitly: event-loop scheduling is not parallel execution.
  for (const deleteFirst of [true, false]) {
    const task = add(s);
    const remove = () => assert.equal(s.deleteTask(task.id), true);
    const update = () => deleteFirst ? assert.throws(() => s.updateTaskStatus(task.id, S.COMPLETED), /Task not found/) : s.updateTaskStatus(task.id, S.COMPLETED);
    await together(deleteFirst ? [remove, update] : [update, remove]);
    assert.equal(s.getTaskIds().has(task.id), false); assert.equal(new TodoListService(file).getTaskIds().has(task.id), false);
  }
  console.log('PASS: both update/delete orderings; deleted tasks never resurrected. Scope: one shared service, not multiple processes.');
});
test('CLI users, input recovery, demo and restart', t => {
  const file = path.join(temp(t), 'todo.csv');
  const run = input => spawnSync(process.execPath, [path.join(__dirname, '../main.js'), file], { input, encoding: 'utf8', timeout: 10000 });
  const first = run('13\n12\nAlice\n1\nReport\n\nSchool\nAlice\n2\n1abc\n10\n13\n11\n');
  assert.equal(first.status, 0, first.stderr); assert.match(first.stdout, /No users registered/); assert.match(first.stdout, /Error: Task ID/);
  assert.match(first.stdout, /Concurrent operations finished/); assert.match(first.stdout, /Registered users \(1\):\n- Alice/);
  const second = run('4\n13\n11\n'); assert.equal(second.status, 0); assert.match(second.stdout, /title='Report'/); assert.match(second.stdout, /- Alice/);
});
