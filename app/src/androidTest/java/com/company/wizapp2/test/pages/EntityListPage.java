package com.company.wizapp2.test.pages;


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
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

public class EntityListPage extends AbstractMasterDetailPage {

    public EntityListPage(int resourceID) {
        super(resourceID);
    }

    public EntityListPage() {
        super(UIElements.EntityListScreen.entityList);
    }

    @Override
    public void clickFirstElement() {
        onData(anything()).inAdapterView(withId(R.id.entity_list)).atPosition(0).perform(click());
    }

    @Override
    public void clickBack() {
        // There is no back ui element on this screen

    }

    public void clickSettings() {
        try {
            UiObject settingsButton = new UiObject(new UiSelector().descriptionContains(UIElements.EntityListScreen.settingsToolBar));
            settingsButton.click();
            UiObject settingsText = new UiObject(new UiSelector().textContains("Settings"));
            settingsText.click();
        } catch (UiObjectNotFoundException e) {
        }
    }


    public void clickEntity(int i) {
        onData(anything())
                .inAdapterView(withId(R.id.entity_list))
                .atPosition(i)
                .perform(click());

    }


}
