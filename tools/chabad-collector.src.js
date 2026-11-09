/* Chabad.org English collector (owner has Chabad.org's permission), lean version (Chrome cut the longer bookmark short).
   Run on www.chabad.org in Chrome from a bookmark. Opens the daily-study pages one at a time: Chumash with Rashi for every
   day Oct 2025 – Oct 2026 (the whole Torah) and Tehillim for the 30 days of Tishrei 5787 (all of Tehillim), keeps only the
   English text pieces with a short label of where each sits on the page, and downloads chabad-en.json at the end. */
(async()=>{
if(!/chabad\.org$/.test(location.hostname))return alert('Open www.chabad.org first, then tap the bookmark again.');
const J=[],add=(k,a,b)=>{for(let d=new Date(a+'T12:00');d<=new Date(b+'T12:00');d.setDate(d.getDate()+1))J.push([k,new Date(d)])};
add('torahreading','2025-10-12','2026-10-10');add('tehillim','2026-09-12','2026-10-11');
const X=document.createElement('div');X.style.cssText='position:fixed;left:8px;right:8px;bottom:8px;z-index:2147483647;background:#161a2e;color:#fff;font:16px system-ui;padding:14px;border-radius:14px';
X.innerHTML='<b>Collecting Chabad.org English</b><div id=cS>Starting…</div><div style="height:8px;background:#fff3;margin:8px 0"><div id=cB style="height:8px;width:0;background:#8c9bff"></div></div><button id=cD>Save what I have now</button>';
document.body.appendChild(X);const S=t=>X.querySelector('#cS').textContent=t,G={};
const save=()=>{const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([JSON.stringify({source:'chabad.org (used with permission)',pages:G})]));a.download='chabad-en.json';X.appendChild(a);a.click()};
X.querySelector('#cD').onclick=save;try{await navigator.wakeLock.request('screen')}catch(e){}
const lab=e=>e.tagName.toLowerCase()+(typeof e.className=='string'&&e.className.trim()?'.'+e.className.trim().split(/\s+/).join('.'):'');
const L=t=>(t.match(/[A-Za-z]/g)||[]).length;
for(let i=0;i<J.length;i++){const[k,d]=J[i],u='/dailystudy/'+k+'.asp?tdate='+(d.getMonth()+1)+'/'+d.getDate()+'/'+d.getFullYear();
 X.querySelector('#cB').style.width=i/J.length*100+'%';S((i+1)+' of '+J.length);let ok=0;
 for(let t=0;t<4&&!ok;t++){try{const r=await fetch(u);if(!r.ok)throw Error('HTTP '+r.status);
   const D=new DOMParser().parseFromString(await r.text(),'text/html');D.querySelectorAll('script,style,noscript,iframe,svg,header,footer,nav').forEach(x=>x.remove());
   const tot=L(D.body.textContent);let R=D.body;D.querySelectorAll('div,section,main,table,td').forEach(e=>{if(L(e.textContent)>=tot*.9&&e.textContent.length<R.textContent.length)R=e});
   const I=[],w=D.createTreeWalker(R,4);let n;
   while(n=w.nextNode()){const t=n.textContent.replace(/\s+/g,' ').trim(),h=(t.match(/[֐-׿]/g)||[]).length;if(!t||!(L(t)>h||/^[\d\s.:,;()\-–]+$/.test(t)))continue;
    const p=[];for(let e=n.parentElement;e&&e!==R.parentElement&&p.length<4;e=e.parentElement)p.unshift(lab(e));I.push([p.join('>'),t])}
   if(I.length<5)throw Error('no text');G[k+':'+d.toISOString().slice(0,10)]={u,h:[...D.querySelectorAll('h1,h2,h3')].map(x=>x.textContent.trim()).slice(0,8),I};ok=1}
  catch(e){S((i+1)+': '+e.message+', trying again…');await new Promise(r=>setTimeout(r,t?20000:4000))}}
 await new Promise(r=>setTimeout(r,600))}
S('Done. Saving chabad-en.json…');save()})().catch(e=>alert('Stopped: '+e.message));
