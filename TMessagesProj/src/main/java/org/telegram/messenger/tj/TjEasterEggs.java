package org.telegram.messenger.tj;

import android.content.Intent;
import android.net.Uri;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TjLocale;
import org.telegram.messenger.XiaomiUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

public final class TjEasterEggs {

    private TjEasterEggs() {
    }

    public static void handleXiaomi(BaseFragment fragment) {
        if (fragment == null) {
            return;
        }

        if (XiaomiUtilities.isMIUI()) {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.info, TjLocale.getString(R.string.TjXiaomiFailure))
                    .show();

            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ApplicationLoader.applicationContext.startActivity(intent);
        } else {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.info, TjLocale.getString(R.string.TjXiaomiSuccess))
                    .show();
        }
    }
}
