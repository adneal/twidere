/*
 *				Twidere - Twitter client for Android
 * 
 * Copyright (C) 2012 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util;

import static org.mariotaku.twidere.util.ContentResolverUtils.bulkDelete;
import static org.mariotaku.twidere.util.ContentResolverUtils.bulkInsert;
import static org.mariotaku.twidere.util.Utils.getAccountScreenNames;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.twitter.Extractor;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.CroutonStyle;

import org.mariotaku.twidere.Constants;
import org.mariotaku.twidere.R;
import org.mariotaku.twidere.activity.BaseSupportActivity;
import org.mariotaku.twidere.app.TwidereApplication;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.ParcelableUser;
import org.mariotaku.twidere.provider.TweetStore.Filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("Registered")
public class MultiSelectEventHandler implements Constants, ActionMode.Callback, MultiSelectManager.Callback {

	private TwidereApplication mApplication;

	private AsyncTwitterWrapper mTwitterWrapper;

	private MultiSelectManager mMultiSelectManager;

	private ActionMode mActionMode;

	private final BaseSupportActivity mActivity;

	public MultiSelectEventHandler(final BaseSupportActivity activity) {
		mActivity = activity;
	}

	/**
	 * Call before super.onCreate
	 */
	public void dispatchOnCreate() {
		mApplication = mActivity.getTwidereApplication();
		mTwitterWrapper = mApplication.getTwitterWrapper();
		mMultiSelectManager = mApplication.getMultiSelectManager();
	}

	/**
	 * Call after super.onStart
	 */
	public void dispatchOnStart() {
		mMultiSelectManager.registerCallback(this);
		updateMultiSelectState();
	}

	/**
	 * Call before super.onStop
	 */
	public void dispatchOnStop() {
		mMultiSelectManager.unregisterCallback(this);
	}

	@Override
	public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
		final List<Object> selected_items = mMultiSelectManager.getSelectedItems();
		if (selected_items.isEmpty()) return false;
		switch (item.getItemId()) {
			case MENU_REPLY: {
				final Extractor extractor = new Extractor();
				final Intent intent = new Intent(INTENT_ACTION_REPLY_MULTIPLE);
				final Bundle bundle = new Bundle();
				final String[] account_names = getAccountScreenNames(mActivity);
				final NoDuplicatesArrayList<String> all_mentions = new NoDuplicatesArrayList<String>();
				for (final Object object : selected_items) {
					if (object instanceof ParcelableStatus) {
						final ParcelableStatus status = (ParcelableStatus) object;
						all_mentions.add(status.user_screen_name);
						all_mentions.addAll(extractor.extractMentionedScreennames(status.text_plain));
					} else if (object instanceof ParcelableUser) {
						final ParcelableUser user = (ParcelableUser) object;
						all_mentions.add(user.screen_name);
					}
				}
				all_mentions.removeAll(Arrays.asList(account_names));
				final Object first_obj = selected_items.get(0);
				if (first_obj instanceof ParcelableStatus) {
					final ParcelableStatus first_status = (ParcelableStatus) first_obj;
					bundle.putLong(EXTRA_ACCOUNT_ID, first_status.account_id);
					bundle.putLong(EXTRA_IN_REPLY_TO_ID, first_status.id);
				} else if (first_obj instanceof ParcelableUser) {
					final ParcelableUser first_user = (ParcelableUser) first_obj;
					bundle.putLong(EXTRA_ACCOUNT_ID, first_user.account_id);
				}
				bundle.putStringArray(EXTRA_SCREEN_NAMES, all_mentions.toArray(new String[all_mentions.size()]));
				intent.putExtras(bundle);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mActivity.startActivity(intent);
				mode.finish();
				break;
			}
			case MENU_MUTE_USER: {
				final ContentResolver resolver = mActivity.getContentResolver();
				final Uri uri = Filters.Users.CONTENT_URI;
				final ArrayList<ContentValues> values_list = new ArrayList<ContentValues>();
				final List<String> names_list = new NoDuplicatesArrayList<String>();
				for (final Object object : selected_items) {
					if (object instanceof ParcelableStatus) {
						final ParcelableStatus status = (ParcelableStatus) object;
						names_list.add(status.user_screen_name);
					} else if (object instanceof ParcelableUser) {
						final ParcelableUser user = (ParcelableUser) object;
						names_list.add(user.screen_name);
					} else {
						continue;
					}
				}
				bulkDelete(resolver, uri, Filters.Users.VALUE, names_list, null, true);
				for (final String screen_name : names_list) {
					final ContentValues values = new ContentValues();
					values.put(Filters.VALUE, screen_name);
					values_list.add(values);
				}
				bulkInsert(resolver, uri, values_list);
				Crouton.showText(mActivity, R.string.users_muted, CroutonStyle.INFO);
				mode.finish();
				mActivity.sendBroadcast(new Intent(BROADCAST_MULTI_MUTESTATE_CHANGED));
				break;
			}
			case MENU_BLOCK: {
				final long account_id = MultiSelectManager.getFirstSelectAccountId(selected_items);
				final long[] user_ids = MultiSelectManager.getSelectedUserIds(selected_items);
				if (account_id > 0 && user_ids != null) {
					mTwitterWrapper.createMultiBlockAsync(account_id, user_ids);
				}
				mode.finish();
				break;
			}
			case MENU_REPORT_SPAM: {
				final long account_id = MultiSelectManager.getFirstSelectAccountId(selected_items);
				final long[] user_ids = MultiSelectManager.getSelectedUserIds(selected_items);
				if (account_id > 0 && user_ids != null) {
					mTwitterWrapper.reportMultiSpam(account_id, user_ids);
				}
				mode.finish();
				break;
			}
		}
		return true;
	}

	@Override
	public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
		new MenuInflater(mActivity).inflate(R.menu.action_multi_select_contents, menu);
		return true;
	}

	@Override
	public void onDestroyActionMode(final ActionMode mode) {
		if (mMultiSelectManager.getCount() != 0) {
			mMultiSelectManager.clearSelectedItems();
		}
		mActionMode = null;
	}

	@Override
	public void onItemsCleared() {
		updateMultiSelectState();
	}

	@Override
	public void onItemSelected(final Object item) {
		updateMultiSelectState();
	}

	@Override
	public void onItemUnselected(final Object item) {
		updateMultiSelectState();
	}

	@Override
	public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
		return true;
	}

	private void updateMultiSelectState() {
		if (mMultiSelectManager.isActive()) {
			if (mActionMode == null) {
				mActionMode = mActivity.startActionMode(this);
			}
			final int count = mMultiSelectManager.getCount();
			mActionMode.setTitle(mActivity.getResources().getQuantityString(R.plurals.Nitems_selected, count, count));
		} else {
			if (mActionMode != null) {
				mActionMode.finish();
				mActionMode = null;
			}
		}
	}

}
