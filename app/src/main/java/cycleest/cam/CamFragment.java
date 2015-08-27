package cycleest.cam;

import android.app.Fragment;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class CamFragment extends Fragment implements View.OnClickListener, SurfaceHolder.Callback {

    private Button actionButton;
    private Button switchButton;
    private SurfaceView previewScreen;
    private int rotation = OrientationEventListener.ORIENTATION_UNKNOWN;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.layout_live_preview, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUI();
        ((CamActivity) getActivity()).onPreviewPrepared();
        if(rotation != OrientationEventListener.ORIENTATION_UNKNOWN){
            switchButton.setRotation(rotation);
            actionButton.setRotation(rotation);
        }
    }

    private void initializeUI() {
        View root = getView();
        actionButton = (Button) root.findViewById(R.id.actionButton);
        switchButton = (Button) root.findViewById(R.id.swapButton);
        previewScreen = (SurfaceView) root.findViewById(R.id.previewScreen);

        previewScreen.getHolder().addCallback(this);

        previewScreen.setOnClickListener(this);//

        actionButton.setScaleY(.5f);
        //switchButton.setRotation(-90);
        actionButton.setOnClickListener(this);
        switchButton.setOnClickListener(this);
    }

    public void setPreviewDims(Point dims){//should anyway implement case when preview taller than screen

        //int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getActivity().getWindowManager().getDefaultDisplay().getHeight();
        //int height = (int) Math.round((double)width*(double)dims.x/(double)dims.y);
        int width = (int) Math.round((double)height*(double)dims.x/(double)dims.y);
        //preview.getLayoutParams().height = height;
        previewScreen.getLayoutParams().width = width;
    }

    public void setRotation(int rotation){
        this.rotation = rotation;
        if (switchButton != null) {
            switchButton.setRotation(rotation);
        }
        if(actionButton != null){
            actionButton.setRotation(rotation);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == actionButton || v == previewScreen) {
            ((CamActivity) getActivity()).onAction();
        } else if (v == switchButton) {
            ((CamActivity) getActivity()).onSwitch();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        ((CamActivity) getActivity()).onPreviewPrepared(previewScreen);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}


