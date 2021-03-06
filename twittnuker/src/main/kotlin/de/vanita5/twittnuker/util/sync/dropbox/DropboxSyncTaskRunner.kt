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

package de.vanita5.twittnuker.util.sync.dropbox

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.util.TaskServiceRunner
import de.vanita5.twittnuker.util.sync.ISyncAction
import de.vanita5.twittnuker.util.sync.SyncTaskRunner
import de.vanita5.twittnuker.util.sync.UserColorsSyncProcessor


class DropboxSyncTaskRunner(context: Context, val authToken: String) : SyncTaskRunner(context) {

    override fun onRunningTask(action: String, callback: (Boolean) -> Unit): Boolean {
        val requestConfig = DbxRequestConfig.newBuilder("twittnuker-android/${BuildConfig.VERSION_NAME}")
                .build()
        val client = DbxClientV2(requestConfig, authToken)
        val syncAction: ISyncAction = when (action) {
            TaskServiceRunner.ACTION_SYNC_DRAFTS -> DropboxDraftsSyncAction(context, client)
            TaskServiceRunner.ACTION_SYNC_FILTERS -> DropboxFiltersDataSyncAction(context, client)
            TaskServiceRunner.ACTION_SYNC_USER_COLORS -> DropboxPreferencesValuesSyncAction(context,
                    client, userColorNameManager.colorPreferences, UserColorsSyncProcessor,
                    "/Common/user_colors.xml")
            else -> null
        } ?: return false
        task {
            syncAction.execute()
        }.successUi {
            callback(true)
        }.failUi {
            callback(false)
        }
        return true
    }

}