// sw.js — minimal service worker. Its main job is enabling installability;
// it also does light caching of the static app shell so the icon/manifest/
// layout load instantly on repeat visits. It deliberately never caches API
// responses (anything under /auth, /hostels, /rooms) — those must always be
// fresh, since caching auth/task/message data would show stale or wrong
// information, which matters a lot more here than saving a network round trip.

const CACHE_NAME = "hostelapp-shell-v1";
const SHELL_FILES = [
  "/",
  "/index.html",
  "/manifest.json",
  "/icons/icon-192.png",
  "/icons/icon-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_FILES)).catch(() => {
      // Non-fatal — if the shell can't be pre-cached (e.g. offline on first
      // install), the app still works normally, just without offline support yet.
    })
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

function isApiRequest(url) {
  return ["/auth", "/hostels", "/rooms", "/health"].some((prefix) => url.pathname.startsWith(prefix));
}

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);

  if (event.request.method !== "GET" || isApiRequest(url)) {
    return; // let the browser handle it normally — always hit the network
  }

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        const clone = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
        return response;
      })
      .catch(() => caches.match(event.request))
  );
});
