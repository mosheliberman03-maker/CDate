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

self.addEventListener('fetch', (e) => {
  const r = e.request;
  if (r.method !== 'GET') return;
  const u = new URL(r.url);
  if (u.origin !== self.location.origin) return;
  if (r.mode !== 'navigate' && !/(?:\/|\.html|\.webmanifest|\/sw\.js)$/.test(u.pathname)) return;
  e.respondWith((async () => {
    try {
      const res = await fetch(r.url, { cache: 'no-cache', credentials: 'same-origin' });
      // a navigation may not be answered with a followed redirect; send the redirect itself instead
      if (res.redirected && r.mode === 'navigate') return Response.redirect(res.url, 302);
      return res;
    } catch (x) {
      try { return await fetch(r.url, { cache: 'force-cache', credentials: 'same-origin' }); }
      catch (y) { return fetch(r); }
    }
  })());
});
