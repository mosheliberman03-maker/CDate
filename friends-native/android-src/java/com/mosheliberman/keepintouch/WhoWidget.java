package com.mosheliberman.keepintouch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/**
 * "Who's up now": a resizable list of saved friends with a coloured dot for awake / borderline / asleep,
 * their live local time, and Call + WhatsApp buttons. Filter chips (Awake, Favourites, Mine, All) and a
 * sort button (Best time, A-Z, Place) on the widget itself; each widget remembers its own choice.
 */
public class WhoWidget extends AppWidgetProvider {
    static final String ACTION_FILTER = "com.mosheliberman.keepintouch.FILTER";
    static final String ACTION_SORT = "com.mosheliberman.keepintouch.SORT";

    static final String[] FILTERS = {KitData.F_UP, KitData.F_FAV, KitData.F_MINE, KitData.F_ALL};
    static final int[] CHIP_IDS = {R.id.kit_f_up, R.id.kit_f_fav, R.id.kit_f_mine, R.id.kit_f_all};
    static final String[] SORTS = {KitData.S_BEST, KitData.S_NAME, KitData.S_PLACE};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        KitData.updateAll(ctx);
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        android.content.SharedPreferences.Editor e = KitData.state(ctx).edit();
        for (int id : ids) e.remove("f_" + id).remove("s_" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context ctx, Intent i) {
        String a = i.getAction();
        int id = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (ACTION_FILTER.equals(a) && id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            KitData.state(ctx).edit().putString("f_" + id, i.getStringExtra("v")).apply();
            refreshOne(ctx, id);
            return;
        }
        if (ACTION_SORT.equals(a) && id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            String cur = KitData.sortOf(ctx, id);
            int k = 0;
            for (int j = 0; j < SORTS.length; j++) if (SORTS[j].equals(cur)) k = j;
            KitData.state(ctx).edit().putString("s_" + id, SORTS[(k + 1) % SORTS.length]).apply();
            refreshOne(ctx, id);
            return;
        }
        if (KitData.ACTION_TICK.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            KitData.updateAll(ctx);
            return;
        }
        super.onReceive(ctx, i);
    }

