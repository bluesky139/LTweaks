package android.service.trust;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lilingfeng on 2017/7/6.
 */

public class TrustAgentService extends Service {

    public final void grantTrust(
            final CharSequence message, final long durationMs, final int flags) {
    }

    public final void revokeTrust() {
    }

    public final void setManagingTrust(boolean managingTrust) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
