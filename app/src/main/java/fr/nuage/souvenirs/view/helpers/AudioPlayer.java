package fr.nuage.souvenirs.view.helpers;


import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;

import fr.nuage.souvenirs.model.AudioElement;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;

public class AudioPlayer implements View.OnScrollChangeListener {

    private AlbumViewModel albumViewModel;
    private MediaPlayer mediaPlayer;
    private int lastPosition;

    public AudioPlayer(AlbumViewModel albumViewModel) {
        this.albumViewModel = albumViewModel;
        //init media player
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );


    }

    private void play(String audioPath) {
        if (mediaPlayer.isPlaying()) {
            noplay();
        }
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.w(getClass().getName(),"Error when playing audio file :"+audioPath);
        }
    }

    private void noplay() {
        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    public void stop() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public void onScrollChange(View view, int i, int i1, int i2, int i3) {
        if (view instanceof RecyclerView) {
            RecyclerView.LayoutManager layoutManager = ((RecyclerView)view).getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                int firstPos = ((LinearLayoutManager)layoutManager).findFirstCompletelyVisibleItemPosition();
                if (firstPos == -1) {
                    return;
                }
                //Log.d("TRACE",String.valueOf(firstPos)+"/"+String.valueOf(lastPosition));
                if (lastPosition < firstPos) {
                    PageViewModel pageViewModel = albumViewModel.getPage(firstPos);
                    AudioElement audioElement = pageViewModel.getAudioElement();
                    if (audioElement != null) {
                        if (audioElement.isStop()) {
                            noplay();
                        } else {
                            String audioPath = audioElement.getAudioPath();
                            play(audioPath);
                        }
                    }
                }
                lastPosition = firstPos;
            }
        }
    }
}
