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

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.database.Cursor
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.ViewCompat
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v7.app.AlertDialog
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.*
import android.widget.AbsListView
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AutoCompleteTextView
import android.widget.ListView
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_content_listview.*
import org.mariotaku.ktextension.setGroupAvailability
import org.mariotaku.sqliteqb.library.Columns
import org.mariotaku.sqliteqb.library.Expression
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.TwittnukerConstants.EXTRA_URI
import de.vanita5.twittnuker.activity.iface.IControlBarActivity
import de.vanita5.twittnuker.adapter.ComposeAutoCompleteAdapter
import de.vanita5.twittnuker.adapter.SourceAutoCompleteAdapter
import de.vanita5.twittnuker.extension.invertSelection
import de.vanita5.twittnuker.extension.selectAll
import de.vanita5.twittnuker.extension.selectNone
import de.vanita5.twittnuker.extension.updateSelectionItems
import de.vanita5.twittnuker.fragment.AbsContentListViewFragment
import de.vanita5.twittnuker.fragment.BaseDialogFragment
import de.vanita5.twittnuker.model.`FiltersData$BaseItemCursorIndices`
import de.vanita5.twittnuker.provider.TwidereDataStore.Filters
import de.vanita5.twittnuker.text.style.EmojiSpan
import de.vanita5.twittnuker.util.ParseUtils
import de.vanita5.twittnuker.util.ThemeUtils
import de.vanita5.twittnuker.util.Utils


