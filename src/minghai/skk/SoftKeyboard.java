/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package minghai.skk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import static android.view.inputmethod.EditorInfo.*;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import static minghai.skk.InputMode.*;

/**
 * Example of writing an input method for a soft keyboard. This code is focused
 * on simplicity over completeness, so it should in no way be considered to be a
 * complete soft keyboard implementation. Its purpose is to provide a basic
 * example for how you would get started writing an input method, to be fleshed
 * out as appropriate.
 */
public class SoftKeyboard extends InputMethodService implements
    KeyboardView.OnKeyboardActionListener {
  static final boolean DEBUG = false;

  /**
   * This boolean indicates the optional example code for performing processing
   * of hard keys in addition to regular text generation from on-screen
   * interaction. It would be used for input methods that perform language
   * translations (such as converting text entered on a QWERTY keyboard to
   * Chinese), but may not be used for input methods that are primarily intended
   * to be used for on-screen text entry.
   */
  static final boolean PROCESS_HARD_KEYS = true;
  static final String DICTIONARY = "/sdcard/skk_dict_btree";
  static final String BTREE_NAME = "skk_dict";

  private LatinKeyboardView mInputView;
  private CandidateViewContainer mCandidateViewContainer;
  private CandidateView mCandidateView;
  private CompletionInfo[] mCompletions;
  private ArrayList<String> mSuggestions;

  private StringBuilder mComposing = new StringBuilder();
  private StringBuilder mKanji = new StringBuilder();
  private boolean mPredictionOn;
  private boolean mCompletionOn;
  private int mLastDisplayWidth;
  private boolean mCapsLock;
  private long mLastShiftTime;
  private long mMetaState;

  public int mChoosedIndex;

  private KeyboardSwitcher mKeyboardSwitcher;

  private LatinKeyboard mCurKeyboard;
  
  private AudioManager mAudioManager;
  private final float FX_VOLUME = 1.0f;
  private boolean mSilentMode;

  private String mWordSeparators;

  private InputMode mInputMode = HIRAKANA;
  private boolean isOkurigana = false;
  private String mOkurigana = null;
  private ArrayList<String> mCandidateList;
  
  private BTree mBTree;

  // ローマ字辞書
  private HashMap<String, String> mRomajiMap = new HashMap<String, String>();
	{
		HashMap<String, String> m = mRomajiMap;
		m.put("a", "あ");m.put("i", "い");m.put("u", "う");m.put("e", "え");m.put("o", "お");
		m.put("ka", "か");m.put("ki", "き");m.put("ku", "く");m.put("ke", "け");m.put("ko", "こ");
		m.put("sa", "さ");m.put("si", "し");m.put("su", "す");m.put("se", "せ");m.put("so", "そ");
		m.put("ta", "た");m.put("ti", "ち");m.put("tu", "つ");m.put("te", "て");m.put("to", "と");
		m.put("na", "な");m.put("ni", "に");m.put("nu", "ぬ");m.put("ne", "ね");m.put("no", "の");
		m.put("ha", "は");m.put("hi", "ひ");m.put("hu", "ふ");m.put("he", "へ");m.put("ho", "ほ");
		m.put("ma", "ま");m.put("mi", "み");m.put("mu", "む");m.put("me", "め");m.put("mo", "も");
		m.put("ya", "や");                 m.put("yu", "ゆ");                  m.put("yo", "よ");
		m.put("ra", "ら");m.put("ri", "り");m.put("ru", "る");m.put("re", "れ");m.put("ro", "ろ");
		m.put("wa", "わ");m.put("wi", "うぃ");m.put("we", "うぇ");m.put("wo", "を");m.put("nn", "ん");
		m.put("ga", "が");m.put("gi", "ぎ");m.put("gu", "ぐ");m.put("ge", "げ");m.put("go", "ご");
		m.put("za", "ざ");m.put("zi", "じ");m.put("zu", "ず");m.put("ze", "ぜ");m.put("zo", "ぞ");
		m.put("da", "だ");m.put("di", "ぢ");m.put("du", "づ");m.put("de", "で");m.put("do", "ど");
		m.put("ba", "ば");m.put("bi", "び");m.put("bu", "ぶ");m.put("be", "べ");m.put("bo", "ぼ");
		m.put("pa", "ぱ");m.put("pi", "ぴ");m.put("pu", "ぷ");m.put("pe", "ぺ");m.put("po", "ぽ");
		m.put("va", "う゛ぁ");m.put("vi", "う゛ぃ");m.put("vu", "う゛");m.put("ve", "う゛ぇ");m.put("vo", "う゛ぉ");

		m.put("xa", "ぁ");m.put("xi", "ぃ");m.put("xu", "ぅ");m.put("xe", "ぇ");m.put("xo", "ぉ");
		m.put("xtu", "っ");m.put("xke", "ヶ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("fa", "ふぁ");m.put("fi", "ふぃ");m.put("fu", "ふぅ");m.put("fe", "ふぇ");m.put("fo", "ふぉ");

		m.put("xya", "ゃ");                m.put("xyu", "ゅ");              m.put("xyo", "ょ");
		m.put("kya", "きゃ");              m.put("kyu", "きゅ");             m.put("kyo", "きょ");
		m.put("gya", "ぎゃ");              m.put("gyu", "ぎゅ");             m.put("gyo", "ぎょ");
		m.put("sya", "しゃ");              m.put("syu", "しゅ");             m.put("syo", "しょ");
		m.put("sha", "しゃ");m.put("shi", "し");m.put("shu", "しゅ");m.put("she", "しぇ");m.put("sho", "しょ");
		m.put("ja",  "じゃ");m.put("ji",  "じ");m.put("ju", "じゅ");m.put("je", "じぇ");m.put("jo", "じょ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("tya", "ちゃ");              m.put("tyu", "ちゅ");m.put("tye", "ちぇ");m.put("tyo", "ちょ");
		m.put("dha", "でゃ");m.put("dhi", "でぃ");m.put("dhu", "でゅ");m.put("dhe", "でぇ");m.put("dho", "でょ");
		m.put("dya", "ぢゃ");m.put("dyi", "ぢぃ");m.put("dyu", "ぢゅ");m.put("dye", "ぢぇ");m.put("dyo", "ぢょ");
		m.put("nya", "にゃ");              m.put("nyu", "にゅ");             m.put("nyo", "にょ");
		m.put("hya", "ひゃ");              m.put("hyu", "ひゅ");             m.put("hyo", "ひょ");
		m.put("pya", "ぴゃ");              m.put("pyu", "ぴゅ");             m.put("pyo", "ぴょ");
		m.put("bya", "びゃ");              m.put("byu", "びゅ");             m.put("byo", "びょ");
    m.put("mya", "みゃ");              m.put("myu", "みゅ");             m.put("myo", "みょ");
    m.put("rya", "りゃ");              m.put("ryu", "りゅ");m.put("rye", "りぇ");m.put("ryo", "りょ");   
	}
    
    /**
   * Main initialization of the input method component. Be sure to call to super
   * class.
   */
  @Override
  public void onCreate() {
    super.onCreate();
    mKeyboardSwitcher = new KeyboardSwitcher(this);
    mWordSeparators = getResources().getString(R.string.word_separators);
    
    // register to receive ringer mode changes for silent mode
    IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
    registerReceiver(mReceiver, filter);
    
    // Open Dictionary
    RecordManager recman;
    long          recid;
    Properties    props;

    props = new Properties();
    try {
      recman = RecordManagerFactory.createRecordManager( DICTIONARY, props );

      // try to reload an existing B+Tree
      recid = recman.getNamedObject( BTREE_NAME );
      if (recid == 0) {
        Log.d("TEST", "Dictionary not found: " + DICTIONARY);
      }

      mBTree = BTree.load( recman, recid );
    
    } catch (IOException e) {
      Log.e("TEST", e.toString());
      Toast t = new Toast(this);
      t.setDuration(Toast.LENGTH_SHORT);
      t.setText("Dictionary not found/loaded: " + DICTIONARY);
      t.show();
    }
  }
  
  @Override public void onDestroy() {
      // mUserDictionary.close();
      unregisterReceiver(mReceiver);
      super.onDestroy();
  }

  @Override
  public void onConfigurationChanged(Configuration conf) {
    /*
      if (!TextUtils.equals(conf.locale.toString(), mLocale)) {
          initSuggest(conf.locale.toString());
      }
    */
      super.onConfigurationChanged(conf);
  }

  /**
   * Called by the framework when your view for creating input needs to be
   * generated. This will be called the first time your input method is
   * displayed, and every time it needs to be re-created such as due to a
   * configuration change.
   */
  @Override
  public View onCreateInputView() {
    Log.d("TEST", "onCreateInputView(): isFullsreenMode() = " + isFullscreenMode());
    mInputView = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.input,
        null);
    mKeyboardSwitcher.setInputView(mInputView);
    mKeyboardSwitcher.makeKeyboards();
    mInputView.setOnKeyboardActionListener(this);
    mInputView.setShifted(false);
    mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT, 0);
    return mInputView;
  }

  /**
   * Called by the framework when your view for showing candidates needs to be
   * generated, like {@link #onCreateInputView}.
   */
  @Override
  public View onCreateCandidatesView() {
    Log.d("TEST", "onCreateCandidatesView(): isFullscreenMode() = " + isFullscreenMode());
    mKeyboardSwitcher.makeKeyboards();
    mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(
        R.layout.candidates, null);
    mCandidateViewContainer.initViews();
    mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
    mCandidateView.setService(this);
    return mCandidateViewContainer;
  }

  /**
   * This is the main point where we do our initialization of the input method
   * to begin operating on an application. At this point we have been bound to
   * the client, and are now receiving all of the detailed information about the
   * target of our edits.
   */
  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    Log.d("TEST", "onStartInput()");
    super.onStartInput(attribute, restarting);
    
    // This method gets called without the input view being created.
    if (mInputView == null) {
        onCreateInputView();
    }
    
    initInputView(attribute, restarting);
  }

  private void initInputView(EditorInfo attribute, boolean restarting) {
    mKeyboardSwitcher.makeKeyboards();

    // Reset our state. We want to do this even if restarting, because
    // the underlying state of the text editor could have changed in any way.
    mComposing.setLength(0);
    mKanji.setLength(0);
    mCandidateList = null;
    isOkurigana = false;
    mCapsLock = false;
    updateCandidates();

    if (!restarting) {
      // Clear shift states.
      mMetaState = 0;
    }

    mPredictionOn = false;
    mCompletionOn = false;
    mCompletions = null;
    mInputMode = ALPHABET;

    // We are now going to initialize our state based on the type of
    // text being edited.
    Log.d("TEST", "case = " + (attribute.inputType & EditorInfo.TYPE_MASK_CLASS));
    Log.d("TEST", "valiation = " + (attribute.inputType & EditorInfo.TYPE_MASK_VARIATION));
    Log.d("TEST", "autocomplete = " + (attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE));
    switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
    case TYPE_CLASS_NUMBER:
    case TYPE_CLASS_DATETIME:
      // Numbers and dates default to the symbols keyboard, with
      // no extra features.
      //mCurKeyboard = mSymbolsKeyboard;
      mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
          attribute.imeOptions);
      mKeyboardSwitcher.toggleSymbols();
      break;

    case TYPE_CLASS_PHONE:
      // Phones will also default to the symbols keyboard, though
      // often you will want to have a dedicated phone keyboard.
      //mCurKeyboard = mPhoneKeyboard;
      mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_PHONE,
          attribute.imeOptions);
      break;

    case TYPE_CLASS_TEXT:
      // This is general text editing. We will default to the
      // normal alphabetic keyboard, and assume that we should
      // be doing predictive text (showing candidates as the
      // user types).
      mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
          attribute.imeOptions);
      mInputMode = HIRAKANA;
      mPredictionOn = true;

      // Make sure that passwords are not displayed in candidate view
      int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
      switch (variation) {
      case TYPE_TEXT_VARIATION_PASSWORD:
      case TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
        mPredictionOn = false;
        mInputMode = ALPHABET;
        break;
      case TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
        mPredictionOn = false;
        mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_EMAIL,
                attribute.imeOptions);
        mInputMode = ALPHABET;
        break;
      case TYPE_TEXT_VARIATION_PERSON_NAME:
        mPredictionOn = true;
        mInputMode = HIRAKANA;
        break;
      case TYPE_TEXT_VARIATION_URI:
        mPredictionOn = true;
        mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_URL,
                attribute.imeOptions);
        mInputMode = ALPHABET;
        break;
      case TYPE_TEXT_VARIATION_SHORT_MESSAGE:
        mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_IM,
            attribute.imeOptions);
        mPredictionOn = true;
        mInputMode = HIRAKANA;
        break;
      case TYPE_TEXT_VARIATION_FILTER:
        mPredictionOn = false;
        mInputMode = ALPHABET;
        break;
      }

      if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
          mPredictionOn = true;
          mCompletionOn = isFullscreenMode();
      }

      // We also want to look at the current state of the editor
      // to decide whether our alphabetic keyboard should start out
      // shifted.
      updateShiftKeyState(attribute);
      break;

    default:
      // For all unknown input types, default to the alphabetic
      // keyboard with no special features.
      mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_TEXT,
          attribute.imeOptions);
      mInputMode = HIRAKANA;
      updateShiftKeyState(attribute);
    }
    Log.d("TEST", "onStartupInput: Result: mPredictionOn = " + mPredictionOn + " mCompletionOn = " + mCompletionOn);

    mInputView.closing();
    if (mCandidateView != null) mCandidateView.setSuggestions(null, false, false);
  }

  /**
   * This is called when the user is done editing a field. We can use this to
   * reset our state.
   */
  @Override
  public void onFinishInput() {
    Log.d("TEST", "onFinishInput()");
    super.onFinishInput();

    // Clear current composing text and candidates.
    mComposing.setLength(0);
    mKanji.setLength(0);
    mCandidateList = null;
    isOkurigana = false;
    updateCandidates();

    // We only hide the candidates window when finishing input on
    // a particular editor, to avoid popping the underlying application
    // up and down if the user is entering text into the bottom of
    // its window.
    setCandidatesViewShown(false);

    if (mInputView != null) {
      mInputView.closing();
    }
  }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    Log.d("TEST", "onStartInputView()");
    super.onStartInputView(attribute, restarting);
    initInputView(attribute, restarting);
  }

  /**
   * Deal with the editor reporting movement of its cursor.
   */
  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd,
      int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
        candidatesStart, candidatesEnd);

    // If the current selection in the text view changes, we should
    // clear whatever candidate text we have.
    /*
     * if (mComposing.length() > 0 && (newSelStart != candidatesEnd || newSelEnd
     * != candidatesEnd)) { Log.d("TEST", "delete in onUpdateSelection");
     * mComposing.setLength(0); updateCandidates(); InputConnection ic =
     * getCurrentInputConnection(); if (ic != null) { ic.finishComposingText();
     * } }
     */
  }

  /**
   * This tells us about completions that the editor has determined based on the
   * current text in it. We want to use this in fullscreen mode to show the
   * completions ourself, since the editor can not be seen in that situation.
   */
  @Override
  public void onDisplayCompletions(CompletionInfo[] completions) {
    Log.d("TEST", "onDisplayCompletions");
    if (mCompletionOn) {
      mCompletions = completions;
      if (completions == null) {
        setSuggestions(null, false, false);
        return;
      }

      ArrayList<String> stringList = new ArrayList<String>();
      for (int i = 0; i < completions.length; i++) {
        CompletionInfo ci = completions[i];
        if (ci != null) {
          CharSequence s = ci.getText();
          if (s != null) stringList.add(s.toString());
        }
      }
      mChoosedIndex = 0;
      setSuggestions(stringList, true, true);
    }
  }

  /**
   * This translates incoming hard key events in to edit operations on an
   * InputConnection. It is only needed when using the PROCESS_HARD_KEYS option.
   */
  private boolean translateKeyDown(int keyCode, KeyEvent event) {
    mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode, event);
    int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
    mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
    if ((mMetaState & MetaKeyKeyListener.META_ALT_ON) != 0) Log.d("TEST", "ALT is on");
    if ((mMetaState & MetaKeyKeyListener.META_SHIFT_ON) != 0) Log.d("TEST", "SHIFT is on");
    if ((mMetaState & MetaKeyKeyListener.META_SYM_ON) != 0) Log.d("TEST", "SYM is on");
    if ((mMetaState & MetaKeyKeyListener.META_ALT_LOCKED) != 0) Log.d("TEST", "ALT is locked");
    if ((mMetaState & MetaKeyKeyListener.META_CAP_LOCKED) != 0) Log.d("TEST", "SHIFT is locked");
    if ((mMetaState & MetaKeyKeyListener.META_SYM_LOCKED) != 0) Log.d("TEST", "SYM is locked");

    InputConnection ic = getCurrentInputConnection();
    if (c == 0 || ic == null) {
      return false;
    }

    if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
      c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
    }

    /** 
     * キャラの結合。英日オンリーではありえない気がするので削除
    if (mComposing.length() > 0) {
      int last = mComposing.length() - 1;
      char accent = mComposing.charAt(last);
      int composed = KeyEvent.getDeadChar(accent, c);

      if (composed != 0) {
        c = composed;
        mComposing.setLength(last);
      }
    }
    */

    onKey(c, null);

    return true;
  }

  /**
   * Use this to monitor key events being delivered to the application. We get
   * first crack at them, and can either resume them or let them continue to the
   * app.
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    Log.d("TEST", "----BEGIN-------------------------------------------------------------------");
    Log.d("TEST", "onKeyDown(): keyCode = " + keyCode + " mInputMode = " + mInputMode);
    Log.d("TEST", "mComposing = " + mComposing + " mKanji = " + mKanji);

    if (mPredictionOn == false) return super.onKeyDown(keyCode, event);
    
    InputConnection ic = getCurrentInputConnection();
    
    switch (keyCode) {
    case KeyEvent.KEYCODE_BACK:
      // The InputMethodService already takes care of the back
      // key for us, to dismiss the input method if it is shown.
      // However, our keyboard could be showing a pop-up window
      // that back should dismiss, so we first allow it to do that.
      if (event.getRepeatCount() == 0 && mInputView != null) {
        if (mInputView.handleBack()) {
          return true;
        }
      }
      break;

    case KeyEvent.KEYCODE_DEL:
      // Special handling of the delete key: if we currently are
      // composing text for the user, we want to modify that instead
      // of let the application to the delete itself.
      handleBackspace();
      return true;

    case KeyEvent.KEYCODE_ENTER:
      Log.d("TEST", "onKeyDown: KEYCODE_ENTER");
      switch (mInputMode) {
      case CHOOSE:
      case ENG2JAP:
      case KANJI:
      case OKURIGANA:
      case REGISTER:
        onKey(0x0A, null);
        return true;
      default:
        return false;
      }
      
    case KeyEvent.KEYCODE_DPAD_LEFT:     
      choosePrevious(ic);
      return true;
    case KeyEvent.KEYCODE_DPAD_RIGHT:
      chooseNext(ic);
      return true;
    default:
      // For all other keys, if we want to do transformations on
      // text being entered with a hard keyboard, we need to process
      // it and do the appropriate action.
      if (PROCESS_HARD_KEYS && mPredictionOn && translateKeyDown(keyCode, event)) {
        return true;
      }
    }
    Log.d("TEST", "traslateKeyDown: can't reach onKey() : mPredictionOn = " + mPredictionOn);
    return super.onKeyDown(keyCode, event);
  }

  // Implementation of KeyboardViewListener
  // This is software key listener
  // ちょっとわかりづらいが、漢字モード(mInputMode == KANJI)とは漢字変換するためのひらがなを入力するモード
  // 漢字にするひらがなが決定したときにモードは漢字選択モードのCHOOSEになる
  public void onKey(int pcode, int[] keyCodes) {
    Log.d("TEST", "onKey():: " + pcode + "(" + (char) pcode + ") mComp = "
        + mComposing + " mKanji = " + mKanji + " im = " + mInputMode + " isFullScreen() = " + isFullscreenMode());
    EditorInfo ciei = getCurrentInputEditorInfo();
    InputConnection ic = getCurrentInputConnection();

    // ハードキーはSHIFTとALPHABETが別のキー入力として入力される
    // ソフトキーは必ず小文字で入力されMetaステートとの結合が必要になる。
    // 共に事前に大文字にしてキーの検査を行い後にシフトを無視する場合小文字に戻す
    if (isAlphabet(pcode) && mInputView != null && mInputView.isShifted()) {
      pcode = Character.toUpperCase(pcode);
      updateShiftKeyState(ciei);
    }

    // 特殊キーの処理
    switch (pcode) {
    case Keyboard.KEYCODE_DELETE:
      handleBackspace();
      return;
    case Keyboard.KEYCODE_SHIFT:
      handleShift();
      return;
    case LatinKeyboardView.KEYCODE_SHIFT_LONGPRESS:
      if (mCapsLock) {
          handleShift();
      } else {
          toggleCapsLock();
      }
      updateShiftKeyState(ciei);
      return;
    case LatinKeyboardView.KEYCODE_SLASH_LONGPRESS:
      if (mInputMode == ALPHABET || mInputMode == ZENKAKU) 
        mInputMode = HIRAKANA;
      return;
    case Keyboard.KEYCODE_CANCEL:
      if (mInputMode == ALPHABET || mInputMode == ZENKAKU) {
        mInputMode = HIRAKANA;
        commitTyped(ic);
        return;
      }
      handleClose();
      return;
    case LatinKeyboardView.KEYCODE_OPTIONS:
      // Show a menu or somethin'
      return;
    case Keyboard.KEYCODE_MODE_CHANGE:
      if (mInputView != null) {
        changeKeyboardMode();
      }
      return;
    case 0x0A: // Enter Key
      switch (mInputMode) {
      case CHOOSE:
        pickSuggestionManually(mChoosedIndex);
        break;
      case ENG2JAP:
        ic.commitText(mComposing, 1);
        mComposing.setLength(0);
        mInputMode = HIRAKANA;
        updateCandidates();
        break;
      case KANJI:
        ic.commitText(mKanji.append(mComposing), 1);
        mComposing.setLength(0);
        mKanji.setLength(0);
        mInputMode = HIRAKANA;
        break;
      default:
        ic.commitText(mKanji.append(mComposing), 1);
        mComposing.setLength(0);
        mKanji.setLength(0);
        keyDownUp(KeyEvent.KEYCODE_ENTER);
        break;
      }
      return;
    case 'l':
      if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
        commitTyped(ic);
        mInputMode = ALPHABET;
        return;
      }
      break;
    case 'L':
      if (mInputMode != ALPHABET && mInputMode != ZENKAKU && mInputMode != ENG2JAP) {
        commitTyped(ic);
        mInputMode = ZENKAKU;
        return;
      }
      break;
    case 'q':
      if (mInputMode == ALPHABET || mInputMode == ENG2JAP || mInputMode == ZENKAKU)
        break;

      switch (mInputMode) {
      case HIRAKANA:
        mInputMode = KATAKANA;
        break;
      case KATAKANA:
        mInputMode = HIRAKANA;
        break;
      case KANJI:
        mInputMode = HIRAKANA;
        if (mKanji.length() > 0) {
          String str = hirakana2katakana(mKanji.toString());
          ic.commitText(str, 1);
          mKanji.setLength(0);
        }
        break;
      }

      if (mComposing.length() > 0) commitTyped(ic);
      return;
    case '/':
      if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
        mInputMode = ENG2JAP;
        return;
      }
      break;
    }

    // ALPHABETならcommitして終了
    if (mInputMode == ALPHABET) {
      ic.commitText(String.valueOf((char) pcode), 1);
      return;
    }

    // Zenkakuなら全角変換しcommitして終了
    if (mInputMode == ZENKAKU) {
      pcode = hankaku2zenkaku(pcode);
      ic.commitText(String.valueOf((char) pcode), 1);
      return;
    }
    

    // 英日変換なら区切り文字で確定するかそのままComposingに積む
    if (mInputMode == ENG2JAP) {
      if (isWordSeparator(pcode)) {
        handleSeparator(pcode, mComposing);
        return;
      }

      handleEnglish(pcode, keyCodes);
      return;
    }

    if (mInputMode == CHOOSE) {
      switch (pcode) {
      case ' ':
        chooseNext(ic);
        return;
      case 'x':
        choosePrevious(ic);
        if (mChoosedIndex == mSuggestions.size() - 1) {
          if (mKanji.length() != 0) {
            // Back to Kanji
            if (isOkurigana) mKanji.append(mOkurigana);
            ic.setComposingText(mKanji, 1);
            mInputMode = KANJI;
          } else {
            ic.setComposingText(mComposing, 1);
            mInputMode = ENG2JAP;
          }
          setSuggestions(null, false, false);
        }
        return;
      case Keyboard.KEYCODE_DELETE:
        handleBackspace();
        updateCandidates();
        //mInputMode = (mKanji.length() > 0) ? HIRAKANA : ENG2JAP;
        return;
      default:
        pickSuggestionManually(mChoosedIndex);
        mInputMode = HIRAKANA;
        onKey(pcode, null);
        return;
      }
    }

    // 漢字モードで区切り文字の場合、変換開始
    if (isWordSeparator(pcode) && mInputMode == KANJI) {
      // 最後に単体の'n'で終わっている場合、'ん'に変換
      if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
        mKanji.append('ん');
        ic.setComposingText(mKanji, 1);
      }

      handleSeparator(pcode, mKanji);
      return;
    }

    // シフトキーの処理
    boolean isUpper = Character.isUpperCase(pcode);
    if (isUpper) { // ローマ字変換のために小文字に戻す
      pcode = Character.toLowerCase(pcode);
    }
    // シフトキーを離すのが面倒なのでOKURIGANA決定時に大文字の時にはシフト無効
    if (mInputMode == OKURIGANA && isUpper) {
      isUpper = false;
    }

    // ここでは既に漢字変換モードであるか平仮名片仮名の入力であるので一部の記号は全角にする
    pcode = changeSeparator2Zenkaku(pcode);

    // 'ん'と'っ'の処理
    if (mComposing.length() == 1) {
      char first = mComposing.charAt(0);
      if (first == 'n') {
        if (!isVowel(pcode) && pcode != 'n' && pcode != 'y') {
          String str = "ん";
          handleNN(ic, str);
        }
      } else if (first == pcode) {
        String str = "っ";
        handleNN(ic, str);
      }
    }

    if ((mInputView != null && mInputView.isShifted()) || isUpper) {
      // シフトキーが押されている状態：
      // 漢字モードなら送り仮名開始であり変換を行なう。辞書のキーは漢字読み+送り仮名アルファベット1文字
      // 最初の平仮名はついシフトキーを押しっぱなしにしてしまうため、mKanjiの長さをチェック
      // mKanjiの長さが0の時はシフトが押されていなかったことにして下方へ継続させる
      if (mKanji.length() > 0 && mInputMode == KANJI) {
        mKanji.append((char) pcode); //辞書検索には送り仮名の子音文字が必要
        ic.setComposingText(mKanji, 1);
        ArrayList<String> cand = findKanji(mKanji.toString());
        // dictionary
        if (cand != null) {
          isOkurigana = true;
          mChoosedIndex = 0;
          
          mComposing.setLength(0);
          mComposing.append((char) pcode);
          mKanji.deleteCharAt(mKanji.length() - 1); // 送り仮名の子音文字を取り除く

          // 「あいうえお」なら即送り仮名決定
          if (isVowel(pcode)) {
            String str = changeAlphabet2Romaji();

            mKanji.append(str);
            mOkurigana = str;
            mInputMode = CHOOSE;
            setSuggestions(cand, true, true);
            ic.setComposingText(cand.get(0).concat(str), 1); // 変換候補の最初をEditorViewに表示
          } else { // それ以外は送り仮名モード
            mInputMode = OKURIGANA;
            mCandidateList = cand;
            updateCandidates();
          }
        } else {
          // 変換失敗、辞書登録
          ic.setComposingText(mKanji, 1);
          mComposing.append((char) pcode);
          mKanji.deleteCharAt(mKanji.length() - 1); // 送り仮名の子音文字を取り除く
          mSuggestions.clear();
          mSuggestions.add("IME：未登録");
          setSuggestions(mSuggestions, false, false);
        }

        return;
      } else if (mInputMode == HIRAKANA) {
        // 平仮名なら漢字変換候補入力の開始。KANJIへの移行
        
        // ローマ字の途中で漢字変換に入った場合、途中までのアルファベットを掃き出す
        if (mComposing.length() > 0) {
          ic.commitText(mComposing, 1);
          mComposing.setLength(0);
        }
        mInputMode = KANJI;
        // ここでは表示のみ修正し下へ抜けさせる
        ic.setComposingText(mComposing, 1);
        updateCandidates();
      }
    }


    String hchr; // ひらがな、1ローマ字単位分、"あ、い、う、、きゃ、き、きゅ、、"
    if (pcode == 'ー') {
      hchr = "ー";
    } else {
      mComposing.append((char) pcode);
      hchr = changeAlphabet2Romaji(); // ローマ字からひらがなに変換
    }
    if (hchr != null) {
      // Success
      if (mInputMode == KATAKANA) {
        hchr = hirakana2katakana(hchr);
      }

      mComposing.setLength(0);

      if (mInputMode == KANJI) {
        mKanji.append(hchr);
        ic.setComposingText(mKanji, 1);
      } else if (mInputMode == OKURIGANA) {
        setSuggestions(mCandidateList, false, true);
        mInputMode = CHOOSE;
        mOkurigana = (mOkurigana == null) ? hchr : mOkurigana.concat(hchr);
        if (mCandidateList != null)
          ic.setComposingText(mCandidateList.get(mChoosedIndex).concat(mOkurigana), 1);
        return;
      } else {
        ic.commitText(hchr, 1);
      }

      // sendKey(pcode);
      updateCandidates();
      return;
    }

    // 表示して終了:
    // ここに来たならmInputModeに限らず未確定
    switch (mInputMode) {
    case HIRAKANA:
    case KATAKANA:
      if (isAlphabet(pcode)) {
        ic.setComposingText(mComposing, 1);
      } else {
        ic.commitText(mComposing, 1);
        mComposing.setLength(0);
      }

      updateCandidates();
      break;
    case KANJI:
    case OKURIGANA:
      String str = "" + mKanji + mComposing;
      ic.setComposingText(str, 1);
      updateCandidates();
      break;
    default:
      ic.commitText(mComposing, 1);
      mComposing.setLength(0);
      break;
    }

    Log.d("TEST", "End: mComposing = " + mComposing + " mKanji = " + mKanji + " mInputMode = " + mInputMode);
    Log.d("TEST", "--------------------------------------------------------------------------------");
  }

  private void changeKeyboardMode() {
    mKeyboardSwitcher.toggleSymbols();
    if (mCapsLock && mKeyboardSwitcher.isAlphabetMode()) {
        ((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
    }

    updateShiftKeyState(getCurrentInputEditorInfo());
  }

  private void choosePrevious(InputConnection ic) {
    if (mSuggestions == null) return;
    String cad;
    mChoosedIndex--;
    if (mChoosedIndex < 0) mChoosedIndex = mSuggestions.size() - 1;
    mCandidateView.choose(mChoosedIndex);
    cad = mSuggestions.get(mChoosedIndex);
    if (isOkurigana) cad = cad.concat(mOkurigana);
    ic.setComposingText(cad, 1);
    return;
  }

  private void chooseNext(InputConnection ic) {
    if (mSuggestions == null) return;
    mChoosedIndex++;
    if (mChoosedIndex >= mSuggestions.size()) mChoosedIndex = 0;
    mCandidateView.choose(mChoosedIndex);
    String cad = mSuggestions.get(mChoosedIndex);
    if (isOkurigana) cad = cad.concat(mOkurigana);
    ic.setComposingText(cad, 1);
    return;
  }

  private void handleSeparator(int pcode, StringBuilder composing) {
    EditorInfo ciei = getCurrentInputEditorInfo();
    InputConnection ic = getCurrentInputConnection();
    String str = composing.toString();
    if (str.length() > 0) {
      ArrayList<String> list = findKanji(str);
      if (list == null)
        return; // FUTURE: REGISTER

      mChoosedIndex = 0;
      mInputMode = CHOOSE;
      ic.setComposingText(list.get(0), 1);
      setSuggestions(list, false, true);

      return;
    }

    // Handle separator
    mComposing.append((char) pcode);
    commitTyped(ic);
    // sendKey(pcode);
    updateShiftKeyState(ciei);
    return;
  }

  // 半角から全角 (UNICODE)
  private int hankaku2zenkaku(int pcode) {
    if (pcode == 0x20) // スペースだけ、特別
      return 0x3000;
    return pcode - 0x20 + 0xFF00;
  }

  // ひらがなでは以下の文字だけ全角になる。自分の趣味で決定してます。適当に修正してください。
  private int changeSeparator2Zenkaku(int pcode) {
    char c;
    switch (pcode) {
    case '.':
      c = '。';
      break;
    case ',':
      c = '、';
      break;
    case '-':
      c = 'ー';
      break;
    case '!':
      c = '！';
      break;
    case '?':
      c = '？';
      break;
    case '~':
      c = '～';
      break;
    default:
      c = (char) pcode;
    }
    return (int) c;
  }

  // "ん"と"っ"を取り扱う
  // KANJIならmKanjiにも足し、出力を変える
  private void handleNN(InputConnection ic, String str) {
    if (mInputMode == KATAKANA) str = hirakana2katakana(str);
    
    if (mInputMode == KANJI) {
      mKanji.append(str);
      ic.setComposingText(mKanji, 1);
    } else if (mInputMode == OKURIGANA) {
      mOkurigana = str;
      mKanji.append(str);
      ic.setComposingText(mKanji, 1);
    } else { // HIRAGANA, KATAKANA
      ic.commitText(str, 1);
    }
    mComposing.setLength(0);
  }

  /**
   * Use this to monitor key events being delivered to the application. We get
   * first crack at them, and can either resume them or let them continue to the
   * app.
   */
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // If we want to do transformations on text being entered with a hard
    // keyboard, we need to process the up events to update the meta key
    // state we are tracking.
    if (PROCESS_HARD_KEYS) {
      if (mPredictionOn) {
        mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState, keyCode, event);
      }
    }

    return super.onKeyUp(keyCode, event);
  }

  /**
   * Helper function to commit any text being composed in to the editor.
   */
  private void commitTyped(InputConnection inputConnection) {
    if (mComposing.length() > 0) {
      inputConnection.commitText(mComposing, 1);
      mComposing.setLength(0);
      updateCandidates();
    }
  }

  /**
   * Helper to update the shift state of our keyboard based on the initial
   * editor state.
   */
  private void updateShiftKeyState(EditorInfo attr) {
    InputConnection ic = getCurrentInputConnection();
    if (attr != null && mInputView != null && mKeyboardSwitcher.isAlphabetMode()
        && ic != null) {
      int caps = 0;
      EditorInfo ei = getCurrentInputEditorInfo();
      if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
        caps = ic.getCursorCapsMode(attr.inputType);
      }
      // カーソル位置で自動シフトオンの機能はうざいのでやめ
      // mInputView.setShifted(mCapsLock || caps != 0);
      mInputView.setShifted(mCapsLock);
    }
  }

  /**
   * Helper to determine if a given character code is alphabetic.
   */
  static public boolean isAlphabet(int code) {
    return ((code >= 0x41 && code <= 0x5A) || (code >= 0x61 && code <= 0x7A)) ? true : false;
  }

  /**
   * Helper to send a key down / key up pair to the current editor.
   */
  private void keyDownUp(int keyEventCode) {
    InputConnection ic = getCurrentInputConnection();
    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
  }

  /**
   * Helper to send a character to the editor as raw key events.
   */
  private void sendKey(int keyCode) {
    switch (keyCode) {
    case '\n':
      keyDownUp(KeyEvent.KEYCODE_ENTER);
      break;
    default:
      if (keyCode >= '0' && keyCode <= '9') {
        keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
      } else {
        getCurrentInputConnection().commitText(String.valueOf((char) keyCode),
            1);
      }
      break;
    }
  }

  // Implementation of KeyboardViewListener

  private ArrayList<String> findKanji(String key) {
    ArrayList<String> list = new ArrayList<String>();
    
    // open database and setup an object cache
    try {
      String value = (String)mBTree.find(key);

      if (value == null) {
        Log.d("TEST", "Dictoinary: Can't find Kanji for " + key);
        return null;
      }

      String[] va = value.split("/");
      Log.d("TEST", "val length = " + va.length);

      if (va.length <= 0) {
        Log.e("TEST", "Invalid value found: Key = " + key + " value = " + value);
        return null;
      }


      // val[0]は常に空文字列なので1から始める
      for (int j = 1; j < va.length; j++) {
        int k = va[j].indexOf(';'); // セミコロンで解説が始まる
        if (k != -1) va[j] = va[j].substring(0, k);
        list.add(va[j]);
      }


    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return list;
  }

  private String changeAlphabet2Romaji() {
    String result = mRomajiMap.get(mComposing.toString());
    return result;
  }

  private boolean isVowel(int p) {
    switch (p) {
    case 'a':
    case 'i':
    case 'u':
    case 'e':
    case 'o':
      return true;
    default:
      return false;
    }
  }

  public void onText(CharSequence text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null)
      return;
    ic.beginBatchEdit();
    if (mComposing.length() > 0) {
      commitTyped(ic);
    }
    ic.commitText(text, 0);
    ic.endBatchEdit();
    updateShiftKeyState(getCurrentInputEditorInfo());
  }

  /**
   * Update the list of available candidates from the current composing text.
   * This will need to be filled in by however you are determining candidates.
   */
  private void updateCandidates() {
    mChoosedIndex = 0;
    int clen = mComposing.length();
    int klen = mKanji.length();
    
    if (clen == 0 && klen == 0) {
      setSuggestions(null, false, true);
      return;
    }

    String str = mComposing.toString();
    ArrayList<String> list = new ArrayList<String>();

    switch (mInputMode) {
    case HIRAKANA:
    case KATAKANA:
    case OKURIGANA:
      list.add(str);
      break;
    case ENG2JAP:
      list.add(str);
      findKeys(str, list);
      break;
    case KANJI:
      if (clen == 0) {
        str = mKanji.toString();
        list.add(str);
      } else {
        list.add(str);
        String tmp = str.concat("a"); // ローマ字入力中はとりあえずア行に借り決めして検索。こうしないと英単語が出て使えない
        tmp = mRomajiMap.get(tmp);
        if (tmp != null) str = tmp;
        str = mKanji.toString().concat(str);
      }

      findKeys(str, list);
      break;
    default:
      Log.d("TEST", "updateCandidates(): Unknown case: " + mInputMode);
    }
    
    setSuggestions(list, false, false);
  }

  private void findKeys(String key, ArrayList<String> list) {
    Log.d("TEST", "findkeys(): key = " + key + " mCompose = " + mComposing + "mKanji = " + mKanji + " mIM = " + mInputMode);
    long start = System.currentTimeMillis();
    Tuple         tuple = new Tuple();
    TupleBrowser  browser;
    try {
      browser = mBTree.browse( key );
      if (browser.getNext(tuple) == false) return;
      // 最初の一つがkeyと同じ場合listに追加しない
      String first = (String)tuple.getKey();
      if (!first.equals(key)) list.add(first);
      
      if (mInputMode == ENG2JAP) {
        for (int i = 0; i < 5; i++) {
          if (browser.getNext(tuple) == false) break;
          list.add((String)tuple.getKey());
        }
        return;
      }
      
      int klen = key.length();
      int c = 0;
      while (c < 6) {
        if (browser.getNext(tuple) == false) break;
        String str = (String)tuple.getKey();
        if ((str.length() == klen + 1) && isAlphabet(str.charAt(klen))) continue;
        list.add(str);
        c++;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Log.d("TEST", "findKeys finished for " + (System.currentTimeMillis() - start) + "[ms]");
  }

  public void setSuggestions(ArrayList<String> suggestions,
      boolean completions, boolean typedWordValid) {
    if (suggestions != null && suggestions.size() > 0) {
      mSuggestions = suggestions;
      setCandidatesViewShown(true);
    } else if (isExtractViewShown()) {
      setCandidatesViewShown(true);
    }
    if (mCandidateView != null) {
      mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
    }
  }

  private void handleBackspace() {
    int clen = mComposing.length();
    int klen = mKanji.length();
    Log.d("TEST", "handleBackspace(): clen = " + clen + " klen = " + klen + " mInp = " + mInputMode + " mComp = " + mComposing + " mKan = " + mKanji);

    InputConnection ic = getCurrentInputConnection();
    if (clen > 1) {
      mComposing.delete(clen - 1, clen);
      ic.setComposingText(mComposing, 1);

    } else if (clen == 1) {
      Log.d("TEST", "delete in handleBackspace()");
      mComposing.setLength(0);
      if (klen > 0) {
        ic.setComposingText(mKanji, 1);
      } else
        ic.commitText("", 0);

    } else { // length == 0
      if (klen > 0) mKanji.delete(klen - 1, klen);
      ic.setComposingText(mKanji, 1);
      if (klen == 0) keyDownUp(KeyEvent.KEYCODE_DEL);
    }
    if (mSuggestions != null) mSuggestions.clear();
    updateCandidates();
    // 削除後の長さで更新
    clen = mComposing.length();
    klen = mKanji.length();
    switch (mInputMode) {
    case CHOOSE:
      if (klen == 0) {
        mInputMode = (clen > 0) ? ENG2JAP : HIRAKANA;
      } else {
        mInputMode = KANJI;
      }
      isOkurigana = false;
      mOkurigana = null;
      break;
    case OKURIGANA:
      if (clen == 0) {
        isOkurigana = false;
        mOkurigana = null;
        mInputMode = KANJI;
      }
      break;
    case KANJI:
      if (klen == 0 && clen == 0) mInputMode = HIRAKANA;
      break;
    case ENG2JAP:
      if (clen == 0) mInputMode = HIRAKANA;
      break;
    }
  }

  private void handleShift() {
    if (mInputView == null) {
      return;
    }

    Keyboard currentKeyboard = mInputView.getKeyboard();
    if (mKeyboardSwitcher.isAlphabetMode()) {
        // Alphabet keyboard
        checkToggleCapsLock();
        mInputView.setShifted(mCapsLock || !mInputView.isShifted());
    } else {
        mKeyboardSwitcher.toggleShift();
    }
  }

  private void handleEnglish(int prime, int[] keyCodes) {
    Log.d("TEST", "handleEnglish()");
    InputConnection ic = getCurrentInputConnection();
    mComposing.append((char) prime);
    ic.setComposingText(mComposing, 1);
    updateShiftKeyState(getCurrentInputEditorInfo());
    updateCandidates();
  }

  /**
   * 文字列・改
   * 
   * @author 佐藤 雅俊さん <okome@siisise.net> http://siisise.net/java/lang/ のコードを改変
   */
  /**
   * ひらがなを全角カタカナにする
   */
  public static String hirakana2katakana(String str) {
    if (str == null)
      return null;

    StringBuilder str2 = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      if (ch >= 0x3040 && ch <= 0x309A) {
        ch += 0x60;
      }
      str2.append(ch);
    }
    return str2.toString();
  }

  private void handleClose() {
    commitTyped(getCurrentInputConnection());
    requestHideSelf(0);
    mInputView.closing();
  }

  private void checkToggleCapsLock() {
    if (mInputView.getKeyboard().isShifted()) {
      toggleCapsLock();
    }
  }

  public boolean isWordSeparator(int code) {
    return mWordSeparators.contains(String.valueOf((char) code));
  }

  public void pickDefaultCandidate() {
    pickSuggestionManually(0);
  }

  public void pickSuggestionManually(int index) {
    Log.d("TEST", "pickSuggestionManually: mCompletionOn = " + mCompletionOn);
    InputConnection ic = getCurrentInputConnection();
    if (mCompletionOn && mCompletions != null && index >= 0 && index < mCompletions.length) {
      CompletionInfo ci = mCompletions[index];
      ic.commitCompletion(ci);
      if (mCandidateView != null) {
        mCandidateView.clear();
      }
      updateShiftKeyState(getCurrentInputEditorInfo());
    } else if (mSuggestions.size() > 0) {
      // If we were generating candidate suggestions for the current
      // text, we would commit one of them here. But for this sample,
      // we will just commit the current text.
      String s = mSuggestions.get(index);
      
      switch (mInputMode) {
      case CHOOSE:
        ic.commitText(s, 1);
        if (isOkurigana) ic.commitText(mOkurigana, 1);

        mComposing.setLength(0);
        mKanji.setLength(0);
        mInputMode = HIRAKANA;
        isOkurigana = false;
        mOkurigana = null;
        updateCandidates();
        break;
      case ENG2JAP:
        ic.setComposingText(s, 1);
        mComposing.setLength(0);
        mComposing.append(s);
        ArrayList<String> list = findKanji(s);
        setSuggestions(list, false, true);
        mInputMode = CHOOSE;
        break;
      case KANJI:
        ic.setComposingText(s, 1);
        int li = s.length() - 1;
        int last = s.codePointAt(li);
        if (isAlphabet(last)) {
          mKanji.setLength(0);
          mKanji.append(s.substring(0, li));
          mComposing.setLength(0);
          onKey(Character.toUpperCase(last), null); 
        } else {
          mKanji.setLength(0);
          mKanji.append(s);
          mComposing.setLength(0);
          list = findKanji(s);
          setSuggestions(list, false, true);
          mInputMode = CHOOSE;
        }
        break;
      }
    }
  }
  
  private void toggleCapsLock() {
      mCapsLock = !mCapsLock;
      if (mKeyboardSwitcher.isAlphabetMode()) {
          ((LatinKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
      }
  }

  public void swipeRight() {
    if (mCompletionOn) {
      pickDefaultCandidate();
    }
  }

  public void swipeLeft() {
    handleBackspace();
  }

  public void swipeDown() {
    handleClose();
  }

  public void swipeUp() {
  }

  public void onPress(int primaryCode) {
  }

  public void onRelease(int primaryCode) {
  }
  
  // receive ringer mode changes to detect silent mode
  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
          updateRingerMode();
      }
  };

  // update flags for silent mode
  private void updateRingerMode() {
      if (mAudioManager == null) {
          mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      }
      if (mAudioManager != null) {
          mSilentMode = (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
      }
  }

  @Override
  public void onBindInput() {
    Log.d("TEST", "onBindInput()");
    super.onBindInput();
  }
}
