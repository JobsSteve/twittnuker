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

package de.vanita5.twittnuker.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;
import com.hannesdorfmann.parcelableplease.annotation.ParcelableThisPlease;

import java.util.Arrays;

@ParcelablePlease
@JsonObject
public class MediaUploadResult implements Parcelable {

    @ParcelableThisPlease
    @JsonField(name = "media_uris")
    public String[] media_uris;

    @ParcelableThisPlease
    @JsonField(name = "error_code")
    public int error_code;

    @ParcelableThisPlease
    @JsonField(name = "error_message")
    public String error_message;

    @ParcelableThisPlease
    @JsonField(name = "extras")
    public String extras;

    @ParcelableThisPlease
    @JsonField(name = "shared_owners")
    public UserKey[] shared_owners;

    public static final Creator<MediaUploadResult> CREATOR = new Creator<MediaUploadResult>() {
        @Override
        public MediaUploadResult createFromParcel(Parcel source) {
            MediaUploadResult target = new MediaUploadResult();
            MediaUploadResultParcelablePlease.readFromParcel(target, source);
            return target;
        }

        @Override
        public MediaUploadResult[] newArray(int size) {
            return new MediaUploadResult[size];
        }
    };

    MediaUploadResult() {
    }

    public MediaUploadResult(final int errorCode, final String errorMessage) {
        if (errorCode == 0) throw new IllegalArgumentException("Error code must not be 0");
        media_uris = null;
        error_code = errorCode;
        error_message = errorMessage;
    }

    public MediaUploadResult(final String[] mediaUris) {
        if (mediaUris == null) throw new IllegalArgumentException("Media uris must not be null");
        media_uris = mediaUris;
        error_code = 0;
        error_message = null;
    }

    @Override
    public String toString() {
        return "MediaUploadResult{media_uris=" + Arrays.toString(media_uris) + ", error_code=" + error_code
                + ", error_message=" + error_message + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        MediaUploadResultParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static MediaUploadResult error(final int errorCode, final String errorMessage) {
        return new MediaUploadResult(errorCode, errorMessage);
    }

    public static MediaUploadResult uploaded(final String... mediaUris) {
        return new MediaUploadResult(mediaUris);
    }
}