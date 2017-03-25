package android.app.usage;

public interface IUsageStatsManager {

    public void setAppInactive(String packageName, boolean inactive, int userId) throws android.os.RemoteException;

    public boolean isAppInactive(String packageName, int userId) throws android.os.RemoteException;

    public static abstract class Stub {

        public static android.app.usage.IUsageStatsManager asInterface(android.os.IBinder obj) {
            throw new UnsupportedOperationException();
        }

    }

}

