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

package de.vanita5.twittnuker.model.account.cred;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringDef;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ParcelablePlease
@JsonObject
public class Credentials implements Parcelable {
    @JsonField(name = "api_url_format")
    public String api_url_format;
    @JsonField(name = "no_version_suffix")
    public boolean no_version_suffix;

    @StringDef({Type.OAUTH, Type.XAUTH, Type.BASIC, Type.EMPTY, Type.OAUTH2})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {

        String OAUTH = "oauth";
        String XAUTH = "xauth";
        String BASIC = "basic";
        String EMPTY = "empty";
        String OAUTH2 = "oauth2";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CredentialsParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static final Creator<Credentials> CREATOR = new Creator<Credentials>() {
        public Credentials createFromParcel(Parcel source) {
            Credentials target = new Credentials();
            CredentialsParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public Credentials[] newArray(int size) {
            return new Credentials[size];
        }
    };
}
