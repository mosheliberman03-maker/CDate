package com.mosheliberman.keepintouch;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

/**
 * "Call now": a small widget that answers one question at a glance. A big number of friends who are good to
 * call right now, and the three best people to call this minute (evening where they are first, favourites
 * first on a tie), each with a Call button. When nobody is up, it says who wakes up next and when.
 */
public class BestWidget extends AppWidgetProvider {
    static final int[] ROWS = {R.id.kit_b1, R.id.kit_b2, R.id.kit_b3};
    static final int[] NAMES = {R.id.kit_b1_name, R.id.kit_b2_name, R.id.kit_b3_name};
    static final int[] TIMES = {R.id.kit_b1_time, R.id.kit_b2_time, R.id.kit_b3_time};
    static final int[] METAS = {R.id.kit_b1_meta, R.id.kit_b2_meta, R.id.kit_b3_meta};
    static final int[] CALLS = {R.id.kit_b1_call, R.id.kit_b2_call, R.id.kit_b3_call};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        KitData.updateAll(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent i) {
        if (Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction()) || Intent.ACTION_TIME_CHANGED.equals(i.getAction())) {
            KitData.updateAll(ctx);
            return;
        }
        super.onReceive(ctx, i);
    }

    static void draw(Context ctx, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.kit_widget_best);
        List<KitData.Person> all = KitData.load(ctx);
        List<KitData.Person> best = new ArrayList<>();
        for (KitData.Person p : KitData.query(ctx, KitData.F_UP, KitData.S_BEST)) if (p.hasPhone()) best.add(p);
        int up = 0;
        for (KitData.Person p : all) if ("day".equals(p.st)) up++;

        v.setTextViewText(R.id.kit_bcount, String.valueOf(up));
        v.setTextColor(R.id.kit_bcount, up > 0 ? KitData.C_DAY : KitData.C_NIGHT);
        v.setTextViewText(R.id.kit_btitle, up == 1 ? "friend is up now" : "friends are up now");
        v.setTextViewText(R.id.kit_bsub, all.isEmpty() ? "Tap to open the app" : best.isEmpty() ? "" : "Best to call right now:");
        v.setOnClickPendingIntent(R.id.kit_bhead, WidgetAction.open(ctx, id * 16 + 10));

        for (int k = 0; k < ROWS.length; k++) {
            if (k >= best.size()) { v.setViewVisibility(ROWS[k], View.GONE); continue; }
            KitData.Person p = best.get(k);
            v.setViewVisibility(ROWS[k], View.VISIBLE);
            v.setTextViewText(NAMES[k], (p.fav ? "★ " : "") + p.name);
            v.setString(TIMES[k], "setTimeZone", p.tz);
            v.setTextViewText(METAS[k], " · " + p.city);
            v.setContentDescription(CALLS[k], "Call " + p.name);
            v.setOnClickPendingIntent(ROWS[k], WidgetAction.act(ctx, id * 16 + 1 + k, "call", p.phone));
            v.setOnClickPendingIntent(CALLS[k], WidgetAction.act(ctx, id * 16 + 4 + k, "call", p.phone));
        }

        String empty = null;
        if (all.isEmpty()) empty = "Save friends in Keep in Touch and they show up here.";
        else if (best.isEmpty()) {
            KitData.Person n = KitData.nextUp(all);
            if (up > 0) empty = "Friends are up, but none has a number saved yet.";
            else if (n != null) empty = "Nobody is up. 🌙 Next: " + KitData.firstName(n.name) + " (" + n.city + ") in " + KitData.inWords(KitData.minsUntilUp(n));
        }
        v.setViewVisibility(R.id.kit_bempty, empty == null ? View.GONE : View.VISIBLE);
        if (empty != null) {
            v.setTextViewText(R.id.kit_bempty, empty);
            v.setOnClickPendingIntent(R.id.kit_bempty, WidgetAction.open(ctx, id * 16 + 11));
        }
        m.updateAppWidget(id, v);
    }
}
