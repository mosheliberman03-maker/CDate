/* Hayom Yom collector. Run on www.chabad.org in the phone's own browser (from a bookmark).
   Visits each day's Hayom Yom page one at a time, keeps the paragraphs of the teaching, and at the end
   downloads hayomyom-en.json. Progress is saved, so running it again continues where it stopped. */
(async()=>{
if(window.__hyRun)return;window.__hyRun=1;
if(!/chabad\.org$/.test(location.hostname)){alert('Open www.chabad.org first, then tap the bookmark again.');window.__hyRun=0;return}
const KEY='cdate_hy_v1',days=[],add=(a,b)=>{for(let d=new Date(a+'T12:00:00');d<=new Date(b+'T12:00:00');d.setDate(d.getDate()+1))days.push(new Date(d))};
add('2023-09-16','2024-10-02');add('2024-12-01','2024-12-01');add('2024-12-31','2024-12-31');add('2025-03-01','2025-03-29');
const iso=d=>d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
let got={};try{got=JSON.parse(localStorage.getItem(KEY)||'{}')}catch(e){}
const box=document.createElement('div');box.style.cssText='position:fixed;left:10px;right:10px;bottom:10px;z-index:2147483647;background:#161a2e;color:#fff;font:16px/1.4 system-ui,sans-serif;padding:16px;border-radius:16px;box-shadow:0 8px 40px #0008';
box.innerHTML='<b>Collecting Hayom Yom</b><div id="hyS" style="margin:8px 0">Starting…</div><div style="height:8px;background:#fff3;border-radius:4px"><div id="hyB" style="height:8px;width:0;background:#8c9bff;border-radius:4px"></div></div><div style="margin-top:8px;font-size:13px;opacity:.8">Keep this screen on and Chrome open.</div><button id="hyD" style="margin-top:10px;padding:10px 14px;border-radius:10px;border:0;font:600 15px system-ui">Save what I have now</button>';
document.body.appendChild(box);const S=t=>box.querySelector('#hyS').textContent=t,B=p=>box.querySelector('#hyB').style.width=(p*100).toFixed(1)+'%';
const save=()=>{const blob=new Blob([JSON.stringify({source:'chabad.org Hayom Yom (English), used with permission',collected:new Date().toISOString(),days:got},null,0)],{type:'application/json'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='hayomyom-en.json';document.body.appendChild(a);a.click();setTimeout(()=>a.remove(),2000)};
box.querySelector('#hyD').onclick=save;
let wl=null;try{wl=await navigator.wakeLock.request('screen')}catch(e){}
document.addEventListener('visibilitychange',async()=>{if(document.visibilityState==='visible'){try{wl=await navigator.wakeLock.request('screen')}catch(e){}}});
const clean=el=>{const c=el.cloneNode(true);c.querySelectorAll('script,style,iframe,img,svg,button,form,noscript').forEach(x=>x.remove());c.querySelectorAll('*').forEach(x=>{for(const a of [...x.attributes])if(a.name!=='href')x.removeAttribute(a.name)});return c.innerHTML.replace(/\s+/g,' ').trim()};
const pick=doc=>{let best=null,score=0;doc.querySelectorAll('div,article,section,main,td').forEach(el=>{let s=0;for(const ch of el.children)if(/^(P|BLOCKQUOTE|H2|H3|H4)$/.test(ch.tagName))s+=ch.textContent.trim().length;if(s>score){score=s;best=el}});return best};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
let fails=0;
for(let i=0;i<days.length;i++){const d=days[i],k=iso(d);B(i/days.length);if(got[k])continue;
 S('Day '+(i+1)+' of '+days.length+' ('+k+')');
 const url='/dailystudy/hayomyom.asp?tdate='+(d.getMonth()+1)+'/'+d.getDate()+'/'+d.getFullYear();
 let ok=false;
 for(let t=0;t<4&&!ok;t++){try{const r=await fetch(url,{credentials:'include'});if(!r.ok)throw new Error('HTTP '+r.status);
   const doc=new DOMParser().parseFromString(await r.text(),'text/html'),el=pick(doc);
   if(!el)throw new Error('no text found');
   const paras=[...el.children].filter(ch=>/^(P|BLOCKQUOTE|H2|H3|H4)$/.test(ch.tagName)&&ch.textContent.trim()).map(clean);
   const h1=doc.querySelector('h1'),hd=[...doc.querySelectorAll('h1,h2,h3,.date,[class*=date]')].map(x=>x.textContent.trim()).filter(Boolean).slice(0,6);
   got[k]={url:location.origin+url,title:doc.title.trim(),h1:h1?h1.textContent.trim():'',heads:hd,paras};ok=true;fails=0}
  catch(e){S('Day '+(i+1)+': '+e.message+'. Waiting and trying again…');await sleep(t<1?5000:30000)}}
 if(!ok){fails++;if(fails>=3){S('Chabad.org stopped answering. Tap “Save what I have now”, or wait a while and tap the bookmark again to continue.');window.__hyRun=0;return}}
 try{localStorage.setItem(KEY,JSON.stringify(got))}catch(e){}
 await sleep(900)}
B(1);S('Done: '+Object.keys(got).length+' days. Saving the file…');save();
setTimeout(()=>S('Done. The file hayomyom-en.json is in your Downloads. Upload it in the chat.'),1500);
window.__hyRun=0})();
