const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  // Auth
  login: (username, password) => ipcRenderer.invoke('auth:login', { username, password }),
  logout: () => ipcRenderer.invoke('auth:logout'),
  getCurrentUser: () => ipcRenderer.invoke('auth:currentUser'),

  // Settings
  getSettings: () => ipcRenderer.invoke('settings:get'),
  saveSettings: (settings) => ipcRenderer.invoke('settings:save', settings),
  selectAndSaveLogo: () => ipcRenderer.invoke('settings:selectLogo'),

  // Invoices
  listInvoices: () => ipcRenderer.invoke('invoice:list'),
  createInvoice: (invoiceDraft) => ipcRenderer.invoke('invoice:create', invoiceDraft),
  getInvoice: (invoiceId) => ipcRenderer.invoke('invoice:get', invoiceId),
  deleteInvoice: (invoiceId) => ipcRenderer.invoke('invoice:delete', invoiceId),
  exportInvoicePDF: (invoiceId) => ipcRenderer.invoke('invoice:exportPDF', invoiceId),
});
