package fr.nuage.souvenirs.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.io.InputStream;

import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.model.VideoElement;

public class VideoElementViewModel extends ImageElementViewModel {

    private final LiveData<String> videoPath;
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();

    public VideoElementViewModel(VideoElement e) {
        super(e);
        videoPath = Transformations.map(e.getLdVideoPath(), videoPath -> videoPath);
    }

    public LiveData<String> getVideoPath() {
        return videoPath;
    }

    public void setVideo(InputStream input, String mimeType) {
        ((VideoElement)element).setVideo(input,mimeType);
    }

    public void setVideo(String localImagePath, String mimeType) {
        ((VideoElement)element).setVideo(localImagePath,mimeType);
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying.postValue(isPlaying);
    }

    public MutableLiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }
}
