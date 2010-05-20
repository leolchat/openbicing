package net.homelinux.penecoptero.android.openbicing.app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(
				OpenBicing.PREFERENCES_NAME);
		addPreferencesFromResource(R.xml.preferences);

		PreferenceScreen psLocation = (PreferenceScreen) this
				.findPreference("openbicing.preferences_location");
		psLocation
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						// TODO Auto-generated method stub
						launchLocationSettings();
						return false;
					}
				});
	}

	private void launchLocationSettings() {
		final Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		this.startActivity(intent);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		this.setResult(RESULT_OK);
	}
}
