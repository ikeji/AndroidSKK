package jp.deadend.noname.skk;

import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class SKKDictionary {

    protected static final String BTREE_NAME = "skk_dict";
    protected String mDicFile;

    protected RecordManager mRecMan;
    protected long          mRecID;
    protected BTree mBTree;

    protected SKKDictionary() {
    }

    protected SKKDictionary(String dic) {
	mDicFile = dic;

	try {
  	    SKKUtils.dlog("sdcard status: " + Environment.getExternalStorageState());
	    mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
	    mRecID = mRecMan.getNamedObject(BTREE_NAME);

	    if (mRecID == 0) {
		Log.e("SKK", "Dictionary not found: " + mDicFile);
	    }
	    
	    mBTree = BTree.load(mRecMan, mRecID);	    
	} catch (IOException e) {
	    Log.e("SKK", "Error: " + e.toString());
	}
    }

    public ArrayList<String> getCandidates(String key) {
	ArrayList<String> list = new ArrayList<String>();
	String[] va_array;

	SKKUtils.dlog("findValue(): key = " + key);
    
	try {
	    String value = (String)mBTree.find(key);

	    if (value == null) return null;

	    va_array = value.split("/");
	    SKKUtils.dlog("dic: " + mDicFile + " " + value);
	    SKKUtils.dlog("length = " + va_array.length);
	    
	    if (va_array.length <= 0) {
		Log.e("SKK", "Invalid value found: Key=" + key + " value=" + value);
		return null;
	    }

	    // va_array[0]は常に空文字列なので1から始める
	    for (int i=1; i<va_array.length; i++) {
		list.add(va_array[i]);
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}

	return list;
    }

    public void findKeys(String key, boolean isKanji, ArrayList<String> list) {
	Tuple         tuple = new Tuple();
	TupleBrowser  browser;

	try {
	    browser = mBTree.browse(key);
	    if (browser.getNext(tuple) == false) return;
	    // 最初の一つがkeyと同じ場合listに追加しない
	    String first = (String)tuple.getKey();
	    if (!first.equals(key)) list.add(first);
      
	    if (!isKanji) {
		for (int i=0; i<5; i++) {
		    if (browser.getNext(tuple) == false) break;
		    list.add((String)tuple.getKey());
		}
	    } else {
		int klen = key.length();
		String str = null;
		for (int i=0; i<5; i++) {
		    if (browser.getNext(tuple) == false) break;
		    str = (String)tuple.getKey();
		    if ((str.length() == klen+1) && SKKUtils.isAlphabet(str.charAt(klen))) continue;
		    list.add(str);
		}
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }
}