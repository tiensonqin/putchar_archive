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

  // add directoryIndex null
  workbox.precaching.precacheAndRoute([
    {
      "url": "/",
      "revision": "2x7f4de41b6647e74c2556fadde42156"
    },
    {
      "url": "{{mainjs}}",
      "revision": "{{mainjs-version}}"
    },
    {
      "url": "{{maincss}}",
      "revision": "{{maincss-version}}"
    },
    {
      "url": "/asciidoctor.min.js",
      "revision": "3x7fxde41b6647e74c2556fadde42159"
    },
    {
      "url": "favicon.png",
      "revision": "c241d4024b4a177c5cc1f3c560692f25"
    },
    {
      "url": "logo-1x.png",
      "revision": "b88ed58a0493c5550ce4e26da5d9efab"
    },
    {
      "url": "logo-2x.png",
      "revision": "973ee13e14d3d93e5fd9801f543202b1"
    },
    {
      "url": "images/logo.png",
      "revision": "620c97b12b9b4d817fcbf4b13f5f5230"
    },
    {
      "url": "manifest.json",
      "revision": "2a90dfcb46e5be374c07b49a804f9559"
    },
    {
      "url": "robots.txt",
      "revision": "dd67f12c54d8d48417745d2b90220e0a"
    }
  ],
                                      {
                                        directoryIndex: null
                                      });

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

  // workbox.googleAnalytics.initialize();

  workbox.routing.registerRoute(
    /.*(?:googleapis|gstatic|cloudflare)\.com.*$/,
    workbox.strategies.staleWhileRevalidate(),
  );

} else {
  console.log(`Boo! Workbox didn't load 😬`);
}
