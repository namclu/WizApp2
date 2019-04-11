package com.company.wizapp2.test.core;

import android.support.test.uiautomator.UiDevice;


public abstract class AbstractLoginPage {
    protected final int WAIT_TIMEOUT = 10000;
    protected UiDevice uiDevice;

    public abstract void authenticate();

}
