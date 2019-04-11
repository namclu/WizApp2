package com.company.wizapp2.mdui;

import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.widget.Toast
import ch.qos.logback.classic.Level
import com.company.wizapp2.R
import com.company.wizapp2.app.ErrorMessage
import com.company.wizapp2.app.SAPWizardApplication
import com.company.wizapp2.logon.ClientPolicyManager
import com.company.wizapp2.logon.SecureStoreManager
import com.sap.cloud.mobile.foundation.common.ClientProvider
import com.sap.cloud.mobile.foundation.logging.Logging
import com.sap.cloud.mobile.onboarding.passcode.ChangePasscodeActivity
import com.sap.cloud.mobile.onboarding.passcode.EnterPasscodeSettings
import com.sap.cloud.mobile.onboarding.passcode.SetPasscodeActivity
import com.sap.cloud.mobile.onboarding.passcode.SetPasscodeSettings
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** This fragment represents the settings screen. */
class SettingsFragment : PreferenceFragmentCompat(), Logging.UploadListener,ClientPolicyManager.LogLevelChangeListener {

    private lateinit var secureStoreManager: SecureStoreManager
    private lateinit var clientPolicyManager: ClientPolicyManager
     private lateinit var logLevelPreference: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        secureStoreManager = (activity?.application as SAPWizardApplication).secureStoreManager
        clientPolicyManager = (activity?.application as SAPWizardApplication).clientPolicyManager

        addPreferencesFromResource(R.xml.preferences)

        logLevelPreference =
            findPreference(activity?.applicationContext?.getString(R.string.log_level)) as ListPreference

        val entries = arrayOf(
            activity?.resources?.getString(R.string.log_level_path),
            activity?.resources?.getString(R.string.log_level_debug),
            activity?.resources?.getString(R.string.log_level_info),
            activity?.resources?.getString(R.string.log_level_warning),
            activity?.resources?.getString(R.string.log_level_error),
            activity?.resources?.getString(R.string.log_level_none)
        )

        val entryValues = arrayOf(
            Level.ALL.levelInt.toString(),
            Level.DEBUG.levelInt.toString(),
            Level.INFO.levelInt.toString(),
            Level.WARN.levelInt.toString(),
            Level.ERROR.levelInt.toString(),
            Level.OFF.levelInt.toString()
        )

        logLevelPreference.entries = entries
        logLevelPreference.entryValues = entryValues
        logLevelPreference.isPersistent = true

        val logLevelStored: Level? = secureStoreManager.getWithPasscodePolicyStore { passcodePolicyStore ->
            passcodePolicyStore.getSerializable(ClientPolicyManager.KEY_CLIENT_LOG_LEVEL)
        }
        val logLevelValueStoredIndex: Int = entryValues.indexOf(logLevelStored?.levelInt.toString())
        logLevelPreference.summary =
                String.format(getString(R.string.log_level_summary), entries[logLevelValueStoredIndex])
        logLevelPreference.value = logLevelStored?.levelInt.toString()
        logLevelPreference.setOnPreferenceChangeListener { preference, newValue ->

            // Get the new value
            val logLevel = Level.toLevel(Integer.valueOf(newValue as String))

            //Write the new value to Secure Store
            secureStoreManager.doWithPasscodePolicyStore { passcodePolicyStore ->
                passcodePolicyStore.put(ClientPolicyManager.KEY_CLIENT_LOG_LEVEL, logLevel)
            }
            Logging.getRootLogger().level = logLevel
            preference.summary =
                    String.format(getString(R.string.log_level_summary), entries[entryValues.indexOf(newValue)])

            true
        }

        clientPolicyManager.setLogLevelChangeListener(this)


        val changePassCodePreference =
            findPreference(activity?.applicationContext?.getString(R.string.manage_passcode)) as Preference
        changePassCodePreference.setOnPreferenceClickListener {
            val intent: Intent
            if (secureStoreManager.isUserPasscodeSet) {
                intent = Intent(this@SettingsFragment.activity, ChangePasscodeActivity::class.java)
                val setPasscodeSettings = SetPasscodeSettings()
                setPasscodeSettings.skipButtonText = getString(R.string.skip_passcode)
                setPasscodeSettings.saveToIntent(intent)
                val currentRetryCount: Int? = secureStoreManager.getWithPasscodePolicyStore { passcodePolicyStore ->
                    passcodePolicyStore.getInt(ClientPolicyManager.KEY_RETRY_COUNT)
                }
                val retryLimit = clientPolicyManager.getClientPolicy(false).passcodePolicy?.retryLimit
                if (retryLimit!! <= currentRetryCount!!) {
                    var enterPasscodeSettings = EnterPasscodeSettings()
                    enterPasscodeSettings.isFinalDisabled = true
                    enterPasscodeSettings.saveToIntent(intent)
                }
                this@SettingsFragment.activity?.startActivity(intent)
            } else {
                intent = Intent(this@SettingsFragment.activity, SetPasscodeActivity::class.java)
                val setPasscodeSettings = SetPasscodeSettings()
                setPasscodeSettings.skipButtonText = getString(R.string.skip_passcode)
                setPasscodeSettings.saveToIntent(intent)
                this@SettingsFragment.activity?.startActivity(intent)
            }
            false
        }
        // Uploading the logs
        val logUploadPreference = findPreference(activity?.applicationContext?.getString(R.string.upload_log))
        logUploadPreference.setOnPreferenceClickListener {
            Logging.uploadLog( ClientProvider.get(), (activity?.application as SAPWizardApplication).settingsParameters!! )
            logUploadPreference.isEnabled = false
            false
        }

        val resetAppPreference = findPreference(activity?.applicationContext?.getString(R.string.reset_app))
        resetAppPreference.setOnPreferenceClickListener {
            val sapWizardApplication = (activity?.application as SAPWizardApplication)
            sapWizardApplication.resetApplicationWithUserConfirmation()
            false
        }
    }

    @Override
    override fun onDestroy() {
        clientPolicyManager.removeLogLevelChangeListener(this)
        super.onDestroy();
    }

    override fun onResume() {
        super.onResume()
        Logging.addLogUploadListener(this)
    }

    override fun onPause() {
        super.onPause()
        Logging.removeLogUploadListener(this)
    }

    override fun onSuccess() {
        enableLogUploadButton()
        Toast.makeText(activity, activity?.resources?.getString(R.string.log_upload_ok), Toast.LENGTH_LONG).show()
        LOGGER.info("Log is uploaded to the server.")
    }

    override fun onError(throwable: Throwable) {
        enableLogUploadButton()
        val errorHandler = (activity?.application as SAPWizardApplication).errorHandler
        val errorCause = throwable.localizedMessage
        errorHandler.sendErrorMessage(
            ErrorMessage(
                activity?.resources?.getString(R.string.log_upload_failed)!!,
                errorCause,
                Exception(throwable),
                false
            )
        )
        LOGGER.error("Log upload failed with error message: $errorCause")
    }

    override fun onProgress(p0: Int) {
        // You could add a progress indicator and update it from here
    }

    private fun enableLogUploadButton() {
        val logUploadPreference = findPreference(activity?.applicationContext?.getString(R.string.upload_log))
        logUploadPreference.isEnabled = true
    }

    override fun logLevelChanged(level: Level) {
        logLevelPreference.callChangeListener(Integer.toString(level.levelInt));
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(SettingsFragment::class.java)
    }
}