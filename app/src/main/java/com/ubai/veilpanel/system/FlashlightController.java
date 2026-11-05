package com.ubai.veilpanel.system;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

/**
 * Public-API flashlight controller.
 */
public final class FlashlightController {

    private final Context context;
    private final CameraManager cameraManager;

    private String cameraId;
    private boolean enabled;
    private boolean available;
    private boolean callbackRegistered;

    private final CameraManager.TorchCallback torchCallback =
            new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(
                        String changedCameraId,
                        boolean isEnabled) {

                    if (cameraId != null
                            && cameraId.equals(changedCameraId)) {
                        available = true;
                        enabled = isEnabled;
                    }
                }

                @Override
                public void onTorchModeUnavailable(
                        String changedCameraId) {

                    if (cameraId != null
                            && cameraId.equals(changedCameraId)) {
                        available = false;
                        enabled = false;
                    }
                }
            };

    public FlashlightController(Context context) {
        this.context = context.getApplicationContext();
        cameraManager =
                (CameraManager) this.context.getSystemService(
                        Context.CAMERA_SERVICE
                );

        refreshHardware();
    }

    public boolean hasPermission() {
        return context.checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isAvailable() {
        refreshHardware();
        return available;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean toggle() {
        refreshHardware();

        if (!available || cameraManager == null || cameraId == null) {
            return false;
        }

        try {
            cameraManager.setTorchMode(cameraId, !enabled);
            return true;
        } catch (CameraAccessException
                 | IllegalArgumentException
                 | SecurityException ignored) {
            return false;
        }
    }

    public void close() {
        if (cameraManager != null && callbackRegistered) {
            try {
                cameraManager.unregisterTorchCallback(torchCallback);
            } catch (Exception ignored) {
            }

            callbackRegistered = false;
        }
    }

    private void refreshHardware() {
        if (!hasPermission() || cameraManager == null) {
            available = false;
            return;
        }

        if (cameraId == null) {
            cameraId = findFlashCamera();
        }

        available = cameraId != null;

        if (available && !callbackRegistered) {
            try {
                cameraManager.registerTorchCallback(
                        context.getMainExecutor(),
                        torchCallback
                );
                callbackRegistered = true;
            } catch (Exception ignored) {
                callbackRegistered = false;
            }
        }
    }

    private String findFlashCamera() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics =
                        cameraManager.getCameraCharacteristics(id);

                Boolean flashAvailable =
                        characteristics.get(
                                CameraCharacteristics.FLASH_INFO_AVAILABLE
                        );

                Integer lensFacing =
                        characteristics.get(
                                CameraCharacteristics.LENS_FACING
                        );

                if (Boolean.TRUE.equals(flashAvailable)
                        && (lensFacing == null
                        || lensFacing
                        == CameraCharacteristics.LENS_FACING_BACK)) {
                    return id;
                }
            }
        } catch (CameraAccessException
                 | SecurityException ignored) {
        }

        return null;
    }
}
