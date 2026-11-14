package ru.dmsh.victorina;
public final class SitePolicyTest {
    public static void main(String[] args) {
        for (String url : new String[]{SitePolicy.HOME, SitePolicy.HOME + "study.html?quiz=test", SitePolicy.HOME + "teacher.html", SitePolicy.HOME + "MP3/Example%20name.mp3"}) {
            if (!SitePolicy.isSite(url)) throw new AssertionError("Rejected site URL: " + url);
        }
        for (String url : new String[]{null, "", "http://sinitsinmike.github.io/DMSH_victorina/", "https://sinitsinmike.github.io.evil.example/DMSH_victorina/", "https://evil.example/DMSH_victorina/", "https://sinitsinmike.github.io/another-project/", "https://evil@sinitsinmike.github.io/DMSH_victorina/", SitePolicy.HOME + "../another-project/", SitePolicy.HOME + "%2e%2e/another-project/", "https://sinitsinmike.github.io:444/DMSH_victorina/", "javascript:alert(1)", "file:///etc/passwd", "intent://example"}) {
            if (SitePolicy.isSite(url)) throw new AssertionError("Accepted unsafe URL: " + url);
        }
        if (!SitePolicy.isExternalWebLink("https://github.com/sinitsinmike/DMSH_victorina")) throw new AssertionError();
        if (SitePolicy.isExternalWebLink("intent://example")) throw new AssertionError();
        System.out.println("Site URL policy: all checks passed");
    }
}
