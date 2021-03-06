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

package de.vanita5.twittnuker.service

import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.util.SimpleArrayMap
import android.text.TextUtils
import android.util.Log
import org.mariotaku.ktextension.addOnAccountsUpdatedListenerSafe
import org.mariotaku.ktextension.removeOnAccountsUpdatedListenerSafe
import android.widget.Toast
import de.vanita5.twittnuker.library.MicroBlogException
import de.vanita5.twittnuker.library.twitter.TwitterUserStream
import de.vanita5.twittnuker.library.twitter.UserStreamCallback
import de.vanita5.twittnuker.library.twitter.model.*
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.BuildConfig
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.activity.HomeActivity
import de.vanita5.twittnuker.constant.IntentConstants
import de.vanita5.twittnuker.constant.SharedPreferenceConstants
import de.vanita5.twittnuker.extension.model.newMicroBlogInstance
import de.vanita5.twittnuker.model.*
import de.vanita5.twittnuker.model.account.cred.OAuthCredentials
import de.vanita5.twittnuker.model.util.AccountUtils
import de.vanita5.twittnuker.model.util.ParcelableActivityUtils
import de.vanita5.twittnuker.model.util.ParcelableStatusUtils
import de.vanita5.twittnuker.provider.TwidereDataStore.*
import de.vanita5.twittnuker.util.*

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset

class StreamingService : Service() {

    private val callbacks = SimpleArrayMap<UserKey, UserStreamCallback>()

    private var notificationManager: NotificationManager? = null

    private var preferences: SharedPreferencesWrapper? = null
    private var twitterWrapper: AsyncTwitterWrapper? = null

    private var accountKeys: Array<UserKey>? = null

