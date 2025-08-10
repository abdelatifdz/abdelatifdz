/**
 * This file will automatically be loaded by webpack and run in the "renderer" context.
 * To learn more about the differences between the "main" and the "renderer" context in
 * Electron, visit:
 *
 * https://electronjs.org/docs/tutorial/process-model
 *
 * By default, Node.js integration in this file is disabled. When enabling Node.js integration
 * in a renderer process, please be aware of potential security implications. You can read
 * more about security risks here:
 *
 * https://electronjs.org/docs/tutorial/security
 *
 * To enable Node.js integration in this file, open up `main.js` and enable the `nodeIntegration`
 * flag:
 *
 * ```
 *  // Create the browser window.
 *  mainWindow = new BrowserWindow({
 *    width: 800,
 *    height: 600,
 *    webPreferences: {
 *      nodeIntegration: true
 *    }
 *  });
 * ```
 */

import './index.css';

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

function show(el) { el.classList.remove('hidden'); }
function hide(el) { el.classList.add('hidden'); }

const loginView = $('#loginView');
const dashboardView = $('#dashboardView');

const loginBtn = $('#loginBtn');
const loginError = $('#loginError');
const logoutBtn = $('#logoutBtn');

const tabs = $$('.tab');
const panels = {
  invoices: $('#tab-invoices'),
  new: $('#tab-new'),
  settings: $('#tab-settings'),
};

const invoicesList = $('#invoicesList');
const addItemBtn = $('#addItemBtn');
const itemsBody = $('#itemsBody');
const taxInput = $('#taxInput');
const subtotalSpan = $('#subtotalSpan');
const taxSpan = $('#taxSpan');
const totalSpan = $('#totalSpan');
const createInvoiceBtn = $('#createInvoiceBtn');

const setCompanyName = $('#setCompanyName');
const setCompanyAddress = $('#setCompanyAddress');
const setCompanyPhone = $('#setCompanyPhone');
const setInvoicePrefix = $('#setInvoicePrefix');
const setNextNumber = $('#setNextNumber');
const uploadLogoBtn = $('#uploadLogoBtn');
const logoPreview = $('#logoPreview');

const custName = $('#custName');
const custAddress = $('#custAddress');
const invDate = $('#invDate');

function activateTab(tabId) {
  tabs.forEach((t) => t.classList.remove('active'));
  $(`.tab[data-tab="${tabId}"]`).classList.add('active');
  Object.entries(panels).forEach(([id, el]) => {
    if (id === tabId) show(el); else hide(el);
  });
  if (tabId === 'invoices') loadInvoices();
}

tabs.forEach((btn) => {
  btn.addEventListener('click', () => activateTab(btn.dataset.tab));
});

async function init() {
  const user = await window.api.getCurrentUser();
  if (user) {
    hide(loginView);
    show(dashboardView);
    activateTab('invoices');
    await loadSettings();
  } else {
    show(loginView);
    hide(dashboardView);
  }
}

loginBtn.addEventListener('click', async () => {
  try {
    const username = $('#loginUsername').value.trim();
    const password = $('#loginPassword').value.trim();
    await window.api.login(username, password);
    loginError.textContent = '';
    hide(loginView);
    show(dashboardView);
    activateTab('invoices');
    await loadSettings();
  } catch (e) {
    loginError.textContent = 'بيانات الدخول غير صحيحة';
  }
});

logoutBtn.addEventListener('click', async () => {
  await window.api.logout();
  show(loginView);
  hide(dashboardView);
});

async function loadSettings() {
  const settings = await window.api.getSettings();
  setCompanyName.value = settings.companyName || '';
  setCompanyAddress.value = settings.companyAddress || '';
  setCompanyPhone.value = settings.companyPhone || '';
  setInvoicePrefix.value = settings.invoicePrefix || 'INV';
  setNextNumber.value = settings.nextInvoiceNumber || 1;
  if (settings.logoPath) {
    logoPreview.src = `file://${settings.logoPath}`;
  } else {
    logoPreview.removeAttribute('src');
  }
}

