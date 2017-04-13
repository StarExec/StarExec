package org.starexec.data.to.tuples;

/**
 * Created by agieg on 4/12/2017.
 */
public class HtmlStatusCodePair {
    public final String html;
    public final int statusCode;

    public HtmlStatusCodePair(String html, int sc) {
        this.html = html;
        this.statusCode = sc;
    }
}
