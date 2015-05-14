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

package de.vanita5.twittnuker.api.twitter.model.impl;

import android.support.annotation.NonNull;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import de.vanita5.twittnuker.api.twitter.model.Activity;
import de.vanita5.twittnuker.api.twitter.model.Status;
import de.vanita5.twittnuker.api.twitter.model.User;
import de.vanita5.twittnuker.api.twitter.model.UserList;

public class ActivityImpl extends TwitterResponseImpl implements Activity {

	public static final JsonMapper<Activity> MAPPER = new JsonMapper<Activity>() {
		@SuppressWarnings("TryWithIdenticalCatches")
		@Override
		public Activity parse(JsonParser jsonParser) throws IOException {
			ActivityImpl instance = new ActivityImpl();
			if (jsonParser.getCurrentToken() == null) {
				jsonParser.nextToken();
			}
			if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
				jsonParser.skipChildren();
				return null;
			}
			while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = jsonParser.getCurrentName();
				jsonParser.nextToken();
				parseField(instance, fieldName, jsonParser);
				jsonParser.skipChildren();
			}
			return instance;
		}

		@Override
		public void serialize(Activity activity, JsonGenerator jsonGenerator, boolean writeStartAndEnd) {
			throw new UnsupportedOperationException();
		}

		public void parseField(ActivityImpl instance, String fieldName, JsonParser jsonParser) throws IOException {
			if ("action".equals(fieldName)) {
				instance.action = Action.parse(jsonParser.getValueAsString());
			} else if ("created_at".equals(fieldName)) {
				try {
					instance.createdAt = DATE_FORMAT.parse(jsonParser.getValueAsString());
				} catch (ParseException e) {
					throw new IOException(e);
				}
			} else if ("min_position".equals(fieldName)) {
				instance.minPosition = jsonParser.getValueAsLong();
			} else if ("max_position".equals(fieldName)) {
				instance.maxPosition = jsonParser.getValueAsLong();
			} else if ("sources_size".equals(fieldName)) {
				instance.sourcesSize = jsonParser.getValueAsInt();
			} else if ("targets_size".equals(fieldName)) {
				instance.targetsSize = jsonParser.getValueAsInt();
			} else if ("target_objects_size".equals(fieldName)) {
				instance.targetObjectsSize = jsonParser.getValueAsInt();
			} else if ("sources".equals(fieldName)) {
				instance.sources = LoganSquare.mapperFor(User.class).parseList(jsonParser).toArray(new User[instance.sourcesSize]);
			} else if ("targets".equals(fieldName)) {
				if (instance.action == null) throw new IOException();
				if (instance.action == Action.FOLLOW || instance.action == Action.MENTION || instance.action == Action.LIST_MEMBER_ADDED) {
					instance.targetUsers = LoganSquare.mapperFor(User.class).parseList(jsonParser).toArray(new User[instance.targetsSize]);
				} else if (instance.action == Action.LIST_CREATED) {
					instance.targetUserLists = LoganSquare.mapperFor(UserList.class).parseList(jsonParser).toArray(new UserList[instance.targetsSize]);
				} else {
					instance.targetStatuses = LoganSquare.mapperFor(Status.class).parseList(jsonParser).toArray(new Status[instance.targetsSize]);
				}
			} else if ("target_objects".equals(fieldName)) {
				if (instance.action == null) throw new IOException();
				if (instance.action == Action.LIST_MEMBER_ADDED) {
					instance.targetObjectUserLists = LoganSquare.mapperFor(UserList.class).parseList(jsonParser).toArray(new UserList[instance.targetObjectsSize]);
				} else {
					instance.targetObjectStatuses = LoganSquare.mapperFor(Status.class).parseList(jsonParser).toArray(new Status[instance.targetObjectsSize]);
				}
			}
		}
	};

	private Action action;

	private Date createdAt;

	private User[] sources, targetUsers;

	private Status[] targetObjectStatuses, targetStatuses;

	private UserList[] targetUserLists, targetObjectUserLists;

	private long maxPosition, minPosition;

	private int targetObjectsSize, targetsSize, sourcesSize;

	ActivityImpl() {
	}

	@Override
	public int compareTo(@NonNull final Activity another) {
		final Date thisDate = getCreatedAt(), thatDate = another.getCreatedAt();
		if (thisDate == null || thatDate == null) return 0;
		return thisDate.compareTo(thatDate);
	}

	@Override
	public Action getAction() {
		return action;
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public long getMaxPosition() {
		return maxPosition;
	}

	@Override
	public long getMinPosition() {
		return minPosition;
	}

	@Override
	public User[] getSources() {
		return sources;
	}

	@Override
	public int getSourcesSize() {
		return sourcesSize;
	}

	@Override
	public int getTargetObjectsSize() {
		return targetObjectsSize;
	}

	@Override
	public Status[] getTargetObjectStatuses() {
		return targetObjectStatuses;
	}

	@Override
	public UserList[] getTargetObjectUserLists() {
		return targetObjectUserLists;
	}

	@Override
	public int getTargetsSize() {
		return targetsSize;
	}

	@Override
	public Status[] getTargetStatuses() {
		return targetStatuses;
	}

	@Override
	public UserList[] getTargetUserLists() {
		return targetUserLists;
	}

	@Override
	public User[] getTargetUsers() {
		return targetUsers;
	}

	@Override
	public String toString() {
		return "ActivityJSONImpl{" +
				"action=" + action +
				", createdAt=" + createdAt +
				", sources=" + Arrays.toString(sources) +
				", targetUsers=" + Arrays.toString(targetUsers) +
				", targetObjectStatuses=" + Arrays.toString(targetObjectStatuses) +
				", targetStatuses=" + Arrays.toString(targetStatuses) +
				", targetUserLists=" + Arrays.toString(targetUserLists) +
				", targetObjectUserLists=" + Arrays.toString(targetObjectUserLists) +
				", maxPosition=" + maxPosition +
				", minPosition=" + minPosition +
				", targetObjectsSize=" + targetObjectsSize +
				", targetsSize=" + targetsSize +
				", sourcesSize=" + sourcesSize +
				'}';
	}

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

}