    private val mStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            //            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            //                if (BuildConfig.DEBUG) Log.d(Constants.LOGTAG, "StreamingService: Received NETWORK_STATE_CHANGED_ACTION");
            //                NetworkInfo wifi = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            //
            //                if ((wifi.isConnected() && !mNetworkOK) || mPreferences.getBoolean(KEY_STREAMING_ON_MOBILE, false)) {
            //                    mNetworkOK = true;
            //                    Log.d(LOGTAG, "Wifi OK");
            //                    initStreaming();
            //                } else {
            //                    mNetworkOK = false;
            //                    Log.d(LOGTAG, "Wifi BAD");
            //                    clearTwitterInstances();
            //                }
            //            } else
            if (IntentConstants.BROADCAST_REFRESH_STREAMING_SERVICE == action) {
                Log.d(TwittnukerConstants.LOGTAG, "Refresh Streaming Service")
                clearTwitterInstances()
                initStreaming()
            }
        }
    }

    private val accountChangeObserver = OnAccountsUpdateListener {
        if (!TwidereArrayUtils.contentMatch(accountKeys, DataStoreUtils.getActivatedAccountKeys(this@StreamingService))) {
            initStreaming()
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = SharedPreferencesWrapper.getInstance(this, TwittnukerConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "Stream service started.")
        }

        //        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        //        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        //        mNetworkOK = wifi.isConnected();

        val filter = IntentFilter()
        //        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(IntentConstants.BROADCAST_REFRESH_STREAMING_SERVICE)
        registerReceiver(mStateReceiver, filter)

        initStreaming()
        AccountManager.get(this).addOnAccountsUpdatedListenerSafe(accountChangeObserver, updateImmediately = false)
    }

    override fun onDestroy() {
        clearTwitterInstances()
        unregisterReceiver(mStateReceiver)
        AccountManager.get(this).removeOnAccountsUpdatedListenerSafe(accountChangeObserver)
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "Stream service stopped.")
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun clearTwitterInstances() {
        var i = 0
        val j = callbacks.size()
        while (i < j) {
            Thread(ShutdownStreamTwitterRunnable(callbacks.valueAt(i))).start()
            i++
        }
        callbacks.clear()
        updateStreamState()
    }

    private fun initStreaming() {
        //FIXME temporary fix for fc
        Toast.makeText(this, "Streaming is broken. Disabling...", Toast.LENGTH_SHORT).show()
        preferences!!.edit().putBoolean(SharedPreferenceConstants.KEY_STREAMING_ENABLED, false).apply()
        //        if (!mPreferences.getBoolean(KEY_STREAMING_ENABLED, true)) return;
        //
        ////        if (mPreferences.getBoolean(KEY_STREAMING_ON_MOBILE, false)
        ////                || mNetworkOK) {
        //            setTwitterInstances();
        ////        }
    }

    private fun setTwitterInstances(): Boolean {
        val accountsList = AccountUtils.getAllAccountDetails(AccountManager.get(this), true).filter { it.credentials is OAuthCredentials }
        val accountKeys = accountsList.map { it.key }.toTypedArray()
        val activatedPreferences = AccountPreferences.getAccountPreferences(this, accountKeys)
        if (BuildConfig.DEBUG) {
            Log.d(LOGTAG, "Setting up twitter stream instances")
        }
        this.accountKeys = accountKeys
        //        clearTwitterInstances();
        var result = false
        accountsList.forEachIndexed { i, account ->
            val twitter = account.newMicroBlogInstance(context = this, cls = TwitterUserStream::class.java)
            val callback = TwidereUserStreamCallback(this, account, preferences!!)
            callbacks.put(account.key, callback)
            Log.d(LOGTAG, String.format("Stream %s starts...", account.key))
            object : Thread() {
                override fun run() {
                    twitter.getUserStream(callback)
                    Log.d(LOGTAG, String.format("Stream %s disconnected", account.key))
                    callbacks.remove(account.key)
                    updateStreamState()
                }
            }.start()
            result = result or true
        }
        return result
    }

    private fun refreshBefore(mAccountId: Array<UserKey>) {
        if (preferences!!.getBoolean(SharedPreferenceConstants.KEY_REFRESH_BEFORE_STREAMING, true)) {
            twitterWrapper!!.refreshAll(mAccountId)
        }
    }

    private fun updateStreamState() {
        Log.d(LOGTAG, "updateStreamState()")
        if (!preferences!!.getBoolean(SharedPreferenceConstants.KEY_STREAMING_NOTIFICATION, true)) {
            notificationManager!!.cancel(TwittnukerConstants.NOTIFICATION_ID_STREAMING)
            return
        }
        if (callbacks.size() > 0) {
            val intent = Intent(this, HomeActivity::class.java)
            val builder = NotificationCompat.Builder(this)
            builder.setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.ic_stat_twittnuker)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.streaming_service_running))
                    .setTicker(getString(R.string.streaming_service_running))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
            notificationManager!!.notify(NOTIFICATION_ID_STREAMING, builder.build())
        } else {
            notificationManager!!.cancel(NOTIFICATION_ID_STREAMING)
        }
    }

    internal class ShutdownStreamTwitterRunnable(private val callback: UserStreamCallback?) : Runnable {

        override fun run() {
            if (callback == null) return
            Log.d(LOGTAG, "Disconnecting stream")
            callback.disconnect()
        }

    }

    internal class TwidereUserStreamCallback(
            private val context: Context,
            private val account: AccountDetails,
            private val mPreferences: SharedPreferences
    ) : UserStreamCallback() {

        private var statusStreamStarted: Boolean = false
        private val mentionsStreamStarted: Boolean = false

        private val mNotificationHelper: NotificationHelper

        init {
            mNotificationHelper = NotificationHelper(context)
        }

        private fun createNotification(fromUser: String, type: String, msg: String?,
                                       status: ParcelableStatus?, sourceUser: User?) {
            if (mPreferences.getBoolean(SharedPreferenceConstants.KEY_STREAMING_NOTIFICATIONS, true)) {
                val pref = AccountPreferences(context,
                        account.key)
                val notification = NotificationContent()
                notification.accountKey = account.key
                notification.objectId = status?.id
                notification.objectUserKey = status?.user_key
                notification.fromUser = fromUser
                notification.type = type
                notification.message = msg
                notification.timestamp = status?.timestamp ?: System.currentTimeMillis()
                notification.originalStatus = status
                notification.sourceUser = sourceUser
                notification.profileImageUrl = if (sourceUser != null)
                    sourceUser.profileImageUrl
                else
                    status?.user_profile_image_url

                mNotificationHelper.cachePushNotification(notification)
                mNotificationHelper.buildNotificationByType(notification, pref, false)
            }
        }

        override fun onConnected() {

        }

        override fun onBlock(source: User, blockedUser: User) {
            val message = String.format("%s blocked %s", source.screenName, blockedUser.screenName)
            Log.d(LOGTAG, message)
        }

        override fun onDirectMessageDeleted(event: DeletionEvent) {
            val where = Expression.equalsArgs(DirectMessages.MESSAGE_ID).sql
            val whereArgs = arrayOf(event.id)
            for (uri in MESSAGES_URIS) {
                context.contentResolver.delete(uri, where, whereArgs)
            }
        }

        override fun onStatusDeleted(event: DeletionEvent) {
            val statusId = event.id
            context.contentResolver.delete(Statuses.CONTENT_URI, Expression.equalsArgs(Statuses.STATUS_ID).sql,
                    arrayOf(statusId))
            context.contentResolver.delete(Activities.AboutMe.CONTENT_URI, Expression.equalsArgs(Activities.STATUS_ID).sql,
                    arrayOf(statusId))
        }

        @Throws(IOException::class)
        override fun onDirectMessage(directMessage: DirectMessage) {
            if (directMessage.id == null) return

            val resolver = context.contentResolver
            val where = Expression.and(Expression.equalsArgs(DirectMessages.ACCOUNT_KEY),
                    Expression.equalsArgs(DirectMessages.MESSAGE_ID)).sql
            val whereArgs = arrayOf(account.key.toString(), directMessage.id)
            for (uri in MESSAGES_URIS) {
                resolver.delete(uri, where, whereArgs)
            }
            val sender = directMessage.sender
            val recipient = directMessage.recipient
            if (TextUtils.equals(sender.id, account.key.id)) {
                val values = ContentValuesCreator.createDirectMessage(directMessage,
                        account.key, true)
                if (values != null) {
                    resolver.insert(DirectMessages.Outbox.CONTENT_URI, values)
                }
            }
            if (TextUtils.equals(recipient.id, account.key.id)) {
                val values = ContentValuesCreator.createDirectMessage(directMessage,
                        account.key, false)
                val builder = DirectMessages.Inbox.CONTENT_URI.buildUpon()
                builder.appendQueryParameter(QUERY_PARAM_NOTIFY, "true")
                if (values != null) {
                    resolver.insert(builder.build(), values)
                }
            }

        }

        override fun onException(ex: Throwable) {
            if (ex is MicroBlogException) {
                Log.w(LOGTAG, String.format("Error %d", ex.statusCode), ex)
                val response = ex.httpResponse
                if (response != null) {
                    try {
                        val body = response.body
                        if (body != null) {
                            val os = ByteArrayOutputStream()
                            body.writeTo(os)
                            val charsetName: String
                            val contentType = body.contentType()
                            if (contentType != null) {
                                val charset = contentType.charset
                                if (charset != null) {
                                    charsetName = charset.name()
                                } else {
                                    charsetName = Charset.defaultCharset().name()
                                }
                            } else {
                                charsetName = Charset.defaultCharset().name()
                            }
                            Log.w(LOGTAG, os.toString(charsetName))
                        }
                    } catch (e: IOException) {
                        Log.w(LOGTAG, e)
                    }

                }
            } else {
                Log.w(LOGTAG, ex)
            }
        }

        override fun onFavorite(source: User, target: User, favoritedStatus: Status) {
            //            ParcelableActivity activity = new ParcelableActivity();
            //            activity.account_id = account.account_id;
            //            activity.timestamp = favoritedStatus.getCreatedAt() != null ? favoritedStatus.getCreatedAt().getTime() : System.currentTimeMillis();
            //            activity.action = Activity.Action.FAVORITE.literal;
            //            activity.sources = ParcelableUser.fromUsers(new User[]{source}, source.getId());
            //            activity.target_users = ParcelableUser.fromUsers(new User[]{target}, target.getId());
            //            activity.target_statuses = ParcelableStatus.fromStatuses(new Status[]{favoritedStatus}, favoritedStatus.getId());
            //            activity.source_ids = new long[] { source.getId() };
            //            activity.is_gap = false;
            //
            //            ContentValues values = ParcelableActivityValuesCreator.create(activity);
            //            resolver.insert(Activities.AboutMe.CONTENT_URI, values);

            if (TextUtils.equals(favoritedStatus.user.id, account.key.id)) {
                createNotification(source.screenName, NotificationContent.NOTIFICATION_TYPE_FAVORITE,
                        Utils.parseURLEntities(favoritedStatus.extendedText, favoritedStatus.urlEntities),
                        ParcelableStatusUtils.fromStatus(favoritedStatus,
                                account.key, false),
                        source)
            }
        }

        override fun onFollow(source: User, followedUser: User) {
            if (TextUtils.equals(followedUser.id, account.key.id)) {
                createNotification(source.screenName, NotificationContent.NOTIFICATION_TYPE_FOLLOWER,
                        null, null, source)
            }
        }

        override fun onFriendList(friendIds: LongArray) {

        }

        override fun onScrubGeo(userId: Long, upToStatusId: Long) {
            val resolver = context.contentResolver

            val where = Expression.and(Expression.equalsArgs(Statuses.USER_KEY),
                    Expression.greaterEqualsArgs(Statuses.SORT_ID)).sql
            val whereArgs = arrayOf(userId.toString(), upToStatusId.toString())
            val values = ContentValues()
            values.putNull(Statuses.LOCATION)
            resolver.update(Statuses.CONTENT_URI, values, where, whereArgs)
        }

        override fun onStallWarning(warn: Warning) {

        }

        @Throws(IOException::class)
        override fun onStatus(status: Status) {
            val resolver = context.contentResolver

            val values = ContentValuesCreator.createStatus(status, account.key)
            if (!statusStreamStarted && !mPreferences.getBoolean(SharedPreferenceConstants.KEY_REFRESH_BEFORE_STREAMING, true)) {
                statusStreamStarted = true
                values.put(Statuses.IS_GAP, true)
            }
            val where = Expression.and(Expression.equalsArgs(AccountSupportColumns.ACCOUNT_KEY),
                    Expression.equalsArgs(Statuses.STATUS_ID)).sql
            val whereArgs = arrayOf(account.key.toString(), status.id.toString())
            resolver.delete(Statuses.CONTENT_URI, where, whereArgs)
            resolver.delete(Activities.AboutMe.CONTENT_URI, where, whereArgs)
            resolver.insert(Statuses.CONTENT_URI, values)
            val rt = status.retweetedStatus
            if (rt != null && rt.extendedText.contains("@" + account.user.screen_name) ||
                    rt == null && status.extendedText.contains("@" + account.user.screen_name)) {

                val activity = Activity.fromMention(account.key.id, status)
                val parcelableActivity = ParcelableActivityUtils.fromActivity(activity,
                        account.key, false)
                parcelableActivity.timestamp = if (status.createdAt != null) status.createdAt.time else System.currentTimeMillis()
                val activityValues = ParcelableActivityValuesCreator.create(parcelableActivity)
                resolver.insert(Activities.AboutMe.CONTENT_URI, activityValues)
            }

            //Retweet
            if (rt != null && TextUtils.equals(rt.user.id, account.key.id)) {
                createNotification(status.user.screenName,
                        NotificationContent.NOTIFICATION_TYPE_RETWEET,
                        Utils.parseURLEntities(rt.extendedText, rt.urlEntities),
                        ParcelableStatusUtils.fromStatus(status,
                                account.key, false), status.user)
                //TODO insert retweet activity
            }
        }

        override fun onTrackLimitationNotice(numberOfLimitedStatuses: Int) {

        }

        override fun onUnblock(source: User, unblockedUser: User) {
            val message = String.format("%s unblocked %s", source.screenName,
                    unblockedUser.screenName)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        override fun onUnfavorite(source: User, target: User, targetStatus: Status) {
            val message = String.format("%s unfavorited %s's tweet: %s", source.screenName,
                    target.screenName, targetStatus.extendedText)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        override fun onUserListCreation(listOwner: User, list: UserList) {

        }

        override fun onUserListDeletion(listOwner: User, list: UserList) {

        }

        override fun onUserListMemberAddition(addedMember: User, listOwner: User, list: UserList) {

        }

        override fun onUserListMemberDeletion(deletedMember: User, listOwner: User, list: UserList) {

        }

        override fun onUserListSubscription(subscriber: User, listOwner: User, list: UserList) {

        }

        override fun onUserListUnsubscription(subscriber: User, listOwner: User, list: UserList) {

        }

        override fun onUserListUpdate(listOwner: User, list: UserList) {

        }

        override fun onUserProfileUpdate(updatedUser: User) {

        }
    }

    companion object {

        private val STATUSES_URIS = arrayOf(Statuses.CONTENT_URI, Mentions.CONTENT_URI)
        private val MESSAGES_URIS = arrayOf(DirectMessages.Inbox.CONTENT_URI, DirectMessages.Outbox.CONTENT_URI)
    }

}