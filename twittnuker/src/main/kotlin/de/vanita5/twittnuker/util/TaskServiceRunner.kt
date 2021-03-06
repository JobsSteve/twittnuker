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

package de.vanita5.twittnuker.util

import android.content.Context
import android.support.annotation.StringDef
import com.squareup.otto.Bus
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.kpreferences.KPreferences
import de.vanita5.twittnuker.constant.IntentConstants.INTENT_PACKAGE_PREFIX
import de.vanita5.twittnuker.constant.dataSyncProviderInfoKey
import de.vanita5.twittnuker.constant.stopAutoRefreshWhenBatteryLowKey
import de.vanita5.twittnuker.model.AccountPreferences
import de.vanita5.twittnuker.model.SimpleRefreshTaskParam
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.provider.TwidereDataStore.*
import de.vanita5.twittnuker.task.GetActivitiesAboutMeTask
import de.vanita5.twittnuker.task.GetHomeTimelineTask
import de.vanita5.twittnuker.task.GetReceivedDirectMessagesTask
import de.vanita5.twittnuker.task.filter.RefreshFiltersSubscriptionsTask


class TaskServiceRunner(
        val context: Context,
        val preferences: KPreferences,
        val bus: Bus
) {

    fun runTask(@Action action: String, callback: (Boolean) -> Unit): Boolean {
        when (action) {
            ACTION_REFRESH_HOME_TIMELINE, ACTION_REFRESH_NOTIFICATIONS,
            ACTION_REFRESH_DIRECT_MESSAGES, ACTION_REFRESH_FILTERS_SUBSCRIPTIONS -> {
                val task = createRefreshTask(action) ?: return false
                task.callback = callback
                TaskStarter.execute(task)
                return true
            }
            ACTION_SYNC_DRAFTS, ACTION_SYNC_FILTERS, ACTION_SYNC_USER_COLORS -> {
                val runner = preferences[dataSyncProviderInfoKey]?.newSyncTaskRunner(context) ?: return false
                return runner.runTask(action, callback)
            }
        }
        return false
    }

    fun createRefreshTask(@Action action: String): AbstractTask<*, *, (Boolean) -> Unit>? {
        if (!Utils.isBatteryOkay(context) && preferences[stopAutoRefreshWhenBatteryLowKey]) {
            // Low battery, don't refresh
            return null
        }
        when (action) {
            ACTION_REFRESH_HOME_TIMELINE -> {
                val task = GetHomeTimelineTask(context)
                task.params = AutoRefreshTaskParam(context, AccountPreferences::isAutoRefreshHomeTimelineEnabled) { accountKeys ->
                    DataStoreUtils.getNewestStatusIds(context, Statuses.CONTENT_URI, accountKeys)
                }
                return task
            }
            ACTION_REFRESH_NOTIFICATIONS -> {
                val task = GetActivitiesAboutMeTask(context)
                task.params = AutoRefreshTaskParam(context, AccountPreferences::isAutoRefreshMentionsEnabled) { accountKeys ->
                    DataStoreUtils.getNewestActivityMaxPositions(context, Activities.AboutMe.CONTENT_URI, accountKeys)
                }
                return task
            }
            ACTION_REFRESH_DIRECT_MESSAGES -> {
                val task = GetReceivedDirectMessagesTask(context)
                task.params = AutoRefreshTaskParam(context, AccountPreferences::isAutoRefreshDirectMessagesEnabled) { accountKeys ->
                    DataStoreUtils.getNewestMessageIds(context, DirectMessages.Inbox.CONTENT_URI, accountKeys)
                }
                return task
            }
            ACTION_REFRESH_FILTERS_SUBSCRIPTIONS -> {
                return RefreshFiltersSubscriptionsTask(context)
            }
        }
        return null
    }

    @StringDef(ACTION_REFRESH_HOME_TIMELINE, ACTION_REFRESH_NOTIFICATIONS, ACTION_REFRESH_DIRECT_MESSAGES,
            ACTION_REFRESH_FILTERS_SUBSCRIPTIONS, ACTION_SYNC_DRAFTS, ACTION_SYNC_FILTERS,
            ACTION_SYNC_USER_COLORS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Action

    class AutoRefreshTaskParam(
            val context: Context,
            val refreshable: (AccountPreferences) -> Boolean,
            val getSinceIds: (Array<UserKey>) -> Array<String?>?
    ) : SimpleRefreshTaskParam() {
        override fun getAccountKeysWorker(): Array<UserKey> {
            val prefs = AccountPreferences.getAccountPreferences(context,
                    DataStoreUtils.getAccountKeys(context)).filter(AccountPreferences::isAutoRefreshEnabled)
            return prefs.filter(refreshable)
                    .map(AccountPreferences::getAccountKey).toTypedArray()
        }

        override val sinceIds: Array<String?>?
            get() = getSinceIds(accountKeys)

    }

    companion object {
        @Action
        const val ACTION_REFRESH_HOME_TIMELINE = INTENT_PACKAGE_PREFIX + "REFRESH_HOME_TIMELINE"
        @Action
        const val ACTION_REFRESH_NOTIFICATIONS = INTENT_PACKAGE_PREFIX + "REFRESH_NOTIFICATIONS"
        @Action
        const val ACTION_REFRESH_DIRECT_MESSAGES = INTENT_PACKAGE_PREFIX + "REFRESH_DIRECT_MESSAGES"
        @Action
        const val ACTION_REFRESH_FILTERS_SUBSCRIPTIONS = INTENT_PACKAGE_PREFIX + "REFRESH_FILTERS_SUBSCRIPTIONS"
        @Action
        const val ACTION_SYNC_DRAFTS = INTENT_PACKAGE_PREFIX + "SYNC_DRAFTS"
        @Action
        const val ACTION_SYNC_FILTERS = INTENT_PACKAGE_PREFIX + "SYNC_FILTERS"
        @Action
        const val ACTION_SYNC_USER_COLORS = INTENT_PACKAGE_PREFIX + "SYNC_USER_COLORS"

        val ACTIONS_SYNC = arrayOf(ACTION_SYNC_DRAFTS, ACTION_SYNC_FILTERS, ACTION_SYNC_USER_COLORS)
    }

    data class SyncFinishedEvent(val syncType: String, val success: Boolean)

}
