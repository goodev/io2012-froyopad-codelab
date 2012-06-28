/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.honeypad.appwidget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.android.honeypad.R;
import com.example.android.honeypad.ui.HomeActivity;
import com.example.android.honeypad.ui.NoteEditFragment;
import com.example.android.honeypad.utils.UiUtils;

@TargetApi(11)
public class WidgetProvider extends AppWidgetProvider {

    public static void updateWidget(Context ctx) {
		if (UiUtils.atLeastHoneycomb()) {
			AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
			awm.notifyAppWidgetViewDataChanged(awm
					.getAppWidgetIds(new ComponentName(ctx,
							WidgetProvider.class)), R.id.stack_view);
		}
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {

            /*
             * Here we setup the intent which points to the StackViewService
             * which will provide the views for this collection.
             */
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetIds[i]);

            /*
             * When intents are compared, the extras are ignored, so we need to
             * embed the extras into the data so that the extras will not be
             * ignored.
             */
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews rv = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            rv.setRemoteAdapter(appWidgetIds[i], R.id.stack_view, intent);

            /*
             * The empty view is displayed when the collection has no items. It
             * should be a sibling of the collection view.
             */
            rv.setEmptyView(R.id.stack_view, R.id.empty_view);

            /*
             * Here we setup the a pending intent template. Individuals items of
             * a collection cannot setup their own pending intents, instead, the
             * collection as a whole can setup a pending intent template, and
             * the individual items can set a fillInIntent to create unique
             * before on an item to item basis.
             */
            Intent viewNoteIntent = new Intent(context, HomeActivity.class);
            viewNoteIntent.setAction(NoteEditFragment.ACTION_VIEW_NOTE);
            viewNoteIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetIds[i]);
            viewNoteIntent.setData(Uri.parse("widgetid" + appWidgetIds[i]));
            PendingIntent viewNotePendingIntent = PendingIntent.getActivity(
                    context, 0, viewNoteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.stack_view, viewNotePendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

}
