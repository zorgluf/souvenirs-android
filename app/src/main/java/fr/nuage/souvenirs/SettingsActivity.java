package fr.nuage.souvenirs;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

public class SettingsActivity extends AppCompatActivity {

    public static final String ALBUMS_PATH = "albums_path";
    public static final String NEXTCLOUD_ENABLED = "nextcloud_enabled";
    public static final String NEXTCLOUD_WIFIONLY = "nextcloud_wifionly";
    public static final String NEXTCLOUD_VERSION = "nextcloud_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_main, rootKey);

            //activate nextcloud SSO when nextcloud activated
            Fragment that = this;
            findPreference(NEXTCLOUD_ENABLED).setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue.equals(true)) {
                    try {
                        AccountImporter.pickNewAccount(that);
                    } catch (NextcloudFilesAppNotInstalledException e) {
                        UiExceptionManager.showDialogForException(getContext(), e);
                    } catch (AndroidGetAccountsPermissionNotGranted e) {
                        AccountImporter.requestAndroidAccountPermissionsAndPickAccount(getActivity());
                    }
                    return false;
                } else {
                    preference.setSummary(null);
                }
                return true;
            });
            //get current SSO account
            try {
                SingleSignOnAccount ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(getContext());
                findPreference(NEXTCLOUD_ENABLED).setSummary(ssoAccount.name);
            } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
            }


            //display version
            findPreference(NEXTCLOUD_VERSION).setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            try {
                AccountImporter.onActivityResult(requestCode, resultCode, data, this, new AccountImporter.IAccountAccessGranted() {
                    @Override
                    public void accountAccessGranted(SingleSignOnAccount account) {
                        SingleAccountHelper.setCurrentAccount(getActivity(), account.name);
                        SingleSignOnAccount ssoAccount = null;
                        try {
                            ssoAccount = SingleAccountHelper.getCurrentSingleSignOnAccount(getContext());
                            findPreference(NEXTCLOUD_ENABLED).setSummary(ssoAccount.name);
                            ((SwitchPreference)findPreference(NEXTCLOUD_ENABLED)).setChecked(true);
                        } catch (NextcloudFilesAppAccountNotFoundException | NoCurrentAccountSelectedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (AccountImportCancelledException ignored) {
            }
        }


        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            AccountImporter.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }

}
