importScripts('https://storage.googleapis.com/workbox-cdn/releases/3.1.0/workbox-sw.js');

var cacheId = "lambdahackers-001";
if (workbox) {
  // console.log(`Yay! Workbox is loaded 🎉`);
  workbox.skipWaiting();
  workbox.clientsClaim();
  workbox.cacheId(cacheId);

  self.addEventListener('activate', () => {
    // Get a list of all the current open windows/tabs under
    // our service worker's control, and force them to reload.
    // This can "unbreak" any open windows/tabs as soon as the new
    // service worker activates, rather than users having to manually reload.

    self.clients.matchAll({ type: 'window' }).then(windowClients => {
      windowClients.forEach(windowClient => {
        windowClient.navigate(windowClient.url);
      });
    });

    event.waitUntil(
      caches.keys().then(function (keyList) {
        return Promise.all(keyList.map(function (key) {
          if (key !== cacheId) {
            console.log('[ServiceWorker] Removing old cache', key);
            return caches.delete(key);
          }
        }));
      })
    );
    return self.clients.claim();
  });

  workbox.routing.registerRoute(
    '/',
    workbox.strategies.networkOnly(),
  );

  // add directoryIndex null
  workbox.precaching.precacheAndRoute([]);

  workbox.routing.registerRoute(
    new RegExp('^https://fonts.(?:googleapis|gstatic).com/(.*)'),
    workbox.strategies.cacheFirst(),
  );

  workbox.routing.registerRoute(
      /\.(?:js|css)$/,
    workbox.strategies.staleWhileRevalidate(),
  );

  workbox.routing.registerRoute(
      /\.(?:png|gif|jpg|jpeg|svg)$/,
    workbox.strategies.cacheFirst({
      cacheName: 'images',
      plugins: [
        new workbox.expiration.Plugin({
          maxEntries: 60,
          maxAgeSeconds: 30 * 24 * 60 * 60, // 30 Days
        }),
      ],
    }),
  );

  workbox.googleAnalytics.initialize();

  workbox.routing.registerRoute(
    /.*(?:googleapis|gstatic)\.com.*$/,
    workbox.strategies.staleWhileRevalidate(),
  );

} else {
  console.log(`Boo! Workbox didn't load 😬`);
}
