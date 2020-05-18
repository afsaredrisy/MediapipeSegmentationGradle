package co.introtuce.mediapipesegmentationgradle.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MyPermissionHelper {

    private static final String TAG = "PermissionHelper";

    public static final String READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int REQUEST_CODE = 0;

    public static boolean permissionsGranted(Activity context, String[] permissions) {
        for (String permission : permissions) {
            int permissionStatus = ContextCompat.checkSelfPermission(context, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void checkAndRequestPermissions(Activity context, String[] permissions) {
        if (!permissionsGranted(context, permissions)) {
            ActivityCompat.requestPermissions(context, permissions, REQUEST_CODE);
        }
    }

    /** Called by context to check if readWrite permissions have been granted. */
    public static boolean readWritePermissionsGranted(Activity context) {
        return permissionsGranted(context, new String[] {WRITE_PERMISSION});
    }

    /**
     * Called by context to check if readWrite permissions have been granted and if not, request them.
     */
    public static void checkAndRequestreadWritePermissions(Activity context) {
        Log.d(TAG, "checkAndRequestCameraPermissions");
        checkAndRequestPermissions(context, new String[] {WRITE_PERMISSION});
    }

    /** Called by context to check if audio permissions have been granted. */
    public static boolean readPermissionsGranted(Activity context) {
        return permissionsGranted(context, new String[] {READ_PERMISSION});
    }

    /** Called by context to check if audio permissions have been granted and if not, request them. */
    public static void checkAndRequestReadPermissions(Activity context) {
        Log.d(TAG, "checkAndRequestAudioPermissions");
        checkAndRequestPermissions(context, new String[] {READ_PERMISSION});
    }

    /** Called by context when permissions request has been completed. */
    public static void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (permissions.length > 0 && grantResults.length != permissions.length) {
            Log.d(TAG, "Permission denied.");
            return;
        }
        for (int i = 0; i < grantResults.length; ++i) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, permissions[i] + " permission granted.");
            }
        }
        // Note: We don't need any special callbacks when permissions are ready because activities
        // using this helper class can have code in onResume() which is called after the
        // permissions dialog box closes. The code can be branched depending on if permissions are
        // available via permissionsGranted(Activity).
        return;
    }


}
