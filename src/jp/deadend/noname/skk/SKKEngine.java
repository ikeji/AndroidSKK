/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package jp.deadend.noname.skk;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import static jp.deadend.noname.skk.InputMode.*;

public class SKKEngine extends InputMethodService {
  public static boolean isDebugMode;

  private CandidateViewContainer mCandidateViewContainer;
  private CandidateView mCandidateView;
  private ArrayList<String> mSuggestions;
    
  public int mChoosedIndex;
  private ArrayList<String> mCandidateList;
    
  private InputMode mInputMode = HIRAKANA;

  private boolean isSKKOn = true;

  // ひらがなや英単語などの入力途中
  private StringBuilder mComposing = new StringBuilder();
  // 漢字変換のキー 送りありの場合最後がアルファベット 変換中は不変
  private StringBuilder mKanji = new StringBuilder();
  // 送りがな 「っ」や「ん」が含まれる場合だけ二文字になる
  private String mOkurigana = null;

  // 単語登録のキー 登録作業中は不変
  private String mRegKey = null;
  // 単語登録する内容
  private StringBuilder mRegEntry = new StringBuilder();
  private boolean isRegistering = false;
    
  static final String DICTIONARY = "skk_dict_btree";
  static final String USER_DICT = "skk_userdict";
  private SKKDictionary mDict;
  private SKKUserDictionary mUserDict;

  private SKKMetaKey mMetaKey = new SKKMetaKey();

  private int mKanaKey = 0;

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
	m.put("ya", "や");                  m.put("yu", "ゆ");                  m.put("yo", "よ");
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
	m.put("fa", "ふぁ");m.put("fi", "ふぃ");m.put("fu", "ふ");m.put("fe", "ふぇ");m.put("fo", "ふぉ");
	
