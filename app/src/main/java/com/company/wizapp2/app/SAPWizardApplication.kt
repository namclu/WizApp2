package com.company.wizapp2.app

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.CookieManager
import com.company.wizapp2.R
import com.company.wizapp2.logon.ClientPolicyManager
import com.company.wizapp2.logon.LogonActivity
import com.company.wizapp2.logon.SecureStoreManager
import com.company.wizapp2.repository.RepositoryFactory
import com.company.wizapp2.service.SAPServiceManager
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.authentication.SamlConfiguration
import com.sap.cloud.mobile.foundation.authentication.SamlInterceptor
import com.sap.cloud.mobile.foundation.common.ClientProvider
import com.sap.cloud.mobile.foundation.common.SettingsParameters
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoader
import com.sap.cloud.mobile.foundation.networking.AppHeadersInterceptor
import com.sap.cloud.mobile.foundation.networking.WebkitCookieJar
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.util.concurrent.TimeUnit

/**
 * This class extends the [Application] class. Its purpose is to configure application-wide services such as error
 * handling and data access and provide access to them. It maintains an [ActivityLifecycleCallbacks] instance, as well.
 * By extending the callback's default implementation the application will be able to react on lifecycle events of the
 * contained activities.
 */
class SAPWizardApplication: Application() {

    /** Manages and provides access to OData stores providing data for the app. */
    lateinit var sapServiceManager: SAPServiceManager
        private set

    /** Manages and provides access to secure key-value-stores used to persist settings and user data. */
    lateinit var secureStoreManager: SecureStoreManager
        private set

    /**
     * Manages and provides access to local and server-provided client policies, including but not limited to passcode
     * requirements, retry count during unlocking etc.
     */
    lateinit var clientPolicyManager: ClientPolicyManager
        private set

    /** Global error handler displaying error messages to the user */
    lateinit var errorHandler: ErrorHandler
        private set

    /** Lifecycle observer, listens for foreground-background state changes. */
    private lateinit var sapWizardLifecycleObserver: SAPWizardLifecycleObserver

    /** Provides access to locally persisted configuration that is loaded via [ConfigurationLoader]. */
    lateinit var configurationData: ConfigurationData
        private set

    /** Application-wide RepositoryFactory */
    lateinit var repositoryFactory: RepositoryFactory
        private set

    var isOnboarded: Boolean
        set(value) {
            secureStoreManager.isOnboarded = value
        }
        get() = secureStoreManager.isOnboarded

    var settingsParameters: SettingsParameters? = null
        private set
        get() {
            try {
                val serviceUrl = configurationData.serviceUrl ?: ""
                val deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
                return SettingsParameters(serviceUrl, APPLICATION_ID, deviceId, APPLICATION_VERSION)
            } catch (e: MalformedURLException){
                errorHandler.sendErrorMessage(ErrorMessage(
                    resources.getString(R.string.configuration_invalid),
                    String.format(resources.getString(R.string.configuration_contained_malformed_url), e.message),
                    e,
                    false
                ))
            }
            return null
        }

    override fun onCreate() {
        super.onCreate()
        startErrorHandler()

        secureStoreManager = SecureStoreManager(this)
        configurationData = ConfigurationData(this, errorHandler)
        if(isOnboarded) { 
            configurationData.loadData()
            initHttpClient()
        }
        sapServiceManager = SAPServiceManager(configurationData)
        clientPolicyManager = ClientPolicyManager(this)
        sapWizardLifecycleObserver = SAPWizardLifecycleObserver(secureStoreManager)
        repositoryFactory = RepositoryFactory(sapServiceManager)

        registerLifecycleCallbacks()
    }


    private fun initHttpClient() {
        val deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
        val serviceUrl = configurationData.serviceUrl ?: ""
        val samlConfiguration = SamlConfiguration.Builder()
                .authUrl(serviceUrl + "SAMLAuthLauncher")
                .build()

        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(SamlInterceptor(samlConfiguration))
                .addInterceptor(AppHeadersInterceptor(APPLICATION_ID, deviceId, APPLICATION_VERSION))
                .cookieJar(WebkitCookieJar())
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()
        ClientProvider.set(okHttpClient)
    }

    /** Registers the SDK-provided lifecycle callback listener for this application. */
    private fun registerLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(AppLifecycleCallbackHandler.getInstance())
    }

    /** Creates a global error handler shared by all app components and starts its background thread. */
    private fun startErrorHandler() {
        errorHandler = ErrorHandler( "SAPWizardErrorHandler" )
        errorHandler.presenter = ErrorPresenterByNotification(this)
        errorHandler.start()
    }

    /**
     * Clears all user-specific data and configuration from the application, essentially resetting it to its initial
     * state. Restarting the application at the end.
     *
     * @param [activity] Activity from which the request originates
     */
    fun resetApplication(activity: Activity) {
        isOnboarded = false
        clientPolicyManager.resetLogLevelChangeListener()
        secureStoreManager.resetStores()
        configurationData.resetConfigurations(applicationContext)
        clearCookies(activity)
        repositoryFactory.reset()
        restartApplication(activity)
    }

    /**
     * Asks confirmation from the user if the application data should be reset, and resets the app if the user confirms
     * the prompt.
     */
    fun resetApplicationWithUserConfirmation() {
        val activity = AppLifecycleCallbackHandler.getInstance().activity!!
        val alert = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setMessage(R.string.reset_app_confirmation)
            // Setting OK Button
            .setPositiveButton(R.string.yes) { _, _ ->
                // reset the application
                resetApplication(activity)
                LOGGER.info("Yes button is clicked. The all information related to this application will be deleted.")
            }
            // Setting Cancel Button
            .setNegativeButton(R.string.cancel) { _, _ -> LOGGER.info("The Cancel button is clicked.") }
        alert.show()
    }

    /**
     * Clears all cookies, making sure no sessions remain in the HTTP client.
     *
     * @param [activity] Activity from which the request originates
     */
    private fun clearCookies(activity: Activity) {
        val webkitCookieManager = CookieManager.getInstance()

        activity.runOnUiThread {
            webkitCookieManager.removeAllCookies { success ->
                if (success!!) {
                    LOGGER.info("Cookies are deleted.")
                } else {
                    LOGGER.error("Cookies couldn't be removed!")
                }
            }
        }
    }

    /**
     * Restarts the application by presenting the logon screen.
     *
     * @param [activity] Activity from which the request originates
     */
    private fun restartApplication(activity: Activity) {
        val intent = Intent(activity, LogonActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SAPWizardApplication::class.java)

        /** ID of the Mobile Services endpoint configured for this application. */
        const val APPLICATION_ID = "com.namlu.sap.wizapp"

        /** Application version sent to Mobile Services, which may be used to control access from outdated clients. */
        const val APPLICATION_VERSION = "1.0"
    }
}
