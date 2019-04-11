package com.company.wizapp2.test.testcases.ui;

import android.os.SystemClock;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.company.wizapp2.app.SAPWizardApplication;
import com.company.wizapp2.logon.ClientPolicy;
import com.company.wizapp2.logon.ClientPolicyManager;
import com.company.wizapp2.logon.LogonActivity;
import com.company.wizapp2.test.core.BaseTest;
import com.company.wizapp2.test.core.Credentials;
import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.Utils;
import com.company.wizapp2.test.core.WizardDevice;
import com.company.wizapp2.test.core.factory.PasscodePageFactory;
import com.company.wizapp2.test.pages.EntityListPage;
import com.company.wizapp2.test.pages.MasterPage;
import com.company.wizapp2.test.pages.PasscodePage;
import com.company.wizapp2.test.pages.SettingsListPage;
import com.company.wizapp2.test.pages.WelcomePage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.company.wizapp2.test.core.UIElements.EntityListScreen.entityList;

@RunWith(AndroidJUnit4.class)
public class PasscodeTests extends BaseTest {

    @Rule
    public ActivityTestRule<LogonActivity> activityTestRule = new ActivityTestRule<>(LogonActivity.class);


    @Test
    public void testPasscodeLockTimeOut() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(UIElements.EntityListScreen.entityList);
        entityListPage.clickFirstElement();
        entityListPage.leavePage();

        MasterPage masterPage = new MasterPage();

        // Get the lockTimeOut (in seconds) from the SecureStoreManager
        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();
        // We put the app into background
        WizardDevice.putApplicationBackground(3000, activityTestRule);
        // We reopen the app
        WizardDevice.reopenApplication();

        SystemClock.sleep(1000);
        // We should arrive in the Item list Page
        entityListPage = new EntityListPage(UIElements.EntityListScreen.entityList);

