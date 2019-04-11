package com.company.wizapp2.logon;

import android.support.v4.app.Fragment
import com.sap.cloud.mobile.onboarding.passcode.PasscodeValidationActionHandler
import com.sap.cloud.mobile.onboarding.passcode.PasscodeValidationException

class PasscodeValidationActionHandler: PasscodeValidationActionHandler {

    @Throws(PasscodeValidationException::class, InterruptedException::class)
    override fun validate(fragment: Fragment, chars: CharArray) {
        // You can extend the validator with your own policy.
    }
}