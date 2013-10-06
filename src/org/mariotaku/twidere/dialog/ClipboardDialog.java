/*
 * Copyright 2013 Andrew Neal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.mariotaku.twidere.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.Toast;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.util.ClipboardUtils;

/**
 * A custom {@link DialogFragment} that allows the user to copy some text to the
 * clipboard
 */
public class ClipboardDialog extends DialogFragment implements OnClickListener {

    /** {@link FragmentManager} tag */
    private static final String TAG = "clipboard_dialog_fragment";

    // Bundle keys
    private static final String TEXT = "clipboard_dialog_text";

    /** The text to be copied */
    private String mText;

    /**
     * Empty constructor as per the {@link Fragment} docs
     */
    public ClipboardDialog() {
    }

    /**
     * @param text The text to be copied
     * @return A new instance of <code>ClipboardDialog</code>
     */
    public static ClipboardDialog newInstance(String text) {
        final Bundle args = new Bundle();
        args.putString(TEXT, text);
        final ClipboardDialog fragment = new ClipboardDialog();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the text
        mText = getArguments().getString(TEXT);

        // Create the Dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mText);
        builder.setItems(new String[] {
                getString(R.string.copy_to_clipboard)
        }, this);
        return builder.create();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Activity act = getActivity();
        ClipboardUtils.setText(act, mText);
        Toast.makeText(act, getString(R.string.copied_confirmation), Toast.LENGTH_SHORT).show();
    }

    /**
     * Used to display the dialog
     * 
     * @param activity The {@link Activity} to use
     * @param text The text to be copied
     */
    public static void show(Activity activity, String text) {
        newInstance(text).show(activity.getFragmentManager(), TAG);
    }

}
