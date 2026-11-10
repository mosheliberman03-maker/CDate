package com.mosheliberman.cdate;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PageGrabPlugin.class); // hidden browser for Chabad.org pages (English shiurim)
        registerPlugin(CalWidgetPlugin.class); // home-screen widget: refresh / info / pin (app version 6)
        super.onCreate(savedInstanceState);
    }
}
