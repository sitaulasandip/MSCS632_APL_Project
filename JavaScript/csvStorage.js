'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { randomUUID } = require('node:crypto');
const { Task, User } = require('./models');
const HEADER = ['type', 'id', 'title', 'description', 'category', 'user', 'createdAt', 'status'];
function parse(text) {
  text = text.replace(/^\uFEFF/, '');
  const rows = []; let row = [], cell = '', quoted = false, closed = false, started = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') { cell += '"'; i++; }
        else { quoted = false; closed = true; }
      } else cell += c;
    } else if ([',', '\r', '\n'].includes(c)) {
      row.push(cell); cell = ''; closed = started = false;
      if (c !== ',') { rows.push(row); row = []; if (c === '\r' && text[i + 1] === '\n') i++; }
    } else if (c === '"' && !started && !closed) { quoted = started = true; }
    else { if (closed || c === '"') throw new Error('Malformed CSV quoting'); cell += c; started = true; }
  }
  if (quoted) throw new Error('Unclosed CSV quote');
  if (started || closed || row.length) { row.push(cell); rows.push(row); }
  return rows;
}
function load(file) {
  const state = { tasks: new Map(), users: new Map(), nextId: 1 };
  let text;
  try { text = fs.readFileSync(file, 'utf8'); }
  catch (error) { if (error.code === 'ENOENT') return state; throw error; }
  const rows = parse(text);
  if (JSON.stringify(rows.shift()) !== JSON.stringify(HEADER)) throw new Error('Invalid CSV header');
  let nextId;
  for (const r of rows) {
    if (r.length !== 8) throw new Error('Each CSV row must have 8 columns');
    switch (r[0]) {
      case 'META':
        if (nextId !== undefined || !/^\d+$/.test(r[1])) throw new Error('Invalid metadata');
        nextId = Number(r[1]);
        if (!Number.isInteger(nextId) || nextId < 1 || nextId > 2147483647) throw new Error('Invalid next ID');
        break;
      case 'USER': {
        const user = new User(r[5]), key = user.name.toLowerCase();
        if (state.users.has(key)) throw new Error('Duplicate user');
        state.users.set(key, user); break;
      }
      case 'TASK': {
        if (!/^\d+$/.test(r[1])) throw new Error('Invalid task ID');
        const task = new Task(Number(r[1]), r[2], r[3], r[4], r[5], r[6], r[7]);
        if (state.tasks.has(task.id)) throw new Error('Duplicate task ID');
        state.tasks.set(task.id, task); break;
      }
      default: throw new Error('Unknown CSV record type');
    }
  }
  if (nextId === undefined) throw new Error('Missing metadata');
  for (const task of state.tasks.values()) {
    if (task.id >= nextId || !state.users.has(task.assignedTo.toLowerCase())) throw new Error('Invalid task reference or next ID');
  }
  state.nextId = nextId;
  return state;
}
function save(file, tasks, users, nextId) {
  const rows = [HEADER, ['META', nextId, '', '', '', '', '', '']];
  for (const user of [...users.values()].sort((a, b) => a.name.localeCompare(b.name))) rows.push(['USER', '', '', '', '', user.name, '', '']);
  for (const t of [...tasks.values()].sort((a, b) => a.id - b.id)) rows.push(['TASK', t.id, t.title, t.description, t.category, t.assignedTo, t.createdAt, t.status]);
  const text = rows.map(row => row.map(cell => `"${String(cell).replaceAll('"', '""')}"`).join(',')).join('\r\n') + '\r\n';
  const temp = path.join(path.dirname(file), `todo-${randomUUID()}.tmp`);
  try {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(temp, text, { encoding: 'utf8', flag: 'wx' });
    fs.renameSync(temp, file);
  } catch (error) { throw new Error(`Cannot save CSV; change was not applied: ${error.message}`); }
  finally { try { fs.unlinkSync(temp); } catch (error) { if (error.code !== 'ENOENT') { /* Preserve original save result. */ } } }
}
module.exports = { load, save };
