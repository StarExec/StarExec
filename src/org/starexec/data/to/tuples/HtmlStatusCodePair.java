package org.starexec.data.to.tuples;

// Simple tuple to hold the HTML from a page along with an HTTP status code.
public class HtmlStatusCodePair {
    public final String html;
    public final int statusCode;

    public HtmlStatusCodePair(String html, int sc) {
        this.html = html;
        this.statusCode = sc;
    }
}
