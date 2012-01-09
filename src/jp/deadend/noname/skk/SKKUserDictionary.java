package jp.deadend.noname.skk;

import android.util.Log;

import java.io.IOException;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;

public class SKKUserDictionary extends SKKDictionary {

    private int mCount = 0;

    protected SKKUserDictionary(String dic) {
	mDicFile = dic;

	try {
	    mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
	    mRecID = mRecMan.getNamedObject(BTREE_NAME);

	    if (mRecID == 0) {
		mBTree = BTree.createInstance(mRecMan, new StringComparator());
		mRecMan.setNamedObject(BTREE_NAME, mBTree.getRecid());
		mRecMan.commit();
		SKKUtils.dlog("New user dictionary created");
	    } else {
		mBTree = BTree.load(mRecMan, mRecID);
	    }	    
	} catch (IOException e) {
	    Log.e("SKK", "Error: " + e.toString());
	}
    }

    public void addEntry(String key, String val) {
	try {
	    String old_val = (String)mBTree.find(key);
	    StringBuilder new_val = new StringBuilder();
	    new_val.append("/");
	    new_val.append(val);
	    new_val.append("/");
	    
	    if (old_val != null) {
		String[] old_va_array = old_val.split("/");
		
		for (int i=1; i<old_va_array.length; i++) {
		    //重複チェック
		    if (!val.equals(old_va_array[i])) {
			new_val.append(old_va_array[i]);
			new_val.append("/");
		    }
		}
	    }

	    mBTree.insert(key, new_val.toString(), true);
	    mCount++;
	    SKKUtils.dlog("add to user dict: key=" + key + " old_val=" + old_val + " new_val=" + new_val.toString() + " count=" + mCount);
	    
	    if (mCount % 1000 == 0) {
		mRecMan.commit();
	    }
	} catch (Exception e) {
	    Log.e("SKK", "Error: " + e.toString());
	}	
    }

    public void commitChanges() {
	try {
	    mRecMan.commit();
	} catch (Exception e) {
	    Log.e("SKK", "Error: " + e.toString());
	}
    }
}