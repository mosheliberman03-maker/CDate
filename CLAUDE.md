# CDate: Chabad Calendar (notes for Claude)

This repository is a hosted web app: a Chabad/Jewish calendar that is installed on the owner's phone (Pixel 6 Pro, Android) from GitHub Pages and sends push reminders. The owner works **from a phone only** and is not a programmer. Explain things in plain words, one step at a time, and do the git work yourself.

- Live site: https://mosheliberman03-maker.github.io/CDate/ (GitHub Pages, `main` branch, root folder)
- Push server: https://cdate-push.mosheliberman03.workers.dev (Cloudflare Worker named `cdate-push`, source in `server/worker.js`)
- Owner's location default: Foshan, China (timezone Asia/Shanghai). Users: fewer than a few dozen people.

## Files
| File | What it is |
|---|---|
| `index.html` | The whole app in one file (HTML + CSS + JS, about 380 KB). Edit it directly. |
| `sw.js` | Service worker (registered for every user on load): shows push notifications, focuses the app on tap, and keeps one saved copy of the app (cache `cdate-app-v1`): opening answers instantly from it and fetches the newest copy in the background; if that changed, it messages the page (`cdate-update`) and a "Tap to update" button appears. No other caching. |
| `manifest.webmanifest`, `icon-*.png`, `apple-touch-icon.png` | Install metadata and icons (a plain aleph in a serif font). |
| `server/worker.js` | Cloudflare Worker source. **Not served from here**: changes must be pasted into the Cloudflare dashboard by the owner (Workers & Pages, then cdate-push, then Edit code, then Deploy). Needs KV binding `KV` and cron `* * * * *`. |

Do not add files named `index (1).html` or similar: only `index.html` is served. GitHub Pages tells browsers to cache for about 10 minutes, but the service worker bypasses that: after Pages publishes (1–2 minutes), the next open shows "Tap to update" (or the open after that shows the new version). If the app was left open in the background, a "Tap to update" button appears when it returns after 2+ minutes.

## Inside index.html (main pieces, by function name)
- **Calendar core:** `hd()` Hebrew date, `t0` today (rolls over at sunset), `sel` selected day, `LC` location `{n,la,lo,tz}`, `setLoc(n,la,lo,tz)`, `zAt()` zmanim, `render()`, `panel()`, `upd()`. Study tracking lives in `DN` (key `YYYY-MM-DD` → `{chumash,tehillim,tanya,rambam3|rambam1,hy}`); `tog(k)` toggles one.
- **Repeating plans only on today:** the user's own daily / every-other-day / weekly plans (`routine(e)`) are shown only on today. Display code uses `infoD(dt)` (filtered); `info(dt)` keeps everything for reminders and for cancelling. Use `infoD` for anything new that displays days.
- **Per-profile storage:** `AK(key)`, `ST(key,default)`. Local profiles and a login gate exist (`cc_local_profiles_v1`).
- **Opening logo animation:** the `#intro` overlay script (10.7 s of timeline played at `SPEED` 1.65, about 6.5 s; late frames pause it instead of skipping). Note: a `<style id="android-blank-screen-fix">` tag sits *inside* the main CSS as text, so its `#intro{display:none}` rule is dead. Leave it: "fixing" it would remove the intro the owner likes.
- **Live sky:** `SKY`, `skyCompute()`, `skyPalette()`, `skyApply()`, `skyTick()` (every 30 s). Sun and moon artwork is procedural: `artSunCanvasKey()` (fields precomputed once in `artSunFieldRows()`), `artMoonCanvas()`, plus `artSunUrl()` / `artMoonUrl()` for `<img>` use.
- **Sky effects (newest work):** `SKYA` state. `skyTrail()` / `skyTrailBuild()` draw the "breath on a window" streak (built once offscreen, then revealed). `skyPlan()` + `skyRun()` play the opening sun/moon animation after the logo finishes: the sun rises to its place; the moon rises to its place; after sunset the sun sets while the sky darkens; after moonset before sunrise the moon rises and sets. `skyWarm()` prepares artwork during the logo. `skyReplay()` reruns it after 5+ minutes away. It is skipped with reduced motion. Movement uses `transform` only, and root colour variables are updated sparingly, for performance.
- **Chitas streak and stats:** `streakInfo()`, chip `#stk` → `stsOpen()`: a bottom sheet with Week / Month / Year / All time pages (horizontal scroll-snap), 270° rings (`sRing()`), per-shiur cards. CSS classes there are prefixed (`shero`, `smc`, `ssm`…) because the app already uses short names like `.hero`, `.mc`, `.sm`. Haptics come from the `H` object (`H.tap/sel/ok`).
- **Reminders:** `notifPlan()` builds "one reminder per occasion" threads; settings sheet `rmOpen()`; in-app cards `rcRender()`. Web push: `PUSH_DEFAULT` holds the server address, `pushTurnOn()`, `pushSync()` (POST `/sync` with future items; each item's body lists everything so far in its thread, and `tag` makes the phone replace the earlier notification), `nTest()` → `/test`.

## Push server (server/worker.js)
Endpoints: `GET /vapid` (public key, generated once into KV), `POST /sync`, `/unsub`, `/test`. The cron sends due items (15-minute grace window), uses an `idx` key so each minute costs one KV read, and rebuilds the index every 10 minutes (KV free plan allows about 1000 list calls a day). RFC 8291 encryption was verified against the RFC test vector.

## Open items / ideas
- Home-screen **widget** (Hebrew/English date, today's special events, tasks today): not possible from a web app on Android. It needs a native app (an earlier Capacitor project exists outside this repo, untested).
- Android web push depends on Google's push service; in mainland China it can be unreliable without a VPN.
- The owner asked for good haptics and a clean, uncrowded UI. Check visuals at phone width (about 412 px) in light and dark sky themes.
