package com.company.wizapp2.test.testcases.ui;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.company.wizapp2.R;
import com.company.wizapp2.app.SAPWizardApplication;
import com.company.wizapp2.logon.LogonActivity;
import com.company.wizapp2.test.core.BaseTest;
import com.company.wizapp2.test.core.Utils;
import com.company.wizapp2.test.pages.EntityListPage;
import com.company.wizapp2.test.pages.SettingsListPage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.qos.logback.classic.Level;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
public class LoggingTests extends BaseTest {

    @Rule
    public ActivityTestRule<LogonActivity> activityTestRule = new ActivityTestRule<>(LogonActivity.class);

    private static final String DEFAULT_LOG_LEVEL = "Warn";

    @Test
    public void testLogPolicyValue() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage();
        entityListPage.clickSettings();
        entityListPage.leavePage();

        // Check log level value
        Level policyLevel = ((SAPWizardApplication)getInstrumentation().getTargetContext().getApplicationContext())
                .getClientPolicyManager().getClientPolicy(true).getLogLevel();
        String policyString;
        // If the policyLevel is null it means there is no policy set on the server
        if (policyLevel == null) {
            policyString = InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, DEFAULT_LOG_LEVEL);
        } else {
            policyString = (policyLevel.levelStr.toLowerCase());
            policyString = policyString.substring(0, 1).toUpperCase() + policyString.substring(1);
            policyString = InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, policyString);
            if (policyString.equals( InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, "All") )) {
                policyString = InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, "Path");
            }
            if (policyString.equals(InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, "Off"))) {
                policyString = InstrumentationRegistry.getTargetContext().getResources().getString(R.string.log_level_summary, "None");
            }
        }
        SettingsListPage settingsListPage = new SettingsListPage();
        settingsListPage.checkLoglevel(policyString);

    }

}
