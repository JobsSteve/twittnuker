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
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream


fun tempFileInputStream(context: Context, write: (OutputStream) -> Unit): InputStream {
    val file = File.createTempFile("twittnuker__temp_is_file", "tmp", context.cacheDir)
    file.outputStream().use { write(it) }
    return TempFileInputStream(file)
}

internal class TempFileInputStream(val file: File) : FileInputStream(file) {
    override fun close() {
        try {
            super.close()
        } finally {
            file.delete()
        }
    }
}