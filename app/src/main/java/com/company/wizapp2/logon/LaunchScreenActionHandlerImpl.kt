package com.company.wizapp2.logon

import android.app.Activity
import android.content.Intent
import android.support.v4.app.Fragment
import android.widget.Toast

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import com.sap.cloud.mobile.foundation.logging.Logging

import com.sap.cloud.mobile.foundation.common.ClientProvider
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoader
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationLoaderCallback
import com.sap.cloud.mobile.foundation.configurationprovider.ConfigurationProviderError
import com.sap.cloud.mobile.foundation.configurationprovider.DiscoveryServiceConfigurationProvider
import com.sap.cloud.mobile.foundation.configurationprovider.ProviderIdentifier
import com.sap.cloud.mobile.foundation.configurationprovider.ProviderInputs
import com.sap.cloud.mobile.foundation.configurationprovider.UserInputs
import com.sap.cloud.mobile.onboarding.activation.ActivationActivity
import com.sap.cloud.mobile.onboarding.activation.ActivationSettings
import com.sap.cloud.mobile.onboarding.launchscreen.WelcomeScreenActionHandlerImpl
import com.sap.cloud.mobile.onboarding.utility.ActivityResultActionHandler
import com.sap.cloud.mobile.onboarding.utility.OnboardingType

import java.io.IOException
import android.provider.Settings
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import com.sap.cloud.mobile.foundation.networking.WebkitCookieJar
import com.sap.cloud.mobile.foundation.networking.AppHeadersInterceptor
import com.sap.cloud.mobile.foundation.authentication.SamlInterceptor
import com.sap.cloud.mobile.foundation.authentication.SamlConfiguration
import com.sap.cloud.mobile.foundation.authentication.SamlWebViewProcessor

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import java.util.concurrent.CountDownLatch

import com.company.wizapp2.R
import com.company.wizapp2.app.ErrorMessage
import com.company.wizapp2.app.SAPWizardApplication
import com.company.wizapp2.logon.ActivationActionHandlerImpl.Companion.DISCOVERY_SVC_EMAIL

class LaunchScreenActionHandlerImpl : WelcomeScreenActionHandlerImpl(), ActivityResultActionHandler {

    private enum class ConfigurationState {
        Initializing,
        NeedsInput,
        FailedConfiguration,
        SuccessfulConfiguration
    }

    private var configurationLoader: ConfigurationLoader? = null
    private var configurationLatch = CountDownLatch(1)
    private var configurationState: ConfigurationState = ConfigurationState.Initializing
    private var configurationPromptType: OnboardingType? = null
    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var application: SAPWizardApplication

