importScripts('https://storage.googleapis.com/workbox-cdn/releases/3.1.0/workbox-sw.js');

var prefix = "lambdahackers";
var suffix = "{{suffix}}";
if (workbox) {
  // console.log(`Yay! Workbox is loaded 🎉`);
  workbox.skipWaiting();
  workbox.clientsClaim();
  workbox.core.setCacheNameDetails({
    prefix: prefix,
    suffix: suffix
  });

  workbox.routing.registerRoute(
    '/',
    workbox.strategies.networkOnly(),
  );

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
          maxAgeSeconds: 10 * 24 * 60 * 60, // 10 Days
        }),
      ],
    }),
  );

  workbox.routing.registerRoute(
    /.*(?:googleapis|gstatic|cloudflare)\.com.*$/,
    workbox.strategies.staleWhileRevalidate(),
  );

} else {
  console.log(`Boo! Workbox didn't load 😬`);
}
