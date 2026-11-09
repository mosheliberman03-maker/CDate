/* Chabad.org English collector (owner has Chabad.org's permission). Run on www.chabad.org in the phone's own Chrome, from a
   bookmark (Chabad.org refuses downloads that don't come from a real browser, HTTP 403). It opens the daily-study pages one
   at a time: Chumash with Rashi for every day from Oct 2025 to Oct 2026 (the whole Torah) and Tehillim for the 30 days of
   Tishrei 5787 (all of Tehillim). From each page it keeps only the English text pieces, each with a short label of where it
   sits on the page (tags + classes), so the app can be built from it. Progress is kept on the phone (IndexedDB), so tapping
   the bookmark again continues. At the end it downloads chabad-en.json. */
(async()=>{
if(window.__chRun)return;window.__chRun=1;
if(!/chabad\.org$/.test(location.hostname)){alert('Open www.chabad.org first, then tap the bookmark again.');window.__chRun=0;return}
const jobs=[],add=(kind,a,b)=>{for(let d=new Date(a+'T12:00:00');d<=new Date(b+'T12:00:00');d.setDate(d.getDate()+1))jobs.push([kind,new Date(d)])};
add('torah','2025-10-12','2026-10-10');add('tehillim','2026-09-12','2026-10-11');
const iso=d=>d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
const PAGE={torah:'torahreading',tehillim:'tehillim'};
// progress store
const db=await new Promise((ok,no)=>{const r=indexedDB.open('cdate_chabad',1);r.onupgradeneeded=()=>r.result.createObjectStore('p');r.onsuccess=()=>ok(r.result);r.onerror=()=>no(r.error)});
const tx=(m,f)=>new Promise((ok,no)=>{const t=db.transaction('p',m),s=t.objectStore('p'),q=f(s);t.oncomplete=()=>ok(q&&q.result);t.onerror=()=>no(t.error)});
const getAll=()=>new Promise(ok=>{const out={},t=db.transaction('p'),c=t.objectStore('p').openCursor();c.onsuccess=()=>{const x=c.result;if(x){out[x.key]=x.value;x.continue()}else ok(out)}});
let got=await getAll();
const box=document.createElement('div');box.style.cssText='position:fixed;left:10px;right:10px;bottom:10px;z-index:2147483647;background:#161a2e;color:#fff;font:16px/1.4 system-ui,sans-serif;padding:16px;border-radius:16px;box-shadow:0 8px 40px #0008';
box.innerHTML='<b>Collecting Chabad.org English</b><div id="chS" style="margin:8px 0">Starting…</div><div style="height:8px;background:#fff3;border-radius:4px"><div id="chB" style="height:8px;width:0;background:#8c9bff;border-radius:4px"></div></div><div style="margin-top:8px;font-size:13px;opacity:.8">Keep this screen on and Chrome open. About 15–20 minutes.</div><button id="chD" style="margin-top:10px;padding:10px 14px;border-radius:10px;border:0;font:600 15px system-ui">Save what I have now</button>';
document.body.appendChild(box);const S=t=>box.querySelector('#chS').textContent=t,B=p=>box.querySelector('#chB').style.width=(p*100).toFixed(1)+'%';
const save=async()=>{got=await getAll();const blob=new Blob([JSON.stringify({source:'chabad.org daily study (English), used with permission',collected:new Date().toISOString(),pages:got})],{type:'application/json'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='chabad-en.json';document.body.appendChild(a);a.click();setTimeout(()=>a.remove(),2000)};
box.querySelector('#chD').onclick=save;
let wl=null;try{wl=await navigator.wakeLock.request('screen')}catch(e){}
document.addEventListener('visibilitychange',async()=>{if(document.visibilityState==='visible'){try{wl=await navigator.wakeLock.request('screen')}catch(e){}}});
const LAT=/[A-Za-z]/,HEB=/[֐-׿]/g;
// the smallest element that still holds ~90% of the page's English letters = the text area
const area=doc=>{const tot=(doc.body.textContent.match(/[A-Za-z]/g)||[]).length;let best=doc.body;
 doc.querySelectorAll('div,article,section,main,table,td').forEach(el=>{const n=(el.textContent.match(/[A-Za-z]/g)||[]).length;if(n>=tot*.9&&el.textContent.length<best.textContent.length)best=el});return best};
const lab=el=>el.tagName.toLowerCase()+(el.className&&typeof el.className==='string'&&el.className.trim()?'.'+el.className.trim().split(/\s+/).join('.'):'');
// [label path (up to 4 parents), text] for every English (or number) text piece
const flat=root=>{const out=[],w=root.ownerDocument.createTreeWalker(root,NodeFilter.SHOW_TEXT);let n;
 while((n=w.nextNode())){const t=n.textContent.replace(/\s+/g,' ').trim();if(!t)continue;const p=n.parentElement;if(!p||/^(SCRIPT|STYLE|NOSCRIPT)$/.test(p.tagName))continue;
  const h=(t.match(HEB)||[]).length,l=(t.match(/[A-Za-z]/g)||[]).length;if(!(l>h||/^[\d\s.:,;()\-–]+$/.test(t)))continue;
  const path=[];for(let e=p;e&&e!==root.parentElement&&path.length<4;e=e.parentElement)path.unshift(lab(e));out.push([path.join('>'),t])}
 return out};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));let fails=0;
for(let i=0;i<jobs.length;i++){const[kind,d]=jobs[i],k=kind+':'+iso(d);B(i/jobs.length);if(got[k])continue;
 S((kind==='torah'?'Chumash':'Tehillim')+' · '+(i+1)+' of '+jobs.length+' ('+iso(d)+')');
 const url='/dailystudy/'+PAGE[kind]+'.asp?tdate='+(d.getMonth()+1)+'/'+d.getDate()+'/'+d.getFullYear();let ok=false;
 for(let t=0;t<4&&!ok;t++){try{const r=await fetch(url,{credentials:'include'});if(!r.ok)throw new Error('HTTP '+r.status);
   const doc=new DOMParser().parseFromString(await r.text(),'text/html');doc.querySelectorAll('script,style,noscript,iframe,svg,header,footer,nav').forEach(x=>x.remove());const root=area(doc);const items=flat(root);
   if(items.length<5)throw new Error('no text found');
   const v={url:location.origin+url,title:doc.title.trim(),heads:[...doc.querySelectorAll('h1,h2,h3')].map(x=>x.textContent.replace(/\s+/g,' ').trim()).filter(Boolean).slice(0,8),root:lab(root),items};
   await tx('readwrite',s=>s.put(v,k));got[k]=1;ok=true;fails=0}
  catch(e){S('#'+(i+1)+': '+e.message+'. Waiting and trying again…');await sleep(t<1?5000:30000)}}
 if(!ok){fails++;if(fails>=3){S('Chabad.org stopped answering. Tap “Save what I have now”, or wait a while and tap the bookmark again to continue.');window.__chRun=0;return}}
 await sleep(700)}
B(1);S('Done. Saving the file…');await save();
setTimeout(()=>S('Done. The file chabad-en.json is in your Downloads. Upload it in the chat.'),1500);
window.__chRun=0})();