    override fun startDemoMode(fragment: Fragment) {
        initVar(fragment)
        activity.runOnUiThread {
            Toast.makeText(
                context,
                "Demo mode onboarding",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun startStandardOnboarding(fragment: Fragment) {
        initVar(fragment)

        if ( application.configurationData.isLoaded ) {
            configurationState = ConfigurationState.SuccessfulConfiguration
        } else {
            startConfigurationLoader()
        }

        // Allow the Configuration Loader to Succeed or Fail, Supplying Input When Necessary
        while (configurationState != ConfigurationState.FailedConfiguration && configurationState != ConfigurationState.SuccessfulConfiguration) {
            // Wait for the configuration loader to do something
            try {
                configurationLatch.await()
            } catch (e: InterruptedException) {
                if (configurationState == ConfigurationState.NeedsInput) {
                    // Tell the Configuration Loader to Fail
                    configurationLoader!!.processRequestedInputs(UserInputs())
                }
                // Drop out of here
                configurationState = ConfigurationState.FailedConfiguration
                // Reestablish the interrupted state
                Thread.currentThread().interrupt()
            }

            // re-arm the latch for the next iteration
            configurationLatch = CountDownLatch(1)

            if (configurationState == ConfigurationState.NeedsInput) {
                // show activation screen
                promptForProviderInput()
            }
        }

        // Successfully Acquired Configuration Information
        // Failures will Fall Through Resulting in Re-invoking this Callback
        if ( (configurationState == ConfigurationState.SuccessfulConfiguration) && authenticateWithServer() ) {
            activity.setResult(RESULT_OK)
            activity.finish()
        }
    }

    private fun startConfigurationLoader() {
        activity.runOnUiThread {
            configurationLoader = ConfigurationLoader(
                context,
                SAPWizardApplication.APPLICATION_ID,
                object : ConfigurationLoaderCallback() {
                    override fun onCompletion(provider: ProviderIdentifier?, success: Boolean) {
                        if (success) {
                            val loaded = application.configurationData.run {
                                loadData()
                            }
                            configurationState = when (loaded) {
                                true -> ConfigurationState.SuccessfulConfiguration
                                false -> ConfigurationState.FailedConfiguration
                            }
                        } else {
                            configurationState = ConfigurationState.FailedConfiguration
                            application.errorHandler.run {
                                val message = ErrorMessage(
                                    context.resources.getString(R.string.config_loader_complete_error_title),
                                    context.resources.getString(R.string.config_loader_complete_error_description)
                                )
                                sendErrorMessage(message)
                            }
                        }

                        configurationLatch.countDown()
                    }

                    override fun onError(
                        loader: ConfigurationLoader,
                        provider: ProviderIdentifier,
                        inputs: UserInputs,
                        providerError: ConfigurationProviderError
                    ) {
                        application.errorHandler.run {
                            val message = ErrorMessage(
                                context.getString(R.string.config_loader_on_error_title),
                                String.format(
                                    context.getString(R.string.config_loader_on_error_description),
                                    provider.toString(),
                                    providerError.errorMessage
                                )
                            )
                            sendErrorMessage(message)
                        }

                        if (provider == ProviderIdentifier.DISCOVERY_SERVICE_CONFIGURATION_PROVIDER) {
                            // Was a supported input provider, prompt for input again
                            configurationState = ConfigurationState.NeedsInput
                            configurationLatch.countDown()
                        } else {
                            // Not a supported input provider, supply empty input to fail
                            loader.processRequestedInputs(UserInputs())
                        }
                    }

                    override fun onInputRequired(loader: ConfigurationLoader, inputs: UserInputs) {
                        val onboardingType = acceptedProviderTypes(inputs)
                        onboardingType?.run {
                            configurationPromptType = this
                            configurationState = ConfigurationState.NeedsInput
                            configurationLatch.countDown()
                        } ?: loader.processRequestedInputs(UserInputs())
                    }

                    fun acceptedProviderTypes(inputs: UserInputs): OnboardingType? {
                        if (inputs.containsKey(ProviderIdentifier.DISCOVERY_SERVICE_CONFIGURATION_PROVIDER)) {
                            return OnboardingType.DISCOVERY_SERVICE_ONBOARDING
                        }

                        return null
                    }
                }
            )
            configurationLoader?.loadConfiguration()
        }
    }

    private fun initHttpClient(application: SAPWizardApplication, samlConfiguration: SamlConfiguration) {
        val deviceId = Settings.Secure.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID)

        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(SamlInterceptor(samlConfiguration))
                .addInterceptor(AppHeadersInterceptor(SAPWizardApplication.APPLICATION_ID, deviceId, SAPWizardApplication.APPLICATION_VERSION))
                .cookieJar(WebkitCookieJar())
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()
        ClientProvider.set(okHttpClient)
    }

    private fun authenticateWithServer(): Boolean {
        var success = false
        val configurationData = application.configurationData

        val serviceUrl = configurationData.serviceUrl

        val warn = Logging.ConfigurationBuilder().apply {
            initialLevel(Level.WARN)
            logToConsole(true)
        }
 
 
        val samlConfiguration = SamlConfiguration.Builder()
              .authUrl(serviceUrl + "SAMLAuthLauncher")
              .build()

        val samlProcessor = SamlWebViewProcessor(samlConfiguration)
        try {
            samlProcessor.authenticate()
            success = true

            Logging.initialize(activity.applicationContext, warn.build())
            initHttpClient(application, samlConfiguration)
           
 
        } catch (e: IOException) {
            application.isOnboarded = false
            activity.runOnUiThread {
                Toast.makeText(context, R.string.error.toString() + ": " + e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
         
        return success
    }



    override fun onActivityResult(fragment: Fragment, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == INPUT_REQUEST) {
            when (resultCode) {
                RESULT_OK -> {
                    val inputs: UserInputs
                    if (data != null && data.hasExtra(DISCOVERY_SVC_EMAIL)) {
                        inputs = UserInputs().also {
                            it.addProvider(
                                ProviderIdentifier.DISCOVERY_SERVICE_CONFIGURATION_PROVIDER,
                                ProviderInputs().also { pi ->
                                    pi.addInput(
                                        DiscoveryServiceConfigurationProvider.EMAIL_ADDRESS,
                                        data.getStringExtra(DISCOVERY_SVC_EMAIL)
                                    )
                                }
                            )
                        }
                        configurationLoader!!.processRequestedInputs(inputs)
                    } else {
                        configurationLoader?.processRequestedInputs(UserInputs())
                    }
                }
                RESULT_CANCELED -> configurationLoader?.processRequestedInputs(UserInputs())
            }
        }
        return false
    }

    private fun promptForProviderInput() {
        Intent(activity, ActivationActivity::class.java).also { inputRequest: Intent ->
            val activationSettings = ActivationSettings().apply {
                activationType = configurationPromptType
                actionHandler = "com.company.wizapp2.logon.ActivationActionHandlerImpl"
                activationTitle = activity.getString(R.string.application_name)
            }
            activationSettings.saveToIntent(inputRequest)
            activity.startActivityForResult(inputRequest, INPUT_REQUEST)
        }
    }

    private fun initVar(fragment: Fragment) {
        activity = (fragment.activity as Activity)
        context = activity.applicationContext
        application = (activity.application as SAPWizardApplication)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LaunchScreenActionHandlerImpl::class.java)
        private const val INPUT_REQUEST = 12345
    }
}
