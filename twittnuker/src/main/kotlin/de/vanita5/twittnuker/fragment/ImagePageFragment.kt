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

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import org.mariotaku.mediaviewer.library.CacheDownloadLoader
import org.mariotaku.mediaviewer.library.subsampleimageview.SubsampleImageViewerFragment
import de.vanita5.twittnuker.TwittnukerConstants.*
import de.vanita5.twittnuker.model.ParcelableMedia
import de.vanita5.twittnuker.model.UserKey
import de.vanita5.twittnuker.util.TwidereMathUtils
import de.vanita5.twittnuker.util.UriUtils
import de.vanita5.twittnuker.util.media.MediaExtra
import java.io.IOException

class ImagePageFragment : SubsampleImageViewerFragment() {

    private var mediaLoadState: Int = 0
    private val sizedResultCreator: CacheDownloadLoader.ResultCreator by lazy { SizedResultCreator(context) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val activity = activity
        if (isVisibleToUser && activity != null) {
            activity.supportInvalidateOptionsMenu()
        }
    }

    override fun getDownloadExtra(): Any? {
        val mediaExtra = MediaExtra()
        mediaExtra.accountKey = accountKey
        val origDownloadUri = super.getDownloadUri()
        val downloadUri = downloadUri
        if (origDownloadUri != null && downloadUri != null) {
            val fallbackUrl = origDownloadUri.toString()
            mediaExtra.fallbackUrl = fallbackUrl
            mediaExtra.isSkipUrlReplacing = fallbackUrl != downloadUri.toString()
        }
        return mediaExtra
    }

    override fun hasDownloadedData(): Boolean {
        return super.hasDownloadedData() && mediaLoadState != State.ERROR
    }

    override fun onMediaLoadStateChange(@State state: Int) {
        mediaLoadState = state
        val activity = activity
        if (userVisibleHint && activity != null) {
            activity.supportInvalidateOptionsMenu()
        }
    }

    override fun setupImageView(imageView: SubsamplingScaleImageView) {
        imageView.maxScale = resources.displayMetrics.density
        imageView.setBitmapDecoderClass(PreviewBitmapDecoder::class.java)
        imageView.setParallelLoadingEnabled(true)
    }

    override fun getImageSource(data: CacheDownloadLoader.Result): ImageSource {
        assert(data.cacheUri != null)
        if (data !is SizedResult) {
            return super.getImageSource(data)
        }
        val imageSource = ImageSource.uri(data.cacheUri!!)
        imageSource.tilingEnabled()
        imageSource.dimensions(data.width, data.height)
        return imageSource
    }

    override fun getPreviewImageSource(data: CacheDownloadLoader.Result): ImageSource? {
        if (data !is SizedResult) return null
        assert(data.cacheUri != null)
        return ImageSource.uri(UriUtils.appendQueryParameters(data.cacheUri, QUERY_PARAM_PREVIEW, true))
    }

    override fun getResultCreator(): CacheDownloadLoader.ResultCreator? {
        return sizedResultCreator
    }

    private val media: ParcelableMedia
        get() = arguments.getParcelable<ParcelableMedia>(EXTRA_MEDIA)

    private val accountKey: UserKey
        get() = arguments.getParcelable<UserKey>(EXTRA_ACCOUNT_KEY)

    internal class SizedResult(cacheUri: Uri, val width: Int, val height: Int) : CacheDownloadLoader.Result(cacheUri, null)

    internal class SizedResultCreator(private val context: Context) : CacheDownloadLoader.ResultCreator {

        override fun create(uri: Uri): CacheDownloadLoader.Result {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            try {
                decodeBitmap(context.contentResolver, uri, o)
            } catch (e: IOException) {
                return CacheDownloadLoader.Result.getInstance(uri)
            }

            if (o.outWidth > 0 && o.outHeight > 0) {
                return SizedResult(uri, o.outWidth, o.outHeight)
            }
            return CacheDownloadLoader.Result.getInstance(uri)
        }

    }

    class PreviewBitmapDecoder : SkiaImageDecoder() {
        @Throws(Exception::class)
        override fun decode(context: Context, uri: Uri): Bitmap {
            if (AUTHORITY_TWITTNUKER_CACHE == uri.authority && uri.getBooleanQueryParameter(QUERY_PARAM_PREVIEW, false)) {
                val o = BitmapFactory.Options()
                o.inJustDecodeBounds = true
                o.inPreferredConfig = Bitmap.Config.RGB_565
                val cr = context.contentResolver
                decodeBitmap(cr, uri, o)
                val dm = context.resources.displayMetrics
                val targetSize = Math.min(1024, Math.max(dm.widthPixels, dm.heightPixels))
                val sizeRatio = Math.ceil(Math.max(o.outHeight, o.outWidth) / targetSize.toDouble())
                o.inSampleSize = TwidereMathUtils.nextPowerOf2(Math.max(1.0, sizeRatio).toInt())
                o.inJustDecodeBounds = false
                val bitmap = decodeBitmap(cr, uri, o) ?: throw IOException()
                return bitmap
            }
            return super.decode(context, uri)
        }

    }

    companion object {

        @Throws(IOException::class)
        internal fun decodeBitmap(cr: ContentResolver, uri: Uri, o: BitmapFactory.Options): Bitmap? {
            cr.openInputStream(uri).use {
                return BitmapFactory.decodeStream(it, null, o)
            }
        }

    }
}