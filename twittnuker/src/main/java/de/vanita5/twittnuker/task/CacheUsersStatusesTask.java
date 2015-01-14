/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2014 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import com.twitter.Extractor;

import org.mariotaku.querybuilder.Expression;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedHashtags;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedStatuses;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedUsers;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;
import de.vanita5.twittnuker.util.TwitterWrapper.TwitterListResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.User;

import static de.vanita5.twittnuker.util.ContentValuesCreator.createCachedUser;
import static de.vanita5.twittnuker.util.ContentValuesCreator.createStatus;
import static de.vanita5.twittnuker.util.content.ContentResolverUtils.bulkDelete;
import static de.vanita5.twittnuker.util.content.ContentResolverUtils.bulkInsert;

public class CacheUsersStatusesTask extends TwidereAsyncTask<Void, Void, Void> implements Constants {

	private final TwitterListResponse<twitter4j.Status>[] all_statuses;
	private final ContentResolver resolver;

	public CacheUsersStatusesTask(final Context context, final TwitterListResponse<twitter4j.Status>... all_statuses) {
		resolver = context.getContentResolver();
		this.all_statuses = all_statuses;
	}

	@Override
	protected Void doInBackground(final Void... args) {
		if (all_statuses == null || all_statuses.length == 0) return null;
		final Extractor extractor = new Extractor();
        final Set<ContentValues> cachedUsersValues = new HashSet<>();
        final Set<ContentValues> cached_statuses_values = new HashSet<>();
        final Set<ContentValues> hashtag_values = new HashSet<>();
        final Set<Long> userIds = new HashSet<>();
        final Set<Long> status_ids = new HashSet<>();
        final Set<String> hashtags = new HashSet<>();
        final Set<User> users = new HashSet<>();

		for (final TwitterListResponse<twitter4j.Status> values : all_statuses) {
			if (values == null || values.list == null) {
				continue;
			}
			final List<twitter4j.Status> list = values.list;
			for (final twitter4j.Status status : list) {
				if (status == null || status.getId() <= 0) {
					continue;
				}
				status_ids.add(status.getId());
				cached_statuses_values.add(createStatus(status, values.account_id));
				hashtags.addAll(extractor.extractHashtags(status.getText()));
				final User user = status.getUser();
				if (user != null && user.getId() > 0) {
					users.add(user);
					final ContentValues filtered_users_values = new ContentValues();
					filtered_users_values.put(Filters.Users.NAME, user.getName());
					filtered_users_values.put(Filters.Users.SCREEN_NAME, user.getScreenName());
					final String filtered_users_where = Expression.equals(Filters.Users.USER_ID, user.getId()).getSQL();
					resolver.update(Filters.Users.CONTENT_URI, filtered_users_values, filtered_users_where, null);
				}
			}
		}

		bulkDelete(resolver, CachedStatuses.CONTENT_URI, CachedStatuses.STATUS_ID, status_ids, null, false);
		bulkInsert(resolver, CachedStatuses.CONTENT_URI, cached_statuses_values);

		for (final String hashtag : hashtags) {
			final ContentValues hashtag_value = new ContentValues();
			hashtag_value.put(CachedHashtags.NAME, hashtag);
			hashtag_values.add(hashtag_value);
		}
		bulkDelete(resolver, CachedHashtags.CONTENT_URI, CachedHashtags.NAME, hashtags, null, true);
		bulkInsert(resolver, CachedHashtags.CONTENT_URI, hashtag_values);

		for (final User user : users) {
			userIds.add(user.getId());
			cachedUsersValues.add(createCachedUser(user));
		}
//        bulkDelete(resolver, CachedUsers.CONTENT_URI, CachedUsers.USER_ID, userIds, null, false);
		bulkInsert(resolver, CachedUsers.CONTENT_URI, cachedUsersValues);
		return null;
	}

	public static Runnable getRunnable(final Context context,
			final TwitterListResponse<twitter4j.Status>... all_statuses) {
		return new ExecuteCacheUserStatusesTaskRunnable(context, all_statuses);
	}

	static class ExecuteCacheUserStatusesTaskRunnable implements Runnable {
		final Context context;
		final TwitterListResponse<twitter4j.Status>[] all_statuses;

		ExecuteCacheUserStatusesTaskRunnable(final Context context,
				final TwitterListResponse<twitter4j.Status>... all_statuses) {
			this.context = context;
			this.all_statuses = all_statuses;
		}

		@Override
		public void run() {
			new CacheUsersStatusesTask(context, all_statuses).executeTask();
		}
	}
}