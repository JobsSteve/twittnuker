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

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.set
import de.vanita5.twittnuker.R
import de.vanita5.twittnuker.constant.IntentConstants.EXTRA_REQUEST_CODE
import de.vanita5.twittnuker.model.analyzer.PurchaseConfirm
import de.vanita5.twittnuker.model.analyzer.PurchaseFinished
import de.vanita5.twittnuker.model.analyzer.PurchaseIntroduction
import de.vanita5.twittnuker.util.Analyzer
import de.vanita5.twittnuker.util.premium.ExtraFeaturesService

class ExtraFeaturesIntroductionDialogFragment : BaseDialogFragment() {

    val feature: String get() = arguments.getString(EXTRA_FEATURE)
    val requestCode: Int get() = arguments.getInt(EXTRA_REQUEST_CODE, 0)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.title_extra_features)
        builder.setView(R.layout.dialog_extra_features_introduction)
        builder.setPositiveButton(R.string.action_purchase) { dialog, which ->
            startPurchase(feature)
            Analyzer.log(PurchaseConfirm(PurchaseFinished.NAME_EXTRA_FEATURES))
        }
        builder.setNegativeButton(R.string.action_later) { dialog, which ->
            onDialogCancelled()
        }
        val restorePurchaseIntent = extraFeaturesService.createRestorePurchaseIntent(context, feature)
        if (restorePurchaseIntent != null) {
            builder.setNeutralButton(R.string.action_restore_purchase) { dialog, which ->
                startActivityForResultOnTarget(restorePurchaseIntent)
            }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            it as Dialog
            it.findViewById(R.id.restorePurchaseHint).visibility = if (restorePurchaseIntent != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            val description = ExtraFeaturesService.getIntroduction(context, feature)
            val featureIcon = it.findViewById(R.id.featureIcon) as ImageView
            val featureDescription = it.findViewById(R.id.featureDescription) as TextView
            featureIcon.setImageResource(description.icon)
            featureDescription.text = description.description
            it.findViewById(R.id.buyFeaturesPack).setOnClickListener {
                startPurchase(ExtraFeaturesService.FEATURE_FEATURES_PACK)
                dismiss()
            }
        }
        if (savedInstanceState == null) {
            Analyzer.log(PurchaseIntroduction(PurchaseFinished.NAME_EXTRA_FEATURES, "introduction dialog"))
        }
        return dialog
    }

    override fun onCancel(dialog: DialogInterface?) {
        onDialogCancelled()
    }

    private fun onDialogCancelled() {
        if (targetRequestCode != 0) {
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
        }
    }

    private fun startPurchase(feature: String) {
        val purchaseIntent = extraFeaturesService.createPurchaseIntent(context, feature) ?: return
        startActivityForResultOnTarget(purchaseIntent)
    }

    private fun startActivityForResultOnTarget(intent: Intent) {
        if (targetFragment != null) {
            targetFragment.startActivityForResult(intent, targetRequestCode)
        } else if (requestCode == 0) {
            startActivity(intent)
        } else if (parentFragment != null) {
            parentFragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    companion object {
        const val EXTRA_FEATURE = "feature"
        fun show(fm: FragmentManager, feature: String, requestCode: Int = 0): ExtraFeaturesIntroductionDialogFragment {
            val df = ExtraFeaturesIntroductionDialogFragment()
            df.arguments = Bundle {
                this[EXTRA_FEATURE] = feature
                this[EXTRA_REQUEST_CODE] = requestCode
            }
            df.show(fm, "extra_features_introduction")
            return df
        }
    }
}