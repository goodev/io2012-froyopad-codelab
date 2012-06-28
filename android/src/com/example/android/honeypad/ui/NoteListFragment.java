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

package com.example.android.honeypad.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.android.honeypad.R;
import com.example.android.honeypad.appwidget.WidgetProvider;
import com.example.android.honeypad.provider.NotesProvider;
import com.example.android.honeypad.utils.UiUtils;

public class NoteListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemLongClickListener, OnItemClickListener {

    // containing Activity must implement this interface
    public interface NoteEventsCallback {

        public void onNoteCreated(Uri noteUri);

        public void onNoteSelected(Uri noteUri);

        public void onNoteDeleted();
    }

    // keys for saving state
    private static final String KEY_CURRENT_ACTIVATED = "KEY_CURRENT_ACTIVATED";

    private static final String KEY_CURRENT_CHECKED = "KEY_CURRENT_CHECKED";

    // the id of our loader
    private static final int LOADER_ID = 0;

    // This is the Adapter being used to display the list's data.
    private SimpleCursorAdapter mAdapter;

    // callback for notifying container of events
    private NoteEventsCallback mContainerCallback;

    private List<Long> mCheckedItems = new ArrayList<Long>();

    private ActionMode mMode;

    // track the currently activated item
    private int mCurrentActivePosition = ListView.INVALID_POSITION;

    // track if we need to set a note to activated once data is loaded
    private long mNoteIdToActivate = -1;

    private static boolean mTwoPaneView;

    // default constructor
    public NoteListFragment() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTwoPaneView = UiUtils.isHoneycombTablet(getActivity());
        setEmptyText(getActivity().getString(R.string.no_notes));

        // create an empty adapter, our Loader will retrieve the data
        // asynchronously
        mAdapter = new NotesListCursorAdapter(
                getActivity(),
                R.layout.list_item_notes,
                null,
                new String[] {
                    NotesProvider.KEY_TITLE
                },
                new int[] {
                    android.R.id.text1
                },
                0);

        setListAdapter(mAdapter);

        // setup our list view
        final ListView notesList = getListView();

        // add listners to handle note selection & contextual action bar
        notesList.setOnItemLongClickListener(this);
        notesList.setOnItemClickListener(this);

