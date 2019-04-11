package com.company.wizapp2.test.pages;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ErrorPage {
    private static final int WAIT_TIMEOUT = 2000;
    UiDevice device;
    Activity activity;

    public ErrorPage(Activity activity) {
        this.activity = activity;
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    public String getErrorTitle() throws InterruptedException {
        UiObject usernameField = device.findObject(new UiSelector()
                .resourceId(UIElements.ErrorScreen.titleResourceId));
        usernameField.waitForExists(WAIT_TIMEOUT);
        // For some reason android.R.id.alertTitle isn't defined, so we have to use getIdentifier.
        return Utils.getStringFromUiWithId(activity.getResources().getIdentifier("alertTitle", "id", "android"));
    }

    public String getErrorMessage() throws InterruptedException {
        UiObject usernameField = device.findObject(new UiSelector()
                .resourceId(UIElements.ErrorScreen.messageResourceId));
        usernameField.waitForExists(WAIT_TIMEOUT);
        return Utils.getStringFromUiWithId(UIElements.ErrorScreen.messageId);
    }

    public void dismiss() {
        onView(withId(UIElements.ErrorScreen.okButton)).check(matches(isDisplayed())).perform(click());
    }
}
