/* Short second-pass collector: the 13 Hayom Yom days that came out in Hebrew. Downloads hayomyom-en-2.json. */
(async()=>{
const D=['2023-10-26','2023-11-02','2023-11-24','2023-12-31','2024-02-01','2024-02-23','2024-03-24','2024-04-17','2024-05-27','2024-06-12','2024-08-13','2024-09-23','2025-03-14'],got={};
const b=document.createElement('div');b.style.cssText='position:fixed;left:10px;right:10px;bottom:10px;z-index:2147483647;background:#161a2e;color:#fff;font:18px system-ui;padding:16px;border-radius:14px';document.body.appendChild(b);
for(let i=0;i<D.length;i++){const[y,m,d]=D[i].split('-').map(Number);b.textContent='Day '+(i+1)+' of 13…';
 for(let t=0;t<3;t++){try{const r=await fetch('/dailystudy/hayomyom.asp?tdate='+m+'/'+d+'/'+y);if(!r.ok)throw 0;
  const doc=new DOMParser().parseFromString(await r.text(),'text/html');let best=null,sc=0;
  doc.querySelectorAll('div,article,section,main,td').forEach(e=>{let s=0;for(const c of e.children)if(/^(P|H2|H3|H4|BLOCKQUOTE)$/.test(c.tagName))s+=(c.textContent.match(/[A-Za-z]/g)||[]).length;if(s>sc){sc=s;best=e}});
  got[D[i]]={paras:[...best.children].filter(c=>/^(P|H2|H3|H4|BLOCKQUOTE)$/.test(c.tagName)&&c.textContent.trim()).map(c=>c.innerHTML.replace(/\s+/g,' ').trim())};break}
 catch(e){await new Promise(r=>setTimeout(r,5000))}}
 await new Promise(r=>setTimeout(r,800))}
const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([JSON.stringify({days:got})],{type:'application/json'}));a.download='hayomyom-en-2.json';document.body.appendChild(a);a.click();
b.textContent='Done: '+Object.keys(got).length+' of 13. Upload hayomyom-en-2.json in the chat.'})();
