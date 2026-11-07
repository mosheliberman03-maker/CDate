// Assistant regression test. Serve the repo (python3 -m http.server 8792), then:
//   node tools/assistant-tests/run.js tools/assistant-tests/phrases.txt > /tmp/out.txt && diff tools/assistant-tests/expected.txt /tmp/out.txt
// The clock is fixed (Thu 5 Nov 2026, 10:00 Asia/Shanghai) so results are stable. Any difference = a change in how requests are read.
// Runs every phrase through the real pipeline (split -> interpretSeg) on a fixed clock and prints compact results.
const {chromium}=require('playwright');const fs=require('fs');
const WV='Mozilla/5.0 (Linux; Android 14; Pixel 6 Pro; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/126.0 Mobile Safari/537.36';
const init=`window.Capacitor={isNativePlatform:()=>true,getPlatform:()=>'android',Plugins:{}};localStorage.setItem('cc_local_profiles_v1',JSON.stringify({moshe:{passwordHash:'x',created:1}}));localStorage.setItem('cc_local_active_profile_v1','moshe');localStorage.setItem('cc_perm_asked','1');localStorage.setItem('cc_local_tour_done_moshe','1');localStorage.setItem('cc_seen_ver_moshe','999');`;
const file=process.argv[2]||'phrases.txt',T=process.argv[3]||'2026-11-05T10:00:00+08:00';
const Q=fs.readFileSync(file,'utf8').split('\n').map(s=>s.trim()).filter(s=>s&&!s.startsWith('#'));
(async()=>{const b=await chromium.launch();const c=await b.newContext({userAgent:WV,timezoneId:'Asia/Shanghai',serviceWorkers:'block',reducedMotion:'reduce'});await c.addInitScript(init);const p=await c.newPage();const errs=[];p.on('pageerror',e=>errs.push(e.message));await p.route(/^https:/,r=>r.abort());
 await p.clock.setFixedTime(new Date(T));await p.goto((process.env.CC_URL||'http://localhost:8792/index.html'));await p.waitForTimeout(2500);
 const R=await p.evaluate(async Q=>{sel=t0;const out=[];for(const q of Q){try{const segs=aiSplitRequests(q);const plans=[];for(const s of segs){const ps=await aiInterpretSeg(s);ps.forEach(x=>plans.push(x))}
   out.push([q,plans.map(x=>x.unsupported?'UNSUPPORTED: '+x.clarify:[x.title,'['+x.recurrence+(x.dows?' '+x.dows.join(''):'')+(x.interval?' i'+x.interval:'')+(x.dates?' n'+x.dates.length:'')+(x.hd?' hd'+x.hd:'')+']',x.date,x.time||'-',x.participant?'w/'+x.participant:'',x.kind=='task'?'TASK':'',x.rem&&x.rem.length?'rem:'+x.rem.map(r=>r.type+(r.min!=null?r.min:'')).join(','):'',x.needsClarification?'CLARIFY: '+x.clarify:'',(x.warnings||[]).filter(w=>!/Reminders are not on/.test(w)).length?'W: '+(x.warnings||[]).filter(w=>!/Reminders are not on/.test(w)).join(' | ').slice(0,90):''].filter(Boolean).join(' · '))])}catch(e){out.push([q,['ERROR '+e.message]])}}return out},Q);
 for(const [q,ps] of R){console.log('» '+q);ps.forEach(x=>console.log('   '+x))}console.log('errors',errs);await b.close()})();
