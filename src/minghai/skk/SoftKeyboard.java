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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.HashMap;

import static minghai.skk.InputMode.*;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;
    static final String DICTIONARY = "/sdcard/skk_dict.db";
    
    private KeyboardView mInputView;
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
    
	public  int mChoosedIndex;
    
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    
    private LatinKeyboard mCurKeyboard;
    
    private String mWordSeparators;

    private InputMode mInputMode = HIRAKANA;
	private boolean isOkurigana = false;
	private String mOkurigana = null;
	private ArrayList<String> mCandidateList;

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
		m.put("mya", "みゃ");              m.put("myu", "みゅ");             m.put("myo", "みょ");
		m.put("nya", "にゃ");              m.put("nyu", "にゅ");             m.put("nyo", "にょ");
		m.put("hya", "ひゃ");              m.put("hyu", "ひゅ");             m.put("hyo", "ひょ");
		m.put("pya", "ぴゃ");              m.put("pyu", "ぴゅ");             m.put("pyo", "ぴょ");
		m.put("bya", "びゃ");              m.put("byu", "びゅ");             m.put("byo", "びょ");
	}
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mWordSeparators = getResources().getString(R.string.word_separators);
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
    	Log.d("TEST", "onInitializeInterface()");
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
    	Log.d("TEST", "onCreateInputView()");
        mInputView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
    	Log.d("TEST", "onCreateCandidatesView()");
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
    	Log.d("TEST", "onStartInput()");
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        mKanji.setLength(0);
        mCandidateList = null;
        isOkurigana = false;
        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                mInputMode = ALPHABET;
                break;
                
            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                mInputMode = ALPHABET;
                break;
                
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mInputMode = HIRAKANA;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                    mInputMode = ALPHABET;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                    mInputMode = ALPHABET;
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                    mInputMode = ALPHABET;
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
            	mInputMode = HIRAKANA;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
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
        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
    	Log.d("TEST", "onStartInputView()");
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        /*
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
        	Log.d("TEST", "delete in onUpdateSelection");
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
        */
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            ArrayList<String> stringList = new ArrayList<String>();
            for (int i=0; i< completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            mChoosedIndex = 0;
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        if (mComposing.length() > 0) {
        	int last = mComposing.length() - 1; 
            char accent = mComposing.charAt(last);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                Log.d("TEST", "in translateKeyDown: last = " + last);
                mComposing.setLength(last);
            }
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.d("TEST", "onKeyDown(): keyCode = " + keyCode + " mInputMode = " + mInputMode);
    	Log.d("TEST", "mComposing = " + mComposing + " mKanji = " + mKanji);
    	
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
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS && mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    // Implementation of KeyboardViewListener
	// This is software key listener
	public void onKey(int pcode, int[] keyCodes) {
	    Log.d("TEST", "----BEGIN-------------------------------------------------------------------");
		Log.d("TEST", "onKey():: " + pcode + "(" + (char)pcode + ") mComp = " + mComposing + " mKanji = " + mKanji + " im = " + mInputMode);
    	EditorInfo ciei = getCurrentInputEditorInfo();
	    InputConnection ic = getCurrentInputConnection();
	    
	    // ハードキーはSHIFTとALPHABETが別のキー入力として入力される
	    // ソフトキーは必ず小文字で入力されMetaステートとの結合が必要になる。
	    // 共に事前に大文字にしてキーの検査を行い後にシフトを無視する場合小文字に戻す
	    if (isAlphabet(pcode) && mInputView != null && mInputView.isShifted()) {
	    	pcode = Character.toUpperCase(pcode);
	    	updateShiftKeyState(ciei);
	    }

	    switch (pcode) {
	    case Keyboard.KEYCODE_DELETE:
	        handleBackspace();
	        return;
	    case Keyboard.KEYCODE_SHIFT:
	        handleShift();
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
	            Keyboard current = mInputView.getKeyboard();
	            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
	                current = mQwertyKeyboard;
	            } else {
	                current = mSymbolsKeyboard;
	            }
	            mInputView.setKeyboard(current);
	            if (current == mSymbolsKeyboard) {
	                current.setShifted(false);
	            }
	    	}
	    	return;
	    case 0x0A:
	    	if (mInputMode == CHOOSE) {
	    		pickSuggestionManually(mChoosedIndex);
	    	} else if (mInputMode == ENG2JAP) {
	    		ic.commitText(mComposing, 1);
	    		mComposing.setLength(0);
	    		mInputMode = HIRAKANA;
	    	} else if (mInputMode == KANJI) {
	    		ic.commitText(mKanji, 1);
	    		ic.commitText(mComposing, 1);
	    		mComposing.setLength(0);
	    		mKanji.setLength(0);
	    		mInputMode = HIRAKANA;
	    	} else {
                keyDownUp(KeyEvent.KEYCODE_ENTER);
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
	    	if (mInputMode != ALPHABET && mInputMode != ZENKAKU) {
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
	    	    
	    if (mInputMode == CHOOSE) {
	    	if (pcode == ' ') {
	    		mChoosedIndex++;
	    		if (mChoosedIndex >= mSuggestions.size()) mChoosedIndex = 0;
	    		mCandidateView.chooseNext(mChoosedIndex);
	    		String cad = mSuggestions.get(mChoosedIndex);
	    		if (isOkurigana) cad = cad.concat(mOkurigana);
	    		ic.setComposingText(cad, 1);
	    		
	    		return;
	    	} else if (pcode == Keyboard.KEYCODE_DELETE) {
	    		handleBackspace();
	    		updateCandidates();
	    		mInputMode = (mKanji.length() > 0) ? HIRAKANA : ENG2JAP;
	    		
	    		return;
	    	} else {
	    		pickSuggestionManually(mChoosedIndex);
	    		if (pcode == '/') {
	    			mInputMode = ENG2JAP;
	    			return;
	    		}
	    		// pcodeを持ったまま下へ抜ける
	    	}
	    }
	    

	    if (mInputMode == ENG2JAP) {
			if (isWordSeparator(pcode)) {
				handleSeparator(pcode, mComposing);
				return;
		    }

	    	handleEnglish(pcode, keyCodes);
	    	return;
	    }
	    		
	    if (isWordSeparator(pcode) && mInputMode == KANJI) {
	    	if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
	    		mKanji.append('ん');
		    	ic.setComposingText(mKanji, 1);
	    	}

	    	handleSeparator(pcode, mKanji);
	    	return;
	    }

	    // シフトキーの処理
	    boolean isUpper = Character.isUpperCase(pcode);
    	if (isUpper) pcode = Character.toLowerCase(pcode);
	    // シフトキーを離すのは面倒なのでOKURIGANAで大文字の時にはシフト無効
	    if (mInputMode == OKURIGANA && isUpper) {
	    	isUpper = false;
	    }
	    
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

	    	// 漢字モードなら送り仮名であり変換開始。辞書のキーは漢字読み+送り仮名アルファベット1文字
	    	// 最初の平仮名はついシフトキーを押しっぱなしにしてしまうため、mKanjiの長さをチェック
	    	// mKanjiの長さが0の時はシフトが押されていなかったことにして下方へ継続させる
	    	if (mKanji.length() > 0 && mInputMode == KANJI) {
	    		mKanji.append((char)pcode);
	    		ArrayList<String> cand = findKanji(mKanji.toString());
	    		// dictionary
	    		if (cand != null) {
	    			isOkurigana = true;
	    			mChoosedIndex = 0;

	    			mComposing.setLength(0);
	    			mComposing.append((char)pcode);
	    			mKanji.deleteCharAt(mKanji.length() - 1);

	    			// 「あいうえお」なら即送り仮名決定
	    			if (isVowel(pcode)) {
	    				String str = changeAlphabet2Romaji();

	    				mKanji.append(str);
	    				mOkurigana = str;
	    				mInputMode = CHOOSE;
	    				if (cand != null) setSuggestions(cand, true, true);
	    				ic.setComposingText(mKanji, 1); // 送り仮名付きの変換前平仮名をTextViewに表示
	    			} else { // それ以外は送り仮名モード
	    				mInputMode = OKURIGANA;
	    				mCandidateList = cand;
	    				updateCandidates();
	    			}
	    		} else {
	    			// 変換失敗、辞書登録
	    		}
	    		
	    		return;
	    	} else if (mInputMode == HIRAKANA) {
	    		// 平仮名なら漢字変換候補入力の開始。KANJIへの移行
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
	    
		mComposing.append((char)pcode);
		
		String tmp = changeAlphabet2Romaji();
		if (tmp != null) {
	        // Success
			if (mInputMode == KATAKANA) {
				tmp = hirakana2katakana(tmp);
			}
			
			mComposing.setLength(0);
			
			if (mInputMode == KANJI) {
				mKanji.append(tmp);
				ic.setComposingText(mKanji, 1);
			} else if (mInputMode == OKURIGANA){
				setSuggestions(mCandidateList, false, true);
				mInputMode = CHOOSE;
				mOkurigana = (mOkurigana == null) ? tmp : mOkurigana.concat(tmp);
				if (mCandidateList != null)
					ic.setComposingText(mCandidateList.get(mChoosedIndex).concat(mOkurigana), 1);
				return;
			} else {
				ic.commitText(tmp, 1);
			}
	
	        //sendKey(pcode);
	        updateShiftKeyState(ciei);
	        updateCandidates();
	        return;
		}
		
	    if (isAlphabet(pcode) && mPredictionOn) {
	    	Log.d("TEST", "pcode " + (char)pcode + " wa alphabet to nintei");
	    	if (mInputMode != KANJI) {
	    		ic.setComposingText(mComposing, 1);
	    	}
	        updateShiftKeyState(ciei);
	        updateCandidates();
	    } else {
	        ic.commitText(mComposing, 1);
	        mComposing.setLength(0);
	    }
	    
	    Log.d("TEST", "End: mComposing = " + mComposing + " mKanji = " + mKanji + " mInputMode = " + mInputMode);
	    Log.d("TEST", "--------------------------------------------------------------------------------");
	}

	private void handleSeparator(int pcode,StringBuilder composing) {
    	EditorInfo ciei = getCurrentInputEditorInfo();
	    InputConnection ic = getCurrentInputConnection();
		String str = composing.toString();
		if (str.length() > 0) {
			ArrayList<String> list = findKanji(str);
			if (list == null) return; // FUTURE: dictionary

			mChoosedIndex = 0;
			mInputMode = CHOOSE;
			setSuggestions(list, true, true);

			return;
		}

		// Handle separator
		mComposing.append((char)pcode);
		commitTyped(ic);
		//sendKey(pcode);
		updateShiftKeyState(ciei);
		return;
	}

	private int hankaku2zenkaku(int pcode) {
		if (pcode == 0x20) return 0x3000;
		return pcode - 0x20 + 0xFF00;
	}

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
		default:
			c = (char)pcode;
		}
		return (int)c;
	}

	// "ん"と"っ"を取り扱う
	// KANJIならmKanjiにも足し、出力を変える
	private void handleNN(InputConnection ic, String str) {
		if (mInputMode == KATAKANA) { str = hirakana2katakana(str);}
		if (mInputMode == KANJI) {
			mKanji.append(str);
			ic.setComposingText(mKanji, 1);
		} else if (mInputMode == OKURIGANA) {
			mOkurigana = str;
			mKanji.append(str);
			ic.setComposingText(mKanji, 1);
		} else {
			ic.commitText(str, 1);
		}
		mComposing.setLength(0);
	}

	/**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
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
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            // カーソル位置で自動シフトオンの機能はうざいのでやめ
            //mInputView.setShifted(mCapsLock || caps != 0);
            mInputView.setShifted(mCapsLock);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
    	return ((code >= 0x41 && code <= 0x5A) || (code >= 0x61 && code <= 0x7A)) ? true : false;
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
    	InputConnection ic = getCurrentInputConnection();
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,   keyEventCode));
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
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    private ArrayList<String> findKanji(String obj) {
    	ArrayList<String> list = new ArrayList<String>();

    	SQLiteDatabase db = SQLiteDatabase.openDatabase(DICTIONARY, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
    	Cursor cr = db.query("dictionary", new String[]{"key", "value"}, "key = ?", new String[]{obj}, null, null, null);
    	if (cr.getCount() <= 0) {
    		Log.d("TEST", "Dictoinary: Can't find Kanji for " + obj);
        	cr.close();
        	db.close();
    		return null;
    	}
    	cr.moveToFirst();
    	int i = cr.getColumnIndex("value");
    	String vs = cr.getString(i);
    	String[] val = vs.split("/");
    	Log.d("TEST", "val length = " + val.length);

    	if (val.length <= 0) return null;

    	// val[0]は常に空文字列なので1から始める
    	for (int j = 1; j < val.length; j++) {
    		int k = val[j].indexOf(';'); // セミコロンで解説が始まる
    		if (k != -1) val[j] = val[j].substring(0, k);
    		list.add(val[j]);
    	}
    	
    	cr.close();
    	db.close();
    	
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
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
        	mChoosedIndex = 0;
        	if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, false, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
    
    public void setSuggestions(ArrayList<String> suggestions, boolean completions,
            boolean typedWordValid) {
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
    	Log.d("TEST", "handleBackspace(): clen = " + clen + " klen = " + klen);

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
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
		if (mSuggestions != null) mSuggestions.clear();
        updateCandidates();
        // 削除後の長さで更新
        clen = mComposing.length();
    	klen = mKanji.length();
        if (mInputMode == CHOOSE) {
        	if (klen == 0) {
        		mInputMode = (clen > 0) ? ENG2JAP : HIRAKANA;
        	} else {
        		mInputMode = KANJI;
        	}
        	isOkurigana = false;
        	mOkurigana = null;
        }
		// 
        else if (klen == 0 && clen == 0) {
			switch (mInputMode) {
			case KANJI:
			case ENG2JAP:
				mInputMode = HIRAKANA;
				break;
			}
		}
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleEnglish(int prime, int[] keyCodes) {
    	Log.d("TEST", "handleEnglish()");
    	InputConnection ic = getCurrentInputConnection();
    	mComposing.append((char)prime);
        ic.setComposingText(mComposing, 1);
        updateShiftKeyState(getCurrentInputEditorInfo());
        updateCandidates();
	}

    /**
     * 文字列・改
     * @author 佐藤 雅俊さん <okome@siisise.net>
     * http://siisise.net/java/lang/
     * のコードを改変
    */
	/**
     * ひらがなを全角カタカナにする
     */
    public static String hirakana2katakana(String str) {
    	if (str == null) return null;
    	
        StringBuilder str2 = new StringBuilder();

        for (int i = 0; i<str.length(); i++) {
            char ch = str.charAt(i);

            if (ch >= 0x3040 && ch <=0x309A) {
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
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
    	Log.d("TEST", "pickSuggestionManually: mCompletionOn = " + mCompletionOn);
        InputConnection ic = getCurrentInputConnection();
		if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            ic.commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mSuggestions.size() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
        	String s = mSuggestions.get(index);
            ic.commitText(s, 1);
            if (isOkurigana) ic.commitText(mOkurigana, 1);

            mComposing.setLength(0);
            mKanji.setLength(0);
            mInputMode = HIRAKANA;
            isOkurigana = false;
            mOkurigana = null;
            updateCandidates();
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

	@Override
	public void onBindInput() {
    	Log.d("TEST", "onBindInput()");
		super.onBindInput();
	}
}
