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

package de.vanita5.twittnuker.loader;

import android.content.Context;
import android.support.annotation.NonNull;

import de.vanita5.twittnuker.api.MicroBlog;
import de.vanita5.twittnuker.api.twitter.TwitterException;
import de.vanita5.twittnuker.api.twitter.model.IDs;
import de.vanita5.twittnuker.api.twitter.model.Paging;
import de.vanita5.twittnuker.model.ParcelableCredentials;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.UserKey;

import java.util.List;

public class StatusRetweetersLoader extends CursorSupportUsersLoader {

    private final String mStatusId;

    public StatusRetweetersLoader(final Context context, final UserKey accountKey,
                                  final String statusId, final List<ParcelableUser> data,
                                  final boolean fromUser) {
        super(context, accountKey, data, fromUser);
        mStatusId = statusId;
    }

    @NonNull
    @Override
    protected IDs getIDs(@NonNull final MicroBlog twitter, @NonNull ParcelableCredentials credentials, @NonNull final Paging paging) throws TwitterException {
        return twitter.getRetweetersIDs(mStatusId, paging);
    }

    @Override
    protected boolean useIDs(@NonNull ParcelableCredentials credentials) {
        return true;
    }
}