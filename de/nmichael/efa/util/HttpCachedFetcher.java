package de.nmichael.efa.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Small reusable HTTP helper that performs conditional GETs using
 * standard HTTP caching headers (If-Modified-Since / If-None-Match).
 *
 * How it works
 * - On a successful 2xx response, this class stores the server-provided validators:
 *   - Last-Modified header value (via HttpURLConnection#getLastModified()).
 *   - ETag response header (entity tag), if present.
 * - On subsequent fetches for the same URL, it sends conditional request headers:
 *   - If-None-Match: <ETag> (when an ETag is available)
 *   - If-Modified-Since: <Last-Modified> (when a last-modified value is available)
 *   According to the HTTP specification, If-None-Match is the primary validator; if it
 *   is present, servers are expected to ignore If-Modified-Since for cache validation.
 * - If the server determines the representation has not changed, it replies with
 *   304 (Not Modified) and no body. In that case this fetcher returns STATUS_NOT_MODIFIED
 *   and does not change its stored validators. If the server returns 2xx, the body is
 *   read and validators are updated.
 *
 * Header details and caching behavior
 * - ETag is an opaque identifier for a specific representation; it can be strong or weak.
 *   Weak ETags (prefixed with W/) are suitable for cache validation but not for range
 *   requests requiring byte-for-byte equality. This fetcher sends the ETag back verbatim
 *   in If-None-Match and relies on the server to evaluate it.
 * - If-Modified-Since uses the resource's modification time with a typical resolution of
 *   whole seconds; clock skew and rounding can affect server evaluation. When an ETag is
 *   available, it is preferred over date-based validation.
 * - Cache-Control: max-age=0 and Pragma: no-cache are set on requests to force revalidation
 *   on every interval while still allowing efficient 304 responses. This does not disable
 *   caching; it instructs caches to validate before using a stored response.
 *
 * Scope and limitations
 * - Only HTTP/HTTPS URLs are supported; callers must handle other protocols themselves.
 * - Per-instance validators are reset whenever setUrl(String) changes the URL.
 * - This class intentionally has no Swing dependencies and can be reused by other modules.
 *
 * Standards and references
 * - HTTP Semantics (RFC 9110): Conditional Requests, ETag, 304 Not Modified
 *   https://datatracker.ietf.org/doc/html/rfc9110
 * - HTTP Caching (RFC 9111): Cache-Control, revalidation semantics
 *   https://datatracker.ietf.org/doc/html/rfc9111
 * - Historic: RFC 7232 (Conditional Requests) — superseded by RFC 9110/9111
 *   https://datatracker.ietf.org/doc/html/rfc7232
 *
 * This documentation is intended to clarify how validators and headers are used here,
 * and why a 304 Not Modified can occur during periodic refreshes.
 */
public class HttpCachedFetcher {

    public static final int STATUS_OK = 1;
    public static final int STATUS_NOT_MODIFIED = 2;
    public static final int STATUS_ERROR = 3;

    private volatile String url;
    private volatile long lastModified = 0L;
    private volatile String etag = null;

    public HttpCachedFetcher() {
    }

    public synchronized void setUrl(String url) {
        this.url = (url == null ? null : EfaUtil.correctUrl(url));
        // reset validators when URL changes
        this.lastModified = 0L;
        this.etag = null;
    }

    public String getUrl() {
        return url;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getEtag() {
        return etag;
    }

    public FetchResult fetch() throws IOException {
        String u = this.url;
        if (u == null || u.trim().isEmpty()) {
            return new FetchResult(STATUS_ERROR, null, null, null, -1, null);
        }
        URL urlObj = new URL(u);
        String protocol = urlObj.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            // Caller should handle non-HTTP(S)
            throw new UnsupportedOperationException("Protocol not supported by HttpCachedFetcher: " + protocol);
        }
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setUseCaches(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        if (lastModified > 0) {
            conn.setIfModifiedSince(lastModified);
        }
        if (etag != null && etag.length() > 0) {
            conn.setRequestProperty("If-None-Match", etag);
        }
        conn.setRequestProperty("Cache-Control", "max-age=0");
        conn.setRequestProperty("Pragma", "no-cache");
        conn.connect();
        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            try { conn.disconnect(); } catch (Exception e) { Logger.logdebug(new Exception(e)); }
            return new FetchResult(STATUS_NOT_MODIFIED, null, null, urlObj, code, null);
        }
        if (code >= 200 && code < 300) {
            long lm = conn.getLastModified();
            String newEtag = conn.getHeaderField("ETag");
            String contentType = conn.getContentType();
            byte[] body;
            try (InputStream is = conn.getInputStream()) {
                body = readAll(is);
            } finally {
                try { conn.disconnect(); } catch (Exception e) { Logger.logdebug(new Exception(e)); }
            }
            // update validators only after successful read
            this.lastModified = lm;
            this.etag = newEtag;
            String charset = parseCharset(contentType);
            return new FetchResult(STATUS_OK, body, charset, urlObj, code, contentType);
        } else {
            try { conn.disconnect(); } catch (Exception e) { Logger.logdebug(new Exception(e)); }
            return new FetchResult(STATUS_ERROR, null, null, urlObj, code, conn.getContentType());
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(4096, in.available()));
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }

    private static String parseCharset(String contentType) {
        if (contentType == null) {
            return null;
        }
        try {
            String[] parts = contentType.split(";");
            for (String p : parts) {
                String s = p.trim();
                if (s.toLowerCase().startsWith("charset=")) {
                    return s.substring(8).trim();
                }
            }
        } catch (Exception e) { Logger.logdebug(new Exception(e)); }
        return null;
    }

    public static final class FetchResult {
        public final int status;
        public final byte[] body; // may be null
        public final String charset; // may be null
        public final URL baseUrl; // never null when status != STATUS_ERROR
        public final int httpStatus;
        public final String contentType; // may be null

        private FetchResult(int status, byte[] body, String charset, URL baseUrl, int httpStatus, String contentType) {
            this.status = status;
            this.body = body;
            this.charset = charset;
            this.baseUrl = baseUrl;
            this.httpStatus = httpStatus;
            this.contentType = contentType;
        }

        public boolean isOk() { return status == STATUS_OK; }
        public boolean isNotModified() { return status == STATUS_NOT_MODIFIED; }
        public boolean isError() { return status == STATUS_ERROR; }
    }
}