abstract class BaseFiltersFragment : AbsContentListViewFragment<SimpleCursorAdapter>(),
        LoaderManager.LoaderCallbacks<Cursor?>, MultiChoiceModeListener {

    override var refreshing: Boolean
        get() = false
        set(value) {
            super.refreshing = value
        }

    private var actionMode: ActionMode? = null

    protected abstract val contentUri: Uri
    protected abstract val contentColumns: Array<String>
    protected open val sortOrder: String? = "${Filters.SOURCE} >= 0"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(this)
        loaderManager.initLoader(0, null, this)
        setRefreshEnabled(false)
        showProgress()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser && actionMode != null) {
            actionMode!!.finish()
        }
    }

    override fun setControlVisible(visible: Boolean) {
        super.setControlVisible(visible || !isQuickReturnEnabled)
    }

    private val isQuickReturnEnabled: Boolean
        get() = actionMode == null

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        setControlVisible(true)
        mode.menuInflater.inflate(R.menu.action_multi_select_items, menu)
        menu.setGroupAvailability(R.id.selection_group, true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        updateTitle(mode)
        listView.updateSelectionItems(menu)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                performDeletion()
                mode.finish()
            }
            R.id.select_all -> {
                listView.selectAll()
            }
            R.id.select_none -> {
                listView.selectNone()
            }
            R.id.invert_selection -> {
                listView.invertSelection()
            }
            else -> {
                return false
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long,
                                           checked: Boolean) {
        val adapter = this.adapter
        if (adapter is SelectableItemAdapter) {
            if (!adapter.isSelectable(position) && checked) {
                listView.setItemChecked(position, false)
            }
        }
        updateTitle(mode)
        mode.invalidate()
    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
        if (ViewCompat.isLaidOut(view)) {
            val childCount = view.childCount
            if (childCount > 0) {
                val firstChild = view.getChildAt(0)
                val activity = activity
                var controlBarHeight = 0
                if (activity is IControlBarActivity) {
                    controlBarHeight = activity.controlBarHeight
                }
                val visible = firstChild.top > controlBarHeight
                setControlVisible(visible)
            } else {
                setControlVisible(true)
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        return CursorLoader(activity, contentUri, contentColumns, null, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        adapter.swapCursor(data)
        if (data != null && data.count > 0) {
            showContent()
        } else {
            showEmpty(R.drawable.ic_info_volume_off, getString(R.string.no_rule))
        }
        actionMode?.invalidate()
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        adapter.swapCursor(null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_filters, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                val args = Bundle()
                args.putParcelable(EXTRA_URI, contentUri)
                val dialog = AddItemFragment()
                dialog.arguments = args
                dialog.show(fragmentManager, "add_rule")
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateAdapter(context: Context): SimpleCursorAdapter {
        return FilterListAdapter(context)
    }

    protected open fun performDeletion() {
        val ids = listView.checkedItemIds
        val where = Expression.inArgs(Columns.Column(Filters._ID), ids.size)
        context.contentResolver.delete(contentUri, where.sql, Array(ids.size) { ids[it].toString() })
    }


    private fun updateTitle(mode: ActionMode?) {
        if (listView == null || mode == null || activity == null) return
        val count = listView!!.checkedItemCount
        mode.title = resources.getQuantityString(R.plurals.Nitems_selected, count, count)
    }

    class AddItemFragment : BaseDialogFragment(), OnClickListener {

        override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val text = text
                    if (TextUtils.isEmpty(text)) return
                    val values = ContentValues()
                    values.put(Filters.VALUE, text)
                    val uri: Uri = arguments.getParcelable(EXTRA_URI)
                    context.contentResolver.insert(uri, values)
                }
            }

        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            val context = activity
            val builder = AlertDialog.Builder(context)
            builder.setView(R.layout.dialog_auto_complete_textview)

            builder.setTitle(R.string.action_add_filter_rule)
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setNegativeButton(android.R.string.cancel, this)
            val dialog = builder.create()
            dialog.setOnShowListener { dialog ->
                val alertDialog = dialog as AlertDialog
                val editText = (alertDialog.findViewById(R.id.edit_text) as AutoCompleteTextView?)!!
                val args = arguments
                val autoCompleteType: Int
                autoCompleteType = args.getInt(EXTRA_AUTO_COMPLETE_TYPE, 0)
                if (autoCompleteType != 0) {
                    val userAutoCompleteAdapter: SimpleCursorAdapter
                    if (autoCompleteType == AUTO_COMPLETE_TYPE_SOURCES) {
                        userAutoCompleteAdapter = SourceAutoCompleteAdapter(activity)
                    } else {
                        val adapter = ComposeAutoCompleteAdapter(activity)
                        adapter.accountKey = Utils.getDefaultAccountKey(activity)
                        userAutoCompleteAdapter = adapter
                    }
                    editText.setAdapter(userAutoCompleteAdapter)
                    editText.threshold = 1
                }
            }
            return dialog
        }

        private val text: String
            get() {
                val alertDialog = dialog as AlertDialog
                val editText = (alertDialog.findViewById(R.id.edit_text) as AutoCompleteTextView?)!!
                return ParseUtils.parseString(editText.text)
            }

    }

    interface SelectableItemAdapter {
        fun isSelectable(position: Int): Boolean
    }


    private class FilterListAdapter(
            context: Context
    ) : SimpleCursorAdapter(context, R.layout.simple_list_item_activated_1, null,
            emptyArray(), intArrayOf(), 0), SelectableItemAdapter {

        private var indices: `FiltersData$BaseItemCursorIndices`? = null
        private val secondaryTextColor = ThemeUtils.getTextColorSecondary(context)

        override fun swapCursor(c: Cursor?): Cursor? {
            val old = super.swapCursor(c)
            if (c != null) {
                indices = `FiltersData$BaseItemCursorIndices`(c)
            }
            return old
        }

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            super.bindView(view, context, cursor)
            val indices = this.indices!!
            val text1 = view.findViewById(android.R.id.text1) as TextView

            val ssb = SpannableStringBuilder(cursor.getString(indices.value))
            if (cursor.getLong(indices.source) >= 0) {
                val start = ssb.length
                ssb.append("*")
                val end = start + 1
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_action_sync)
                drawable.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_ATOP)
                ssb.setSpan(EmojiSpan(drawable), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            text1.text = ssb
        }


        override fun isSelectable(position: Int): Boolean {
            val cursor = this.cursor ?: return false
            if (cursor.moveToPosition(position)) {
                return cursor.getLong(indices!!.source) < 0
            }
            return false
        }
    }

    companion object {

        internal const val EXTRA_AUTO_COMPLETE_TYPE = "auto_complete_type"
        internal const val AUTO_COMPLETE_TYPE_SOURCES = 2
        internal const val REQUEST_ADD_USER_SELECT_ACCOUNT = 201
        internal const val REQUEST_IMPORT_BLOCKS_SELECT_ACCOUNT = 202
        internal const val REQUEST_IMPORT_MUTES_SELECT_ACCOUNT = 203

    }
}