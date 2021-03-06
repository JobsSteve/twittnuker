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

package de.vanita5.twittnuker.model.util;

import android.support.v4.util.Pair;

import de.vanita5.twittnuker.library.twitter.model.DirectMessage;
import de.vanita5.twittnuker.library.twitter.model.User;
import de.vanita5.twittnuker.model.ParcelableDirectMessage;
import de.vanita5.twittnuker.model.SpanItem;
import de.vanita5.twittnuker.model.UserKey;
import de.vanita5.twittnuker.util.InternalTwitterContentUtils;
import de.vanita5.twittnuker.util.TwitterContentUtils;

import java.util.Date;

public class ParcelableDirectMessageUtils {

    private ParcelableDirectMessageUtils() {
    }

    public static ParcelableDirectMessage fromDirectMessage(DirectMessage message, UserKey accountKey, boolean isOutgoing) {
        ParcelableDirectMessage result = new ParcelableDirectMessage();
        result.account_key = accountKey;
        result.is_outgoing = isOutgoing;
        final User sender = message.getSender(), recipient = message.getRecipient();
        assert sender != null && recipient != null;
        final String sender_profile_image_url = TwitterContentUtils.getProfileImageUrl(sender);
        final String recipient_profile_image_url = TwitterContentUtils.getProfileImageUrl(recipient);
        result.id = message.getId();
        result.timestamp = getTime(message.getCreatedAt());
        result.sender_id = sender.getId();
        result.recipient_id = recipient.getId();
        final Pair<String, SpanItem[]> pair = InternalTwitterContentUtils.formatDirectMessageText(message);
        result.text_unescaped = pair.first;
        result.spans = pair.second;
        result.text_plain = InternalTwitterContentUtils.unescapeTwitterStatusText(message.getText());
        result.sender_name = sender.getName();
        result.recipient_name = recipient.getName();
        result.sender_screen_name = sender.getScreenName();
        result.recipient_screen_name = recipient.getScreenName();
        result.sender_profile_image_url = sender_profile_image_url;
        result.recipient_profile_image_url = recipient_profile_image_url;
        result.media = ParcelableMediaUtils.fromEntities(message);
        if (isOutgoing) {
            result.conversation_id = result.recipient_id;
        } else {
            result.conversation_id = result.sender_id;
        }
        return result;
    }

    private static long getTime(final Date date) {
        return date != null ? date.getTime() : 0;
    }

}