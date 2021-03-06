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

package de.vanita5.twittnuker.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback
import de.vanita5.twittnuker.model.SupportTabSpec
import de.vanita5.twittnuker.model.tab.DrawableHolder
import de.vanita5.twittnuker.util.CustomTabUtils.getTabIconDrawable
import de.vanita5.twittnuker.util.Utils.announceForAccessibilityCompat
import de.vanita5.twittnuker.view.iface.PagerIndicator
import de.vanita5.twittnuker.view.iface.PagerIndicator.TabListener
import de.vanita5.twittnuker.view.iface.PagerIndicator.TabProvider
import java.util.*

class SupportTabsAdapter @JvmOverloads constructor(
        private val context: Context,
        fm: FragmentManager,
        private val indicator: PagerIndicator? = null
) : SupportFixedFragmentStatePagerAdapter(fm), TabProvider, TabListener {

    var hasMultipleColumns: Boolean = false

    private val tab = ArrayList<SupportTabSpec>()

    init {
        clear()
    }

    fun addTab(cls: Class<out Fragment>, args: Bundle? = null, name: String,
               icon: DrawableHolder? = null, type: String? = null, position: Int = 0, tag: String? = null) {
        addTab(SupportTabSpec(name = name, icon = icon, cls = cls, args = args,
                position = position, type = type, tag = tag))
    }

    fun addTab(spec: SupportTabSpec) {
        tab.add(spec)
        notifyDataSetChanged()
    }

    fun addTabs(specs: Collection<SupportTabSpec>) {
        tab.addAll(specs)
        notifyDataSetChanged()
    }

    fun clear() {
        tab.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return tab.size
    }

    override fun getItemPosition(obj: Any?): Int {
        if (obj !is Fragment) return PagerAdapter.POSITION_NONE
        val args = obj.arguments ?: return PagerAdapter.POSITION_NONE
        return args.getInt(EXTRA_ADAPTER_POSITION, PagerAdapter.POSITION_NONE)
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        indicator?.notifyDataSetChanged()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tab[position].name
    }

    override fun getPageWidth(position: Int): Float {
        val columnCount = count
        if (columnCount == 0) return 1f
        if (hasMultipleColumns) {
            val resources = context.resources
            val screenWidth = resources.displayMetrics.widthPixels
            val preferredColumnWidth = resources.getDimension(R.dimen.preferred_tab_column_width)
            val pageWidth = preferredColumnWidth / screenWidth
            if (columnCount * preferredColumnWidth < screenWidth) {
                return 1f / columnCount
            }
            return pageWidth
        }
        return 1f
    }

    override fun getItem(position: Int): Fragment {
        val fragment = Fragment.instantiate(context, tab[position].cls.name)
        fragment.arguments = getPageArguments(tab[position], position)
        return fragment
    }

    override fun startUpdate(container: ViewGroup) {
        super.startUpdate(container)
    }

    override fun getPageIcon(position: Int): Drawable {
        return getTabIconDrawable(context, tab[position].icon)
    }

    fun getTab(position: Int): SupportTabSpec {
        return tab[position]
    }

    val tabs: List<SupportTabSpec>
        get() = tab

    override fun onPageReselected(position: Int) {
        if (context !is SupportFragmentCallback) return
        val f = context.currentVisibleFragment
        if (f is RefreshScrollTopInterface) {
            f.scrollToStart()
        }
    }

    override fun onPageSelected(position: Int) {
        if (indicator == null || position < 0 || position >= count) return
        announceForAccessibilityCompat(context, indicator as View?, getPageTitle(position), javaClass)
    }

    override fun onTabLongClick(position: Int): Boolean {
        if (context !is SupportFragmentCallback) return false
        if (context.triggerRefresh(position)) return true
        val f = context.currentVisibleFragment
        if (f is RefreshScrollTopInterface)
            return f.triggerRefresh()
        return false
    }

    fun setTabLabel(position: Int, label: CharSequence) {
        tab.filter { position == it.position }.forEach { it.name = label }
        notifyDataSetChanged()
    }

    private fun getPageArguments(spec: SupportTabSpec, position: Int): Bundle {
        val args = spec.args ?: Bundle()
        args.putInt(EXTRA_ADAPTER_POSITION, position)
        return args
    }

    companion object {

        private val EXTRA_ADAPTER_POSITION = "adapter_position"
    }
}