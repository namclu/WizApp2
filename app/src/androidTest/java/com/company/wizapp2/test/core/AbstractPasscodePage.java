package com.company.wizapp2.test.core;

import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public abstract class AbstractPasscodePage {

    public void clickNext() {
        if (WizardDevice.fromBackground) {
            try {
                new UiObject(new UiSelector().resourceId("com.company.wizapp2:id/done_button")).click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            onView(withId(UIElements.PasscodeScreen.nextButton)).perform(click());
        }
    }

    public void clickSecondNext() {
        onView(withId(UIElements.PasscodeScreen.secondNextButton)).perform(click());
    }

    public void skipFingerprint() {
        onView(withId(UIElements.SetFingerprintPage.skipFingerpintButton)).perform(click());
    }

    public void clickCancelButton() {
        onView(withId(UIElements.PasscodeScreen.cancelButton)).perform(click());
    }

    public void clickDefault() {
        onView(withId(UIElements.PasscodeScreen.useDefaultButton)).perform(click());
    }
}
