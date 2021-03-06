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

package de.vanita5.twittnuker.util;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import de.vanita5.twittnuker.library.twitter.model.DirectMessage;
import de.vanita5.twittnuker.library.twitter.model.SavedSearch;
import de.vanita5.twittnuker.library.twitter.model.Status;
import de.vanita5.twittnuker.library.twitter.model.Trend;
import de.vanita5.twittnuker.library.twitter.model.Trends;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.TwittnukerConstants;
import de.vanita5.twittnuker.model.AccountDetails;
import de.vanita5.twittnuker.model.Draft;
import de.vanita5.twittnuker.model.ParcelableActivity;
import de.vanita5.twittnuker.model.ParcelableActivityValuesCreator;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.ParcelableDirectMessageValuesCreator;
import de.vanita5.twittnuker.model.ParcelableMediaUpdate;
import de.vanita5.twittnuker.model.ParcelableStatus;
import de.vanita5.twittnuker.model.ParcelableStatusValuesCreator;
import de.vanita5.twittnuker.model.ParcelableUser;
import de.vanita5.twittnuker.model.ParcelableUserMention;
import de.vanita5.twittnuker.model.ParcelableUserValuesCreator;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.model.draft.SendDirectMessageActionExtras;
import de.vanita5.twittnuker.model.util.ParcelableActivityExtensionsKt;
import de.vanita5.twittnuker.model.util.ParcelableDirectMessageUtils;
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils;
import de.vanita5.twittnuker.model.util.ParcelableUserUtils;
import de.vanita5.twittnuker.provider.TwidereDataStore.CachedTrends;
import de.vanita5.twittnuker.provider.TwidereDataStore.Drafts;
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters;
import de.vanita5.twittnuker.provider.TwidereDataStore.SavedSearches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ContentValuesCreator implements TwittnukerConstants {
    private ContentValuesCreator() {
    }

    public static ContentValues createCachedUser(final User user) throws IOException {
        if (user == null) return null;
        final ContentValues values = new ContentValues();
        ParcelableUserValuesCreator.writeTo(ParcelableUserUtils.fromUser(user, null), values);
        return values;
    }

    public static ContentValues createDirectMessage(final DirectMessage message,
                                                    final UserKey accountKey,
                                                    final boolean isOutgoing) throws IOException {
        return ParcelableDirectMessageValuesCreator.create(ParcelableDirectMessageUtils.fromDirectMessage(message,
                accountKey, isOutgoing));
    }

    public static ContentValues createDirectMessage(final ParcelableDirectMessage message) throws IOException {
        if (message == null) return null;
        final ContentValues values = new ContentValues();
        ParcelableDirectMessageValuesCreator.writeTo(message, values);
        return values;
    }

    public static ContentValues createFilteredUser(final ParcelableStatus status) {
        if (status == null) return null;
        final ContentValues values = new ContentValues();
        values.put(Filters.Users.USER_KEY, status.user_key.toString());
        values.put(Filters.Users.NAME, status.user_name);
        values.put(Filters.Users.SCREEN_NAME, status.user_screen_name);
        return values;
    }

    public static ContentValues createFilteredUser(final ParcelableUser user) {
        if (user == null) return null;
        final ContentValues values = new ContentValues();
        values.put(Filters.Users.USER_KEY, user.key.toString());
        values.put(Filters.Users.NAME, user.name);
        values.put(Filters.Users.SCREEN_NAME, user.screen_name);
        return values;
    }

    public static ContentValues createFilteredUser(final ParcelableUserMention user) {
        if (user == null) return null;
        final ContentValues values = new ContentValues();
        values.put(Filters.Users.USER_KEY, user.key.toString());
        values.put(Filters.Users.NAME, user.name);
        values.put(Filters.Users.SCREEN_NAME, user.screen_name);
        return values;
    }

    public static ContentValues createMessageDraft(final UserKey accountKey, final String recipientId,
                                                   final String text, final String imageUri) {
        final ContentValues values = new ContentValues();
        values.put(Drafts.ACTION_TYPE, Draft.Action.SEND_DIRECT_MESSAGE);
        values.put(Drafts.TEXT, text);
        values.put(Drafts.ACCOUNT_KEYS, accountKey.toString());
        values.put(Drafts.TIMESTAMP, System.currentTimeMillis());
        if (imageUri != null) {
            final ParcelableMediaUpdate[] mediaArray = {new ParcelableMediaUpdate(imageUri, 0)};
            values.put(Drafts.MEDIA, JsonSerializer.serialize(Arrays.asList(mediaArray),
                    ParcelableMediaUpdate.class));
        }
        final SendDirectMessageActionExtras extra = new SendDirectMessageActionExtras();
        extra.setRecipientId(recipientId);
        values.put(Drafts.ACTION_EXTRAS, JsonSerializer.serialize(extra));
        return values;
    }

    public static ContentValues createSavedSearch(final SavedSearch savedSearch,
                                                  final UserKey accountKey) {
        final ContentValues values = new ContentValues();
        values.put(SavedSearches.ACCOUNT_KEY, accountKey.toString());
        values.put(SavedSearches.SEARCH_ID, savedSearch.getId());
        values.put(SavedSearches.CREATED_AT, savedSearch.getCreatedAt().getTime());
        values.put(SavedSearches.NAME, savedSearch.getName());
        values.put(SavedSearches.QUERY, savedSearch.getQuery());
        return values;
    }

    public static ContentValues[] createSavedSearches(final List<SavedSearch> savedSearches,
                                                      final UserKey accountKey) {
        final ContentValues[] resultValuesArray = new ContentValues[savedSearches.size()];
        for (int i = 0, j = savedSearches.size(); i < j; i++) {
            resultValuesArray[i] = createSavedSearch(savedSearches.get(i), accountKey);
    }
        return resultValuesArray;
    }

    @NonNull
    public static ContentValues createStatus(final Status orig, final UserKey accountKey) throws IOException {
        return ParcelableStatusValuesCreator.create(ParcelableStatusUtils.INSTANCE.fromStatus(orig,
                accountKey, false));
    }

    @NonNull
    public static ContentValues createActivity(final ParcelableActivity activity,
                                               final AccountDetails details,
                                               final UserColorNameManager manager) throws IOException {
        final ContentValues values = new ContentValues();
        final ParcelableStatus status = ParcelableActivityExtensionsKt.getActivityStatus(activity);

        activity.account_color = details.color;

        if (status != null) {
            ParcelableStatusUtils.INSTANCE.updateExtraInformation(status, details);

            activity.status_id = status.id;
            activity.status_retweet_id = status.retweet_id;
            activity.status_my_retweet_id = status.my_retweet_id;

            if (status.is_retweet) {
                activity.status_retweeted_by_user_key = status.retweeted_by_user_key;
            } else if (status.is_quote) {
                activity.status_quote_spans = status.quoted_spans;
                activity.status_quote_text_plain = status.quoted_text_plain;
                activity.status_quote_source = status.quoted_source;
                activity.status_quoted_user_key = status.quoted_user_key;
            }
            activity.status_user_key = status.user_key;
            activity.status_user_following = status.user_is_following;
            activity.status_spans = status.spans;
            activity.status_text_plain = status.text_plain;
            activity.status_source = status.source;
        }
        ParcelableActivityValuesCreator.writeTo(activity, values);
        return values;
    }

    public static ContentValues[] createTrends(final List<Trends> trendsList) {
        if (trendsList == null) return new ContentValues[0];
        final List<ContentValues> resultList = new ArrayList<>();
        for (final Trends trends : trendsList) {
//            final long timestamp = trends.getAsOf().getTime();
            for (final Trend trend : trends.getTrends()) {
                final ContentValues values = new ContentValues();
                values.put(CachedTrends.NAME, trend.getName());
                values.put(CachedTrends.TIMESTAMP, System.currentTimeMillis());
                resultList.add(values);
            }
        }
        return resultList.toArray(new ContentValues[resultList.size()]);
    }


}