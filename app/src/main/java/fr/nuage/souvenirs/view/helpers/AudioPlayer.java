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
import java.util.ArrayList;

import fr.nuage.souvenirs.model.AudioElement;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.VideoElementViewModel;

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
                int lastPos = ((LinearLayoutManager)layoutManager).findLastCompletelyVisibleItemPosition();
                if (firstPos == -1) {
                    return;
                }
                if (lastPos == albumViewModel.getSize()-1) {
                    firstPos = lastPos;
                }
                //for audio
                if ((lastPosition < firstPos) || (firstPos == 0)) {
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
                //for video
                if ((lastPosition != firstPos) || (firstPos == 0)) {
                    Log.d(getClass().getName(), "Page scroll : "+lastPosition +"/"+ firstPos);
                    //start video if one present on new page
                    PageViewModel pageViewModel = albumViewModel.getPage(firstPos);
                    ArrayList<VideoElementViewModel> videos = pageViewModel.getVideoElements();
                    for (VideoElementViewModel video : videos) {
                        video.setIsPlaying(true);
                    }
                    //stop videos on previous pages
                    if (lastPosition != firstPos) {
                        pageViewModel = albumViewModel.getPage(lastPosition);
                        videos = pageViewModel.getVideoElements();
                        for (VideoElementViewModel video : videos) {
                            video.setIsPlaying(false);
                        }
                    }
                }
                lastPosition = firstPos;
            }
        }
    }
}
