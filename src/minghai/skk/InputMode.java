package minghai.skk;

public enum InputMode {
	HIRAKANA,  // 平仮名
	KATAKANA,  // 片仮名
	KANJI,     // 漢字変換候補(ひらかな)入力中
	CHOOSE,    // 漢字選択中
	REGISTER,  // 辞書登録
	ALPHABET,  // 無変換モード
	ZENKAKU,   // 全角英字モード
	ENG2JAP,   // 英日変換用英単語入力モード
	OKURIGANA; // 送り仮名入力中
}
