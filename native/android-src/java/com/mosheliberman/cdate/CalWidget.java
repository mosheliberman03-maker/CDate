package com.mosheliberman.cdate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The home-screen widget (owner, app version 6): today's Hebrew and English date, the special days, the user's own plans
 * with their times, and how many of today's shiurim are done. The web app writes eight days ahead into the Preferences
 * store ("CapacitorStorage", key "cc_widget"), so the widget stays right with the app closed: it switches days at
 * midnight, and to the next Hebrew date (and "Tonight: …") at sunset, waking itself with an alarm.
 */
public class CalWidget extends AppWidgetProvider {
    static final String TICK = "com.mosheliberman.cdate.CAL_WIDGET_TICK";
    static final int[] L = {R.id.cw_l1, R.id.cw_l2, R.id.cw_l3, R.id.cw_l4};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) draw(c, m, id);
        schedule(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, android.os.Bundle o) {
        draw(c, m, id);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (TICK.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            updateAll(c);
            return;
        }
        super.onReceive(c, i);
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, CalWidget.class));
        for (int id : ids) draw(c, m, id);
        if (ids.length > 0) schedule(c);
    }

    /** Wake up at the next midnight or sunset (whichever is first), and at least every 3 hours. */
    static void schedule(Context c) {
        long now = System.currentTimeMillis(), next = now + 3 * 3600_000L;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 5);
        next = Math.min(next, cal.getTimeInMillis());
        JSONObject day = today(c);
        if (day != null) {
            long ss = day.optLong("ss", 0);
            if (ss > now) next = Math.min(next, ss + 30_000L);
        }
        long z = nextZmanAt(c, now);
        if (z > now) next = Math.min(next, z + 30_000L);
        Intent i = new Intent(c, CalWidget.class).setAction(TICK);
        PendingIntent pi = PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC, next, pi);
        } catch (Exception e) {
            am.set(AlarmManager.RTC, next, pi);
        }
    }

    static JSONObject data(Context c) {
        try {
            String s = c.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE).getString("cc_widget", null);
            return s == null ? null : new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    static JSONObject today(Context c) {
        JSONObject d = data(c);
        if (d == null) return null;
        JSONArray days = d.optJSONArray("days");
        if (days == null) return null;
        String k = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        for (int i = 0; i < days.length(); i++) {
            JSONObject o = days.optJSONObject(i);
            if (o != null && k.equals(o.optString("d"))) return o;
        }
        return null;
    }

    static void addAll(List<String> out, JSONArray a, String prefix) {
        if (a == null) return;
        for (int i = 0; i < a.length(); i++) {
            String s = a.optString(i, "");
            if (s.length() > 0) out.add(prefix + s);
        }
    }

    /** The next zman still ahead (sunrise, Shema, chatzos, sunset, nightfall, candle lighting…), e.g. "Sunset 4:58 PM". */
    static long nextZmanAt(Context c, long now) {
        JSONObject d = data(c);
        JSONArray days = d == null ? null : d.optJSONArray("days");
        if (days == null) return 0;
        for (int i = 0; i < days.length(); i++) {
            JSONObject o = days.optJSONObject(i);
            JSONArray z = o == null ? null : o.optJSONArray("z");
            if (z == null) continue;
            for (int k = 0; k < z.length(); k++) {
                JSONArray e = z.optJSONArray(k);
                if (e != null && e.optLong(1, 0) > now) return e.optLong(1, 0);
            }
        }
        return 0;
    }

    static String nextZman(Context c, long now) {
        JSONObject d = data(c);
        JSONArray days = d == null ? null : d.optJSONArray("days");
        if (days == null) return "";
        for (int i = 0; i < days.length(); i++) {
            JSONObject o = days.optJSONObject(i);
            JSONArray z = o == null ? null : o.optJSONArray("z");
            if (z == null) continue;
            for (int k = 0; k < z.length(); k++) {
                JSONArray e = z.optJSONArray(k);
                if (e != null && e.optLong(1, 0) > now) return e.optString(0) + " " + e.optString(2);
            }
        }
        return "";
    }

    static String join(List<String> a) {
        StringBuilder b = new StringBuilder();
        for (String s : a) { if (b.length() > 0) b.append(" · "); b.append(s); }
        return b.toString();
    }

    static void draw(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.cc_widget);
        JSONObject day = today(c);
        long now = System.currentTimeMillis();
        List<String> extra = new ArrayList<>();
        if (day == null) {
            v.setTextViewText(R.id.cw_he, "Chabad Calendar");
            v.setTextViewText(R.id.cw_en, "");
            v.setTextViewText(R.id.cw_main, "Open the app once");
            v.setTextViewText(R.id.cw_sub, "to fill this in");
        } else {
            boolean night = day.optLong("ss", Long.MAX_VALUE) <= now;
            v.setTextViewText(R.id.cw_he, night && day.has("heN") ? day.optString("heN") : day.optString("he"));
            v.setTextViewText(R.id.cw_en, day.optString("enS", day.optString("en")));
            List<String> sp = new ArrayList<>(), tk = new ArrayList<>();
            addAll(sp, day.optJSONArray(night ? "spN" : "sp"), "");
            addAll(tk, day.optJSONArray("tk"), "");
            String main;
            if (!sp.isEmpty()) { main = (night ? "Tonight: " : "") + join(sp); extra.addAll(tk); }
            else if (!tk.isEmpty()) { main = tk.get(0); extra.addAll(tk.subList(1, tk.size())); }
            else main = "No special day";
            v.setTextViewText(R.id.cw_main, main);
            String sub = nextZman(c, now);
            int sh = day.optInt("sh", -1);
            if (sh >= 0) {
                StringBuilder dots = new StringBuilder();
                if (sh >= 4) dots.append("Shiurim ✓");
                else for (int k = 0; k < 4; k++) dots.append(k < sh ? "●" : "○");
                sub = sub.length() > 0 ? sub + "   " + dots : dots.toString();
            }
            v.setTextViewText(R.id.cw_sub, sub);
        }
        int h = 0;
        try { h = m.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0); } catch (Exception e) { }
        int room = h >= 150 ? 4 : h >= 120 ? 3 : h >= 95 ? 2 : 0;
        v.setViewVisibility(R.id.cw_more, room > 0 && !extra.isEmpty() ? View.VISIBLE : View.GONE);
        for (int k = 0; k < L.length; k++) {
            if (k < room && k < extra.size()) {
                v.setViewVisibility(L[k], View.VISIBLE);
                v.setTextViewText(L[k], extra.get(k));
            } else v.setViewVisibility(L[k], View.GONE);
        }
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.cw_root, pi);
        }
        m.updateAppWidget(id, v);
    }
}