        // restore any saved state
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CURRENT_ACTIVATED)) {
                mCurrentActivePosition = savedInstanceState.getInt(
                        KEY_CURRENT_ACTIVATED, ListView.INVALID_POSITION);
            }
            if (savedInstanceState.containsKey(KEY_CURRENT_CHECKED)) {
                final long[] checked = savedInstanceState
                        .getLongArray(KEY_CURRENT_CHECKED);
                for (long l : checked) {
                    mCheckedItems.add(l);
                }
                startContextualActionMode();
            }
        }

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mContainerCallback = (NoteEventsCallback) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement NoteSelectedCallback");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ACTIVATED, mCurrentActivePosition);
        if (mCheckedItems != null && mCheckedItems.size() > 0) {
            long[] checked = new long[mCheckedItems.size()];
            final int N = mCheckedItems.size();
            for (int i = 0; i < N; i++) {
                checked[i] = mCheckedItems.get(i);
            }
            outState.putLongArray(KEY_CURRENT_CHECKED, checked);
        }
    }

    /**
     * Helper method to set the activation state of a note
     * 
     * @param noteId The id of the note to be activated
     */
    protected void setActivatedNote(long noteId) {
        if (mAdapter != null && mTwoPaneView) {
            // work out the position in the list of note with the given id
            final int N = mAdapter.getCount();
            for (int position = 0; position < N; position++) {
                if (mAdapter.getItemId(position) == noteId) {
                    if (position != mCurrentActivePosition) {
                        clearActivation();
                        mCurrentActivePosition = position;
                        View row = getListView().getChildAt(position);
                        if (row != null) {
                            UiUtils.setActivatedCompat(row, true);
                        }
                    }
                    break;
                }
            }
        } else {
            setActivatedNoteAfterLoad(noteId);
        }
    }

    protected void setActivatedNoteAfterLoad(long noteId) {
        // if we have not loaded our cursor yet then store the note id
        // for now & activate once loaded
        mNoteIdToActivate = noteId;
    }

    /**
     * Helper method to clear the list's activated state
     */
    protected void clearActivation() {
        if (mTwoPaneView && mCurrentActivePosition != ListView.INVALID_POSITION) {
            UiUtils.setActivatedCompat(getListView().getChildAt(mCurrentActivePosition), false);
        }
        mCurrentActivePosition = ListView.INVALID_POSITION;
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        return new CursorLoader(getActivity(), NotesProvider.CONTENT_URI,
                NotesQuery.PROJECTION,
                null, null, NotesProvider.KEY_TITLE + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
        // check if we need to set one of the (now loaded) notes as activated
        if (mTwoPaneView && mNoteIdToActivate > -1) {
            setActivatedNote(mNoteIdToActivate);
            mNoteIdToActivate = -1;
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> list, View v, int position, long id) {
        if (mMode == null) {
            if (mTwoPaneView) {
                clearActivation();
                mCurrentActivePosition = position;
                UiUtils.setActivatedCompat(v, true);
            }
            mContainerCallback.onNoteSelected(ContentUris.withAppendedId(
                    NotesProvider.CONTENT_URI, id));
        } else {
            if (mCheckedItems.contains(id)) {
                ((CheckedTextView) v).setChecked(false);
                mCheckedItems.remove(id);
            } else {
                ((CheckedTextView) v).setChecked(true);
                mCheckedItems.add(id);
            }

            if (mCheckedItems.size() > 0) {
                setSelectedCount();
            } else {
                mMode.finish();
            }
        }
    }

    private void setSelectedCount() {
        if (mMode != null) {
            mMode.setTitle(getActivity().getString(R.string.num_selected,
                    mCheckedItems.size()));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ((CheckedTextView) view).setChecked(true);
        mCheckedItems.add(id);
        if (mMode == null) {
            startContextualActionMode();
        }
        return true;
    }

    private void startContextualActionMode() {
        mMode = getSherlockActivity().startActionMode(new NotesListActionMode());
        setSelectedCount();
    }

    private final class NotesListActionMode implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete_notes:
                    int deletedCount = 0;

                    for (long id : mCheckedItems) {
                        deletedCount += getActivity().getContentResolver().delete(
                                ContentUris.withAppendedId(
                                        NotesProvider.CONTENT_URI, id), null, null);
                    }

                    // clear any selections
                    clearActivation();

                    // update container
                    mContainerCallback.onNoteDeleted();

                    // show a toast to confirm delete
                    Toast.makeText(
                            getActivity(),
                            String.format(
                                    getActivity().getString(R.string.num_deleted),
                                    deletedCount, (deletedCount == 1 ? "" : "s")),
                            Toast.LENGTH_SHORT).show();

                    WidgetProvider.updateWidget(getActivity());

                    // clear the contextual action bar
                    mode.finish();
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.notes_list_context, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // clear state and refresh view
            mAdapter.notifyDataSetChanged();
            mCheckedItems.clear();
            mMode = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

    }

    /**
     * A trivial extension to {@link SimpleCursorAdapter} that sets a specified
     * item's Activated and Checked states.
     */
    private class NotesListCursorAdapter extends SimpleCursorAdapter {

        public NotesListCursorAdapter(Context context, int layout, Cursor c,
                String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final CheckedTextView ctv = (CheckedTextView) view;
            ctv.setText(cursor.getString(NotesQuery.TITLE));
            ctv.setChecked(mCheckedItems.contains(cursor.getLong(NotesQuery.ID)));
            UiUtils.setActivatedCompat(ctv, cursor.getPosition() == mCurrentActivePosition);
        }

    }

    private interface NotesQuery {

        final static String[] PROJECTION = {
                NotesProvider.KEY_ID,
                NotesProvider.KEY_TITLE
        };

        final static int ID = 0;
        final static int TITLE = 1;
    }

}
