package com.company.wizapp2.test.pages;

import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.company.wizapp2.R;
import com.company.wizapp2.test.core.AbstractMasterDetailPage;
import com.company.wizapp2.test.core.UIElements;
import com.company.wizapp2.test.core.WizardDevice;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

public class MasterPage extends AbstractMasterDetailPage {

    public MasterPage() {
        super(UIElements.MasterScreen.refreshButton);
    }

    public MasterPage(int resourceID) {
        super(resourceID);
    }

    @Override
    public void clickFirstElement() {
        // If the current ui is the itemlist page
        onView(withId(R.id.item_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    }

    @Override
    public void clickBack() {

        if (WizardDevice.fromBackground) {
            try {
                new UiObject(new UiSelector().descriptionContains(UIElements.MasterScreen.toolBarBackButton)).click();
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            onView(withContentDescription(UIElements.MasterScreen.toolBarBackButton)).perform(click());
        }
    }

    public void clickMaster(int i) {
        onData(anything())
                .inAdapterView(withId(R.id.item_list))
                .atPosition(i)
                .perform(click());

    }
}
