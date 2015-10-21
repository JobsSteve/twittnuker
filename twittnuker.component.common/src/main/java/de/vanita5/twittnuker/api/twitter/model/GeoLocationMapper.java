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

package de.vanita5.twittnuker.api.twitter.model;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import de.vanita5.twittnuker.api.twitter.model.impl.GeoPoint;

public class GeoLocationMapper extends JsonMapper<GeoLocation> {
    @Override
    public GeoLocation parse(JsonParser jsonParser) throws IOException {
        final GeoPoint geoPoint = LoganSquare.mapperFor(GeoPoint.class).parse(jsonParser);
        if (geoPoint == null) return null;
        return geoPoint.getGeoLocation();
    }

    @Override
    public void serialize(GeoLocation object, JsonGenerator generator, boolean writeStartAndEnd) throws IOException {
        throw new UnsupportedOperationException();
    }
}