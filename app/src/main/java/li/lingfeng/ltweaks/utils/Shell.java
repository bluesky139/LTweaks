package li.lingfeng.ltweaks.utils;

import android.os.AsyncTask;
import android.os.Handler;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lilingfeng on 2017/6/27.
 */

public class Shell extends AsyncTask<Void, Void, Boolean> {

    private String mInterpreter;
    private String[] mCmds;
    private Callback.C3<Boolean, List<String>, List<String>> mCallback;
    private Process mProcess;
    private StreamGobbler mStderrGobbler;
    private StreamGobbler mStdoutGobbler;

    public Shell(String interpreter, String[] cmds, long timeout, Callback.C3<Boolean, List<String>, List<String>> callback) {
        mInterpreter = interpreter;
        mCmds = cmds;
        mCallback = callback;

        if (timeout > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    forceClean();
                }
            }, timeout);
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Logger.d("Run cmd in " + mInterpreter + ":\n  " + StringUtils.join(mCmds, "\n  "));
            mProcess = Runtime.getRuntime().exec(mInterpreter);
            DataOutputStream outputStream = new DataOutputStream(mProcess.getOutputStream());
            mStderrGobbler = new StreamGobbler("STDERR", mProcess.getErrorStream());
            mStderrGobbler.start();
            mStdoutGobbler = new StreamGobbler("STDOUT", mProcess.getInputStream());
            mStdoutGobbler.start();
            for (String cmd : mCmds) {
                outputStream.writeBytes(cmd + "\n");
            }
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            mProcess.waitFor();
            Logger.d("Run cmd end.");

            synchronized (this) {
                if (mProcess == null) {
                    return false;
                } else {
                    mProcess = null;
                    while (!mStderrGobbler.isEnded() || !mStdoutGobbler.isEnded()) {
                        wait();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Logger.e("Run cmd failed, " + e);
            return false;
        }
    }

    private void forceClean() {
        synchronized (this) {
            if (mProcess != null) {
                Logger.d("Force clean cmd.");
                mProcess.destroy();
                mProcess = null;
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean isOk) {
        mCallback.onResult(isOk, mStderrGobbler.getOutputs(), mStdoutGobbler.getOutputs());
    }

    private class StreamGobbler extends Thread {

        private String mType;
        private InputStream mInputStream;
        private List<String> mOutputs = new ArrayList<>(); // Since all outputs are put into memory, this should not be used for large outputs.
        private boolean mIsEnded = false;

        public StreamGobbler(String type, InputStream inputStream) {
            mType = type;
            mInputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.d("[" + mType + "] " + line);
                    mOutputs.add(line);
                }
                Logger.d("StreamGobbler " + mType + " end.");
            } catch (Exception e) {
                Logger.e("StreamGobbler " + mType + " error, " + e);
            }

            synchronized (Shell.this) {
                mIsEnded = true;
                Shell.this.notify();
            }
        }

        public boolean isEnded() {
            return mIsEnded;
        }

        public List<String> getOutputs() {
            return mOutputs;
        }
    }
}
