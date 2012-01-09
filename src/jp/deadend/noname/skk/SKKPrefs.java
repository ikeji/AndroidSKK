package jp.deadend.noname.skk;

import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SKKPrefs extends PreferenceActivity {

    private static final String PREFKEY_DICT_DIR = "pref_dict_dir";
    private static final String PREFKEY_KUTOUTEN_TYPE = "pref_kutouten_type";
    private static final String PREFKEY_KANA_KEY = "pref_kana_key";
  
    @Override protected void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	addPreferencesFromResource(R.xml.prefs);

	Preference kanaKeyPref = getPreferenceManager().findPreference(PREFKEY_KANA_KEY);
	if (kanaKeyPref != null) {
	  SetKeyPreference config = (SetKeyPreference)kanaKeyPref;
	  config.setKey(PREFKEY_KANA_KEY);
	  config.setPrefs(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));
        }
    }

    public static String getPrefDictDir(Context context) {
	return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_DICT_DIR, "/sdcard/.skk");
    }

    public static String getPrefKutoutenType(Context context) {
	return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_KUTOUTEN_TYPE, "en");
    }
  /*
    public static String getPrefDefaultMode(Context context) {
	return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_DEFAULT_MODE, "");
    }
  */

    public static int getPrefKanaKey(Context context) {
	return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KANA_KEY, 93);
    }
}
