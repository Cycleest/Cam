package cycleest.cam;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoFragment extends Fragment implements View.OnClickListener{

    private Button saveButton;
    private BitmapDrawable image;
    private ImageView preview;
    private int rotation = OrientationEventListener.ORIENTATION_UNKNOWN;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.layout_photo_preview, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUI();
    }

    private void initializeUI() {
        View root = getView();
        preview = (ImageView) root.findViewById(R.id.photo);
        if(image != null){
            preview.setImageDrawable(image);
        }
        saveButton = (Button) root.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);
        if(rotation != OrientationEventListener.ORIENTATION_UNKNOWN){
            saveButton.setRotation(rotation);
        }
    }

    public void setImage(Bitmap img){
        image = new BitmapDrawable(img);
        if(preview != null){
            preview.setImageDrawable(image);
        }
    }

    public void setRotation(int rotation){
        this.rotation = rotation;
        if(saveButton != null) {
            saveButton.setRotation(rotation);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == saveButton) {
            ((CamActivity) getActivity()).onSave();
            saveButton.setVisibility(View.INVISIBLE);
        }
    }
}
