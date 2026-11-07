package com.mosheliberman.keepintouch;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * An invisible screen that widget taps go through: "call" opens the phone's dialer with the number filled in
 * (one more tap to ring, so a stray touch never calls someone at 3 AM), "wa" opens WhatsApp, "open" opens the app.
 */
public class WidgetAction extends Activity {
    static Intent fill(String action, String phone) {
        return new Intent().putExtra("a", action).putExtra("p", phone == null ? "" : phone);
    }

    static PendingIntent open(Context ctx, int req) {
        Intent i = new Intent(ctx, WidgetAction.class).putExtra("a", "open").setData(Uri.parse("kit://open/" + req));
        return PendingIntent.getActivity(ctx, req, i, KitData.piFlags(false));
    }

    static PendingIntent act(Context ctx, int req, String action, String phone) {
        Intent i = new Intent(ctx, WidgetAction.class).putExtra("a", action).putExtra("p", phone == null ? "" : phone)
                .setData(Uri.parse("kit://" + action + "/" + req));
        return PendingIntent.getActivity(ctx, req, i, KitData.piFlags(false));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        String a = in.getStringExtra("a"), p = in.getStringExtra("p");
        if (p == null) p = "";
        p = p.replaceAll("[^0-9]", "");
        Intent go;
        try {
            if ("call".equals(a) && p.length() >= 6) {
                go = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+" + p));
            } else if ("wa".equals(a) && p.length() >= 6) {
                go = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + p));
            } else {
                go = getPackageManager().getLaunchIntentForPackage(getPackageName());
            }
            if (go != null) {
                go.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(go);
            }
        } catch (Exception ignored) {}
        finish();
        overridePendingTransition(0, 0);
    }
}