    static void refreshOne(Context ctx, int id) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        draw(ctx, m, id);
        m.notifyAppWidgetViewDataChanged(new int[]{id}, R.id.kit_list);
    }

    static String sortLabel(String s) {
        return KitData.S_NAME.equals(s) ? "A–Z" : KitData.S_PLACE.equals(s) ? "Place" : "Best time";
    }

    static void draw(Context ctx, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.kit_widget_who);
        String filter = KitData.filterOf(ctx, id), sort = KitData.sortOf(ctx, id);
        List<KitData.Person> all = KitData.load(ctx);

        // Header: how many are good to call right now (the whole point), then the others.
        int day = 0, gray = 0, night = 0;
        for (KitData.Person p : all) { if ("day".equals(p.st)) day++; else if ("gray".equals(p.st)) gray++; else night++; }
        SpannableStringBuilder sub = new SpannableStringBuilder();
        if (all.isEmpty()) sub.append("Open the app to add friends");
        else {
            int s0 = sub.length();
            sub.append(String.valueOf(day)).append(" good to call");
            sub.setSpan(new ForegroundColorSpan(KitData.C_DAY), s0, sub.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sub.setSpan(new StyleSpan(Typeface.BOLD), s0, s0 + String.valueOf(day).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (gray > 0) sub.append(" · ").append(String.valueOf(gray)).append(" borderline");
            if (night > 0) sub.append(" · ").append(String.valueOf(night)).append(" asleep");
        }
        v.setTextViewText(R.id.kit_sub, sub);

        // Filter chips: the chosen one is filled.
        for (int k = 0; k < FILTERS.length; k++) {
            boolean on = FILTERS[k].equals(filter);
            v.setInt(CHIP_IDS[k], "setBackgroundResource", on ? R.drawable.kit_chip_on : R.drawable.kit_chip_off);
            v.setTextColor(CHIP_IDS[k], on ? 0xFF0B1022 : 0xFFC9D0EE);
            Intent fi = new Intent(ctx, WhoWidget.class).setAction(ACTION_FILTER)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("v", FILTERS[k])
                    .setData(Uri.parse("kit://filter/" + id + "/" + FILTERS[k]));
            v.setOnClickPendingIntent(CHIP_IDS[k], PendingIntent.getBroadcast(ctx, id * 16 + k, fi, KitData.piFlags(false)));
        }
        v.setTextViewText(R.id.kit_sort, "⇅ " + sortLabel(sort));
        Intent si = new Intent(ctx, WhoWidget.class).setAction(ACTION_SORT)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).setData(Uri.parse("kit://sort/" + id));
        v.setOnClickPendingIntent(R.id.kit_sort, PendingIntent.getBroadcast(ctx, id * 16 + 9, si, KitData.piFlags(false)));

        // Title opens the app.
        v.setOnClickPendingIntent(R.id.kit_head, WidgetAction.open(ctx, id * 16 + 10));

        // The list itself.
        Intent svc = new Intent(ctx, WhoWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.kit_list, svc);
        v.setEmptyView(R.id.kit_list, R.id.kit_empty);
        v.setTextViewText(R.id.kit_empty, emptyText(ctx, all, filter));
        v.setOnClickPendingIntent(R.id.kit_empty, WidgetAction.open(ctx, id * 16 + 11));
        Intent tpl = new Intent(ctx, WidgetAction.class);
        v.setPendingIntentTemplate(R.id.kit_list, PendingIntent.getActivity(ctx, id * 16 + 12, tpl, KitData.piFlags(true)));

        m.updateAppWidget(id, v);
    }

    static CharSequence emptyText(Context ctx, List<KitData.Person> all, String filter) {
        if (!KitData.hasData(ctx) || all.isEmpty())
            return "No friends saved yet.\nTap here to open Keep in Touch and save the people you want to call.";
        if (KitData.F_FAV.equals(filter)) return "No favourites yet.\nTap ☆ next to a name in the app.";
        if (KitData.F_MINE.equals(filter)) return "Nobody from your own yeshiva is saved yet.";
        KitData.Person n = KitData.nextUp(all);
        if (n == null) return "Nobody here right now.";
        return "Nobody is up right now. 🌙\nNext: " + n.name + " (" + n.city + ") in " + KitData.inWords(KitData.minsUntilUp(n));
    }

    /** One row of the list (used by WhoWidgetService). */
    static RemoteViews row(Context ctx, KitData.Person p) {
        RemoteViews r = new RemoteViews(ctx.getPackageName(), R.layout.kit_widget_row);
        int col = p.color();
        r.setTextColor(R.id.kit_dot, col);
        r.setTextViewText(R.id.kit_name, (p.fav ? "★ " : "") + p.name);
        r.setString(R.id.kit_time, "setTimeZone", p.tz);
        r.setTextColor(R.id.kit_time, col);

        SpannableStringBuilder meta = new SpannableStringBuilder();
        if (!"day".equals(p.st)) {
            meta.append(" · ").append(KitData.label(p.st));
            meta.setSpan(new ForegroundColorSpan(col), 0, meta.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        meta.append(" · ").append(p.city);
        if (!p.hasPhone()) meta.append(" · no number yet");
        r.setTextViewText(R.id.kit_meta, meta);

        boolean ph = p.hasPhone();
        r.setViewVisibility(R.id.kit_call, ph ? View.VISIBLE : View.GONE);
        r.setViewVisibility(R.id.kit_wa, ph ? View.VISIBLE : View.GONE);
        // Awake = a bright green Call button; borderline/asleep = a quiet outlined one (you can still call).
        boolean up = "day".equals(p.st);
        r.setInt(R.id.kit_call, "setBackgroundResource", up ? R.drawable.kit_call_on : R.drawable.kit_call_off);
        r.setInt(R.id.kit_call, "setColorFilter", up ? 0xFF0B1022 : 0xFFC9D0EE);
        r.setContentDescription(R.id.kit_call, "Call " + p.name);
        r.setContentDescription(R.id.kit_wa, "WhatsApp " + p.name);

        r.setOnClickFillInIntent(R.id.kit_row, WidgetAction.fill(ph ? "call" : "open", p.phone));
        r.setOnClickFillInIntent(R.id.kit_call, WidgetAction.fill("call", p.phone));
        r.setOnClickFillInIntent(R.id.kit_wa, WidgetAction.fill("wa", p.phone));
        return r;
    }
}
