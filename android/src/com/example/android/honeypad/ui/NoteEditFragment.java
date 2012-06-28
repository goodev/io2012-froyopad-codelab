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

import android.app.Fragment;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.honeypad.R;
import com.example.android.honeypad.appwidget.WidgetProvider;
import com.example.android.honeypad.provider.NotesProvider;
import com.example.android.honeypad.ui.NoteListFragment.NoteEventsCallback;
import com.example.android.honeypad.utils.UiUtils;

public class NoteEditFragment extends Fragment {

    // launch actions
    public static final String ACTION_CREATE_NOTE = "com.example.android.honeypad.ACTION_CREATE_NOTE";
    public static final String ACTION_VIEW_NOTE = "com.example.android.honeypad.ACTION_VIEW_NOTE";
    public static final String ACTION_EDIT_NOTE = "com.example.android.honeypad.ACTION_EDIT_NOTE";

    private EditText mTitleText;
    private EditText mBodyText;

    // expose the currently displayed note
    protected Uri mCurrentNote;

    private static boolean mTwoPaneView;

    // default constructor
    public NoteEditFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mTwoPaneView = UiUtils.isHoneycombTablet(getActivity());
        View v = inflater.inflate(R.layout.fragment_note_edit, container, false);

        mTitleText = (EditText) v.findViewById(R.id.title);
        mBodyText = (EditText) v.findViewById(R.id.body);
        Button confirmButton = (Button) v.findViewById(R.id.confirm);
        if (mTwoPaneView) {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    saveNote();
                }
            });
        } else {
            confirmButton.setVisibility(View.GONE);
            setHasOptionsMenu(true);
        }

        if (savedInstanceState != null
                && savedInstanceState.containsKey(NotesProvider.KEY_ID)) {
            mCurrentNote = Uri.parse((String) savedInstanceState
                    .getString(NotesProvider.KEY_ID));
        }

        populateFields();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_note, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_note:
                saveNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Display a particular note in this fragment.
     * 
     * @param noteUri The Uri of the note to display
     */
    protected void loadNote(Uri noteUri) {
        mCurrentNote = noteUri;
        if (isAdded()) {
            populateFields();
        }
    }

    /**
     * Clear all fields on this fragment.
     */
    protected void clear() {
        mTitleText.setText(null);
        mBodyText.setText(null);
        mCurrentNote = null;
    }

    /**
     * Helper method which retrieves & displays the content of the current note.
     */
    private void populateFields() {
        if (mCurrentNote != null) {

            Cursor c = null;
            try {
                c = getActivity().getContentResolver().query(mCurrentNote,
                        null, null, null, null);
                if (c.moveToFirst()) {
                    mTitleText.setText(c.getString(NotesProvider.TITLE_COLUMN));
                    mBodyText.setText(c.getString(NotesProvider.BODY_COLUMN));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentNote != null) {
            outState.putString(NotesProvider.KEY_ID, mCurrentNote.toString());
        }
    }

    /**
     * Persists the details of the current note. This will either create a new
     * note, or update an existing note.
     */
    private void saveNote() {
        ContentValues values = new ContentValues(2);
        values.put(NotesProvider.KEY_TITLE, mTitleText.getText().toString());
        values.put(NotesProvider.KEY_BODY, mBodyText.getText().toString());
        final boolean updating = mCurrentNote != null;
        if (updating) {
            getActivity().getContentResolver().update(mCurrentNote, values,
                    null, null);
        } else {
            Uri newNote = getActivity().getContentResolver().insert(
                    NotesProvider.CONTENT_URI, values);

            if (newNote != null) {
                mCurrentNote = newNote;
            }
        }

        // show a toast confirmation
        Toast.makeText(getActivity(),
                updating ? R.string.note_updated : R.string.note_saved,
                Toast.LENGTH_SHORT).show();

        WidgetProvider.updateWidget(getActivity());

        ((NoteEventsCallback) getActivity()).onNoteCreated(mCurrentNote);
    }

}
