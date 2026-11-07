const DEVICE_HEADERS = ['device_id', 'label', 'enabled', 'notes', 'updated_at', 'updated_by', 'model', 'device_name', 'last_seen'];

function database_() {
  const id = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');
  if (!id) throw Error('ยังไม่ได้ตั้งค่าฐานข้อมูล Google Sheets');
  return SpreadsheetApp.openById(id);
}

function tab_(name) {
  const sheet = database_().getSheetByName(name);
  if (!sheet) throw Error('ไม่พบชีต ' + name);
  return sheet;
}

// Run once from the Apps Script editor to initialize or upgrade the existing sheet.
function setupDatabase() {
  initialize_();
  console.log(database_().getUrl());
}

function setupDatabase_() {
  const props = PropertiesService.getScriptProperties();
  const id = props.getProperty('SPREADSHEET_ID');
  const book = id ? SpreadsheetApp.openById(id) : SpreadsheetApp.create('Gozza Control Center');
  if (!id) props.setProperty('SPREADSHEET_ID', book.getId());
  for (const [name, headers] of [
    ['Devices', DEVICE_HEADERS],
    ['Settings', ['key', 'value']],
    ['AuditLogs', ['timestamp', 'actor', 'action', 'target', 'details']]
  ]) {
    const sheet = book.getSheetByName(name) || book.insertSheet(name);
    const count = sheet.getLastColumn();
    if (count > headers.length || (count && sheet.getRange(1, 1, 1, count).getValues()[0]
        .some((value, i) => value !== headers[i]))) throw Error('Incorrect headers: ' + name);
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
    sheet.setFrozenRows(1);
  }
  const devices = tab_('Devices');
  devices.getRange('A:A').setNumberFormat('@');
  tab_('Settings').getRange('B:B').setNumberFormat('@');
  const settings = settings_();
  if (!Object.prototype.hasOwnProperty.call(settings, 'enabled')) tab_('Settings').appendRow(['enabled', 'true']);
  if (!Object.prototype.hasOwnProperty.call(settings, 'admin_password')) {
    tab_('Settings').appendRow(['admin_password', Utilities.getUuid().replace(/-/g, '').slice(0, 16)]);
  }
}

function initialize_() {
  const props = PropertiesService.getScriptProperties();
  if (props.getProperty('GOZZA_SHEET_SCHEMA') === '2') return;
  locked_(() => {
    if (props.getProperty('GOZZA_SHEET_SCHEMA') === '2') return;
    setupDatabase_();
    SpreadsheetApp.flush();
    props.setProperty('GOZZA_SHEET_SCHEMA', '2');
  });
}

function doGet() {
  return json_({ ok: true, service: 'gozza-control-center', apiVersion: 2 });
}

function doPost(event) {
  let request;
  try {
    const body = event && event.postData && event.postData.contents;
    if (!body || body.length > 4096) throw Error('Invalid request');
    request = JSON.parse(body);
    if (!request || typeof request !== 'object' || Array.isArray(request)) throw Error('Invalid request');
  } catch (_) {
    return json_({ ok: false, allowed: false, error: 'คำขอไม่ถูกต้อง' });
  }

  if (request.type === 'admin') {
    try {
      initialize_();
      return json_({ ok: true, state: adminRequest_(request.password, request.action, request.payload) });
    } catch (error) {
      if (error instanceof RequestError) return json_({ ok: false, error: error.message });
      console.error(String(error.message || error));
      return json_({ ok: false, error: 'จัดการข้อมูลใน Google Sheets ไม่สำเร็จ' });
    }
  }

  try {
    const id = hash_(request.deviceId);
    initialize_();
    return json_({ ok: true, allowed: locked_(() => register_(id, request)) });
  } catch (error) {
    console.error(String(error.message || error));
    return json_({ ok: false, allowed: false });
  }
}

function json_(value) {
  return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON);
}

function settings_() {
  return Object.fromEntries(tab_('Settings').getDataRange().getValues().slice(1)
    .map(([key, value]) => [String(key), String(value)]));
}

function locked_(operation) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const result = operation();
    SpreadsheetApp.flush();
    return result;
  } finally {
    lock.releaseLock();
  }
}

function hash_(value) {
  const hash = typeof value === 'string' ? value.toLowerCase() : '';
  if (!/^[a-f0-9]{64}$/.test(hash)) throw new RequestError('รหัสเครื่องต้องเป็นเลขฐานสิบหก 64 ตัว');
  return hash;
}

