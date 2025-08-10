const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const Store = require('electron-store');
const { v4: uuidv4 } = require('uuid');

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) {
  app.quit();
}

const store = new Store({
  name: 'invoice-app',
  defaults: {
    users: [
      { id: 'admin', username: 'admin', password: 'admin', role: 'admin', displayName: 'Administrator' },
    ],
    currentUserId: null,
    settings: {
      companyName: 'My Company',
      companyAddress: '',
      companyPhone: '',
      invoicePrefix: 'INV',
      nextInvoiceNumber: 1,
      logoPath: '',
    },
    invoices: [],
  },
});

let mainWindow;

const createWindow = () => {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      preload: MAIN_WINDOW_PRELOAD_WEBPACK_ENTRY,
    },
  });

  mainWindow.loadURL(MAIN_WINDOW_WEBPACK_ENTRY);
  // mainWindow.webContents.openDevTools();
};

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Helpers
function formatInvoiceNumber(prefix, number) {
  const padded = String(number).padStart(5, '0');
  return `${prefix}-${padded}`;
}

function getCurrentUser() {
  const currentUserId = store.get('currentUserId');
  const users = store.get('users');
  return users.find((u) => u.id === currentUserId) || null;
}

function ensureAuthenticated() {
  const user = getCurrentUser();
  if (!user) {
    const error = new Error('Not authenticated');
    error.code = 'AUTH_REQUIRED';
    throw error;
  }
  return user;
}

// IPC: Auth
ipcMain.handle('auth:login', async (_event, { username, password }) => {
  const users = store.get('users');
  const user = users.find((u) => u.username === username && u.password === password);
  if (!user) {
    const error = new Error('Invalid credentials');
    error.code = 'INVALID_CREDENTIALS';
    throw error;
  }
  store.set('currentUserId', user.id);
  return { id: user.id, username: user.username, role: user.role, displayName: user.displayName };
});

ipcMain.handle('auth:logout', async () => {
  store.set('currentUserId', null);
  return true;
});

ipcMain.handle('auth:currentUser', async () => {
  const user = getCurrentUser();
  if (!user) return null;
  return { id: user.id, username: user.username, role: user.role, displayName: user.displayName };
});

// IPC: Settings
ipcMain.handle('settings:get', async () => {
  return store.get('settings');
});

ipcMain.handle('settings:save', async (_event, newSettings) => {
  const settings = store.get('settings');
  const merged = { ...settings, ...newSettings };
  store.set('settings', merged);
  return merged;
});

ipcMain.handle('settings:selectLogo', async () => {
  const result = await dialog.showOpenDialog(mainWindow, {
    properties: ['openFile'],
    filters: [
      { name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'gif', 'svg'] },
    ],
  });
  if (result.canceled || result.filePaths.length === 0) return null;

  const logoPath = result.filePaths[0];
  const settings = store.get('settings');
  settings.logoPath = logoPath;
  store.set('settings', settings);
  return settings;
});

// IPC: Invoices
ipcMain.handle('invoice:list', async () => {
  return store.get('invoices');
});

ipcMain.handle('invoice:get', async (_event, invoiceId) => {
  const invoices = store.get('invoices');
  return invoices.find((i) => i.id === invoiceId) || null;
});

ipcMain.handle('invoice:create', async (_event, draft) => {
  const user = ensureAuthenticated();
  const settings = store.get('settings');
  const nextNumber = settings.nextInvoiceNumber;
  const invoiceNumber = formatInvoiceNumber(settings.invoicePrefix, nextNumber);

  const invoice = {
    id: uuidv4(),
    number: invoiceNumber,
    sequence: nextNumber,
    date: draft.date || new Date().toISOString().slice(0, 10),
    customer: draft.customer,
    items: draft.items || [],
    notes: draft.notes || '',
    subtotal: draft.subtotal || 0,
    tax: draft.tax || 0,
    total: draft.total || 0,
    createdBy: { id: user.id, username: user.username },
    createdAt: new Date().toISOString(),
  };

  const invoices = store.get('invoices');
  invoices.push(invoice);
  store.set('invoices', invoices);

  // increment the next invoice number
  store.set('settings', { ...settings, nextInvoiceNumber: nextNumber + 1 });

  return invoice;
});

ipcMain.handle('invoice:delete', async (_event, invoiceId) => {
  const invoices = store.get('invoices');
  const filtered = invoices.filter((i) => i.id !== invoiceId);
  store.set('invoices', filtered);
  return true;
});

// IPC: Export PDF
ipcMain.handle('invoice:exportPDF', async (_event, invoiceId) => {
  const invoice = (store.get('invoices') || []).find((i) => i.id === invoiceId);
  if (!invoice) {
    const error = new Error('Invoice not found');
    error.code = 'NOT_FOUND';
    throw error;
  }

  // Create a hidden window for print layout
  const printWindow = new BrowserWindow({
    show: false,
    width: 1024,
    height: 1440,
    webPreferences: {
      offscreen: true,
    },
  });

  const printHtml = generateInvoiceHtml(store.get('settings'), invoice);
  await printWindow.loadURL('data:text/html;charset=utf-8,' + encodeURIComponent(printHtml));

  const pdfData = await printWindow.webContents.printToPDF({
    marginsType: 1,
    printBackground: true,
    pageSize: 'A4',
    landscape: false,
  });

  printWindow.destroy();

  const { filePath } = await dialog.showSaveDialog(mainWindow, {
    title: 'Save Invoice PDF',
    defaultPath: `${invoice.number}.pdf`,
    filters: [{ name: 'PDF', extensions: ['pdf'] }],
  });
  if (!filePath) return null;

  fs.writeFileSync(filePath, pdfData);
  return filePath;
});

