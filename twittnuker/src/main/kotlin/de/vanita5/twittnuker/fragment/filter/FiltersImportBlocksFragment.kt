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

package de.vanita5.twittnuker.fragment.filter

import android.content.Context
import android.os.Bundle
import de.vanita5.twittnuker.constant.IntentConstants
import de.vanita5.twittnuker.loader.CursorSupportUsersLoader
import de.vanita5.twittnuker.loader.UserBlocksLoader
import de.vanita5.twittnuker.model.UserKey

class FiltersImportBlocksFragment : BaseFiltersImportFragment() {

    override fun onCreateUsersLoader(context: Context, args: Bundle, fromUser: Boolean):
            CursorSupportUsersLoader {
        val accountKey = args.getParcelable<UserKey>(IntentConstants.EXTRA_ACCOUNT_KEY)
        val loader = UserBlocksLoader(context, accountKey, adapter.data, fromUser)
        loader.cursor = nextCursor
        loader.page = nextPage
        return loader
    }
}