function sheetText_(value, length) {
  const text = typeof value === 'string' ? value.trim().slice(0, length) : '';
  return /^[=+@-]/.test(text) ? "'" + text : text;
}

function enabled_(value) {
  return String(value).toLowerCase() === 'true';
}

function devices_(sheet) {
  const count = sheet.getLastRow() - 1;
  if (count < 1) return [];
  return sheet.getRange(2, 1, count, DEVICE_HEADERS.length).getValues()
    .map((values, index) => ({ row: index + 2, values })).filter(entry => entry.values[0]);
}

function register_(id, device) {
  const sheet = tab_('Devices');
  const globalEnabled = enabled_(settings_().enabled);
  const current = devices_(sheet).find(entry => String(entry.values[0]).toLowerCase() === id);
  const now = new Date();
  const model = sheetText_(device.model, 100);
  const name = sheetText_(device.deviceName, 100);
  if (current) {
    sheet.getRange(current.row, 7, 1, 3).setValues([[model, name, now]]);
    return globalEnabled && enabled_(current.values[2]);
  }
  sheet.appendRow([id, name || model || 'เครื่องใหม่', true, '', now, 'APK', model, name, now]);
  return globalEnabled;
}

class RequestError extends Error {}

function adminRequest_(password, action, payload) {
  return locked_(() => {
    const settings = settings_();
    if (!settings.admin_password) throw new RequestError('ตั้งค่า admin_password ในชีต Settings ก่อน');
    if (typeof password !== 'string' || password !== settings.admin_password) throw new RequestError('รหัสแอดมินไม่ถูกต้อง');

    const p = payload || {};
    const sheet = tab_('Devices');
    if (action === 'setGlobalEnabled') {
      if (typeof p.enabled !== 'boolean') throw new RequestError('สถานะที่ส่งมาไม่ถูกต้อง');
      const config = tab_('Settings');
      const row = config.getDataRange().getValues().findIndex(values => values[0] === 'enabled') + 1;
      if (row > 1) config.getRange(row, 2).setValue(String(p.enabled));
      else config.appendRow(['enabled', String(p.enabled)]);
      tab_('AuditLogs').appendRow([new Date(), 'admin', action, 'ALL', String(p.enabled)]);
    } else if (['upsertDevice', 'setDeviceEnabled'].includes(action)) {
      const id = hash_(p.deviceId);
      const current = devices_(sheet).find(entry => String(entry.values[0]).toLowerCase() === id);
      if (action === 'upsertDevice') {
        const label = sheetText_(p.label, 80);
        if (!label || typeof p.enabled !== 'boolean') throw new RequestError('กรอกชื่อเครื่องและสถานะ');
        const values = [id, label, p.enabled, sheetText_(p.notes, 500), new Date(), 'admin'];
        if (current) sheet.getRange(current.row, 1, 1, 6).setValues([values]);
        else sheet.appendRow(values);
      } else if (!current) {
        throw new RequestError('ไม่พบเครื่องนี้');
      } else {
        if (typeof p.enabled !== 'boolean') throw new RequestError('สถานะที่ส่งมาไม่ถูกต้อง');
        sheet.getRange(current.row, 3).setValue(p.enabled);
        sheet.getRange(current.row, 5, 1, 2).setValues([[new Date(), 'admin']]);
      }
      tab_('AuditLogs').appendRow([new Date(), 'admin', action, id, '']);
    } else if (action !== 'getState') {
      throw new RequestError('ไม่รู้จักคำสั่ง');
    }
    return state_(sheet);
  });
}

function state_(sheet) {
  const log = tab_('AuditLogs');
  const length = Math.min(30, log.getLastRow() - 1);
  return {
    enabled: enabled_(settings_().enabled),
    devices: devices_(sheet).map(({ values: v }) => ({
      deviceId: String(v[0]), label: String(v[1]), enabled: enabled_(v[2]), notes: String(v[3] || ''),
      model: String(v[6] || ''), deviceName: String(v[7] || ''),
      lastSeen: v[8] instanceof Date ? v[8].getTime() : 0
    })),
    audit: length > 0 ? log.getRange(log.getLastRow() - length + 1, 1, length, 4).getValues()
      .reverse().map(v => ({ time: String(v[0]), actor: String(v[1]), action: String(v[2]), target: String(v[3]) })) : []
  };
}
