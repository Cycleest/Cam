package cycleest.cam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CamLogic implements Camera.PictureCallback {

    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;

    private Camera deviceCamera;
    private byte[] lastShot;
    private int lastShotOrientation;
    private Callback receiver;
    private boolean isBusy;
    private int currentCamId;
    private SurfaceHolder preview;
    private Camera.CameraInfo camInfo;
    private int deviceOrientation;

    public CamLogic() {
        camInfo = new Camera.CameraInfo();
        lastShotOrientation = 0;
        //rotation = 0;
    }

    public interface Callback {
        public void onCapture(Bitmap image);
    }

    public void doOneShot(Callback receiver) {
        if (isBusy) return;
        isBusy = true;
        if (receiver != null) {
            this.receiver = receiver;
        }
        boolean cameraWasOpened;
        if (deviceCamera != null) {
            cameraWasOpened = true;
        } else {
            cameraWasOpened = false;
            deviceCamera = Camera.open(currentCamId);
            //camIni();
        }
        lastShotOrientation = deviceOrientation;
        deviceCamera.takePicture(null, null, null, this);
        if (!cameraWasOpened) {
            deviceCamera.release();//potentially bug, release before receiving a photo. UPD: confirmed.
            deviceCamera = null;
        }

    }

    public void setPreview(SurfaceHolder preview) throws IOException {
        if (preview != null) {
            if (deviceCamera == null) {
                deviceCamera = Camera.open(currentCamId);
                //camIni();
            } else {
                if (this.preview != null) {
                    deviceCamera.stopPreview();
                }
            }
            this.preview = preview;
            deviceCamera.setPreviewDisplay(preview);
            deviceCamera.startPreview();
        }
    }

    public void unsetPreview() {
        if (deviceCamera != null) {
            if (this.preview != null) {
                this.preview = null;
                deviceCamera.stopPreview();
                deviceCamera.release();
                deviceCamera = null;
            }
        }
    }

//    public void doShot(Callback receiver) {
//        if (isBusy) {
//            return;
//        }
//        isBusy = true;
//        if (receiver != null) {
//            this.receiver = receiver;
//        }
//        deviceCamera.takePicture(null, null, null, this);
//    }

    public Bitmap getLastShotBitmap() {
        return BitmapFactory.decodeByteArray(lastShot, 0, lastShot.length);
    }

    public byte[] getLastShotBytes() {
        return lastShot;
    }

    public void switchCamera() {
        if (isBusy) return;
        boolean cameraWasOpened;
        if (deviceCamera != null) {
            cameraWasOpened = true;
            if (preview != null) {
                deviceCamera.stopPreview();//any bugs if no preview previously? should test it
            }
            deviceCamera.release();
            deviceCamera = null;
        } else {
            cameraWasOpened = false;
        }
        if (currentCamId < Camera.getNumberOfCameras() - 1) {
            currentCamId++;
        } else {
            currentCamId = 0;
        }
        Camera.getCameraInfo(currentCamId, camInfo);
        if (cameraWasOpened) {
            deviceCamera = Camera.open(currentCamId);
            //camIni();
            if (preview != null) {
                try {
                    deviceCamera.setPreviewDisplay(preview);
                    deviceCamera.startPreview();

                } catch (IOException e) {
                    Log.d("preview", "failed to use previous preview on camera switch");
                }
            }
        }
    }

    public void setCamera(int id) {
        //should be implemented for handling restarts
    }

    public int getCameraFacing() {
        Camera.getCameraInfo(currentCamId, camInfo);
        int facing;
        switch (camInfo.orientation) {
            case Camera.CameraInfo.CAMERA_FACING_BACK:
                facing = CAMERA_FACING_BACK;
                break;
            case Camera.CameraInfo.CAMERA_FACING_FRONT:
                facing = CAMERA_FACING_FRONT;
                break;
            default:
                facing = CAMERA_FACING_BACK;
        }
        return facing;
    }

    public Point getCamDimens() {
        boolean cameraWasOpened;
        if (deviceCamera == null) {
            cameraWasOpened = false;
            deviceCamera = Camera.open(currentCamId);
            //camIni();
        } else {
            cameraWasOpened = true;
        }
        Camera.Parameters parameters = deviceCamera.getParameters();
        int width, height;
        width = parameters.getPictureSize().width;
        height = parameters.getPictureSize().height;
        Point dimens = new Point(width, height);
        if (!cameraWasOpened) {
            deviceCamera.release();
            deviceCamera = null;
        }
        return dimens;
    }

//    public int getCamerasCount() {
//        return camerasCount;
//    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        lastShot = data;
        Bitmap photo = getLastShotBitmap();
        int w = photo.getWidth();
        int h = photo.getHeight();
        photo = getLastShotBitmap();
        Matrix mtx = new Matrix();


        //mtx.postScale(-1, 1, 0, 0);
        //photo = Bitmap.createBitmap(photo, 0, 0, w, h, mtx, true);
        if (receiver != null) {
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if(lastShotOrientation == 90 || lastShotOrientation == 270){
                    receiver.onCapture(photo);
                }
                else {
                    mtx.postRotate(-180);
                    receiver.onCapture(Bitmap.createBitmap(photo, 0, 0, w, h, mtx, true));
                }
            }
            else{
                receiver.onCapture(photo);
            }

            //receiver.onCapture(photo);
            receiver = null;
        }
        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            lastShotOrientation = lastShotOrientation - 180;
            mtx.postRotate(-90+lastShotOrientation);
            //mtx.postScale(-1, 1, 0, 0);
        } else {
            mtx.postRotate(90+lastShotOrientation);
        }
        photo = Bitmap.createBitmap(photo, 0, 0, w, h, mtx, true);
        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Bitmap photo = (Bitmap) params[0];
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                lastShot = stream.toByteArray();

                isBusy = false;
                return null;
            }
        }.execute(photo);
        isBusy = false;
    }

    public void onDeviceOrientationChanged(int orientation){
        this.deviceOrientation = orientation;
    }
}
