package li.lingfeng.ltweaks.services;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;

import java.io.FileNotFoundException;

import li.lingfeng.ltweaks.utils.ContextUtils;

/**
 * Created by lilingfeng on 2018/1/22.
 */

public class ResourceProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException("ResourceProvider mode " + mode + " is not supported.");
        }
        String type = uri.getPathSegments().get(0);
        String name = uri.getPathSegments().get(1);
        if (type.equals("raw")) {
            int rawId = ContextUtils.getRawId(name);
            return getContext().getResources().openRawResourceFd(rawId);
        } else {
            throw new NotImplementedException("ResourceProvider openFile type " + type);
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        throw new NotImplementedException("ResourceProvider query");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new NotImplementedException("ResourceProvider getType");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("ResourceProvider insert is not supported.");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("ResourceProvider delete is not supported.");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("ResourceProvider update is not supported.");
    }
}
