package minghai.skk;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.AutoText;

public class SKKsettings extends PreferenceActivity {
  
  private static final String QUICK_FIXES_KEY = "quick_fixes";
  private static final String SHOW_SUGGESTIONS_KEY = "show_suggestions";
  private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
  
  private CheckBoxPreference mQuickFixes;
  private CheckBoxPreference mShowSuggestions;
  
  @Override
  protected void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      addPreferencesFromResource(R.xml.prefs);
      mQuickFixes = (CheckBoxPreference) findPreference(QUICK_FIXES_KEY);
      mShowSuggestions = (CheckBoxPreference) findPreference(SHOW_SUGGESTIONS_KEY);
  }

  @Override
  protected void onResume() {
      super.onResume();
      int autoTextSize = AutoText.getSize(getListView());
      if (autoTextSize < 1) {
          ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY))
              .removePreference(mQuickFixes);
      } else {
          mShowSuggestions.setDependency(QUICK_FIXES_KEY);
      }
  }
}
