package com.mosheliberman.cdate;

import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Opens a Chabad.org page in an invisible WebView (a real browser, so Chabad.org treats it as a normal visitor instead of
 * refusing it with HTTP 403 like plain downloads), waits until the page's text has loaded, and returns its HTML.
 * The web app calls Capacitor.Plugins.PageGrab.get({url}). Only chabad.org pages are allowed.
 */
@CapacitorPlugin(name = "PageGrab")
public class PageGrabPlugin extends Plugin {
    private static final String JS = "(function(){var b=document.body?(document.body.innerText||''):'';"
            + "var n=(b.match(/[A-Za-z]/g)||[]).length;"
            + "return JSON.stringify({t:document.title||'',n:n,h:document.documentElement?document.documentElement.outerHTML:''})})()";

    @PluginMethod
    public void get(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || !url.startsWith("https://www.chabad.org/")) {
            call.reject("only chabad.org pages");
            return;
        }
        final int timeout = call.getInt("timeout", 25000);
        final int need = call.getInt("need", 1500);
        getActivity().runOnUiThread(() -> {
            final Handler h = new Handler(Looper.getMainLooper());
            final WebView w = new WebView(getContext());
            final boolean[] done = {false};
            final long t0 = System.currentTimeMillis();
            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setBlockNetworkImage(true);
            String ua = s.getUserAgentString();
            if (ua != null) s.setUserAgentString(ua.replace("; wv)", ")"));
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
            w.setWebViewClient(new WebViewClient());
            final Runnable[] poll = new Runnable[1];
            poll[0] = () -> {
                if (done[0]) return;
                w.evaluateJavascript(JS, value -> {
                    if (done[0]) return;
                    try {
                        Object v = new JSONTokener(value).nextValue();
                        if (!(v instanceof String)) throw new Exception("not ready");
                        JSONObject o = new JSONObject((String) v);
                        int n = o.optInt("n");
                        boolean late = System.currentTimeMillis() - t0 > timeout;
                        if (n >= need || late) {
                            done[0] = true;
                            JSObject r = new JSObject();
                            r.put("html", o.optString("h"));
                            r.put("title", o.optString("t"));
                            r.put("letters", n);
                            r.put("complete", n >= need);
                            call.resolve(r);
                            w.destroy();
                        } else {
                            h.postDelayed(poll[0], 1200);
                        }
                    } catch (Exception e) {
                        h.postDelayed(poll[0], 1200);
                    }
                });
            };
            w.loadUrl(url);
            h.postDelayed(poll[0], 2500);
            h.postDelayed(() -> {
                if (!done[0]) {
                    done[0] = true;
                    call.reject("timeout");
                    w.destroy();
                }
            }, timeout + 8000);
        });
    }
}
