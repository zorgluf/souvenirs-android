package fr.nuage.souvenirs.view;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;

public class RecordSoundDialogFragment extends DialogFragment {

    private MediaRecorder recorder;
    private File destFile;

    public RecordSoundDialogFragment(File recordFile) {
        super();
        destFile = recordFile;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.AppTheme_MaterialDialog_Alert);
        View recordLayout = getLayoutInflater().inflate(R.layout.record_sound_dialog,null);
        ImageView micImage = recordLayout.findViewById(R.id.mic_image);
        micImage.setOnClickListener(view -> {
            stopRecording();
            dismiss();
        });
        builder.setView(recordLayout)
                .setCancelable(false);

        //start recording
        startRecording();

        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(destFile.getPath());
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(getClass().getName(), "mediarecorder prepare failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

}
