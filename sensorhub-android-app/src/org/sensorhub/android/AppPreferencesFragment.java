package org.sensorhub.android;

import static android.content.Context.WIFI_SERVICE;

import android.app.LocaleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.LocaleList;
import android.preference.PreferenceManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


public class AppPreferencesFragment extends PreferenceFragmentCompat {
    private static final String[] LANGUAGE_LABELS = {"English", "中文 (台灣)", "Español", "Français", "Deutsch", "Italiano", "Português"};
    private static final String[] LANGUAGE_VALUES = {"en", "zh-TW", "es", "fr", "de", "it", "pt"};
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_app, rootKey);

        Preference languagePref = findPreference("app_language");
        if (languagePref != null) {
            languagePref.setOnPreferenceClickListener(preference -> {
                showLanguageDialog();
                return true;
            });
        }

        Preference aboutPref = findPreference("app_about");
        if (aboutPref != null) {
            aboutPref.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.about_description)
                    .setIcon(R.drawable.ic_launcher)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
                return true;
            });
        }

        Preference helpPref = findPreference("app_help");
        if (helpPref != null) {
            helpPref.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), HelpFaqActivity.class));
                return true;
            });
        }

        Preference versionPref = findPreference("app_version");
        if (versionPref != null) {
            String version = getString(R.string.title_version);
            try {
                PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            versionPref.setSummary(version);
        }

        //DEVICE IP ADDRESS
       getDeviceIpAddress();
    }

    private void getDeviceIpAddress() {
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = getString(R.string.unable_to_get_ip);
        }

        Preference ipAddressLabel = getPreferenceScreen().findPreference("nop_ipAddress");
        ipAddressLabel.setSummary(ipAddressString);
    }


    private void showLanguageDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String currentLanguage = prefs.getString("app_language", "en");

        int checkedIndex = 0;
        for (int i = 0; i < LANGUAGE_VALUES.length; i++) {
            if (LANGUAGE_VALUES[i].equals(currentLanguage)) {
                checkedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_language_dialog)
                .setSingleChoiceItems(LANGUAGE_LABELS, checkedIndex, (dialog, which) -> {
                    String selected = LANGUAGE_VALUES[which];
                    prefs.edit().putString("app_language", selected).apply();
                    applyLanguage(selected);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void applyLanguage(String localeTag) {
        LocaleManager localeManager = requireContext().getSystemService(LocaleManager.class);
        localeManager.setApplicationLocales(LocaleList.forLanguageTags(localeTag));
    }
}
