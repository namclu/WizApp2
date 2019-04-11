package com.company.wizapp2.test.pages;

import android.os.SystemClock;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.pgssoft.espressodoppio.idlingresources.ViewIdlingResource;
import com.company.wizapp2.R;
import com.company.wizapp2.test.core.AbstractPasscodePage;
import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.Utils;
import com.company.wizapp2.test.core.WizardDevice;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class PasscodePage {


    public class CreatePasscodePage extends AbstractPasscodePage {
        private ViewIdlingResource viewIdlingResource;

        public CreatePasscodePage() {
            viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
                    withId(UIElements.PasscodeScreen.createPasscodeText)).register();
        }

        public void createPasscode(String passcode) {
            WizardDevice.fillInputField(UIElements.PasscodeScreen.createPasscodeText, passcode);
        }

        public void leavePage() {
            viewIdlingResource.unregister();
        }

        public void clickSignIn() {
            this.clickNext();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }

        public void clickUseDefault() {
            this.clickDefault();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }
    }

    public class VerifyPasscodePage extends AbstractPasscodePage {
        private ViewIdlingResource viewIdlingResource;

        public VerifyPasscodePage() {
            viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
                    withId(UIElements.PasscodeScreen.verifyPasscodeText)).register();
        }

        public void verifyPasscode(String passcode) {
            WizardDevice.fillInputField(UIElements.PasscodeScreen.verifyPasscodeText, passcode);
        }

        public void leavePage() {
            viewIdlingResource.unregister();
        }

        public void clickBack() {
             onView(withId(UIElements.PasscodeScreen.backButton)).perform(click());
        }

        public void clickSignIn() {
            this.clickNext();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }
    }

    public class SetFingerprintPage extends AbstractPasscodePage {
        private ViewIdlingResource viewIdlingResource;

        public SetFingerprintPage() {
            viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
            withId(UIElements.SetFingerprintPage.confirmFingerprintLabel)).register();
        }

        public void leavePage() {
            viewIdlingResource.unregister();
        }
    }


    public class EnterPasscodePage extends AbstractPasscodePage {
        private ViewIdlingResource viewIdlingResource;

        public EnterPasscodePage() {
            viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
                    withId(UIElements.PasscodeScreen.verifyPasscodeText)).register();
        }

        public void enterPasscode(String passcode) {

            if (WizardDevice.fromBackground) {
                try {
                    new UiObject(new UiSelector().resourceId("com.company.wizapp2:id/passcode_field")).setText(passcode);
                } catch (UiObjectNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                WizardDevice.fillInputField(UIElements.PasscodeScreen.enterPasscodeText, passcode);
            }
        }

        public void leavePage() {
            viewIdlingResource.unregister();
        }

        public void clickSignIn() {
            this.clickNext();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }

        public void clickSecondNextButton() {
            this.clickSecondNext();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }

        public void clickCancel() {
            this.clickCancelButton();
            // Wait 2 sec
            SystemClock.sleep(2000);
        }

        public void clickResetAppButton() {
            onView(withId(UIElements.PasscodeScreen.resetAppButton))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                    .perform(click());
        }

    }

}
