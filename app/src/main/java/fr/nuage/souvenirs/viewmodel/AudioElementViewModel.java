package fr.nuage.souvenirs.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import fr.nuage.souvenirs.model.AudioElement;
import fr.nuage.souvenirs.model.Element;

public class AudioElementViewModel extends ElementViewModel{

    private final LiveData<String> audioPath;
    private final LiveData<Boolean> stop;

    public AudioElementViewModel(AudioElement e) {
        super(e);
        audioPath = Transformations.map(e.getLdAudioPath(), audioPath -> audioPath);
        stop = Transformations.map(e.getLdStop(), stop -> stop);
    }

    public LiveData<String> getAudioPath() {
        return audioPath;
    }

    public LiveData<Boolean> getStop() {
        return stop;
    }

    public boolean isStop() {
        return ((AudioElement)element).isStop();
    }

    public void delete() {
        element.delete();
    }
}
