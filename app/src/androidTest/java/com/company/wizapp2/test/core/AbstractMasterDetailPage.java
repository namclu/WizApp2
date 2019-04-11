package com.company.wizapp2.test.core;

import com.pgssoft.espressodoppio.idlingresources.ViewIdlingResource;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public abstract class AbstractMasterDetailPage {

    private ViewIdlingResource viewIdlingResource;

    protected AbstractMasterDetailPage(int resourceID) {
        viewIdlingResource = (ViewIdlingResource) new ViewIdlingResource(
                withId(resourceID)).register();
    }

    public abstract void clickFirstElement();

    public abstract void clickBack();

    public void checkPageVisible(int resourceId) {
        onView(withId(resourceId))
                .check(matches(isDisplayed()));
    }

    public void checkFloatingButton(boolean shouldExists) {

        if (shouldExists) {
            onView(withId(UIElements.MasterScreen.floatingActionButton))
                    .check(matches(isDisplayed()));
        } else {
            onView(withId(UIElements.MasterScreen.floatingActionButton))
                    .check(doesNotExist());
        }

    }

    public void leavePage() {
        if (viewIdlingResource != null) {
            viewIdlingResource.unregister();
        }
    }

}
