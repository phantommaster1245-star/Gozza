/** Gozza Control Center API - secrets live ONLY in Apps Script Properties. */
const DEVICE_HEADERS=['device_id','label','enabled','notes','updated_at','updated_by'];
const LOG_HEADERS=['timestamp','actor','action','target','details'];
const SETTING_HEADERS=['key','value'];
const PROPS=()=>PropertiesService.getScriptProperties();
function setting(k){return PROPS().getProperty(k)||'';}
function required(k){const v=setting(k);if(!v)throw Error('Missing property: '+k);return v;}
function db(){return SpreadsheetApp.openById(required('SPREADSHEET_ID'));}
function tab(name){const t=db().getSheetByName(name);if(!t)throw Error('Missing sheet '+name);return t;}
function setupDatabase(){
  required('OWNER_EMAIL');
  let id=setting('SPREADSHEET_ID');
  const ss=id?SpreadsheetApp.openById(id):SpreadsheetApp.create('Gozza Control Center');
  if(!id)PROPS().setProperty('SPREADSHEET_ID',ss.getId());
  [['Devices',DEVICE_HEADERS],['Settings',SETTING_HEADERS],['AuditLogs',LOG_HEADERS]].forEach(([name,headers])=>{
    const t=ss.getSheetByName(name)||ss.insertSheet(name);
    if(t.getLastRow()===0)t.appendRow(headers);
    if(t.getRange(1,1,1,headers.length).getValues()[0].join('|')!==headers.join('|'))throw Error('Incorrect headers: '+name);
    t.setFrozenRows(1);
  });
  const s=tab('Settings');
  if(s.getLastRow()===1)s.getRange(2,1,5,2).setValues([
    ['enabled','true'],['revision','0'],['last_published_revision','-1'],['last_synced_at',''],['last_error','']
  ]);
  console.log(ss.getUrl());return ss.getUrl();
}
function allowedOrigins(){return required('FRONTEND_ORIGINS').split(',').map(x=>x.trim()).filter(Boolean);}
function doGet(){return HtmlService.createHtmlOutput('Gozza Control Center API online').setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);}
function doPost(e){
  let origin='',id='',payload;
  try{
    if(!e||!e.parameter||!e.parameter.packet||e.parameter.packet.length>16000)throw Error('Invalid packet');
    const p=JSON.parse(e.parameter.packet);origin=String(p.origin||'');id=String(p.id||'').slice(0,100);
    if(!allowedOrigins().includes(origin))throw Error('Origin not allowed');
    const actor=auth(p.token);payload={app:'gozza-control-v1',id,ok:true,result:dispatch(p.action,p.payload||{},actor)};
  }catch(err){payload={app:'gozza-control-v1',id,ok:false,error:String(err.message||err).slice(0,300)};}
  const target=allowedOrigins().includes(origin)?origin:allowedOrigins()[0];
  const safe=JSON.stringify(payload).replace(/</g,'\\u003c').replace(/>/g,'\\u003e').replace(/&/g,'\\u0026');
  return HtmlService.createHtmlOutput('<script>window.top.postMessage('+safe+','+JSON.stringify(target)+')</script>').setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}
