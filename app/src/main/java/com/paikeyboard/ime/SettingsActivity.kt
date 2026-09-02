package com.paikeyboard.ime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val haptic = androidx.preference.SwitchPreferenceCompat(context).apply {
                key = "haptic_feedback"
                title = "Haptic feedback"
                summary = "Light vibration on key press (power efficient)"
                setDefaultValue(true)
            }

            val sound = androidx.preference.SwitchPreferenceCompat(context).apply {
                key = "sound_feedback"
                title = "Key sound"
                summary = "Play subtle click sound"
                setDefaultValue(false)
            }

            val numberRow = androidx.preference.SwitchPreferenceCompat(context).apply {
                key = "show_number_row"
                title = "Always show number row"
                summary = "Faster number access (recommended)"
                setDefaultValue(true)
            }

            screen.addPreference(haptic)
            screen.addPreference(sound)
            screen.addPreference(numberRow)

            preferenceScreen = screen
        }
    }
}