        // Put and reopen the app
        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);
        WizardDevice.reopenApplication();
        // We should arrive in the EnterPasscodeScreen

        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        // Go Back from the Master page
        masterPage.clickBack();
        masterPage.leavePage();

        // We should arrive in the EntityListPage
        entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickResetApp();
        settingsListPage.leavePage();
    }


    @Test
    public void testManagePasscodeBackground() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(UIElements.EntityListScreen.entityList);
        entityListPage.clickFirstElement();

        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();

        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);

        WizardDevice.reopenApplication();
        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        MasterPage masterPage = new MasterPage();
        masterPage.clickBack();
        masterPage.leavePage();

        entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickManagePasscode();
        settingsListPage.leavePage();

        enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSecondNextButton();
        enterPasscodePage.leavePage();

        PasscodePageFactory.NewPasscodeFlow();

        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);
        WizardDevice.reopenApplication();

        enterPasscodePage.enterPasscode(Credentials.NEWPASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickResetApp();
        settingsListPage.checkConfirmationDialog();
        settingsListPage.clickYes();
    }


    @Test
    public void testManagePasscodeCancelBackground() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickManagePasscode();

        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);

        enterPasscodePage.clickCancel();

        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();

        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);

        WizardDevice.reopenApplication();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSignIn();

        settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickResetApp();

        settingsListPage.checkConfirmationDialog();

        settingsListPage.clickYes();
    }


    @Test
    public void testManagePasscodeDefaultBackground() {
        SAPWizardApplication sapWizardApplication = ((SAPWizardApplication) activityTestRule.getActivity().getApplication());

        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(UIElements.EntityListScreen.entityList);
        entityListPage.clickFirstElement();

        int lockTimeOut = sapWizardApplication.getSecureStoreManager().getPasscodeLockTimeout();

        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);
        WizardDevice.reopenApplication();

        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        MasterPage masterPage = new MasterPage();
        masterPage.clickBack();
        masterPage.leavePage();

        entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickManagePasscode();
        settingsListPage.leavePage();

        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSecondNextButton();
        enterPasscodePage.leavePage();

        // Get the current clientpolicy
        ClientPolicy clientPolicy = sapWizardApplication.getClientPolicyManager().getClientPolicy(true);
        PasscodePage.CreatePasscodePage createPasscodePage = new PasscodePage().new CreatePasscodePage();

        PasscodePageFactory.NewPasscodeFlow();

        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);
        WizardDevice.reopenApplication();

        enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        enterPasscodePage.enterPasscode(Credentials.NEWPASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        settingsListPage.clickResetApp();

        settingsListPage.clickYes();
        WelcomePage welcomePage = new WelcomePage();
    }


    @Test
    public void testPasscodeRetryLimitBackground() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();
        EntityListPage entityListPage = new EntityListPage(UIElements.EntityListScreen.entityList);
        entityListPage.clickFirstElement();
        entityListPage.leavePage();

        // Get the lockTimeOut (in seconds) from the SecureStoreManager
        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();

        // We put the app into background
        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000, activityTestRule);

        // We reopen the app
        WizardDevice.reopenApplication();
        // Try the retry limit flow
        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        ClientPolicyManager clientPolicyManager = ((SAPWizardApplication) activityTestRule.getActivity().getApplication()).getClientPolicyManager();
        for (int i = 0; i < clientPolicyManager.getClientPolicy(false).getPasscodePolicy().getRetryLimit(); i++) {
            enterPasscodePage.enterPasscode(Credentials.WRONGPASSCODE);
            enterPasscodePage.clickSignIn();
        }
        enterPasscodePage.clickResetAppButton();
    }

    @Test
    public void testManagePasscodeRetryLimitBack() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();
        
        EntityListPage entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();
        
        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickManagePasscode();
        settingsListPage.leavePage();
        
        // Try the retry limit flow
        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        ClientPolicyManager clientPolicyManager = ((SAPWizardApplication)activityTestRule.getActivity().getApplication()).getClientPolicyManager();
        int retryLimit = clientPolicyManager.getClientPolicy(false).getPasscodePolicy().getRetryLimit();
        for (int i = 0; i < retryLimit; i++) {
            enterPasscodePage.enterPasscode(Credentials.WRONGPASSCODE);
            enterPasscodePage.clickSecondNextButton();
        }
        Utils.pressBack();
        Utils.pressBack();
        enterPasscodePage.leavePage();
        settingsListPage = new SettingsListPage();
        settingsListPage.clickManagePasscode();

        enterPasscodePage.clickResetAppButton();
        }

    @Test
    public void testSetPasscodeBack() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboardingBack();

        EntityListPage entityListPage = new EntityListPage(entityList);
        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);
        settingsListPage.clickManagePasscode();
        settingsListPage.leavePage();

        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();

        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSecondNextButton();
        enterPasscodePage.leavePage();

        PasscodePageFactory.NewPasscodeFlowBack();

    }

    @Test
    public void testEnterPasscodeBack() {
        // Take care of welcome screen, authentication, and passcode flow.
        Utils.doOnboarding();

        EntityListPage entityListPage = new EntityListPage(entityList);
        // Get the lockTimeOut (in seconds) from the SecureStoreManager
        int lockTimeOut = ((SAPWizardApplication) activityTestRule.getActivity().getApplication())
                .getSecureStoreManager().getPasscodeLockTimeout();
        // We put the app into background
        WizardDevice.putApplicationBackground((lockTimeOut + 1) * 1000,activityTestRule);

        // We reopen the app
        WizardDevice.reopenApplication();

        PasscodePage.EnterPasscodePage enterPasscodePage = new PasscodePage().new EnterPasscodePage();
        Utils.pressBack();
        enterPasscodePage.leavePage();

        // We reopen the app
        WizardDevice.reopenApplication();

        enterPasscodePage = new PasscodePage().new EnterPasscodePage();

        enterPasscodePage.enterPasscode(Credentials.PASSCODE);
        enterPasscodePage.clickSignIn();
        enterPasscodePage.leavePage();

        entityListPage.clickSettings();
        entityListPage.leavePage();

        SettingsListPage settingsListPage = new SettingsListPage(UIElements.SettingsScreen.settingsList);

        settingsListPage.clickResetApp();

        settingsListPage.checkConfirmationDialog();

        settingsListPage.clickYes();

    }
}
