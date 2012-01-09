package jp.deadend.noname.skk;

import android.view.KeyEvent;
import android.util.Log;

public class SKKMetaKey {
  public static final int KEY_NUM = 2;
  public static final int SHIFT_KEY = 0;
  public static final int ALT_KEY = 1;

  public static final int STATE_NONE = 0;
  public static final int STATE_ON = 1;
  public static final int STATE_LOCKED = 2;

  private int[] mState = new int[KEY_NUM];
  private boolean[] isPressed = new boolean[KEY_NUM];
  private boolean[] isUsed = new boolean[KEY_NUM];

  public SKKMetaKey() {
	clearMetaKeyState();
  }

  public void clearMetaKeyState() {
	for (int i=0; i<KEY_NUM; i++) {
	  mState[i] = STATE_NONE;
	  isPressed[i] = false;
	  isUsed[i] = false;
	}
  }
  public void pushMetaKey(int key) {
	  isPressed[key] = true;
	  isUsed[key] = false;
	  switch (mState[key]) {
	  case STATE_NONE:
		mState[key] = STATE_ON;
		SKKUtils.dlog("metakey(" + key + "): off->on");
		break;
	  case STATE_ON:
		mState[key] = STATE_LOCKED;
		SKKUtils.dlog("metakey(" + key + "): on->locked");
		break;
	  case STATE_LOCKED:
		SKKUtils.dlog("metakey(" + key + "): locked->off");
		mState[key] = STATE_NONE;
		break;
	  }
  }

  public void releaseMetaKey(int key) {
	isPressed[key] = false;
	SKKUtils.dlog("metakey(" + key + ") released");
	if (mState[key] == STATE_ON && isUsed[key]) {
	  mState[key] = STATE_NONE;
	  SKKUtils.dlog("metakey(" + key + "): on->off");
	}
  }

  public int useMetaState() {
	int meta = 0;

	if (useMetaKey(SHIFT_KEY)) meta |= KeyEvent.META_SHIFT_ON;
	if (useMetaKey(ALT_KEY)) meta |= KeyEvent.META_ALT_ON;

	return meta;
  }	

  private boolean useMetaKey(int key) {
	switch (mState[key]) {
	case STATE_NONE:
	  SKKUtils.dlog("metakey(" + key + "): off");
	  return false;
	case STATE_ON:
	  if (isPressed[key]) {
		SKKUtils.dlog("metakey(" + key + "): on");
		isUsed[key] = true;
	  } else {
		mState[key] = STATE_NONE;
		SKKUtils.dlog("metakey(" + key + "): on->off");
	  }
	  return true;
	case STATE_LOCKED:
	  SKKUtils.dlog("metakey(" + key + "): on");
	  return true;
	}

	return false;
  }
}