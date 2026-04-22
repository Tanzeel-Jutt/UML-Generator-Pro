const CACHE_NAME = 'uml-gen-v2';
const ASSETS = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './parsers.js',
  './renderers.js',
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS))
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(keys.map((key) => {
        if (key !== CACHE_NAME) return caches.delete(key);
      }));
    })
  );
});

self.addEventListener('fetch', (event) => {
  // Network First strategy
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request))
  );
});
