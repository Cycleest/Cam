package cycleest.cam;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class CamActivity extends Activity implements CamLogic.Callback {

    private final String uiPreviewFragmentTag = "uiPreviewFragment";
    private final String uiPhotoFragmentTag = "uiPhotoFragment";
    private CamLogic camLogic;
    private int rotation;
    private OrientationListener orientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cam);

        camLogic = new CamLogic();
        FragmentManager fragmentManager = getFragmentManager();
        CamFragment livePreview = (CamFragment) fragmentManager.findFragmentByTag(uiPreviewFragmentTag);
        if (livePreview == null) {
            livePreview = new CamFragment();
        }
        //ViewGroup insertPoint = (ViewGroup) findViewById(R.id.activity_cam);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_cam, livePreview, uiPreviewFragmentTag);
        fragmentTransaction.commit();



    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        rotation = getWindowManager().getDefaultDisplay().getRotation();
        orientationListener = new OrientationListener(this, SensorManager.SENSOR_DELAY_UI);
        orientationListener.enable();
        Toast.makeText(this, "create", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camLogic.unsetPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CamFragment preview = (CamFragment) getFragmentManager().findFragmentByTag(uiPreviewFragmentTag);
        try {
            camLogic.setPreview(((SurfaceView) preview.getView().findViewById(R.id.previewScreen)).getHolder());
        } catch (IOException e) {
            Log.d("preview", "failed to restore preview after pause");
        }
    }

    public void onAction() {
        camLogic.doOneShot(this);
    }

    public void onSwitch() {
        camLogic.switchCamera();
    }

    public void onSave() {
        new AsyncTask() {

            public void addToGallery(Context context, String path, String fileName) {
                MediaScanner scanner = new MediaScanner(path + "/" + fileName, null);
                MediaScannerConnection connection = new MediaScannerConnection(context, scanner);
                scanner.connection = connection;
                connection.connect();
            }

            @Override
            protected Object doInBackground(Object[] params) {
                byte[] data = camLogic.getLastShotBytes();
                File camDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CamFolder");
                camDir.mkdirs();
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(camDir, fileName);
                FileOutputStream outStream;
                try {
                    outStream = new FileOutputStream(outFile);
                    outStream.write(data);
                    outStream.flush();
                    outStream.close();
                    addToGallery(getApplicationContext(), camDir.getAbsolutePath(), fileName);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onPictureSaved();
                        }
                    });

                } catch (IOException e) {
                    Log.d("file", "filed to write photo to storage");
                }
                return null;
            }
        }.execute();
    }

    public void onPreviewPrepared() {
        Point dims = camLogic.getCamDimens();
        ((CamFragment) getFragmentManager().findFragmentByTag(uiPreviewFragmentTag)).setPreviewDims(dims);
    }

    public void onPictureSaved() {
        Toast.makeText(getApplicationContext(), "file saved", Toast.LENGTH_SHORT).show();
    }

    public void onPreviewPrepared(SurfaceView preview) {
        try {
            camLogic.setPreview(preview.getHolder());
        } catch (IOException e) {
            Log.d("preview", "failed to set preview");
        }
    }

    @Override
    public void onCapture(Bitmap image) {
        FragmentManager fragmentManager = getFragmentManager();
        PhotoFragment photoFragment = (PhotoFragment) fragmentManager.findFragmentByTag(uiPhotoFragmentTag);
        if (photoFragment == null) {
            photoFragment = new PhotoFragment();
        }
        photoFragment.setImage(image);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.activity_cam, photoFragment, uiPhotoFragmentTag);
        photoFragment.setRotation(calculateRotation(rotation));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void handleOrientationCallback(int orientation) {
        if (orientation >= 320 || orientation <= 40) {
            if(this.rotation != 0){
                this.rotation = 0;
                onOrientationChanged();
            }
        } else if (orientation >= 230 && orientation <= 310) {
            if(this.rotation != 270){
                this.rotation = 270;
                onOrientationChanged();
            }
        } else if (orientation >= 140 && orientation <= 220) {
            if(this.rotation != 180){
                this.rotation = 180;
                onOrientationChanged();
            }
        } else if (orientation >= 50 && orientation <= 130) {
            if(this.rotation != 90){
                this.rotation = 90;
                onOrientationChanged();
            }
        }
    }

    private int calculateRotation(int deviceRotation){
        int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
        if(screenRotation == Surface.ROTATION_90){
            screenRotation = 90;
        } else if (screenRotation == Surface.ROTATION_270){
            screenRotation = 270;
        } else if (screenRotation == Surface.ROTATION_180){
            screenRotation = 180;
        }
        int rotation;
        rotation = deviceRotation+screenRotation;
        if(deviceRotation == 0 || deviceRotation == 180){
            rotation = rotation-180;
        }
        return rotation;
    }

    private void onOrientationChanged(){
        camLogic.onDeviceOrientationChanged(rotation);
        CamFragment livePreview = (CamFragment) getFragmentManager().findFragmentByTag(uiPreviewFragmentTag);
        if (livePreview != null) {
            livePreview.setRotation(calculateRotation(rotation));
        }
        PhotoFragment photoPreview = (PhotoFragment) getFragmentManager().findFragmentByTag(uiPhotoFragmentTag);
        if(photoPreview != null){
            photoPreview.setRotation(calculateRotation(rotation));
        }
    }

    class OrientationListener extends OrientationEventListener {

        public OrientationListener(Context context) {
            super(context);
        }

        public OrientationListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            handleOrientationCallback(orientation);
        }
    }
}

class MediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private final String path;
    private final String mimeType;
    MediaScannerConnection connection;

    public MediaScanner(String path, String mimeType) {
        this.path = path;
        this.mimeType = mimeType;
    }

    @Override
    public void onMediaScannerConnected() {
        connection.scanFile(path, mimeType);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        connection.disconnect();
    }
}
