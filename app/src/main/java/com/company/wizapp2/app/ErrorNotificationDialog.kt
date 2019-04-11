package com.company.wizapp2.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import com.company.wizapp2.R

/**
 * This is an activity which is presented as a dialog for presenting error notifications to the user. The notifications
 * can have a short title, a detailed message describing the error and its consequences. Finally, notifications have a
 * so-called fatal flag. It it were true, then the application is killed after the user pressed the OK button.
 */
class ErrorNotificationDialog: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startIntent = intent
        val title = startIntent.getStringExtra(TITLE)
        val msg = startIntent.getStringExtra(MSG)
        val isFatal = startIntent.getBooleanExtra(FATAL, false)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener({ dialog : DialogInterface -> onDismissed(isFatal) }).show()
    }

    private fun onDismissed(isFatal: Boolean) {
        if (isFatal) {
            val activityManager =
                this@ErrorNotificationDialog.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = activityManager.appTasks
            for (task in tasks) {
                task.finishAndRemoveTask()
            }
        } else {
            this@ErrorNotificationDialog.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ErrorPresenterByNotification.errorDialogDismissed()
    }

    companion object {
        val TITLE = "error_title"
        val MSG = "error_msg"
        val FATAL = "isFatal"
    }
}