package com.mosheliberman.keepintouch;

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
}
