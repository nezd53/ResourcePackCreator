package com.nezdats.rpc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final String KEY_CUSTOM_TEMPLATE = "pref_custom_template";
    static final String KEY_RESIZE_IMAGE = "pref_resize_image";
    static final String KEY_SHOW_UNSUPPORTED = "pref_show_unsupported";


    private static boolean customTemplate;
    private static boolean resizeImage;
    private static boolean showUnsupported;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        // set summary for each preference based on current setting
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        findPreference(KEY_CUSTOM_TEMPLATE).setSummary(sharedPreferences.getBoolean(KEY_CUSTOM_TEMPLATE, false) ?
                R.string.settings_resource_pack_template_summary_on : R.string.settings_resource_pack_template_summary_off);
        findPreference(KEY_RESIZE_IMAGE).setSummary(sharedPreferences.getBoolean(KEY_RESIZE_IMAGE, true) ?
                R.string.settings_resize_image_summary_on : R.string.settings_resize_image_summary_off);
        //findPreference(KEY_SHOW_UNSUPPORTED).setSummary(sharedPreferences.getBoolean(KEY_SHOW_UNSUPPORTED, false) ?
                //R.string.settings_show_unsupported_summary_on : R.string.settings_show_unsupported_summary_off);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case KEY_CUSTOM_TEMPLATE: {
                setCustomTemplate(sharedPreferences.getBoolean(KEY_CUSTOM_TEMPLATE, false));
                Preference p = findPreference(KEY_CUSTOM_TEMPLATE);
                p.setSummary(isCustomTemplate() ?
                        R.string.settings_resource_pack_template_summary_on : R.string.settings_resource_pack_template_summary_off);
                break;
            }
            case KEY_RESIZE_IMAGE: {
                setResizeImage(sharedPreferences.getBoolean(KEY_RESIZE_IMAGE, true));
                findPreference(KEY_RESIZE_IMAGE).setSummary(isResizeImage() ?
                        R.string.settings_resize_image_summary_on : R.string.settings_resize_image_summary_off);
                break;
            }
            case KEY_SHOW_UNSUPPORTED: {
                setShowUnsupported(sharedPreferences.getBoolean(KEY_SHOW_UNSUPPORTED, false));
                findPreference(KEY_SHOW_UNSUPPORTED).setSummary(isShowUnsupported() ?
                        R.string.settings_show_unsupported_summary_on : R.string.settings_show_unsupported_summary_off);
                break;
            }
        }
    }

    public static void setCustomTemplate(Boolean b) {
        customTemplate = b;
    }

    public static boolean isCustomTemplate() {
        return customTemplate;
    }

    public static void setResizeImage(boolean resizeImage) {
        SettingsFragment.resizeImage = resizeImage;
    }

    public static boolean isResizeImage() {
        return resizeImage;
    }

    public static void setShowUnsupported(boolean showUnsupported) {
        SettingsFragment.showUnsupported = showUnsupported;
    }

    public static boolean isShowUnsupported() {
        return showUnsupported;
    }


    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
