package com.example.ummatelemedicineapp;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import com.twilio.video.Camera2Capturer;
import com.twilio.video.VideoCapturer;

public class CameraCapturerCompat {
    private static final String TAG = "CameraCapturerCompat";

    public static String getFrontCameraId(Context context) {
        return getCameraIdWithFacing(context, CameraCharacteristics.LENS_FACING_FRONT);
    }

    public static String getBackCameraId(Context context) {
        return getCameraIdWithFacing(context, CameraCharacteristics.LENS_FACING_BACK);
    }

    private static String getCameraIdWithFacing(Context context, int lensFacing) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == lensFacing) {
                    return cameraId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting camera ID", e);
        }
        return null;
    }
}
