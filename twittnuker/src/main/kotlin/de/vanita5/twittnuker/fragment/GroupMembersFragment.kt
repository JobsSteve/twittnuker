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

package de.vanita5.twittnuker.fragment

import android.content.Context
import android.os.Bundle
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.loader.CursorSupportUsersLoader
import de.vanita5.twittnuker.loader.GroupMembersLoader
import de.vanita5.twittnuker.model.UserKey

class GroupMembersFragment : CursorUsersListFragment() {

    public override fun onCreateUsersLoader(context: Context,
                                            args: Bundle, fromUser: Boolean): CursorSupportUsersLoader {
        val accountId = args.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)
        val groupId = args.getString(EXTRA_GROUP_ID)
        val groupName = args.getString(EXTRA_GROUP_NAME)
        val loader = GroupMembersLoader(context, accountId, groupId,
                groupName, adapter.getData(), fromUser)
        loader.cursor = nextCursor
        loader.page = nextPage
        return loader
    }
}