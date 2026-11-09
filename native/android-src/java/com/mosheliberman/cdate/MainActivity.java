package com.mosheliberman.cdate;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PageGrabPlugin.class); // hidden browser for Chabad.org pages (English shiurim)
        super.onCreate(savedInstanceState);
    }
}