$('#saveSettingsBtn').addEventListener('click', async () => {
  const saved = await window.api.saveSettings({
    companyName: setCompanyName.value.trim(),
    companyAddress: setCompanyAddress.value.trim(),
    companyPhone: setCompanyPhone.value.trim(),
    invoicePrefix: setInvoicePrefix.value.trim() || 'INV',
    nextInvoiceNumber: Number(setNextNumber.value) || 1,
  });
  await loadSettings();
});

uploadLogoBtn.addEventListener('click', async () => {
  const res = await window.api.selectAndSaveLogo();
  if (res && res.logoPath) logoPreview.src = `file://${res.logoPath}`;
});

function addItemRow(item = { description: '', quantity: 1, unitPrice: 0 }) {
  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td><input type="text" class="desc" value="${item.description || ''}"/></td>
    <td><input type="number" class="qty" value="${item.quantity || 1}" step="1"/></td>
    <td><input type="number" class="price" value="${item.unitPrice || 0}" step="0.01"/></td>
    <td class="lineTotal">0.00</td>
    <td><button class="secondary remove">حذف</button></td>
  `;
  itemsBody.appendChild(tr);
  tr.querySelectorAll('input').forEach((inp) => inp.addEventListener('input', recalcTotals));
  tr.querySelector('.remove').addEventListener('click', () => { tr.remove(); recalcTotals(); });
  recalcTotals();
}

addItemBtn.addEventListener('click', () => addItemRow());

taxInput.addEventListener('input', recalcTotals);

function recalcTotals() {
  let subtotal = 0;
  $$('#itemsBody tr').forEach((tr) => {
    const qty = Number(tr.querySelector('.qty').value) || 0;
    const price = Number(tr.querySelector('.price').value) || 0;
    const line = qty * price;
    tr.querySelector('.lineTotal').textContent = line.toFixed(2);
    subtotal += line;
  });
  const tax = Number(taxInput.value) || 0;
  const total = subtotal + tax;
  subtotalSpan.textContent = subtotal.toFixed(2);
  taxSpan.textContent = tax.toFixed(2);
  totalSpan.textContent = total.toFixed(2);
}

createInvoiceBtn.addEventListener('click', async () => {
  const items = $$('#itemsBody tr').map((tr) => ({
    description: tr.querySelector('.desc').value,
    quantity: Number(tr.querySelector('.qty').value) || 0,
    unitPrice: Number(tr.querySelector('.price').value) || 0,
  }));
  const draft = {
    customer: { name: custName.value, address: custAddress.value },
    date: invDate.value,
    items,
    tax: Number(taxInput.value) || 0,
  };
  const inv = await window.api.createInvoice(draft);
  // reset form
  itemsBody.innerHTML = '';
  custName.value = '';
  custAddress.value = '';
  invDate.value = '';
  taxInput.value = '0';
  recalcTotals();
  activateTab('invoices');
});

async function loadInvoices() {
  const invoices = await window.api.listInvoices();
  invoicesList.innerHTML = '';
  invoices.slice().reverse().forEach((inv) => {
    const row = document.createElement('div');
    row.className = 'row';
    row.innerHTML = `
      <div>
        <div style="font-weight:700;">${inv.number}</div>
        <div style="color:#666;font-size:12px;">${inv.date} — ${inv.customer?.name || ''}</div>
      </div>
      <div>
        <button class="secondary" data-id="${inv.id}" data-action="pdf">PDF</button>
        <button class="secondary" data-id="${inv.id}" data-action="delete">حذف</button>
      </div>
    `;
    row.addEventListener('click', async (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;
      const id = btn.getAttribute('data-id');
      const action = btn.getAttribute('data-action');
      if (action === 'delete') {
        await window.api.deleteInvoice(id);
        await loadInvoices();
      } else if (action === 'pdf') {
        await window.api.exportInvoicePDF(id);
      }
    });
    invoicesList.appendChild(row);
  });
}

window.addEventListener('DOMContentLoaded', init);
