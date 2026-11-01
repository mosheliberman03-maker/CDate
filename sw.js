// Chabad Calendar service worker: shows reminders pushed from the server, and makes the app open its newest version.
// Opening the app always asks GitHub Pages for a fresh copy (instead of a copy the phone saved for up to 10 minutes).
// With no internet it falls back to the copy the phone already has. Nothing else is cached here.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (e) => e.waitUntil(self.clients.claim()));
self.addEventListener('push', (e) => {
  let d = {};
  try { d = e.data ? e.data.json() : {}; } catch (x) { d = { title: 'Chabad Calendar', body: e.data ? e.data.text() : '' }; }
  e.waitUntil(self.registration.showNotification(d.title || 'Chabad Calendar', {
    body: d.body || '', tag: d.tag || undefined, renotify: !!d.tag,
    icon: 'icon-192.png', badge: 'icon-192.png',
  }));
});
self.addEventListener('notificationclick', (e) => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((cs) => {
    for (const c of cs) { if ('focus' in c) return c.focus(); }
    return self.clients.openWindow('./');
  }));
});

// Opening the app: answer at once with the copy saved on the phone (no waiting on the network), and fetch the newest
// copy from GitHub in the background. If it changed, tell the open app so it can offer "Tap to update".
// First open (nothing saved yet) or no saved copy: wait for the network. Offline with nothing saved: normal browser behaviour.
const APP_CACHE = 'cdate-app-v1';
const sig = (res) => res.headers.get('etag') || res.headers.get('last-modified') || '';
self.addEventListener('fetch', (e) => {
  const r = e.request;
  if (r.method !== 'GET') return;
  const u = new URL(r.url);
  if (u.origin !== self.location.origin) return;
  const scope = new URL(self.registration.scope);
  const isApp = r.mode === 'navigate' || u.pathname === scope.pathname || u.pathname === scope.pathname + 'index.html';
  const isHy = u.pathname === scope.pathname + 'hayomyom-en.json'; // the English Hayom Yom text: same treatment, so it works offline
  if (!isHy && (!isApp || (u.pathname !== scope.pathname && u.pathname !== scope.pathname + 'index.html'))) return;
  const key = isHy ? scope.href + 'hayomyom-en.json' : scope.href; // one saved copy each, whatever ?query the link had
  e.respondWith((async () => {
    const cache = await caches.open(APP_CACHE);
    const saved = await cache.match(key);
    const net = (async () => {
      const res = await fetch(key, { cache: 'no-cache', credentials: 'same-origin' });
      if (res.ok && !res.redirected) {
        let changed = false;
        if (saved) {
          const a = sig(saved), b = sig(res);
          changed = a && b ? a !== b : (await saved.clone().text()) !== (await res.clone().text());
        }
        await cache.put(key, res.clone());
        if (changed && !isHy) {
          const cs = await self.clients.matchAll({ type: 'window' });
          cs.forEach((c) => c.postMessage({ type: 'cdate-update' }));
        }
      }
      return res;
    })();
    if (saved) { e.waitUntil(net.catch(() => {})); return saved; }
    try {
      const res = await net;
      if (res.redirected && r.mode === 'navigate') return Response.redirect(res.url, 302);
      return res;
    } catch (x) { return fetch(r); }
  })());
});
