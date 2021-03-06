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

import android.graphics.Color;

import de.vanita5.twittnuker.TwittnukerConstants;

import static android.text.TextUtils.isEmpty;

public final class ParseUtils implements TwittnukerConstants {

    private ParseUtils() {
    }

    public static String parseString(final String object) {
        return object;
    }

    public static String parseString(final Object object) {
        return parseString(object, null);
    }

    public static String parseString(final int object) {
        return String.valueOf(object);
    }

    public static String parseString(final long object) {
        return String.valueOf(object);
    }

    public static String parseString(final Object object, final String def) {
        if (object == null) return def;
        return String.valueOf(object);
    }

    public static int parseColor(String str, int def) {
        if (isEmpty(str)) return def;
        try {
            return Color.parseColor(str);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

}