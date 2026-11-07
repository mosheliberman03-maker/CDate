package com.mosheliberman.keepintouch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Everything the widgets know. The web app hands over the saved people (name, place, time zone, number,
 * favourite, "my yeshiva") through KitWidgetPlugin; the widgets work out who is awake right now by themselves,
 * with the SAME rules as the web app (st0 / callRank in friends/index.html), so they stay right with the app closed.
 */
public final class KitData {
    private KitData() {}

    static final String PREFS = "kit_widget";
    static final String KEY = "data";

    // Status colours, same as the app (COL in friends/index.html).
    static final int C_DAY = 0xFF6FD6A0, C_GRAY = 0xFFE0CC7C, C_NIGHT = 0xFFA7ADBD;

    // Filters and sort orders the widget's buttons switch between.
    static final String F_UP = "up", F_FAV = "fav", F_MINE = "mine", F_ALL = "all";
    static final String S_BEST = "best", S_NAME = "name", S_PLACE = "place";

    static final class Person {
        String name, city, tz, phone;
        boolean fav, mine;
        int min;          // minutes since local midnight where they are
        String st;        // "day" (good to call), "gray" (borderline), "night" (asleep)
        double rank;      // lower = better time to call (only for "day")
        int ord() { return "day".equals(st) ? 0 : "gray".equals(st) ? 1 : 2; }
        int color() { return colorOf(st); }
        boolean hasPhone() { return phone != null && phone.length() >= 6; }
    }

    static int colorOf(String st) { return "day".equals(st) ? C_DAY : "gray".equals(st) ? C_GRAY : C_NIGHT; }

    static String label(String st) {
        return "day".equals(st) ? "Good to call" : "gray".equals(st) ? "Borderline" : "Asleep";
    }

    static int localMin(String tz) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz));
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    // Same as st0() in the web app: asleep before 7:00, borderline 7:00-8:30 and from 23:00, otherwise good.
    static String status(int t) {
        if (t < 420) return "night";
        if (t < 510 || t >= 1380) return "gray";
        return "day";
    }

    // Same as callRank() in the web app: evenings first (21:30-22:45 best), then afternoon, midday, late morning.
    static double rank(int m, String st) {
        if (!"day".equals(st)) return 9;
        int b = m >= 1290 && m < 1365 ? 0 : m >= 900 && m < 1290 ? 1 : m >= 720 && m < 900 ? 2 : m >= 600 && m < 720 ? 3 : 4;
        return b + Math.abs(m - 1320) / 3000.0;
    }

    static List<Person> load(Context ctx) {
        List<Person> out = new ArrayList<>();
        String raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (raw == null) return out;
        try {
            JSONArray a = new JSONObject(raw).optJSONArray("ppl");
            if (a == null) return out;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Person p = new Person();
                p.name = o.optString("n", "");
                p.city = o.optString("c", "");
                p.tz = o.optString("tz", "UTC");
                p.phone = o.optString("p", "");
                p.fav = o.optInt("f", 0) == 1;
                p.mine = o.optInt("m", 0) == 1;
                if (p.name.isEmpty()) continue;
                p.min = localMin(p.tz);
                p.st = status(p.min);
                p.rank = rank(p.min, p.st);
                out.add(p);
            }
        } catch (Exception ignored) {}
        return out;
    }

    static boolean hasData(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) != null;
    }

    static List<Person> query(Context ctx, String filter, String sort) {
        List<Person> all = load(ctx), out = new ArrayList<>();
        for (Person p : all) {
            if (F_UP.equals(filter) && !"day".equals(p.st)) continue;
            if (F_FAV.equals(filter) && !p.fav) continue;
            if (F_MINE.equals(filter) && !p.mine) continue;
            out.add(p);
        }
        Collections.sort(out, (a, b) -> {
            int r;
            if (S_NAME.equals(sort)) {
                r = a.name.compareToIgnoreCase(b.name);
            } else if (S_PLACE.equals(sort)) {
                r = a.city.compareToIgnoreCase(b.city);
                if (r == 0) r = a.ord() - b.ord();
                if (r == 0) r = a.name.compareToIgnoreCase(b.name);
            } else {
                r = a.ord() - b.ord();
                if (r == 0) r = Double.compare(a.rank, b.rank);
                if (r == 0) r = (b.fav ? 1 : 0) - (a.fav ? 1 : 0);
                if (r == 0) r = a.name.compareToIgnoreCase(b.name);
            }
            return r;
        });
        return out;
    }

    /** Minutes until this person is good to call again (8:30 their time), or 0 if they already are. */
    static int minsUntilUp(Person p) {
        if ("day".equals(p.st)) return 0;
        int d = 510 - p.min;
        if (d < 0) d += 1440;
        return d;
    }

    /** The asleep/borderline person who will be good to call soonest, or null. */
    static Person nextUp(List<Person> all) {
        Person best = null;
        for (Person p : all) {
            if ("day".equals(p.st)) continue;
            if (best == null || minsUntilUp(p) < minsUntilUp(best)) best = p;
        }
        return best;
    }

    static String inWords(int mins) {
        if (mins < 60) return mins + " min";
        int h = mins / 60, m = mins % 60;
        if (m < 8) return h + " h";
        return h + " h " + m + " min";
    }

    static String clock(int min) {
        int h = min / 60, m = min % 60;
        return String.format(Locale.US, "%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM");
    }

    static String firstName(String n) {
        int i = n.indexOf(' ');
        return i > 0 ? n.substring(0, i) : n;
    }

    // ---- per-widget settings (filter + sort) ----
    static SharedPreferences state(Context ctx) { return ctx.getSharedPreferences("kit_widget_state", Context.MODE_PRIVATE); }
    static String filterOf(Context ctx, int id) { return state(ctx).getString("f_" + id, F_UP); }
    static String sortOf(Context ctx, int id) { return state(ctx).getString("s_" + id, S_BEST); }

    static int piFlags(boolean mutable) {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) f |= mutable ? PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_IMMUTABLE;
        else if (!mutable && Build.VERSION.SDK_INT >= 23) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    /** Redraw every widget of both kinds, then book the next redraw for the next time someone wakes up or goes to sleep. */
    static void updateAll(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        int[] who = m.getAppWidgetIds(new ComponentName(ctx, WhoWidget.class));
        for (int id : who) WhoWidget.draw(ctx, m, id);
        if (who.length > 0) m.notifyAppWidgetViewDataChanged(who, R.id.kit_list);
        int[] best = m.getAppWidgetIds(new ComponentName(ctx, BestWidget.class));
        for (int id : best) BestWidget.draw(ctx, m, id);
        schedule(ctx, who.length + best.length > 0);
    }

    static final String ACTION_TICK = "com.mosheliberman.keepintouch.TICK";

    /** Status only changes at 7:00, 8:30 and 23:00 local time for each person: wake up at the next such moment. */
    static void schedule(Context ctx, boolean any) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, WhoWidget.class).setAction(ACTION_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 7, i, piFlags(false));
        am.cancel(pi);
        if (!any) return;
        int soonest = 60; // at least once an hour (the counts in the header, and as a safety net)
        for (Person p : load(ctx)) {
            for (int b : new int[]{420, 510, 1380}) {
                int d = b - p.min;
                if (d <= 0) d += 1440;
                if (d < soonest) soonest = d;
            }
        }
        long now = System.currentTimeMillis();
        long at = now - (now % 60000) + soonest * 60000L + 5000; // just after the minute turns
        am.set(AlarmManager.RTC, at, pi); // not a wake-up alarm: if the phone sleeps, it runs when the screen comes on
    }
}
