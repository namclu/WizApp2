package com.company.wizapp2.test.testcases.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.company.wizapp2.app.ErrorMessage;
import com.company.wizapp2.app.SAPWizardApplication;
import com.company.wizapp2.logon.LogonActivity;
import com.company.wizapp2.test.core.BaseTest;
import com.company.wizapp2.test.pages.ErrorPage;
import com.company.wizapp2.test.pages.WelcomePage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ErrorHandlerTests extends BaseTest {

    @Rule
    public ActivityTestRule<LogonActivity> activityTestRule = new ActivityTestRule<>(LogonActivity.class);

    @Test
    public void errorDialogGetsShown() throws InterruptedException {
        String errorTitle = "Error Title";
        String errorDetails = "This is the details of the error.";
        Exception exception = new Exception();
        ErrorMessage errorMessage = new ErrorMessage(errorTitle, errorDetails, exception, false);
        ((SAPWizardApplication)getInstrumentation().getTargetContext().getApplicationContext())
                .getErrorHandler().sendErrorMessage(errorMessage);

        ErrorPage errorPage = new ErrorPage(activityTestRule.getActivity());
        assertEquals("Unexpected error title", errorTitle, errorPage.getErrorTitle());
        assertEquals("Unexpected error message", errorDetails, errorPage.getErrorMessage());

        errorPage.dismiss();
    }

    @Test
    public void errorDialogsShownInOrder() throws InterruptedException {
        String errorTitle1 = "Error Title 1";
        String errorDetails1 = "This is the details of the first error.";
        Exception exception = new Exception();
        ErrorMessage errorMessage1 = new ErrorMessage(errorTitle1, errorDetails1, exception, false);
        ((SAPWizardApplication)getInstrumentation().getTargetContext().getApplicationContext())
                .getErrorHandler().sendErrorMessage(errorMessage1);

        String errorTitle2 = "Error Title 2";
        String errorDetails2 = "This is the details of the second error.";
        ErrorMessage errorMessage2 = new ErrorMessage(errorTitle2, errorDetails2, exception, false);
        ((SAPWizardApplication)getInstrumentation().getTargetContext().getApplicationContext())
                .getErrorHandler().sendErrorMessage(errorMessage2);

        ErrorPage errorPage = new ErrorPage(activityTestRule.getActivity());
        assertEquals("Unexpected error title", errorTitle1, errorPage.getErrorTitle());
        assertEquals("Unexpected error message", errorDetails1, errorPage.getErrorMessage());
        errorPage.dismiss();

        ErrorPage errorPage2 = new ErrorPage(activityTestRule.getActivity());
        assertEquals("Unexpected error title", errorTitle2, errorPage2.getErrorTitle());
        assertEquals("Unexpected error message", errorDetails2, errorPage2.getErrorMessage());
        errorPage.dismiss();
    }
}
