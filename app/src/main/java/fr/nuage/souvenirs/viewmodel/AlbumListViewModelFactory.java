package fr.nuage.souvenirs.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class AlbumListViewModelFactory implements ViewModelProvider.Factory {

    private static AlbumListViewModel albumListViewModel;
    private Application application;

    public AlbumListViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AlbumListViewModel.class)) {
            if (albumListViewModel == null) {
                albumListViewModel = new AlbumListViewModel(application);
            }
            return (T) albumListViewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }

    public static AlbumListViewModel getAlbumListViewModel() {
        return albumListViewModel;
    }
}
