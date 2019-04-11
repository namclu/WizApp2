package com.company.wizapp2.test.pages;

import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.pgssoft.espressodoppio.idlingresources.ViewIdlingResource;
import com.company.wizapp2.test.core.UIElements;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.company.wizapp2.test.core.Constants.APPLICATION_AUTH_TYPE;

public class WelcomePage {

    // Default constructor
    public WelcomePage() {
        ViewIdlingResource viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
                withId(UIElements.WelcomePage.getStartedButton)).register();

    }

    public void clickGetStarted() {
        // Close the soft keyboard first, since it might be covering the get started button.
        onView(withId(UIElements.WelcomePage.getStartedButton)).perform(closeSoftKeyboard(), click());
    }

public void waitForCredentials() {
        if (APPLICATION_AUTH_TYPE == APPLICATION_AUTH_TYPE.BASIC) {
            UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiObject usernameField = uiDevice.findObject(new UiSelector()
                    .resourceId(UIElements.LoginScreen.BasicAuthScreen.usernameID));
            usernameField.waitForExists(2000);
        } else if (APPLICATION_AUTH_TYPE == APPLICATION_AUTH_TYPE.OAUTH || APPLICATION_AUTH_TYPE == APPLICATION_AUTH_TYPE.SAML) {
            UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiObject usernameField = uiDevice.findObject(new UiSelector()
                    .resourceId(UIElements.LoginScreen.OauthScreen.oauthUsernameText));
            usernameField.waitForExists(2000);
        }
    }
}
