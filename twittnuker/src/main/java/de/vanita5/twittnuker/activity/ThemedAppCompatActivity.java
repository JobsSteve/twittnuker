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

package de.vanita5.twittnuker.activity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.ATEActivity;

import de.vanita5.twittnuker.BuildConfig;
import de.vanita5.twittnuker.Constants;
import de.vanita5.twittnuker.activity.iface.IAppCompatActivity;
import de.vanita5.twittnuker.activity.iface.IThemedActivity;
import de.vanita5.twittnuker.util.StrictModeUtils;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.util.Utils;
import de.vanita5.twittnuker.view.ShapedImageView.ShapeStyle;

public abstract class ThemedAppCompatActivity extends ATEActivity implements Constants,
        IThemedActivity, IAppCompatActivity {

    // Data fields
    private int mCurrentThemeColor;
    private int mCurrentActionBarColor;
    private int mCurrentThemeBackgroundAlpha;
    @ShapeStyle
    private int mProfileImageStyle;
    private String mCurrentThemeBackgroundOption;
    private String mCurrentThemeFontFamily;

    private Toolbar mToolbar;

    @Override
    public String getCurrentThemeFontFamily() {
        return mCurrentThemeFontFamily;
    }

    @Override
    public int getCurrentThemeBackgroundAlpha() {
        return mCurrentThemeBackgroundAlpha;
    }

    @Override
    public String getCurrentThemeBackgroundOption() {
        return mCurrentThemeBackgroundOption;
    }

    @Override
    public int getCurrentThemeColor() {
        return mCurrentThemeColor;
    }

    @Override
    public int getCurrentActionBarColor() {
        return mCurrentActionBarColor;
    }

    @Override
    public int getThemeBackgroundAlpha() {
        return ThemeUtils.getUserThemeBackgroundAlpha(this);
    }

    @Override
    public String getThemeBackgroundOption() {
        return ThemeUtils.getThemeBackgroundOption(this);
    }

    @Override
    public String getThemeFontFamily() {
        return ThemeUtils.getThemeFontFamily(this);
    }

    @Override
    @ShapeStyle
    public int getCurrentProfileImageStyle() {
        return mProfileImageStyle;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictModeUtils.detectAllVmPolicy();
            StrictModeUtils.detectAllThreadPolicy();
        }
        final int themeColor = getThemeColor();

        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public String getATEKey() {
        return ThemeUtils.getATEKey(this);
    }

    @Override
    protected void onApplyThemeResource(@NonNull Resources.Theme theme, int resId, boolean first) {
        mCurrentThemeColor = getThemeColor();
        mCurrentActionBarColor = getActionBarColor();
        mCurrentThemeBackgroundAlpha = getThemeBackgroundAlpha();
        mProfileImageStyle = Utils.getProfileImageStyle(this);
        mCurrentThemeBackgroundOption = getThemeBackgroundOption();
        mCurrentThemeFontFamily = getThemeFontFamily();
        super.onApplyThemeResource(theme, resId, first);
        final Window window = getWindow();
        if (shouldApplyWindowBackground()) {
            ThemeUtils.applyWindowBackground(this, window, mCurrentThemeBackgroundOption,
                    mCurrentThemeBackgroundAlpha);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        ThemeUtils.fixNightMode(getResources(), newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Nullable
    public final Toolbar peekActionBarToolbar() {
        return mToolbar;
    }

    @Nullable
    public final Toolbar getActionBarToolbar() {
        if (mToolbar != null) return mToolbar;
        final View actionBarView = getWindow().findViewById(android.support.v7.appcompat.R.id.action_bar);
        if (actionBarView instanceof Toolbar) {
            return (Toolbar) actionBarView;
        }
        return null;
    }

    protected boolean shouldApplyWindowBackground() {
        return true;
    }

}