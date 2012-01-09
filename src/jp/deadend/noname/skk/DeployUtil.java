package jp.deadend.noname.skk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class DeployUtil {
	/**
	 * returns whether debuggable is on
	 * @param ctx
	 * @return
	 */
	public static boolean isDebuggable(Context ctx) {
		PackageManager manager = ctx.getPackageManager();
		ApplicationInfo appInfo = null;
		try {
			appInfo = manager.getApplicationInfo(ctx.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			return false;
		}
		if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
			return true;
		return false;
	}
}
