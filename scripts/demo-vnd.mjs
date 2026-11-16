// Local development only. No deletes, credential resets, or changes to existing records.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

if (!process.argv.includes('--local-demo')) throw Error('Requires --local-demo. Never run against production.');
// Deliberately not configurable: cannot accidentally point this seed at a remote server.
const base = 'http://127.0.0.1:8080';
const dir = fileURLToPath(new URL('../.local/', import.meta.url));
fs.mkdirSync(dir, { recursive: true });
const lock = path.join(dir, 'demo-vnd.lock');
const handle = fs.openSync(lock, 'wx');
let token;
async function api(route, method = 'GET', body) {
  const response = await fetch(base + '/api' + route, {
    method, redirect: 'error',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw Error(`${method} ${route}: HTTP ${response.status} (no sensitive response logged)`);
  return response.status === 204 ? null : response.json();
}
try {
  const credentialsFile = path.join(dir, 'demo-vnd-credentials.json');
  let credentials;
  if (fs.existsSync(credentialsFile)) credentials = JSON.parse(fs.readFileSync(credentialsFile, 'utf8'));
  else {
    credentials = { email: `demo-vnd-${crypto.randomUUID().slice(0, 8)}@example.com`, password: crypto.randomBytes(24).toString('base64url'), fullName: 'Demo tài chính tháng 11' };
    fs.writeFileSync(credentialsFile, JSON.stringify(credentials, null, 2), { flag: 'wx', mode: 0o600 });
  }
  // Reuse the dedicated account. Never change an existing user's password.
  try { token = (await api('/auth/login', 'POST', credentials)).accessToken; }
  catch {
    await api('/auth/register', 'POST', credentials);
    token = (await api('/auth/login', 'POST', credentials)).accessToken;
  }
  if (!token) throw Error('Missing access token');
  const definitions = [
    ['Lương', 'INCOME'], ['Công việc bán thời gian', 'INCOME'], ['Thu nhập khác', 'INCOME'],
    ['Ăn uống', 'EXPENSE'], ['Đi lại', 'EXPENSE'], ['Mua sắm', 'EXPENSE'], ['Học tập', 'EXPENSE'], ['Giải trí', 'EXPENSE'],
  ];
  const categories = await api('/categories');
  const ids = {};
  for (const [name, type] of definitions) {
    let category = categories.find(c => c.name === name && c.type === type);
    if (!category) { category = await api('/categories', 'POST', { name, type }); categories.push(category); }
    ids[name] = category.id;
  }
  const rows = [
    [1, 'Lương', 12000000, 'Lương tháng 11'],
    [10, 'Công việc bán thời gian', 4000000, 'Thanh toán dự án thiết kế'],
    [15, 'Thu nhập khác', 2500000, 'Thu nhập dạy kèm'],
    [1, 'Ăn uống', 420000, 'Mua thực phẩm đầu tháng'],
    [2, 'Ăn uống', 65000, 'Cơm trưa văn phòng'],
    [3, 'Ăn uống', 85000, 'Bữa tối cùng bạn'],
    [5, 'Ăn uống', 380000, 'Thực phẩm và rau củ'],
    [7, 'Ăn uống', 180000, 'Ăn cuối tuần'],
    [10, 'Ăn uống', 95000, 'Bữa trưa và cà phê'],
    [12, 'Ăn uống', 350000, 'Đi chợ giữa tháng'],
    [16, 'Ăn uống', 125000, 'Ăn tối sau giờ học'],
    [1, 'Đi lại', 150000, 'Đổ xăng đầu tháng'],
    [4, 'Đi lại', 80000, 'Xe công nghệ khi trời mưa'],
    [8, 'Đi lại', 150000, 'Đổ xăng cuối tuần'],
    [11, 'Đi lại', 120000, 'Vé xe và gửi xe'],
    [16, 'Đi lại', 150000, 'Đổ xăng giữa tháng'],
    [2, 'Mua sắm', 650000, 'Giày đi làm'],
    [6, 'Mua sắm', 450000, 'Áo sơ mi'],
    [11, 'Mua sắm', 700000, 'Đồ dùng cá nhân'],
    [14, 'Mua sắm', 500000, 'Đồ dùng phòng trọ'],
    [3, 'Học tập', 320000, 'Sách lập trình Java'],
    [8, 'Học tập', 450000, 'Khóa học tiếng Anh'],
    [13, 'Học tập', 130000, 'In tài liệu và văn phòng phẩm'],
    [4, 'Giải trí', 180000, 'Vé xem phim'],
    [7, 'Giải trí', 150000, 'Cà phê cùng bạn'],
    [9, 'Giải trí', 220000, 'Cầu lông và thuê sân'],
    [14, 'Giải trí', 200000, 'Gặp mặt cuối tuần'],
    [15, 'Giải trí', 200000, 'Tham quan triển lãm'],
  ];
  async function transactions() {
    const result = [];
    for (let page = 0; ; page++) {
      const data = await api(`/transactions?page=${page}&size=100`);
      result.push(...data.content);
      if (data.last || page + 1 >= data.totalPages) return result;
    }
  }
  const existing = await transactions();
  let created = 0;
  for (const [day, name, amount, label] of rows) {
    const description = `[DEMO-VND-202611] ${label}`;
    const body = { type: definitions.find(d => d[0] === name)[1], amount, categoryId: ids[name], description, transactionDate: `2026-11-${String(day).padStart(2, '0')}` };
    const matches = existing.filter(t => t.description === description);
    if (matches.length > 1) throw Error('Duplicate demo identity detected; manual review required');
    if (matches.length) {
      const t = matches[0];
      if (Number(t.amount) !== amount || t.categoryId !== body.categoryId || t.type !== body.type || t.transactionDate !== body.transactionDate) throw Error('Existing demo record differs; not overwriting');
    } else { existing.push(await api('/transactions', 'POST', body)); created++; }
  }
  const budgets = await api('/budgets');
  for (const [name, amount] of [['Ăn uống', 3000000], ['Đi lại', 1000000], ['Mua sắm', 2000000], ['Học tập', 1000000], ['Giải trí', 1000000]]) {
    const found = budgets.filter(b => b.categoryId === ids[name] && b.startDate === '2026-11-01' && b.endDate === '2026-11-30');
    if (found.length > 1 || (found.length && Number(found[0].amount) !== amount)) throw Error('Existing budget differs; not overwriting');
    if (!found.length) budgets.push(await api('/budgets', 'POST', { categoryId: ids[name], amount, startDate: '2026-11-01', endDate: '2026-11-30' }));
  }
  const stored = (await transactions()).filter(t => t.transactionDate >= '2026-11-01' && t.transactionDate <= '2026-11-30');
  const total = type => stored.filter(t => t.type === type).reduce((a,t) => a + BigInt(String(t.amount).split('.')[0]), 0n).toString();
  const summary = await api('/dashboard/summary?fromDate=2026-11-01&toDate=2026-11-30');
  const report = await api('/reports/monthly?month=2026-11');
  for (const result of [summary, report]) {
    if (Number(result.totalIncome) !== Number(total('INCOME')) || Number(result.totalExpense) !== Number(total('EXPENSE')) || result.transactionCount !== stored.length) throw Error('Summary/report totals mismatch');
  }
  const usage = await Promise.all(budgets.map(b => api(`/budgets/${b.id}/usage`)));
  if (!['SAFE','WARNING','OVER_BUDGET'].every(s => usage.some(u => u.status === s))) throw Error('Missing budget status');
  const output = { account: credentials.email, createdTransactions: created, transactionCount: stored.length, income: summary.totalIncome, expense: summary.totalExpense, balance: summary.balance, budgets: usage.map(u => ({category:u.categoryName, spent:u.spentAmount, limit:u.limitAmount, percent:u.usagePercentage, status:u.status})), suspiciousSmallAmounts: stored.filter(t => Number(t.amount) < 1000).map(t => ({id:t.id,amount:t.amount})) };
  console.log(JSON.stringify(output, null, 2));
} finally { fs.closeSync(handle); fs.unlinkSync(lock); }
