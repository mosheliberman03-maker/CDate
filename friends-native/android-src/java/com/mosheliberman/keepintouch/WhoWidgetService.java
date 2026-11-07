package com.mosheliberman.keepintouch;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

/** Feeds the rows of the "Who's up now" list. */
public class WhoWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext(), intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0));
    }

    static final class Factory implements RemoteViewsFactory {
        private final Context ctx;
        private final int id;
        private List<KitData.Person> rows = new ArrayList<>();

        Factory(Context ctx, int id) { this.ctx = ctx; this.id = id; }

        @Override public void onCreate() {}
        @Override public void onDestroy() { rows.clear(); }

        @Override
        public void onDataSetChanged() {
            rows = KitData.query(ctx, KitData.filterOf(ctx, id), KitData.sortOf(ctx, id));
        }

        @Override public int getCount() { return rows.size(); }

        @Override
        public RemoteViews getViewAt(int i) {
            if (i < 0 || i >= rows.size()) return null;
            return WhoWidget.row(ctx, rows.get(i));
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int i) { return i; }
        @Override public boolean hasStableIds() { return false; }
    }
}
