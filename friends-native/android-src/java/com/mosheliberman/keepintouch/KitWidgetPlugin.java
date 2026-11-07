package com.mosheliberman.keepintouch;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** The web app calls Capacitor.Plugins.KitWidget.save({data}) whenever the saved people, numbers or favourites change. */
@CapacitorPlugin(name = "KitWidget")
public class KitWidgetPlugin extends Plugin {
    @PluginMethod
    public void save(PluginCall call) {
        String data = call.getString("data");
        if (data == null) { call.reject("no data"); return; }
        getContext().getSharedPreferences(KitData.PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putString(KitData.KEY, data).apply();
        KitData.updateAll(getContext());
        JSObject r = new JSObject();
        r.put("ok", true);
        call.resolve(r);
    }

    /** How many of each widget are on the home screen, and whether this phone lets the app add one itself. */
    @PluginMethod
    public void info(PluginCall call) {
        AppWidgetManager m = AppWidgetManager.getInstance(getContext());
        JSObject r = new JSObject();
        r.put("who", m.getAppWidgetIds(new ComponentName(getContext(), WhoWidget.class)).length);
        r.put("best", m.getAppWidgetIds(new ComponentName(getContext(), BestWidget.class)).length);
        r.put("canPin", Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported());
        call.resolve(r);
    }

    /** Shows Android's own "Add to home screen" popup for one of the widgets ({kind: "who" | "best"}). */
    @PluginMethod
    public void pin(PluginCall call) {
        AppWidgetManager m = AppWidgetManager.getInstance(getContext());
        JSObject r = new JSObject();
        if (Build.VERSION.SDK_INT < 26 || !m.isRequestPinAppWidgetSupported()) {
            r.put("ok", false);
            call.resolve(r);
            return;
        }
        Class<?> c = "best".equals(call.getString("kind")) ? BestWidget.class : WhoWidget.class;
        r.put("ok", m.requestPinAppWidget(new ComponentName(getContext(), c), null, null));
        call.resolve(r);
    }
}
