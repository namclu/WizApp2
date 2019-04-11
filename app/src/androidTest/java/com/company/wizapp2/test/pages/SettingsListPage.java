package com.company.wizapp2.test.pages;

import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.PreferenceMatchers;

import com.company.wizapp2.R;
import com.company.wizapp2.app.SAPWizardApplication;
import com.company.wizapp2.test.core.AbstractMasterDetailPage;
import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.Utils;
import com.company.wizapp2.test.core.matcher.ToastMatcher;

import ch.qos.logback.classic.Level;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

public class SettingsListPage extends AbstractMasterDetailPage {

    public SettingsListPage(int resourceID) {
        super(resourceID);
    }

    public SettingsListPage() {
        super((R.id.recycler_view));
    }

    @Override
    public void clickFirstElement() {
        onData(anything()).inAdapterView(withId(android.R.id.list)).atPosition(0).perform(click());
    }

    @Override
    public void clickBack() {
        // There is no back ui element on this screen
        onView(withContentDescription(UIElements.MasterScreen.toolBarBackButton)).perform(click());
    }

    public void clickLogLevel() {
        onData(PreferenceMatchers.withKey(Utils.getResourceString(R.string.log_level)))
                .perform(click());
    }

    public void clickUploadLog() {
      onView(withText(R.string.upload_log_summary)).perform(click());
    }

    public void clickManagePasscode() {
        onView(withId( UIElements.SettingsScreen.settingsList))
            .perform(RecyclerViewActions
                .actionOnItem(hasDescendant(withText( R.string.manage_passcode )), click()) );
    }

    public void clickResetApp() {
        onView(withText(R.string.reset_app_summary)).perform(click());
    }

    public void clickYes() {
        onView(withId(UIElements.SettingsScreen.yesButtonResetApp))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
    }

    public void clickCancelOnDialog() {
        onView(withId(UIElements.SettingsScreen.noButtonResetApp))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    /* Checkers */

    public void checkLoglevel(String expectedLoglevel) {
        onView(withText(expectedLoglevel)).check(matches(isDisplayed()));
    }

    public void checkLogUploadToast() {

        Level policyLevel = ((SAPWizardApplication) getInstrumentation().getTargetContext().getApplicationContext())
                .getClientPolicyManager().getClientPolicy(true).getLogLevel();

        if (!policyLevel.levelStr.equals("OFF")) {
        onView(withText(R.string.log_upload_ok)).inRoot(new ToastMatcher())
                .check(matches(withText(R.string.log_upload_ok)));
        }

    }

     public void checkConfirmationDialog() {
         onView(withText(R.string.reset_app_confirmation))
                .check(matches(isDisplayed()));
     }

}
