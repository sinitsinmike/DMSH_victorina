package ru.dmsh.victorina;

import java.net.URI;

/** Keep the wrapper on this site's HTTPS origin and project path. */
final class SitePolicy {
    static final String HOME = "https://sinitsinmike.github.io/DMSH_victorina/";
    static boolean isSite(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"sinitsinmike.github.io".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || path == null || !path.startsWith("/DMSH_victorina/")) return false;
            for (String part : path.split("/")) if (part.equals(".") || part.equals("..")) return false;
            return !path.contains("\\");
        } catch (Exception ignored) { return false; }
    }
    static boolean isExternalWebLink(String url) {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (Exception ignored) { return false; }
    }
}
