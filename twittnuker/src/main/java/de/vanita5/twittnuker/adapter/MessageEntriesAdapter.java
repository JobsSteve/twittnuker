/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.adapter.iface.IContentCardAdapter;
import de.vanita5.twittnuker.app.TwittnukerApplication;
import de.vanita5.twittnuker.provider.TwidereDataStore.DirectMessages.ConversationEntries;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ImageLoaderWrapper;
import de.vanita5.twittnuker.util.ImageLoadingHandler;
import de.vanita5.twittnuker.util.MultiSelectManager;
import de.vanita5.twittnuker.util.SharedPreferencesWrapper;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.holder.MessageEntryViewHolder;

public class MessageEntriesAdapter extends Adapter<ViewHolder> implements Constants, IContentCardAdapter, OnClickListener {

	private final Context mContext;
	private final LayoutInflater mInflater;
	private final ImageLoaderWrapper mImageLoader;
	private final MultiSelectManager mMultiSelectManager;
    private final int mTextSize;
    private final int mProfileImageStyle;
    private final int mMediaPreviewStyle;
	private Cursor mCursor;
	private MessageEntriesAdapterListener mListener;

	public Context getContext() {
		return mContext;
	}

    @Override
    public ImageLoadingHandler getImageLoadingHandler() {
        return null;
    }

    @Override
    public int getProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    public int getMediaPreviewStyle() {
        return mMediaPreviewStyle;
    }

    @Override
    public AsyncTwitterWrapper getTwitterWrapper() {
        return null;
    }

    @Override
    public float getTextSize() {
        return mTextSize;
    }

	public ImageLoaderWrapper getImageLoader() {
		return mImageLoader;
	}

	@Override
    public boolean isGapItem(int position) {
        return false;
    }

    @Override
    public void onGapClick(ViewHolder holder, int position) {

    }

    @Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		final View view = mInflater.inflate(R.layout.list_item_message_entry, parent, false);
		return new MessageEntryViewHolder(this, view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		final Cursor c = mCursor;
		c.moveToPosition(position);
		((MessageEntryViewHolder) holder).displayMessage(c);
	}

    @Override
    public void onItemActionClick(ViewHolder holder, int id, int position) {

    }

    @Override
    public void onItemMenuClick(ViewHolder holder, View menuView, int position) {

    }

	public void onMessageClick(int position) {
		if (mListener == null) return;
		mListener.onEntryClick(position, getEntry(position));
	}

	public void setCursor(Cursor cursor) {
		mCursor = cursor;
		notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		final Cursor c = mCursor;
		if (c == null) return 0;
		return c.getCount();
	}

	public MessageEntriesAdapter(final Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		final TwittnukerApplication app = TwittnukerApplication.getInstance(context);
		mMultiSelectManager = app.getMultiSelectManager();
		mImageLoader = app.getImageLoaderWrapper();
		final SharedPreferencesWrapper preferences = SharedPreferencesWrapper.getInstance(context,
				SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mProfileImageStyle = Utils.getProfileImageStyle(preferences.getString(KEY_PROFILE_IMAGE_STYLE, null));
        mMediaPreviewStyle = Utils.getMediaPreviewStyle(preferences.getString(KEY_MEDIA_PREVIEW_STYLE, null));
        mTextSize = preferences.getInt(KEY_TEXT_SIZE, context.getResources().getInteger(R.integer.default_text_size));
	}

	public static class DirectMessageEntry {

		public final long account_id, conversation_id;
		public final String screen_name, name;

		DirectMessageEntry(Cursor cursor) {
			account_id = cursor.getLong(ConversationEntries.IDX_ACCOUNT_ID);
			conversation_id = cursor.getLong(ConversationEntries.IDX_CONVERSATION_ID);
			screen_name = cursor.getString(ConversationEntries.IDX_SCREEN_NAME);
			name = cursor.getString(ConversationEntries.IDX_NAME);
		}

	}

	public DirectMessageEntry getEntry(final int position) {
		final Cursor c = mCursor;
		if (c == null || c.isClosed() || !c.moveToPosition(position)) return null;
		return new DirectMessageEntry(c);
	}

	@Override
	public void onClick(final View view) {
//        if (mMultiSelectManager.isActive()) return;
//        final Object tag = view.getTag();
//        final int position = tag instanceof Integer ? (Integer) tag : -1;
//        if (position == -1) return;
//        switch (view.getId()) {
//            case R.id.profile_image: {
//                if (mContext instanceof Activity) {
//                    final long account_id = getAccountId(position);
//                    final long user_id = getConversationId(position);
//                    final String screen_name = getScreenName(position);
//                    openUserProfile(mContext, account_id, user_id, screen_name, null);
//                }
//                break;
//            }
//        }
	}

	public void setListener(MessageEntriesAdapterListener listener) {
		mListener = listener;
	}

	public interface MessageEntriesAdapterListener {
		public void onEntryClick(int position, DirectMessageEntry entry);
	}

}