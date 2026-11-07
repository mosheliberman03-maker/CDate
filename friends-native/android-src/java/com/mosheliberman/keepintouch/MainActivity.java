package com.mosheliberman.keepintouch;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(KitWidgetPlugin.class); // the bridge to the home-screen widgets
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        KitData.updateAll(this); // leaving the app: make sure the widgets show the latest
    }
}
