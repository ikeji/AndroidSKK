package jp.deadend.noname.skk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.TextView;

public class SetKeyPreference extends DialogPreference implements OnKeyListener {

  private int mKeyCode;
  private TextView mKeyCodeText;
  private SharedPreferences mSharedPrefs;
  private String mKey = null;

  public SetKeyPreference(Context context, AttributeSet attrs) {
	super(context, attrs);
  }
  
  public SetKeyPreference(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
  }

  @Override protected View onCreateDialogView() {
	if (mSharedPrefs != null && mKey != null) {
	  mKeyCode = mSharedPrefs.getInt(mKey, 118);
	} else {
	  mKeyCode = 118;
	}

	mKeyCodeText = new TextView(this.getContext());
	mKeyCodeText.setText(" Key code: " + mKeyCode);
	mKeyCodeText.setGravity(Gravity.CENTER);
	mKeyCodeText.setTextSize(30);
	return mKeyCodeText;
  }

  @Override protected void showDialog(Bundle state) {
	super.showDialog(state);
	getDialog().setOnKeyListener(this);
	getDialog().takeKeyEvents(true);
  }
  
  public void setPrefs(SharedPreferences sharedPreferences) {
	mSharedPrefs = sharedPreferences;
  }

  public void setKey(String s) {
	mKey = s;
  }

  @Override protected void onDialogClosed(boolean positiveResult) {
	super.onDialogClosed(positiveResult);
                
	if (positiveResult) {
	  // save changes
	  SharedPreferences.Editor editor = mSharedPrefs.edit();
	  editor.putInt(mKey, mKeyCode);
	  editor.commit();
	}
  }

  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
	switch (keyCode) {
	case KeyEvent.KEYCODE_BACK:
	  return false;
	case KeyEvent.KEYCODE_ENTER:
	  getDialog().dismiss();
	  return true;
	default:
	  mKeyCodeText.setText(" Key code: " + mKeyCode);
	  mKeyCode = keyCode;
	  return true;
	}
  }
}
