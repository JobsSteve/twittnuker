/*
 * Twittnuker - Twitter client for Android
 *
 * Copyright (C) 2013-2015 vanita5 <mail@vanita5.de>
 *
 * This program incorporates a modified version of Twidere.
 * Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package de.vanita5.twittnuker.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.model.BarSet;
import com.db.chart.model.ChartSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.StackBarChartView;
import com.desmond.asyncmanager.AsyncManager;
import com.desmond.asyncmanager.TaskRunnable;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.model.NetworkUsageInfo;
import de.vanita5.twittnuker.model.RequestType;
import de.vanita5.twittnuker.util.MathUtils;
import de.vanita5.twittnuker.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NetworkUsageSummaryPreferences extends Preference {

    private StackBarChartView mChartView;
    private NetworkUsageInfo mUsage;
    private TextView mTotalUsage;
    private TextView mDayUsageMax;
    private TextView mDayMin, mDayMid, mDayMax;

    public NetworkUsageSummaryPreferences(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.layout_preference_network_usage);
        getUsageInfo();
    }

    public NetworkUsageSummaryPreferences(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public NetworkUsageSummaryPreferences(Context context) {
        this(context, null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final View view = super.onCreateView(parent);
        mChartView = (StackBarChartView) view.findViewById(R.id.chart);
        mTotalUsage = (TextView) view.findViewById(R.id.total_usage);
        mDayUsageMax = (TextView) view.findViewById(R.id.day_usage_max);
        mDayMin = (TextView) view.findViewById(R.id.day_min);
        mDayMid = (TextView) view.findViewById(R.id.day_mid);
        mDayMax = (TextView) view.findViewById(R.id.day_max);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x20000000);
        mChartView.setYLabels(AxisController.LabelPosition.NONE);
        mChartView.setXLabels(AxisController.LabelPosition.NONE);
        return view;
    }

    private void getUsageInfo() {
        final Calendar now = Calendar.getInstance();
        final Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, now.get(Calendar.MONTH));
        final int dayMin = now.getActualMinimum(Calendar.DAY_OF_MONTH);
        final int dayMax = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, dayMin);
        cal.setTimeZone(now.getTimeZone());
        final Date start = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, dayMax);
        cal.add(Calendar.DATE, 1);
        final Date end = cal.getTime();
        final TaskRunnable<Object[], NetworkUsageInfo, NetworkUsageSummaryPreferences> task;
        task = new TaskRunnable<Object[], NetworkUsageInfo, NetworkUsageSummaryPreferences>() {
            @Override
            public NetworkUsageInfo doLongOperation(Object[] params) throws InterruptedException {
                return NetworkUsageInfo.get((Context) params[0], (Date) params[1], (Date) params[2], dayMin, dayMax);
            }

            @Override
            public void callback(NetworkUsageSummaryPreferences handler, NetworkUsageInfo result) {
                handler.setUsage(result);
            }
        };
        task.setResultHandler(this);
        task.setParams(new Object[]{getContext(), start, end});
        AsyncManager.runBackgroundTask(task);
    }

    private void setUsage(NetworkUsageInfo usage) {
        mUsage = usage;
        notifyChanged();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        final NetworkUsageInfo usage = mUsage;
        if (usage == null) return;
        final double[][] chartUsage = usage.getChartUsage();
        final int days = chartUsage.length;

        final BarSet apiSet = new BarSet();
        final BarSet mediaSet = new BarSet();
        final BarSet usageStatisticsSet = new BarSet();

        double dayUsageMax = 0;
        for (int i = 0; i < days; i++) {
            String day = String.valueOf(i + 1);
            final double[] dayUsage = chartUsage[i];
            apiSet.addBar(day, (float) dayUsage[RequestType.API.getValue()]);
            mediaSet.addBar(day, (float) dayUsage[RequestType.MEDIA.getValue()]);
            usageStatisticsSet.addBar(day, (float) dayUsage[RequestType.USAGE_STATISTICS.getValue()]);
            dayUsageMax = Math.max(dayUsageMax, MathUtils.sum(dayUsage));
        }

        apiSet.setColor(Color.RED);
        mediaSet.setColor(Color.GREEN);
        usageStatisticsSet.setColor(Color.BLUE);

        final ArrayList<ChartSet> data = new ArrayList<>();
        data.add(apiSet);
        data.add(mediaSet);
        data.add(usageStatisticsSet);
        mChartView.addData(data);
        mChartView.show();
        mTotalUsage.setText(Utils.calculateProperSize((usage.getTotalSent() + usage.getTotalReceived()) * 1024));
        mDayUsageMax.setText(Utils.calculateProperSize((usage.getDayUsageMax()) * 1024));
        mDayMin.setText(String.valueOf(usage.getDayMin()));
        mDayMid.setText(String.valueOf((usage.getDayMin() + usage.getDayMax()) / 2));
        mDayMax.setText(String.valueOf(usage.getDayMax()));
    }

}