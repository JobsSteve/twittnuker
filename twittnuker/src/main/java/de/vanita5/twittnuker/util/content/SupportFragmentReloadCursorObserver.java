/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.util.content;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import de.vanita5.twittnuker.TwittnukerConstants;

public class SupportFragmentReloadCursorObserver extends ContentObserver implements TwittnukerConstants {

    private final Fragment mFragment;
    private final int mLoaderId;
    private final LoaderCallbacks<Cursor> mCallback;

    public SupportFragmentReloadCursorObserver(final Fragment fragment, final int loaderId,
                                               final LoaderCallbacks<Cursor> callback) {
        super(createHandler());
        mFragment = fragment;
        mLoaderId = loaderId;
        mCallback = callback;
    }

    private static Handler createHandler() {
        if (Thread.currentThread().getId() != 1) return new Handler(Looper.getMainLooper());
        return new Handler();
    }

    @Override
    public final void onChange(final boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(final boolean selfChange, @Nullable final Uri uri) {
        if (mFragment == null || mFragment.getActivity() == null || mFragment.isDetached()) return;
        // Handle change.
        mFragment.getLoaderManager().restartLoader(mLoaderId, null, mCallback);
    }
}