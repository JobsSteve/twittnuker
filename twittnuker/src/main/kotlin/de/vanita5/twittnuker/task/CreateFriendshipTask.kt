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

package de.vanita5.twittnuker.task

import android.content.Context
import android.text.TextUtils
import de.vanita5.twittnuker.Constants
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.USER_TYPE_FANFOU_COM
import de.vanita5.twittnuker.annotation.AccountType
import de.vanita5.twittnuker.constant.nameFirstKey
import de.vanita5.twittnuker.library.MicroBlog
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.model.User
import de.vanita5.twittnuker.model.AccountDetails
import de.vanita5.twittnuker.model.ParcelableUser
import de.vanita5.twittnuker.model.message.FriendshipTaskEvent
import de.vanita5.twittnuker.util.Utils

class CreateFriendshipTask(context: Context) : AbsFriendshipOperationTask(context, FriendshipTaskEvent.Action.FOLLOW), Constants {

    @Throws(MicroBlogException::class)
    override fun perform(twitter: MicroBlog, details: AccountDetails, args: AbsFriendshipOperationTask.Arguments): User {
        when (details.type) {
            AccountType.FANFOU -> {
                return twitter.createFanfouFriendship(args.userKey.id)
            }
        }
        return twitter.createFriendship(args.userKey.id)
    }

    override fun succeededWorker(twitter: MicroBlog, details: AccountDetails, args: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        user.is_following = true
        Utils.setLastSeen(context, user.key, System.currentTimeMillis())
    }

    override fun showErrorMessage(params: AbsFriendshipOperationTask.Arguments, exception: Exception?) {
        if (USER_TYPE_FANFOU_COM == params.accountKey.host) {
            // Fanfou returns 403 for follow request
            if (exception is MicroBlogException) {
                if (exception.statusCode == 403 && !TextUtils.isEmpty(exception.errorMessage)) {
                    Utils.showErrorMessage(context, exception.errorMessage, false)
                    return
                }
            }
        }
        Utils.showErrorMessage(context, R.string.action_following, exception, false)
    }

    override fun showSucceededMessage(params: AbsFriendshipOperationTask.Arguments, user: ParcelableUser) {
        val nameFirst = kPreferences[nameFirstKey]
        val message: String
        if (user.is_protected) {
            message = context.getString(R.string.sent_follow_request_to_user,
                    manager.getDisplayName(user, nameFirst))
        } else {
            message = context.getString(R.string.followed_user,
                    manager.getDisplayName(user, nameFirst))
        }
        Utils.showOkMessage(context, message, false)
    }

}