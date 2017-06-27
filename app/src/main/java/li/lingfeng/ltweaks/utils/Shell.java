package li.lingfeng.ltweaks.utils;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by lilingfeng on 2017/6/27.
 */

public class Shell {

    private String mInterpreter;
    private Process mProcess;
    private DataOutputStream mOutputStream;

    public Shell(String interpreter) {
        mInterpreter = interpreter;
    }

    public void run(String cmd) throws Exception {
        try {
            Logger.d("Run \"" + cmd + "\" in \"" + mInterpreter + "\"");
            if (mProcess == null) {
                mProcess = Runtime.getRuntime().exec(mInterpreter);
            }
            mOutputStream = new DataOutputStream(mProcess.getOutputStream());
            mOutputStream.writeBytes(cmd + "\n");
            mOutputStream.flush();
        } catch (Exception e) {
            Logger.e("Run \"" + cmd + "\" in \"" + mInterpreter + "\" failed, " + e);
            throw e;
        }
    }

    public void close() {
        try {
            mOutputStream.writeBytes("exit\n");
            mOutputStream.flush();
            mOutputStream.close();
        } catch (Exception _) {
            mProcess.destroy();
        }
    }
}
