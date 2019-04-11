package com.company.wizapp2.mdui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import com.company.wizapp2.R
import com.company.wizapp2.viewmodel.EntityViewModel
import com.sap.cloud.mobile.fiori.indicator.FioriProgressBar
import com.sap.cloud.mobile.odata.EntityValue
import kotlinx.android.synthetic.main.activity_item_list.view.*

/**
 * Generic Dialog to confirm execution of delete operation for an entity type
 *
 * @param context application context
 * @param viewModel view model of the entity type
 */
class EntityDeleteDialog<T : EntityValue>(
        private val context: Context,
        private val viewModel: EntityViewModel<T>) {

    /* Fiori progress bar component to indicate delete operation is being executed */
    private var progressBar: FioriProgressBar? = null

    /*
     * Build and present an alert dialog to confirm delete operation.
     * Perform delete via view model if confirmed
     */
    fun confirmDelete() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogStyle))

        if (viewModel.numberOfSelected() > 1) {
            builder.setTitle(R.string.delete_dialog_title).setMessage(R.string.delete_more_items)
        } else {
            builder.setTitle(R.string.delete_dialog_title).setMessage(R.string.delete_one_item)
        }

        builder.setPositiveButton(R.string.ok) { _, _ ->
            if (null == progressBar) {
                progressBar = (context as Activity).window.decorView.indeterminateBar
            }
            progressBar!!.visibility = View.VISIBLE
            viewModel.deleteSelected()
        }

        builder.setNegativeButton(R.string.cancel) { _, _ ->
            // User cancelled the dialog
        }

        val dialog = builder.create()
        dialog.show()
    }
}