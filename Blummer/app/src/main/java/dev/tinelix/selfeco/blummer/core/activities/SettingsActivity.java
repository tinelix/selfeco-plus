package dev.tinelix.selfeco.blummer.core.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import dev.tinelix.selfeco.blummer.R;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    private SharedPreferences global_prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);
        listenPreferences();
        global_prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void listenPreferences() {
        Preference proxySettings = findPreference("proxySettings");
        proxySettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openProxySettingsDialog();
                return false;
            }
        });
    }

    @SuppressLint("InflateParams")
    private void openProxySettingsDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        View proxy_settings_view = getLayoutInflater().inflate(R.layout.dialog_proxy_settings, null, false);
        builder.setView(proxy_settings_view);
        final EditText proxy_address = proxy_settings_view.findViewById(R.id.proxy_address);
        final EditText proxy_port = proxy_settings_view.findViewById(R.id.proxy_port);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = global_prefs.edit();
                if(proxy_port.getText().length() > 0) {
                    editor.putString("proxyAddress", String.format("%s:%s",
                            proxy_address.getText().toString(), proxy_port.getText().toString()));
                } else {
                    editor.putString("proxyAddress", String.format("%s:8080",
                            proxy_address.getText().toString()));
                }
                editor.commit();
                if(global_prefs.contains("proxyAddress")) {
                    if(global_prefs.getString("proxyAddress", "").length() > 0) {
                        (findPreference("proxySettings")).setSummary(
                                global_prefs.getString("proxyAddress", "")
                        );
                    }
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        proxy_address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
                        proxy_address.getText().length() > 0 && isValidCharacters(proxy_address.getText().toString())
                );
            }
        });
        dialog.show();
        if(global_prefs.contains("proxyAddress")) {
            if (global_prefs.getString("proxyAddress", "").length() > 0) {
                String[] address_split =
                        global_prefs.getString("proxyAddress", "").split(":");
                proxy_address.setText(address_split[0]);
                proxy_port.setText(address_split[1]);
            }
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
                proxy_address.getText().length() > 0 &&
                        isValidCharacters(proxy_address.getText().toString())
        );
    }

    private boolean isValidCharacters(String text) {
        return !text.contains(":") && !text.contains("/") && !text.contains("@");
    }
}
