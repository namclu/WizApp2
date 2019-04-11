package com.company.wizapp2.mdui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.company.wizapp2.R

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager.beginTransaction().replace(R.id.settings_container, SettingsFragment()).commit()
    }
}