function generateInvoiceHtml(settings, invoice) {
  const logoImg = settings.logoPath
    ? `<img src="file://${settings.logoPath}" style="height:80px;object-fit:contain;"/>`
    : `<div style="font-weight:700;font-size:24px;">${settings.companyName}</div>`;

  // compute totals if not present
  const itemsRows = (invoice.items || []).map((it, idx) => {
    const lineTotal = (Number(it.quantity) || 0) * (Number(it.unitPrice) || 0);
    return `
      <tr>
        <td style="padding:8px;border-bottom:1px solid #eee;">${idx + 1}</td>
        <td style="padding:8px;border-bottom:1px solid #eee;">${it.description || ''}</td>
        <td style="padding:8px;border-bottom:1px solid #eee;text-align:right;">${it.quantity || 0}</td>
        <td style="padding:8px;border-bottom:1px solid #eee;text-align:right;">${it.unitPrice || 0}</td>
        <td style="padding:8px;border-bottom:1px solid #eee;text-align:right;">${lineTotal.toFixed(2)}</td>
      </tr>`;
  }).join('');

  const subtotal = (invoice.items || []).reduce((sum, it) => sum + (Number(it.quantity) || 0) * (Number(it.unitPrice) || 0), 0);
  const tax = Number(invoice.tax) || 0;
  const total = subtotal + tax;

  const header = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
      <div>${logoImg}</div>
      <div style="text-align:right;">
        <div style="font-size:28px;font-weight:800;letter-spacing:2px;">فاتورة</div>
        <div style="margin-top:8px;color:#666;">${settings.companyAddress || ''}</div>
        <div style="color:#666;">${settings.companyPhone || ''}</div>
      </div>
    </div>`;

  const meta = `
    <div style="display:flex;justify-content:space-between;margin:16px 0;padding:12px;background:#f8f9fb;border:1px solid #e9eef5;border-radius:8px;">
      <div>
        <div style="color:#555;">العميل</div>
        <div style="font-weight:700;">${invoice.customer?.name || ''}</div>
        <div style="color:#666;">${invoice.customer?.address || ''}</div>
      </div>
      <div style="text-align:right;">
        <div><span style="color:#555;">رقم الفاتورة:</span> <span style="font-weight:700;">${invoice.number}</span></div>
        <div><span style="color:#555;">التاريخ:</span> ${invoice.date}</div>
      </div>
    </div>`;

  const table = `
    <table style="width:100%;border-collapse:collapse;margin-top:8px;">
      <thead>
        <tr style="background:#eff3f9;color:#333;">
          <th style="padding:10px;text-align:left;">#</th>
          <th style="padding:10px;text-align:left;">الوصف</th>
          <th style="padding:10px;text-align:right;">الكمية</th>
          <th style="padding:10px;text-align:right;">سعر الوحدة</th>
          <th style="padding:10px;text-align:right;">الإجمالي</th>
        </tr>
      </thead>
      <tbody>${itemsRows}</tbody>
    </table>`;

  const totals = `
    <div style="display:flex;justify-content:flex-end;margin-top:16px;">
      <div style="width:320px;border:1px solid #e9eef5;border-radius:8px;padding:12px;background:#fdfefe;">
        <div style="display:flex;justify-content:space-between;padding:6px 0;">
          <span>المجموع</span><span>${subtotal.toFixed(2)}</span>
        </div>
        <div style="display:flex;justify-content:space-between;padding:6px 0;border-top:1px dashed #e0e6ef;">
          <span>الضريبة</span><span>${tax.toFixed(2)}</span>
        </div>
        <div style="display:flex;justify-content:space-between;padding:10px 0;border-top:2px solid #d7deea;font-weight:800;font-size:18px;">
          <span>الإجمالي</span><span>${total.toFixed(2)}</span>
        </div>
      </div>
    </div>`;

  const footer = `
    <div style="position:fixed;bottom:0;left:0;right:0;text-align:center;color:#999;font-size:12px;padding:8px 0;border-top:1px solid #eee;">
      شكراً لتعاملكم معنا — ${settings.companyName}
    </div>`;

  const styles = `
    <style>
      @page { size: A4; margin: 16mm; }
      body { font-family: "Cairo", system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, "Apple Color Emoji", "Segoe UI Emoji"; direction: rtl; }
    </style>`;

  return `<!DOCTYPE html><html lang="ar"><head><meta charset="utf-8"/>${styles}<title>${invoice.number}</title></head><body>${header}${meta}${table}${totals}${footer}</body></html>`;
}
