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
import android.support.v4.app.hasRunningLoadersSafe
import android.support.v4.content.Loader
import android.text.TextUtils
import com.squareup.otto.Subscribe
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.adapter.ListParcelableStatusesAdapter
import de.vanita5.twittnuker.adapter.iface.ILoadMoreSupportAdapter
import de.vanita5.twittnuker.constant.IntentConstants.*
import de.vanita5.twittnuker.loader.MicroBlogAPIStatusesLoader
import de.vanita5.twittnuker.model.BaseRefreshTaskParam
import de.vanita5.twittnuker.model.ParcelableStatus
import de.vanita5.twittnuker.model.RefreshTaskParam
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.model.message.FavoriteTaskEvent
import de.vanita5.twittnuker.model.message.StatusDestroyedEvent
import de.vanita5.twittnuker.model.message.StatusListChangedEvent
import de.vanita5.twittnuker.model.message.StatusRetweetedEvent
import de.vanita5.twittnuker.util.Utils
import java.util.*

abstract class ParcelableStatusesFragment : AbsStatusesFragment() {

    private var lastId: String? = null
    private var page = 1
    private var pageDelta: Int = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            page = savedInstanceState.getInt(EXTRA_PAGE)
        }
    }

    fun deleteStatus(statusId: String) {
        val list = adapterData ?: return
        val dataToRemove = HashSet<ParcelableStatus>()
        for (i in 0 until list.size) {
            val status = list[i]
            if (status.id == statusId || status.retweet_id == statusId) {
                dataToRemove.add(status)
            } else if (status.my_retweet_id == statusId) {
                status.my_retweet_id = null
                status.retweet_count = status.retweet_count - 1
            }
        }
        if (list is MutableList) {
            list.removeAll(dataToRemove)
        }
        adapterData = list
    }

    override fun getStatuses(param: RefreshTaskParam): Boolean {
        if (!loaderInitialized) return false
        val args = Bundle(arguments)
        val maxIds = param.maxIds
        if (maxIds != null) {
            args.putString(EXTRA_MAX_ID, maxIds[0])
            args.putBoolean(EXTRA_MAKE_GAP, false)
        }
        val sinceIds = param.sinceIds
        if (sinceIds != null) {
            args.putString(EXTRA_SINCE_ID, sinceIds[0])
        }
        args.putBoolean(EXTRA_LOADING_MORE, param.isLoadingMore)
        args.putBoolean(EXTRA_FROM_USER, true)
        if (param is StatusesRefreshTaskParam) {
            if (param.page > 0) {
                args.putInt(EXTRA_PAGE, param.page)
            }
        }
        loaderManager.restartLoader(0, args, this)
        return true
    }

    override fun onStart() {
        super.onStart()
        bus.register(this)
    }

    override fun onStop() {
        bus.unregister(this)
        super.onStop()
    }

    override fun hasMoreData(data: List<ParcelableStatus>?): Boolean {
        if (data == null || data.isEmpty()) return false
        val tmpLastId = lastId
        lastId = data[data.size - 1].id
        return !TextUtils.equals(lastId, tmpLastId)
    }

    override val accountKeys: Array<UserKey>
        get() = Utils.getAccountKeys(context, arguments) ?: emptyArray()

    override fun createMessageBusCallback(): Any {
        return ParcelableStatusesBusCallback()
    }

    override fun onCreateAdapter(context: Context): ListParcelableStatusesAdapter {
        return ListParcelableStatusesAdapter(context)
    }

    override fun onStatusesLoaded(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?) {
        refreshEnabled = true
        refreshing = false
        setLoadMoreIndicatorPosition(ILoadMoreSupportAdapter.NONE)
        if (adapter.itemCount > 0) {
            showContent()
        } else if (loader is MicroBlogAPIStatusesLoader) {
            val e = loader.exception
            if (e != null) {
                showError(R.drawable.ic_info_error_generic, Utils.getErrorMessage(context, e) ?:
                        context.getString(R.string.error_unknown_error))
            } else {
                showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh))
            }
        } else {
            showEmpty(R.drawable.ic_info_refresh, getString(R.string.swipe_down_to_refresh))
        }
    }

    override fun onLoadMoreContents(position: Long) {
        // Only supports load from end, skip START flag
        if (position and ILoadMoreSupportAdapter.START !== 0L || refreshing) return
        super.onLoadMoreContents(position)
        if (position == 0L) return
        // Load the last item
        val idx = adapter.statusStartIndex + adapter.rawStatusCount - 1
        if (idx < 0) return
        val status = adapter.getStatus(idx) ?: return
        val accountKeys = arrayOf(status.account_key)
        val maxIds = arrayOf<String?>(status.id)
        val param = StatusesRefreshTaskParam(accountKeys, maxIds, null, page + pageDelta)
        param.isLoadingMore = true
        getStatuses(param)
    }

    fun replaceStatusStates(status: ParcelableStatus?) {
        if (status == null) return
        val lm = layoutManager
        val rangeStart = Math.max(adapter.statusStartIndex, lm.findFirstVisibleItemPosition())
        val rangeEnd = Math.min(lm.findLastVisibleItemPosition(), adapter.statusStartIndex + adapter.statusCount - 1)
        for (i in rangeStart..rangeEnd) {
            val item = adapter.getStatus(i)
            if (status == item) {
                item.favorite_count = status.favorite_count
                item.retweet_count = status.retweet_count
                item.reply_count = status.reply_count

                item.is_favorite = status.is_favorite
            }
        }
        adapter.notifyItemRangeChanged(rangeStart, rangeEnd)
    }

    override fun triggerRefresh(): Boolean {
        super.triggerRefresh()
        val accountKeys = accountKeys
        if (adapter.statusCount > 0) {
            val firstStatus = adapter.getStatus(0)!!
            val sinceIds = Array(accountKeys.size) {
                return@Array if (firstStatus.account_key == accountKeys[it]) firstStatus.id else null
            }
            getStatuses(BaseRefreshTaskParam(accountKeys, null, sinceIds))
        } else {
            getStatuses(BaseRefreshTaskParam(accountKeys, null, null))
        }
        return true
    }

    override var refreshing: Boolean
        get() {
            if (context == null || isDetached) return false
            return loaderManager.hasRunningLoadersSafe()
        }
        set(value) {
            super.refreshing = value
        }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState!!.putInt(EXTRA_PAGE, page)
    }

    protected open val savedStatusesFileArgs: Array<String>?
        get() = null

    override fun onHasMoreDataChanged(hasMoreData: Boolean) {
        pageDelta = if (hasMoreData) 1 else 0
    }

    private fun updateFavoritedStatus(status: ParcelableStatus) {
        replaceStatusStates(status)
    }

    private fun updateRetweetedStatuses(status: ParcelableStatus?) {
        val data = adapterData
        if (status == null || status.retweet_id == null || data == null) return
        data.forEach { orig ->
            if (orig.account_key == status.account_key && TextUtils.equals(orig.id, status.retweet_id)) {
                orig.my_retweet_id = status.my_retweet_id
                orig.retweet_count = status.retweet_count
            }
        }
        adapterData = data
    }

    protected inner class ParcelableStatusesBusCallback {

        @Subscribe
        fun notifyFavoriteTask(event: FavoriteTaskEvent) {
            if (event.isSucceeded) {
                updateFavoritedStatus(event.status!!)
            }
        }

        @Subscribe
        fun notifyStatusDestroyed(event: StatusDestroyedEvent) {
            deleteStatus(event.status.id)
        }

        @Subscribe
        fun notifyStatusListChanged(event: StatusListChangedEvent) {
            adapter.notifyDataSetChanged()
        }

        @Subscribe
        fun notifyStatusRetweeted(event: StatusRetweetedEvent) {
            updateRetweetedStatuses(event.status)
        }

    }

    protected class StatusesRefreshTaskParam(
            accountKeys: Array<UserKey>,
            maxIds: Array<String?>?,
            sinceIds: Array<String?>?,
            var page: Int = -1
    ) : BaseRefreshTaskParam(accountKeys, maxIds, sinceIds)

}