	m.put("xya", "ゃ");                 m.put("xyu", "ゅ");                 m.put("xyo", "ょ");
	m.put("kya", "きゃ");               m.put("kyu", "きゅ");               m.put("kyo", "きょ");
	m.put("gya", "ぎゃ");               m.put("gyu", "ぎゅ");               m.put("gyo", "ぎょ");
	m.put("sya", "しゃ");               m.put("syu", "しゅ");               m.put("syo", "しょ");
	m.put("sha", "しゃ");m.put("shi", "し");m.put("shu", "しゅ");m.put("she", "しぇ");m.put("sho", "しょ");
	m.put("ja",  "じゃ");m.put("ji",  "じ");m.put("ju", "じゅ");m.put("je", "じぇ");m.put("jo", "じょ");
	m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
	m.put("tya", "ちゃ");               m.put("tyu", "ちゅ");m.put("tye", "ちぇ");m.put("tyo", "ちょ");
	m.put("dha", "でゃ");m.put("dhi", "でぃ");m.put("dhu", "でゅ");m.put("dhe", "でぇ");m.put("dho", "でょ");
	m.put("dya", "ぢゃ");m.put("dyi", "ぢぃ");m.put("dyu", "ぢゅ");m.put("dye", "ぢぇ");m.put("dyo", "ぢょ");
	m.put("nya", "にゃ");               m.put("nyu", "にゅ");               m.put("nyo", "にょ");
	m.put("hya", "ひゃ");               m.put("hyu", "ひゅ");               m.put("hyo", "ひょ");
	m.put("pya", "ぴゃ");               m.put("pyu", "ぴゅ");               m.put("pyo", "ぴょ");
	m.put("bya", "びゃ");               m.put("byu", "びゅ");               m.put("byo", "びょ");
	m.put("mya", "みゃ");               m.put("myu", "みゅ");               m.put("myo", "みょ");
	m.put("rya", "りゃ");               m.put("ryu", "りゅ");m.put("rye", "りぇ");m.put("ryo", "りょ");
	m.put("z,", "‥");m.put("z-", "〜");m.put("z.", "…");m.put("z/", "・");m.put("z[", "『");m.put("z]", "』");m.put("zh", "←");m.put("zj", "↓");m.put("zk", "↑");m.put("zl", "→");
  }
  // 全角で入力する記号リスト
  private HashMap<String, String> mZenkakuSeparatorMap = new HashMap<String, String>();
  {
	HashMap<String, String> m = mZenkakuSeparatorMap;
	m.put("-", "ー");m.put("!", "！");m.put("?", "？");m.put("~", "〜");m.put("[", "「");m.put("]", "」");
  }
    
  @Override public void onCreate() {
	super.onCreate();

	isDebugMode = DeployUtil.isDebuggable(this);
	// Log.d("SKK", "isDebugMode = " + isDebugMode);

	Context bc = getBaseContext();

	String kutouten = SKKPrefs.getPrefKutoutenType(bc);
	SKKUtils.dlog("kutouten type: " + kutouten);
	if (kutouten.equals("en")) {
	  mZenkakuSeparatorMap.put(".", "．");
	  mZenkakuSeparatorMap.put(",", "，");
	} else if (kutouten.equals("jp")) {
	  mZenkakuSeparatorMap.put(".", "。");
	  mZenkakuSeparatorMap.put(",", "、");
	} else if (kutouten.equals("jp_en")) {
	  mZenkakuSeparatorMap.put(".", "。");
	  mZenkakuSeparatorMap.put(",", "，");
	} else {
	  mZenkakuSeparatorMap.put(".", "．");
	  mZenkakuSeparatorMap.put(",", "，");
	}

	mKanaKey = SKKPrefs.getPrefKanaKey(bc);
	SKKUtils.dlog("Kana key code: " + mKanaKey);

	String dd = SKKPrefs.getPrefDictDir(bc);
	SKKUtils.dlog("dict dir: " + dd);

	int waitCount = 0;
	int maxWait = 9;
	while (!(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))) {
	  SKKUtils.dlog("waiting mount SD ... " + waitCount);
	  try{
		Thread.sleep(3000);
	  } catch (InterruptedException e) {}

	  waitCount++;
	  if (waitCount > maxWait) {
		Log.e("SKK", "no SD card found!");
		break;
	  }
	}

	mDict = new SKKDictionary(dd + "/" + DICTIONARY);
	mUserDict = new SKKUserDictionary(dd + "/" + USER_DICT);
  }

  @Override public void onDestroy() {
	mUserDict.commitChanges();

	super.onDestroy();
  }

  @Override public View onCreateInputView() {
	return null;
  }

  /**
   * Called by the framework when your view for showing candidates
   * needs to be generated, like {@link #onCreateInputView}.
   */
  @Override public View onCreateCandidatesView() {
	mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(R.layout.candidates, null);
	mCandidateViewContainer.initViews();
	mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
	mCandidateView.setService(this);

	return mCandidateViewContainer;
  }

  /**
   * This is the main point where we do our initialization of the
   * input method to begin operating on an application. At this
   * point we have been bound to the client, and are now receiving
   * all of the detailed information about the target of our edits.
   */
  @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
	super.onStartInput(attribute, restarting);

	//if (isSKKOn) changeMode(HIRAKANA, true);
	if (isSKKOn) changeMode(mInputMode, true);
  }

  /**
   * Use this to monitor key events being delivered to the
   * application. We get first crack at them, and can either resume
   * them or let them continue to the app.
   */
  @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
	if (!isSKKOn) return super.onKeyUp(keyCode, event);

	switch (keyCode) {
	case KeyEvent.KEYCODE_SHIFT_LEFT:
	case KeyEvent.KEYCODE_SHIFT_RIGHT:
	  mMetaKey.releaseMetaKey(mMetaKey.SHIFT_KEY);
	  return true;
	case KeyEvent.KEYCODE_ALT_LEFT:
	case KeyEvent.KEYCODE_ALT_RIGHT:
	  mMetaKey.releaseMetaKey(mMetaKey.ALT_KEY);
	  return true;
	case KeyEvent.KEYCODE_ENTER:
	  return true;
	default:
	  break;
	}

	return super.onKeyUp(keyCode, event);
  }
    
  /**
   * This translates incoming hard key events in to edit operations
   * on an InputConnection.
   */
  private boolean translateKeyDown(int keyCode, KeyEvent event) {
	// Shift・Altキーの状態をチェック
	int metaBit = mMetaKey.useMetaState();

	int c = event.getUnicodeChar(metaBit);
	SKKUtils.dlog("key:" + keyCode + " translated:" + c);

	InputConnection ic = getCurrentInputConnection();
	if (c == 0 || ic == null) {
	  SKKUtils.dlog("traslateKeyDown(): didn't go processKey()");
	  return false;
	}

	processKey(c, null);

	return true;
  }

  /**
   * Use this to monitor key events being delivered to the
   * application. We get first crack at them, and can either resume
   * them or let them continue to the app.
   */
  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
	InputConnection ic = getCurrentInputConnection();
	SKKUtils.dlog("onKeyDown(): keyCode=" + keyCode + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);

	if (!isSKKOn) {
	  if (keyCode == mKanaKey) {
		toggleSKK();
		return true;
	  } else {
		return super.onKeyDown(keyCode, event);
	  }
	}

	if (keyCode == mKanaKey) { // かなモードに移行するためのキー(設定可)
	  switch (mInputMode) {
	  case ZENKAKU:
	  case ENG2JP:
	  case KATAKANA:
		changeMode(HIRAKANA, true);
		break;
	  case HIRAKANA:
		if (!isRegistering) toggleSKK();
		break;
	  case CHOOSE:
		handleEnter();
		break;
	  default:
		break;
	  }
	  return true;
	}

	// Process special keys
	switch (keyCode) {
	case KeyEvent.KEYCODE_BACK:
	  if (isRegistering) {
		isRegistering = false;
		mRegKey = null;
		mRegEntry.setLength(0);
		reset();
	  } else {
		reset();
		keyDownUp(keyCode);
	  }
	  break;
	case KeyEvent.KEYCODE_DEL:
	  handleBackspace();
	  break;
	case KeyEvent.KEYCODE_ENTER:
	  handleEnter();
	  break;
	case KeyEvent.KEYCODE_SHIFT_LEFT:
	case KeyEvent.KEYCODE_SHIFT_RIGHT:
	  mMetaKey.pushMetaKey(mMetaKey.SHIFT_KEY);
	  break;
	case KeyEvent.KEYCODE_ALT_LEFT:
	case KeyEvent.KEYCODE_ALT_RIGHT:
	  mMetaKey.pushMetaKey(mMetaKey.ALT_KEY);
	  break;
	case KeyEvent.KEYCODE_DPAD_LEFT:
	  if (mInputMode == CHOOSE) {
		choosePrevious(ic);
	  } else if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) {
		return super.onKeyDown(keyCode, event);
	  }
	  break;
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	  if (mInputMode == CHOOSE) {
		chooseNext(ic);
	  } else if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) {
		return super.onKeyDown(keyCode, event);
	  }
	  break;
	case KeyEvent.KEYCODE_DPAD_UP:
	case KeyEvent.KEYCODE_DPAD_DOWN:
	  if (!isRegistering && mComposing.length() == 0 && mKanji.length() == 0) return super.onKeyDown(keyCode, event);
	  break;
	default:
	  // For all other keys, if we want to do transformations on
	  // text being entered with a hard keyboard, we need to
	  // process it and do the appropriate action.
	  if (translateKeyDown(keyCode, event)) {
		return true;
	  } else {
		return super.onKeyDown(keyCode, event);
	  }
	}
	return true;
  }

  public void processKey(int pcode, int[] keyCodes) {
	InputConnection ic = getCurrentInputConnection();

	switch (mInputMode) {
	case ZENKAKU:
	  // 全角変換しcommitして終了
	  pcode = SKKUtils.hankaku2zenkaku(pcode);
	  commitTextSKK(ic, String.valueOf((char) pcode), 1);
	  break;
	case ENG2JP:
	  // スペースで変換するかそのままComposingに積む
	  if (pcode == ' ') {
		if (mComposing.length() != 0) {
		  mKanji.setLength(0);
		  mKanji.append(mComposing);
		  conversionStart(mKanji);
		}
	  } else {
		mComposing.append((char) pcode);
		setComposingTextSKK(ic, mComposing, 1);
		updateCandidates();
	  }
	  break;
	case CHOOSE:
	  // 最初の候補より戻ると変換に戻る 最後の候補より進むと登録
	  switch (pcode) {
	  case ' ':
		if (mChoosedIndex == mSuggestions.size() - 1) {
		  if (!isRegistering) registerStart(mKanji.toString());
		} else {
		  chooseNext(ic);
		}
		break;
	  case 'x':
		if (mChoosedIndex == 0) {
		  if (mComposing.length() != 0) {
			// KANJIモードに戻る
			if (mOkurigana != null) {
			  mOkurigana = null;
			  mKanji.deleteCharAt(mKanji.length() -1);
			}
			setComposingTextSKK(ic, mKanji, 1);
			changeMode(KANJI, false);
		  } else {
			mKanji.setLength(0);
			setComposingTextSKK(ic, mComposing, 1);
			changeMode(ENG2JP, false);
		  }
		  updateCandidates();
		} else {
		  choosePrevious(ic);
		}
		break;
	  default:
		pickSuggestionManually(mChoosedIndex);
		changeMode(HIRAKANA, true);
		processKey(pcode, null);
		break;
	  }
	  break;
	case HIRAKANA:
	case KATAKANA:
	case KANJI:
	case OKURIGANA:
	  // モード変更操作
	  if (mInputMode == HIRAKANA || mInputMode == KATAKANA) {
		switch (pcode) {
		case 'q':
		  if (mInputMode == HIRAKANA) {
			changeMode(KATAKANA, true);
		  } else {
			changeMode(HIRAKANA, true);
		  }
		  return;
		case 'l':
		  if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
			if (!isRegistering) toggleSKK();
			return;
		  } // →を入力するための例外
		  break;
		case 'L':
		  changeMode(ZENKAKU, true);
		  return;
		case '/':
		  if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
			changeMode(ENG2JP, true);
			return;
		  } // 中黒を入力するための例外
		  break;
		default:
		  break;
		}
	  }

	  SKKUtils.dlog("doJapaneseConversion():: " + pcode + "(" + (char) pcode + ") mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
	  doJapaneseConversion(pcode);
	  SKKUtils.dlog("End: mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
	  SKKUtils.dlog("--------------------------------------------------------------------------------");

	  break;
	default:
	  SKKUtils.dlog("Unknown mode!");
	  break;
	}
  }

  // processKey()が長くて疲れるので，日本語変換関係だけ分けたもの
  private void doJapaneseConversion(int pcode) {
	InputConnection ic = getCurrentInputConnection();
	String hchr = null; // かな1単位ぶん
	
	// シフトキーの状態をチェック
	boolean isUpper = Character.isUpperCase(pcode);
	if (isUpper) { // ローマ字変換のために小文字に戻す
	  pcode = Character.toLowerCase(pcode);
	}

	switch (mInputMode) {
	case OKURIGANA:
	  // 「ん」か「っ」を処理したらここで終わり
	  if (handleNN(ic, pcode)) return;
	  SKKUtils.dlog("okuri: mOkuri=" + mOkurigana + " mComp=" + mComposing + " pcode=" + (char)pcode);
	  // 送りがなが確定すれば変換，そうでなければComposingに積む
	  mComposing.append((char) pcode);
	  hchr = mRomajiMap.get(mComposing.toString());
	  if (mOkurigana != null) { //「ん」か「っ」がある場合
		if (hchr != null) {
		  mComposing.setLength(0);
		  mOkurigana = mOkurigana + hchr;
		  conversionStart(mKanji);
		} else {
		  setComposingTextSKK(ic, mKanji.toString().substring(0, mKanji.length()-1) + mOkurigana + mComposing.toString(), 1);
		}
	  } else {
		if (hchr != null) {
		  mComposing.setLength(0);
		  mOkurigana = hchr;
		  conversionStart(mKanji);
		} else {
		  setComposingTextSKK(ic, mKanji.toString().substring(0, mKanji.length()-1) + mComposing.toString(), 1);
		}
	  }
	  break;
	case HIRAKANA:
	case KATAKANA:
	  handleNN(ic, pcode);
	  if (isUpper) {
		// 漢字変換候補入力の開始。KANJIへの移行
		if (mComposing.length() > 0) {
		  commitTextSKK(ic, mComposing, 1);
		  mComposing.setLength(0);
		}
		changeMode(KANJI, false);
		doJapaneseConversion(pcode);
	  } else {
		mComposing.append((char) pcode);
		// 全角にする記号ならば全角，そうでなければローマ字変換
		hchr = mZenkakuSeparatorMap.get(mComposing.toString());
		if (hchr == null) {
		  hchr = mRomajiMap.get(mComposing.toString());
		}

		if (hchr != null) { // 確定できるものがあれば確定
		  if (mInputMode == KATAKANA) {
			hchr = SKKUtils.hirakana2katakana(hchr);
		  }
		  mComposing.setLength(0);
		  commitTextSKK(ic, hchr, 1);
		} else { // アルファベットならComposingに積む
		  if (SKKUtils.isAlphabet(pcode)) {
			setComposingTextSKK(ic, mComposing, 1);
		  } else {
			commitTextSKK(ic, mComposing, 1);
			mComposing.setLength(0);
		  }
		}
	  }
	  break;
	case KANJI:
	  handleNN(ic, pcode);
	  if (pcode == 'q') {
		// カタカナ変換
		if (mKanji.length() > 0) {
		  String str = SKKUtils.hirakana2katakana(mKanji.toString());
		  commitTextSKK(ic, str, 1);
		}
		changeMode(HIRAKANA, true);
	  } else if (pcode == ' ') {
		// 変換開始
		// 最後に単体の'n'で終わっている場合、'ん'に変換
		if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
		  mKanji.append('ん');
		  setComposingTextSKK(ic, mKanji, 1);
		}
		mComposing.setLength(0);
		conversionStart(mKanji);
	  } else if (isUpper && mKanji.length() > 0) {
		// 送り仮名開始
		// 最初の平仮名はついシフトキーを押しっぱなしにしてしまうた
		// め、mKanjiの長さをチェックmKanjiの長さが0の時はシフトが
		// 押されていなかったことにして下方へ継続させる
		mKanji.append((char) pcode); //送りありの場合子音文字追加
		mComposing.setLength(0);
		if (SKKUtils.isVowel(pcode)) { // 母音なら送り仮名決定，変換
		  mOkurigana  = mRomajiMap.get(String.valueOf((char) pcode));
		  conversionStart(mKanji);
		} else { // それ以外は送り仮名モード
		  mComposing.append((char) pcode);
		  setComposingTextSKK(ic, mKanji, 1);
		  changeMode(OKURIGANA, false);
		}
	  } else {
		// 未確定
		mComposing.append((char) pcode);
		hchr = mZenkakuSeparatorMap.get(mComposing.toString());
		if (hchr == null) {
		  hchr = mRomajiMap.get(mComposing.toString());
		}

		if (hchr != null) {
		  mComposing.setLength(0);
		  mKanji.append(hchr);
		  setComposingTextSKK(ic, mKanji, 1);
		} else {
		  setComposingTextSKK(ic, mKanji.toString() + mComposing.toString(), 1);
		}
		updateCandidates();
	  }
	  break;
	default:
	  SKKUtils.dlog("Unknown mode!");
	  break;
	}
  }

  // commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
  private void commitTextSKK(InputConnection ic, CharSequence text, int newCursorPosition) {
	if (isRegistering) {
	  mRegEntry.append(text);
	  ic.setComposingText("▼" + mRegKey + "：" + mRegEntry, newCursorPosition);
	} else {
	  ic.commitText(text, newCursorPosition);
	}
  }

  //setComposingTextのラッパー
  private void setComposingTextSKK(InputConnection ic, CharSequence text, int newCursorPosition) {
	StringBuilder ct = new StringBuilder();

	if (isRegistering) {
	  ct.append("▼");
	  ct.append(mRegKey);
	  ct.append("：");
	  ct.append(mRegEntry);
	}

	if (!text.equals("") || mInputMode == ENG2JP) {
	  switch (mInputMode) {
	  case KANJI:
	  case ENG2JP:
	  case OKURIGANA:
		ct.append("▽");
		break;
	  case CHOOSE:
		ct.append("▼");
		break;
	  default:
		break;
	  }
	} 
	ct.append(text);

	ic.setComposingText(ct, newCursorPosition);
  }

  // 変換スタート
  // composingに辞書のキー 送りありの場合最後はアルファベット
  // 送りありの場合は送りがな自体をmOkuriganaに入れておく
  private void conversionStart(StringBuilder composing) {
	InputConnection ic = getCurrentInputConnection();
	String str = composing.toString();

	changeMode(CHOOSE, false);

	ArrayList<String> list = findKanji(str);
	if (list == null) {
	  registerStart(str);
	  return;
	}
	
	mChoosedIndex = 0;

	if (mOkurigana != null) {
	  setComposingTextSKK(ic, SKKUtils.removeAnnotation(list.get(0)).concat(mOkurigana), 1);
	} else {
	  setComposingTextSKK(ic, SKKUtils.removeAnnotation(list.get(0)), 1);
	}

	setSuggestions(list);
  }

  private void registerStart(String str) {
	mRegKey = str;
	mRegEntry.setLength(0);
	isRegistering = true;
	changeMode(HIRAKANA, true);
	//setComposingTextSKK(getCurrentInputConnection(), "", 1);
  }

  // "ん"と"っ"を取り扱う
  // 処理した場合はtrue
  private boolean handleNN(InputConnection ic, int pcode) {
	if (mComposing.length() != 1 || mOkurigana != null) return false;

	char first = mComposing.charAt(0);
	String str;
	if (first == 'n') {
	  if (!SKKUtils.isVowel(pcode) && pcode != 'n' && pcode != 'y') {
		str = "ん";
	  } else {
		return false;
	  }
	} else if (first == pcode) {
	  str = "っ";
	} else {
	  return false;
	}
	if (mInputMode == KATAKANA) str = SKKUtils.hirakana2katakana(str);

	if (mInputMode == OKURIGANA) {
	  mOkurigana = str;
	  setComposingTextSKK(ic, mKanji.toString().substring(0, mKanji.length()-1) + str + (char)pcode, 1);
	  mComposing.setLength(0);
	  mComposing.append((char)pcode);
	} else if (mInputMode == KANJI) {
	  mKanji.append(str);
	  setComposingTextSKK(ic, mKanji, 1);
	  mComposing.setLength(0);
	} else { // HIRAGANA, KATAKANA
	  commitTextSKK(ic, str, 1);
	  mComposing.setLength(0);
	}

	return true;
  }

  private void handleEnter() {
	InputConnection ic = getCurrentInputConnection();
	
	// Shift・Altキーの状態を消費
	int metaBit = mMetaKey.useMetaState();

	switch (mInputMode) {
	case CHOOSE:
	  pickSuggestionManually(mChoosedIndex);
	  break;
	case ENG2JP:
	  if (mComposing.length() > 0) {
		commitTextSKK(ic, mComposing, 1);
		mComposing.setLength(0);
	  }
	  changeMode(HIRAKANA, true);
	  break;
	case KANJI:
	case OKURIGANA:
	  commitTextSKK(ic, mKanji, 1);
	  mComposing.setLength(0);
	  mKanji.setLength(0);
	  changeMode(HIRAKANA, true);
	  break;
	default:
	  if (mComposing.length() == 0) {
		if (isRegistering) {
		  // 単語登録終了
		  isRegistering = false;
		  if (mRegEntry.length() != 0) {
			mUserDict.addEntry(mRegKey, mRegEntry.toString());
			mUserDict.commitChanges();
			ic.commitText(mRegEntry.toString(), 1);
		  }
		  mRegKey = null;
		  mRegEntry.setLength(0);
		  reset();
		} else {
		  keyDownUp(KeyEvent.KEYCODE_ENTER);
		}
	  } else {
		commitTextSKK(ic, mComposing, 1);
		mComposing.setLength(0);
	  }
	}
  }

  private void handleBackspace() {
	int clen = mComposing.length();
	int klen = mKanji.length();
	InputConnection ic = getCurrentInputConnection();
	SKKUtils.dlog("handleBackspace(): clen=" + clen + " klen=" + klen + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
	
	// Shift・Altキーの状態をチェック
	int metaBit = mMetaKey.useMetaState();

	if (clen > 0) {
	  mComposing.deleteCharAt(clen-1);
	} else if (klen > 0) {
	  mKanji.deleteCharAt(klen-1);
	} else if (clen == 0 && klen == 0) {
	  if (isRegistering) {
		int rlen = mRegEntry.length();
		if (rlen > 0) {
		  mRegEntry.deleteCharAt(rlen-1);
		  setComposingTextSKK(ic, "", 1);
		}
	  } else if (mInputMode == ENG2JP) {
		changeMode(HIRAKANA, true);
	  } else {
		keyDownUp(KeyEvent.KEYCODE_DEL);
		/* Alt+BSで全消しをしたいが，やりかたがわからない
		if ((metaBit & KeyEvent.META_ALT_ON) != 0) {
		  SKKUtils.dlog("HandleBackspace(): alt key is on");
		  long eventTime = SystemClock.uptimeMillis();
		  ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_ALT_ON));
		  ic.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_ALT_ON));
		} else {
		  keyDownUp(KeyEvent.KEYCODE_DEL);
		}
		*/
	  }
	  return;
	}
	clen = mComposing.length();
	klen = mKanji.length();
	    
	switch (mInputMode) {
	case HIRAKANA:
	case KATAKANA:
	  setComposingTextSKK(ic, mComposing, 1);
	  break;
	case KANJI:
	  if (klen == 0 && clen == 0) {
		changeMode(HIRAKANA, true);
	  } else {
		setComposingTextSKK(ic, mKanji.toString() + mComposing.toString(), 1);
		updateCandidates();
	  }
	  break;
	case ENG2JP:
	  if (clen == 0) {
		changeMode(HIRAKANA, true);
	  } else {
		setComposingTextSKK(ic, mComposing, 1);
		updateCandidates();
	  }
	  break;
	case OKURIGANA:
	  mComposing.setLength(0);
	  mOkurigana = null;
	  setComposingTextSKK(ic, mKanji, 1);
	  changeMode(KANJI, false);
	  break;
	case CHOOSE:
	  if (klen == 0) {
		changeMode(HIRAKANA, true);
	  } else {
		if (clen > 0) { // 英語変換中
		  changeMode(ENG2JP, false);
		  setComposingTextSKK(ic, mComposing, 1);
		  updateCandidates();
		} else { // 漢字変換中
		  if (mOkurigana != null) {
			mOkurigana = null;
		  }
		  changeMode(KANJI, false);
		  setComposingTextSKK(ic, mKanji, 1);
		  updateCandidates();
		}
	  }
	  break;
	default:
	  SKKUtils.dlog("handleBackspace() do nothing");
	  break;
	}

	SKKUtils.dlog("handleBackspace() end: clen=" + clen + " klen=" + klen + " mComp=" + mComposing + " mKanji=" + mKanji + " im=" + mInputMode);
  }

  private ArrayList<String> findKanji(String key) {

	SKKUtils.dlog("findKanji(): key=" + key);
	ArrayList<String> list1 = mDict.getCandidates(key);
	ArrayList<String> list2 = mUserDict.getCandidates(key);

	if (list1 == null && list2 == null) {
	  SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
	  return null;
	}

	if (list1 == null) list1 = new ArrayList<String>();
	if (list2 != null) {
	  int idx = 0;
	  for (String s : list2) {
		//個人辞書の候補を先頭に追加
		list1.remove(s);
		list1.add(idx, s);
		idx++;
	  }
	}

	return list1;
  }

  private void updateCandidates() {
	mChoosedIndex = 0;
	int clen = mComposing.length();
	int klen = mKanji.length();
	
	if (clen == 0 && klen == 0) {
	  setSuggestions(null);
	  return;
	}

	String str = mComposing.toString();
	ArrayList<String> list = new ArrayList<String>();

	switch (mInputMode) {
	case ENG2JP:
	  list.add(str);
	  mDict.findKeys(str, false, list);
	  break;
	case KANJI:
	  if (clen == 0) {
		str = mKanji.toString();
		list.add(str);
	  } else {
		//list.add(str);
		String tmp = str.concat("a"); // ローマ字入力中はとりあえずア行に借り決めして検索。こうしないと英単語が出て使えない
		tmp = mRomajiMap.get(tmp);
		if (tmp != null) str = tmp;
		str = mKanji.toString().concat(str);
	  }
	    
	  mDict.findKeys(str, true, list);
	  break;
	default:
	  SKKUtils.dlog("updateCandidates(): " + mInputMode);
	}
    
	setSuggestions(list);
  }

  private void choosePrevious(InputConnection ic) {
	if (mSuggestions == null) return;

	String cad;
	mChoosedIndex--;
	if (mChoosedIndex < 0) mChoosedIndex = mSuggestions.size() - 1;
	mCandidateView.choose(mChoosedIndex);
	cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
	if (mOkurigana != null) cad = cad.concat(mOkurigana);
	setComposingTextSKK(ic, cad, 1);

	return;
  }

  private void chooseNext(InputConnection ic) {
	if (mSuggestions == null) return;

	String cad;
	mChoosedIndex++;
	if (mChoosedIndex >= mSuggestions.size()) mChoosedIndex = 0;
	mCandidateView.choose(mChoosedIndex);
	cad = SKKUtils.removeAnnotation(mSuggestions.get(mChoosedIndex));
	if (mOkurigana != null) cad = cad.concat(mOkurigana);
	setComposingTextSKK(ic, cad, 1);

	return;
  }

  public void setSuggestions(ArrayList<String> suggestions) {
	if (suggestions != null && suggestions.size() > 0) {
	  mSuggestions = suggestions;
	  setCandidatesViewShown(true);
	}
	
	if (mCandidateView != null) {
	  mCandidateView.setSuggestions(suggestions);
	}
  }

  public void pickSuggestionManually(int index) {
	InputConnection ic = getCurrentInputConnection();

	if (mSuggestions.size() > 0) {
	  String s = mSuggestions.get(index);
      
	  switch (mInputMode) {
	  case CHOOSE:
		commitTextSKK(ic, SKKUtils.removeAnnotation(s), 1);
		if (mOkurigana != null) commitTextSKK(ic, mOkurigana, 1);
		SKKUtils.dlog("Fixed: s=" + s + " mKanji=" + mKanji + " mComp=" + mComposing + " mOkuri=" + mOkurigana);
		mUserDict.addEntry(mKanji.toString(), s);

		changeMode(HIRAKANA, true);
		break;
	  case ENG2JP:
		setComposingTextSKK(ic, s, 1);
		mComposing.setLength(0);
		mComposing.append(s);
		conversionStart(mComposing);
		break;
	  case KANJI:
		setComposingTextSKK(ic, s, 1);
		int li = s.length() - 1;
		int last = s.codePointAt(li);
		if (SKKUtils.isAlphabet(last)) {
		  mKanji.setLength(0);
		  mKanji.append(s.substring(0, li));
		  mComposing.setLength(0);
		  processKey(Character.toUpperCase(last), null); 
		} else {
		  mKanji.setLength(0);
		  mKanji.append(s);
		  mComposing.setLength(0);
		  conversionStart(mKanji);
		}
		break;
	  }
	}
  }

  /**
   * Helper to send a key down / key up pair to the current editor.
   */
  private void keyDownUp(int keyEventCode) {
	InputConnection ic = getCurrentInputConnection();
	ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
	ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
  }

  private void reset() {
	mComposing.setLength(0);
	mKanji.setLength(0);
	mOkurigana = null;
	mCandidateList = null;
	setSuggestions(null);
	getCurrentInputConnection().setComposingText("", 1);
	mMetaKey.clearMetaKeyState();
  }

  private void toggleSKK() {
	if (isSKKOn) {
	  isSKKOn = false;
	  showStatusIcon(0);
	  reset();
	} else {
	  isSKKOn = true;
	  changeMode(HIRAKANA, true);
	}
	getCurrentInputConnection().clearMetaKeyStates(KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON);
  }

  // change the mode and set the status icon
  private void changeMode(InputMode im, boolean doReset) {
	int icon = 0;

	if (doReset) reset();

	switch (im) {
	case HIRAKANA:
	  mInputMode = HIRAKANA;
	  icon = R.drawable.immodeic_hiragana;
	  //setCandidatesViewShown()ではComposingTextがflushされるっぽい
	  //単語登録中だと変になるので注意
	  setCandidatesViewShown(false);
	  break;
	case KATAKANA:
	  mInputMode = KATAKANA;
	  icon = R.drawable.immodeic_katakana;
	  setCandidatesViewShown(false);
	  break;
	case KANJI:
	  mInputMode = KANJI;
	  break;
	case CHOOSE:
	  mInputMode = CHOOSE;
	  break;
	case ZENKAKU:
	  mInputMode = ZENKAKU;
	  icon = R.drawable.immodeic_full_alphabet;
	  setCandidatesViewShown(false);
	  break;
	case ENG2JP:
	  mInputMode = ENG2JP;
	  icon = R.drawable.immodeic_eng2jp;
	  setComposingTextSKK(getCurrentInputConnection(), "", 1);
	  break;
	case OKURIGANA:
	  mInputMode = OKURIGANA;
	  break;
	default:
	  break;
	}

	if (icon != 0) showStatusIcon(icon);
	if (isRegistering) {
	  setComposingTextSKK(getCurrentInputConnection(), "", 1);
	} // ComposingTextのflush回避のため，登録中はここまで来てからComposingText復活
  }

}
