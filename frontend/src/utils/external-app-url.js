const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1', '::1'])

/**
 * Resolve a standalone app URL for the current browser host.
 * A localhost value from development config must not send LAN users to their own computer.
 */
export function resolveExternalAppUrl(configuredUrl, defaultPort) {
  const fallback = `http://${window.location.hostname}:${defaultPort}`
  const candidate = configuredUrl?.trim() || fallback

  try {
    const url = new URL(candidate, window.location.origin)
    if (LOOPBACK_HOSTS.has(url.hostname)) {
      url.hostname = window.location.hostname
    }
    return url.toString().replace(/\/$/, '')
  } catch {
    return fallback
  }
}
