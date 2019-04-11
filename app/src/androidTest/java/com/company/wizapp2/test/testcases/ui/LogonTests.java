package com.company.wizapp2.test.testcases.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.company.wizapp2.app.SAPWizardApplication;
import com.company.wizapp2.logon.LogonActivity;
import com.company.wizapp2.test.core.BaseTest;
import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.Utils;
import com.company.wizapp2.test.core.Credentials;
import com.company.wizapp2.test.core.WizardDevice;
import com.company.wizapp2.test.pages.DetailPage;
import com.company.wizapp2.test.pages.PasscodePage;
import com.company.wizapp2.test.pages.EntityListPage;
import com.company.wizapp2.test.pages.MasterPage;
import com.company.wizapp2.test.pages.SettingsListPage;
import com.company.wizapp2.test.pages.WelcomePage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.company.wizapp2.test.core.UIElements.EntityListScreen.entityList;

@RunWith(AndroidJUnit4.class)
public class LogonTests extends BaseTest {

    @Rule
    public ActivityTestRule<LogonActivity> activityTestRule = new ActivityTestRule<>(LogonActivity.class);


    @Test
    public void testLogonFlow() {

        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        // Actions on the entitylist Page
        EntityListPage entityListPage = new EntityListPage(entityList);
        entityListPage.clickFirstElement();
        entityListPage.leavePage();

        // Actions on the master Page
        MasterPage masterPage = new MasterPage(UIElements.MasterScreen.refreshButton);
        masterPage.clickFirstElement();
        masterPage.leavePage();

        DetailPage detailPage = new DetailPage();
        detailPage.clickBack();
        detailPage.leavePage();

        masterPage = new MasterPage(UIElements.MasterScreen.refreshButton);
        masterPage.clickBack();
        masterPage.leavePage();

        entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage();
        settingsListPage.clickResetApp();

        settingsListPage.checkConfirmationDialog();

        settingsListPage.clickYes();
    }


    @Test
    public void logonFlowPutAppIntoBackground() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(entityList);
        entityListPage.clickFirstElement();
        entityListPage.leavePage();

        MasterPage masterPage = new MasterPage(UIElements.MasterScreen.refreshButton);
        masterPage.clickFirstElement();
        masterPage.leavePage();

        // Get the lockTimeOut (in seconds) from the SecureStoreManager
        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();

        // Put the app into background and immediately start again
        WizardDevice.putApplicationBackground(0, activityTestRule);
        WizardDevice.reopenApplication();

        if (lockTimeOut == 0) {
            PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
            enterPasscodePage.enterPasscode(Credentials.PASSCODE);
            enterPasscodePage.clickSignIn();
            enterPasscodePage.leavePage();
        }

        DetailPage mDetailPage = new DetailPage(UIElements.DetailScreen.deleteButton);
        mDetailPage.clickBack();
        mDetailPage.leavePage();

        masterPage = new MasterPage(UIElements.MasterScreen.refreshButton);
        masterPage.clickBack();
        masterPage.leavePage();

        entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage();
        settingsListPage.clickResetApp();

        settingsListPage.checkConfirmationDialog();

        settingsListPage.clickYes();
    }

    @Test
    public void LogonFlowBack () {
        Utils.checkCredentials();
        WelcomePage welcomePage = new WelcomePage();
        welcomePage.clickGetStarted();
        welcomePage.waitForCredentials();
        Utils.pressBack();
        Utils.doOnboarding();
        EntityListPage entityListPage = new EntityListPage(entityList);
    }

}
