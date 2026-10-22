package com.vibecode404.savefile;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File saving for CaptionPop exports.
 *
 * Callers pass either:
 *   - sourcePath: a file already staged on disk (PREFERRED — the call itself
 *     stays tiny, which matters for saveWithPicker: the bridge retains and
 *     persists the call while the system picker activity is open, and a call
 *     carrying megabytes of base64 crashes the app on large exports), or
 *   - base64: the file contents inline (fine for small files only).
 *
 *  - saveWithPicker: opens the Android system "Save as" picker
 *    (ACTION_CREATE_DOCUMENT) so the user chooses the folder and file name.
 *  - saveToDownloads: saves straight into the public Downloads folder
 *    (MediaStore on Android 10+, legacy file write below that).
 */
@CapacitorPlugin(
    name = "SaveFile",
    permissions = {
        @Permission(alias = "storage", strings = { Manifest.permission.WRITE_EXTERNAL_STORAGE })
    }
)
public class SaveFilePlugin extends Plugin {

    private InputStream openSource(PluginCall call) throws Exception {
        String src = call.getString("sourcePath");
        if (src != null && !src.isEmpty()) {
            if (src.startsWith("content://")) {
                return getContext().getContentResolver().openInputStream(Uri.parse(src));
            }
            return new FileInputStream(new File(src));
        }
        String b64 = call.getString("base64");
        if (b64 == null) throw new Exception("sourcePath or base64 is required");
        return new ByteArrayInputStream(Base64.decode(b64, Base64.DEFAULT));
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        out.flush();
    }

    // ------------------------------------------------------------- picker

    @PluginMethod
    public void saveWithPicker(PluginCall call) {
        String fileName = call.getString("fileName", "captionpop-export");
        String mime = call.getString("mimeType", "application/octet-stream");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(call, intent, "savePickerResult");
    }

    @ActivityCallback
    private void savePickerResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }
        Intent data = result.getData();
        if (result.getResultCode() != Activity.RESULT_OK || data == null || data.getData() == null) {
            call.reject("cancelled");
            return;
        }
        Uri uri = data.getData();
        try (InputStream in = openSource(call);
             OutputStream os = getContext().getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new Exception("No output stream for " + uri);
            copy(in, os);
            JSObject ret = new JSObject();
            ret.put("uri", uri.toString());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------- downloads

    @PluginMethod
    public void saveToDownloads(PluginCall call) {
        String fileName = call.getString("fileName");
        String mime = call.getString("mimeType", "application/octet-stream");
        if (fileName == null) {
            call.reject("fileName is required");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Context ctx = getContext();
                ContentResolver cr = ctx.getContentResolver();
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                cv.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) {
                    call.reject("Could not create a Downloads entry");
                    return;
                }
                try (InputStream in = openSource(call);
                     OutputStream os = cr.openOutputStream(uri)) {
                    if (os == null) throw new Exception("No output stream for " + uri);
                    copy(in, os);
                }
                cv.clear();
                cv.put(MediaStore.Downloads.IS_PENDING, 0);
                cr.update(uri, cv, null, null);
                JSObject ret = new JSObject();
                ret.put("uri", uri.toString());
                ret.put("folder", "Downloads");
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("Save failed: " + e.getMessage());
            }
        } else {
            if (getPermissionState("storage") != PermissionState.GRANTED) {
                requestPermissionForAlias("storage", call, "legacySaveCallback");
                return;
            }
            legacyWrite(call, fileName);
        }
    }

    @PermissionCallback
    private void legacySaveCallback(PluginCall call) {
        if (getPermissionState("storage") == PermissionState.GRANTED) {
            legacyWrite(call, call.getString("fileName"));
        } else {
            call.reject("Storage permission denied");
        }
    }

    private void legacyWrite(PluginCall call, String fileName) {
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, fileName);
            try (InputStream in = openSource(call);
                 FileOutputStream fos = new FileOutputStream(out)) {
                copy(in, fos);
            }
            JSObject ret = new JSObject();
            ret.put("path", out.getAbsolutePath());
            ret.put("folder", "Downloads");
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        }
    }
}
