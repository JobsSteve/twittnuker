/*
 *  Twittnuker - Twitter client for Android
 *
 *  Copyright (C) 2013-2016 vanita5 <mail@vanit.as>
 *
 *  This program incorporates a modified version of Twidere.
 *  Copyright (C) 2012-2016 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vanita5.twittnuker.model.tab.extra;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bluelinelabs.logansquare.LoganSquare;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import de.vanita5.twittnuker.annotation.CustomTabType;

import java.io.IOException;

@JsonObject
public abstract class TabExtras implements Parcelable {
    @CallSuper
    public void copyToBundle(Bundle bundle) {

    }

    @Nullable
    public static TabExtras parse(@NonNull @CustomTabType String type, @Nullable String json) throws IOException {
        if (json == null) return null;
        switch (type) {
            case CustomTabType.NOTIFICATIONS_TIMELINE: {
                return LoganSquare.parse(json, InteractionsTabExtras.class);
            }
            case CustomTabType.HOME_TIMELINE: {
                return LoganSquare.parse(json, HomeTabExtras.class);
            }
        }
        return null;
    }
}