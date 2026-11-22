package com.mosheliberman.keepintouch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

/**
 * Owner: "select a widget to only display a certain community". Tapping ▾ on a widget opens this small list
 * (Everyone + my communities); the choice is kept for that one widget, so two widgets can show two communities.
 */
public class PickCommunity extends Activity {
    static PendingIntent open(Context ctx, int widgetId, int req) {
        Intent i = new Intent(ctx, PickCommunity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData(Uri.parse("kit://pick/" + widgetId));
        return PendingIntent.getActivity(ctx, req, i, KitData.piFlags(false));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        final List<String[]> cs = KitData.comms(this);
        final String cur = KitData.commOf(this, id);
        String[] names = new String[cs.size() + 1];
        names[0] = "Everyone";
        int sel = 0;
        for (int k = 0; k < cs.size(); k++) { names[k + 1] = cs.get(k)[1]; if (cs.get(k)[0].equals(cur)) sel = k + 1; }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Show on this widget")
                .setSingleChoiceItems(names, sel, (d, w) -> {
                    KitData.state(this).edit().putString("c_" + id, w == 0 ? "" : cs.get(w - 1)[0]).apply();
                    redraw(this, id);
                    d.dismiss();
                })
                .setOnDismissListener(d -> finish())
                .show();
    }

    static void redraw(Context ctx, int id) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        for (int w : m.getAppWidgetIds(new ComponentName(ctx, WhoWidget.class))) if (w == id) { WhoWidget.refreshOne(ctx, id); return; }
        for (int w : m.getAppWidgetIds(new ComponentName(ctx, BestWidget.class))) if (w == id) { BestWidget.draw(ctx, m, id); return; }
    }
}