function auth(token){
  if(typeof token!=='string'||token.length<100||token.length>6000)throw Error('Google sign-in required');
  const res=UrlFetchApp.fetch('https://oauth2.googleapis.com/tokeninfo?id_token='+encodeURIComponent(token),{muteHttpExceptions:true});
  if(res.getResponseCode()!==200)throw Error('Expired Google session');
  const c=JSON.parse(res.getContentText());
  if(c.aud!==required('GOOGLE_CLIENT_ID')||String(c.email_verified)!=='true'||Number(c.exp)<Date.now()/1000)throw Error('Invalid token');
  const email=String(c.email||'').toLowerCase();
  if(!required('OWNER_EMAIL').toLowerCase().split(',').map(x=>x.trim()).includes(email))throw Error('Not authorized');
  return email;
}
function settings(){
  const rows=tab('Settings').getDataRange().getValues().slice(1),out={};
  rows.forEach(row=>out[String(row[0])]=String(row[1]));return out;
}
function writeSetting(key,value){
  const t=tab('Settings'),rows=t.getDataRange().getValues();
  const i=rows.findIndex(row=>row[0]===key);
  if(i<0)t.appendRow([key,value]);else t.getRange(i+1,2).setValue(value);
}
function records(){
  const t=tab('Devices'),n=t.getLastRow();if(n<2)return [];
  return t.getRange(2,1,n-1,6).getValues().filter(x=>x[0]).map(x=>({
    deviceId:String(x[0]),label:String(x[1]),enabled:x[2]===true||String(x[2]).toLowerCase()==='true',
    notes:String(x[3]||''),updatedAt:String(x[4]||''),updatedBy:String(x[5]||'')
  }));
}
function logs(){
  const t=tab('AuditLogs'),n=t.getLastRow();if(n<2)return [];
  return t.getRange(Math.max(2,n-49),1,Math.min(48,n-1),5).getValues().reverse().map(x=>({
    time:String(x[0]),actor:String(x[1]),action:String(x[2]),target:String(x[3]),details:String(x[4])
  }));
}
function state(){
  const s=settings(),rev=Number(s.revision||0);
  return {enabled:s.enabled==='true',devices:records(),audit:logs(),sync:{
    inSync:s.last_published_revision===String(rev),revision:rev,lastPublishedRevision:Number(s.last_published_revision||-1),
    lastSyncedAt:s.last_synced_at||'',lastError:s.last_error||''
  }};
}
function hash(v){v=String(v||'').toLowerCase();if(!/^[a-f0-9]{64}$/.test(v))throw Error('Invalid 64-character device hash');return v;}
function dispatch(action,p,actor){
  if(action==='getState')return state();
  const lock=LockService.getScriptLock();lock.waitLock(20000);
  try{
    if(action==='publish'){publish();return state();}
    let target='';
    const t=tab('Devices');
    const data=records();
    if(action==='setGlobalEnabled'){if(typeof p.enabled!=='boolean')throw Error('Boolean required');writeSetting('enabled',String(p.enabled));target='ALL';}
    else if(action==='upsertDevice'){
      const id=hash(p.deviceId),label=String(p.label||'').trim(),notes=String(p.notes||'').trim();
      if(!label||label.length>80||notes.length>500||typeof p.enabled!=='boolean')throw Error('Invalid device');
      const i=data.findIndex(x=>x.deviceId===id);
      if(i<0&&data.length>=200)throw Error('Maximum 200 devices');
      const row=[id,label,p.enabled,notes,new Date(),actor];
      if(i<0)t.appendRow(row);else t.getRange(i+2,1,1,6).setValues([row]);target=id;
    }else if(action==='setDeviceEnabled'){
      const id=hash(p.deviceId),i=data.findIndex(x=>x.deviceId===id);
      if(i<0||typeof p.enabled!=='boolean')throw Error('Invalid device');
      t.getRange(i+2,3).setValue(p.enabled);t.getRange(i+2,5,1,2).setValues([[new Date(),actor]]);target=id;
    }else if(action==='deleteDevice'){
      const id=hash(p.deviceId),i=data.findIndex(x=>x.deviceId===id);
      if(i<0)throw Error('Device not found');t.deleteRow(i+2);target=id;
    }else throw Error('Unknown action');
    writeSetting('revision',String(Number(settings().revision||0)+1));
    tab('AuditLogs').appendRow([new Date(),actor,action,target,'']);
    try{publish();}catch(err){writeSetting('last_error',String(err.message||err).slice(0,300));}
    return state();
  }finally{lock.releaseLock();}
}
function publish(){
  const s=settings(),owner=required('GITHUB_OWNER'),repo=required('GITHUB_REPO'),branch=setting('GITHUB_BRANCH')||'main';
  if(!/^[\w.-]+$/.test(owner)||!/^[\w.-]+$/.test(repo)||!/^[\w.\/-]+$/.test(branch))throw Error('Invalid GitHub config');
  const url='https://api.github.com/repos/'+owner+'/'+repo+'/contents/control/access.json';
  const headers={Authorization:'Bearer '+required('GITHUB_TOKEN'),Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
  const old=UrlFetchApp.fetch(url+'?ref='+encodeURIComponent(branch),{headers,muteHttpExceptions:true});
  if(old.getResponseCode()!==200)throw Error('Cannot read GitHub policy: '+old.getResponseCode());
  const sha=JSON.parse(old.getContentText()).sha;
  const body=JSON.stringify({schema:1,enabled:s.enabled==='true',devices:records().filter(x=>x.enabled).map(x=>x.deviceId).sort()},null,2)+'\n';
  const bytes=Utilities.newBlob(body).getBytes();
  const data={message:'Update Gozza tester access via Control Center',content:Utilities.base64Encode(bytes),sha,branch};
  const put=UrlFetchApp.fetch(url,{method:'put',headers,contentType:'application/json',payload:JSON.stringify(data),muteHttpExceptions:true});
  if(put.getResponseCode()!==200&&put.getResponseCode()!==201)throw Error('GitHub update failed: '+put.getResponseCode());
  writeSetting('last_published_revision',s.revision||'0');writeSetting('last_synced_at',new Date().toISOString());writeSetting('last_error','');
}
