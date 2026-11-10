package com.mosheliberman.cdate;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** The web app calls Capacitor.Plugins.CalWidget: refresh() after writing "cc_widget", info() and pin() for "Add the widget". */
@CapacitorPlugin(name = "CalWidget")
public class CalWidgetPlugin extends Plugin {
    @PluginMethod
    public void refresh(PluginCall call) {
        CalWidget.updateAll(getContext());
        JSObject r = new JSObject();
        r.put("ok", true);
        call.resolve(r);
    }

    @PluginMethod
    public void info(PluginCall call) {
        AppWidgetManager m = AppWidgetManager.getInstance(getContext());
        JSObject r = new JSObject();
        r.put("count", m.getAppWidgetIds(new ComponentName(getContext(), CalWidget.class)).length);
        r.put("canPin", Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported());
        call.resolve(r);
    }

    @PluginMethod
    public void pin(PluginCall call) {
        AppWidgetManager m = AppWidgetManager.getInstance(getContext());
        JSObject r = new JSObject();
        if (Build.VERSION.SDK_INT < 26 || !m.isRequestPinAppWidgetSupported()) {
            r.put("ok", false);
            call.resolve(r);
            return;
        }
        r.put("ok", m.requestPinAppWidget(new ComponentName(getContext(), CalWidget.class), null, null));
        call.resolve(r);
    }
}
