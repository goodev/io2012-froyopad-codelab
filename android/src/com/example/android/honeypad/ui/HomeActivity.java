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

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.honeypad.R;
import com.example.android.honeypad.provider.NotesProvider;
import com.example.android.honeypad.ui.NoteListFragment.NoteEventsCallback;
import com.example.android.honeypad.utils.UiUtils;

public class HomeActivity extends FragmentActivity implements NoteEventsCallback {

    // extra for the above action
    public static final String EXTRA_NOTE_ID = "noteId";

    // key for adding NoteEditFragment to this Activity
    private static final String NOTE_EDIT_TAG = "Edit";

    private static boolean mTwoPaneView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        mTwoPaneView = UiUtils.isHoneycombTablet(this);
        if (NoteEditFragment.ACTION_VIEW_NOTE.equals(getIntent().getAction())) {
            viewNote(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NoteEditFragment.ACTION_VIEW_NOTE.equals(intent.getAction())) {
            viewNote(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_note:
                if (mTwoPaneView) {
                    showNote(null);
                    NoteListFragment list = (NoteListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.list);
                    list.clearActivation();
                    return true;
                } else {
                    startActivity(new Intent(NoteEditFragment.ACTION_CREATE_NOTE));
                    ;
                }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void viewNote(Intent launchIntent) {
        final long noteId = launchIntent.getLongExtra(EXTRA_NOTE_ID, -1);
        showNote(ContentUris.withAppendedId(NotesProvider.CONTENT_URI, noteId));
    }

    /**
     * This method controls both fragments, instructing them to display a
     * certain note.
     * 
     * @param noteUri The {@link Uri} of the note to show. To create a new note,
     *            pass {@code null}.
     */
    private void showNote(final Uri noteUri) {

        if (mTwoPaneView) {
            // check if the NoteEditFragment has been added
            FragmentManager fm = getSupportFragmentManager();
            NoteEditFragment edit = (NoteEditFragment) fm
                    .findFragmentByTag(NOTE_EDIT_TAG);
            final boolean editNoteAdded = (edit != null);

            if (editNoteAdded) {
                if (edit.mCurrentNote != null && edit.mCurrentNote.equals(noteUri)) {
                    // clicked on the currently selected note
                    return;
                }

                NoteEditFragment editFrag = (NoteEditFragment) fm
                                .findFragmentByTag(NOTE_EDIT_TAG);
                if (noteUri != null) {
                    // load an existing note
                    editFrag.loadNote(noteUri);
                    NoteListFragment list = (NoteListFragment) fm
                                    .findFragmentById(R.id.list);
                    list.setActivatedNote(Long.valueOf(noteUri.getLastPathSegment()));
                } else {
                    // creating a new note - clear the form & list
                    // activation
                    if (editNoteAdded) {
                        editFrag.clear();
                    }
                    NoteListFragment list = (NoteListFragment) fm
                                    .findFragmentById(R.id.list);
                    list.clearActivation();
                }
            } else {
                // add the NoteEditFragment to the container
                FragmentTransaction ft = fm.beginTransaction();
                edit = new NoteEditFragment();
                ft.add(R.id.note_detail_container, edit, NOTE_EDIT_TAG);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
                edit.loadNote(noteUri);
            }
        } else {
            startActivity(new Intent(NoteEditFragment.ACTION_VIEW_NOTE, noteUri));
        }
    }

    /**
     * Callback from child fragment
     */
    public void onNoteSelected(Uri noteUri) {
        showNote(noteUri);
    }

    /**
     * Callback from child fragment
     */
    public void onNoteDeleted() {
        // remove the NoteEditFragment after a deletion
        FragmentManager fm = getSupportFragmentManager();
        NoteEditFragment edit = (NoteEditFragment) fm
                .findFragmentByTag(NOTE_EDIT_TAG);
        if (edit != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(edit);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

    @Override
    public void onNoteCreated(Uri noteUri) {
        NoteListFragment list = (NoteListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.list);
        list.setActivatedNoteAfterLoad(Long.valueOf(noteUri.getLastPathSegment()));
    }
}
