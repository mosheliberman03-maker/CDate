/* Nightly (GitHub Actions): save the next days' shiur texts from Sefaria into sefaria/, so the app can read them from
   its own site. Sefaria's Cloudflare protection blocks requests from phones-in-apps and from Cloudflare Workers, but
   answered GitHub's servers. Runs the app's own reader code (rdBuild) in a headless browser, so exactly the texts the
   reader asks for get saved, under the file names the reader looks for (rdLocal). Builds in 'both' mode, so the
   English translations are saved too (v76).
   Usage: node tools/prefetch-shiurim.js [days ahead, default 8] [--fake samples.json for testing] */
const {chromium}=require('playwright');const fs=require('fs');const path=require('path');
const ROOT=path.join(__dirname,'..'),OUT=path.join(ROOT,'sefaria'),days=+(process.argv[2]||8);
const fakeI=process.argv.indexOf('--fake'),FAKE=fakeI>0?JSON.parse(fs.readFileSync(process.argv[fakeI+1],'utf8')):null;
const KEYS=['chumash','tehillim','tanya','rambam3','rambam1'];
// keep only what the reader uses
function trim(u,j){
 if(/\/api\/calendars/.test(u))return{calendar_items:(j.calendar_items||[]).filter(x=>/Parashat Hashavua|Tanya Yomi|Daily Rambam/.test(x.title&&x.title.en)).map(x=>({title:x.title,displayValue:x.displayValue,url:x.url,ref:x.ref,extraDetails:x.extraDetails}))};
 if(/\/api\/v2\/raw\/index\//.test(u))return{alt_structs:{Parasha:{nodes:((j.alt_structs&&j.alt_structs.Parasha&&j.alt_structs.Parasha.nodes)||[]).map(n=>({wholeRef:n.wholeRef,refs:n.refs,sharedTitle:n.sharedTitle}))}}};
 const v=(j.versions||[])[0]||{};return{ref:j.ref,heRef:j.heRef,sections:j.sections,toSections:j.toSections,next:j.next,versions:[{text:v.text,versionTitle:v.versionTitle}]}}
async function getJSON(u){if(FAKE){const k=Object.keys(FAKE).find(x=>decodeURI(x)===decodeURI(u));if(!k)throw new Error('no sample '+u);return FAKE[k]}
 for(let t=0;t<3;t++){try{const r=await fetch(u,{headers:{'User-Agent':'CDate calendar (nightly shiur prefetch)'}});if(r.ok)return await r.json();if(r.status===404)throw new Error('404 '+u)}catch(e){if(/^404/.test(e.message))throw e}await new Promise(r=>setTimeout(r,3000*(t+1)))}throw new Error('failed '+u)}
(async()=>{fs.mkdirSync(OUT,{recursive:true});const b=await chromium.launch();const ctx=await b.newContext({timezoneId:'Asia/Shanghai'});const pg=await ctx.newPage();
 let saved=0,errors=[];
 await ctx.route('**/*',async r=>{const u=r.request().url();
  if(u.startsWith('https://www.sefaria.org/api/')){try{const j=await getJSON(u),t=trim(u,j),file=await pg.evaluate(x=>rdLocal(x),u);fs.writeFileSync(path.join(ROOT,file),JSON.stringify(t));saved++;return r.fulfill({contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify(t)})}catch(e){errors.push(e.message);return r.fulfill({status:502,headers:{'access-control-allow-origin':'*'},body:'{}'})}}
  if(/^https?:/.test(u))return r.abort();return r.continue()});   // nothing else leaves the machine (fonts, relay, saved copies)
 await pg.addInitScript(()=>{localStorage.setItem('cc_local_profiles_v1',JSON.stringify({bot:{h:'x'}}));localStorage.setItem('cc_local_active_profile_v1','bot');localStorage.setItem('cc_local_tour_done_bot','1')});
 await pg.goto('file://'+path.join(ROOT,'index.html'));await pg.waitForFunction(()=>typeof rdBuild==='function');
 const today=new Date(new Date().toLocaleString('en-US',{timeZone:'Asia/Shanghai'}));
 for(let i=-1;i<=days;i++){const d=new Date(today.getFullYear(),today.getMonth(),today.getDate()+i,12);
  for(const k of KEYS){const res=await pg.evaluate(async([k,t])=>{try{await rdBuild(k,new Date(t),'both');return 'ok'}catch(e){return 'ERR '+e.message}},[k,+d]);
   console.log(d.toISOString().slice(0,10),k,res)}}
 // tidy: day schedules older than 3 days are no longer needed (texts are kept; they repeat in later years)
 const cut=new Date(today.getFullYear(),today.getMonth(),today.getDate()-3);
 for(const f of fs.readdirSync(OUT)){const m=/^calendars~3fyear~3d(\d+)~26month~3d(\d+)~26day~3d(\d+)/.exec(f);if(m&&new Date(+m[1],+m[2]-1,+m[3])<cut){fs.unlinkSync(path.join(OUT,f))}}
 console.log('saved',saved,'files; errors',errors.length);errors.slice(0,10).forEach(e=>console.log('  ',e));
 await b.close();if(errors.length&&!saved)process.exit(1)})();
