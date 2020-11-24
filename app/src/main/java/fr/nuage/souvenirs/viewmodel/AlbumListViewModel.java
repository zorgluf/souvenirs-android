package fr.nuage.souvenirs.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import fr.nuage.souvenirs.SettingsActivity;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Albums;
import fr.nuage.souvenirs.model.nc.APIProvider;
import fr.nuage.souvenirs.model.nc.AlbumNC;
import fr.nuage.souvenirs.model.nc.AlbumsNC;
import fr.nuage.souvenirs.model.nc.NCAPI;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class AlbumListViewModel extends AndroidViewModel {
    private AlbumsNC albumsNC;
    private MediatorLiveData<List<AlbumViewModel>> albumList = new MediatorLiveData<>();
    private Albums albums;
    private List<AlbumViewModel> albumViewModels;
    private Observer<Integer> albumsNCStateObserver;

    public AlbumListViewModel(@NonNull Application application) {
        super(application);
        //loads path from prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        String albumPathPref = prefs.getString(SettingsActivity.ALBUMS_PATH, null);
        //load albums
        albums = Albums.getInstance(albumPathPref);
        albumViewModels = new CopyOnWriteArrayList<>(new ArrayList<>());
        //build livedata and load album list
        albumList.addSource(albums.getLiveDataAlbumList(), albums -> updateAlbumList());
        //load NC albums
        NCUtils.getIsNCEnable().observeForever(aBoolean -> {
            if (aBoolean) {
                APIProvider.init(getApplication().getApplicationContext());
                albumsNC = AlbumsNC.getInstance();
                albumList.addSource(albumsNC.getLiveDataAlbumList(), albumsNC -> updateAlbumList());
                albumsNCStateObserver = state -> {
                    for (AlbumViewModel albumViewModel: albumViewModels) {
                        albumViewModel.onAlbumsNCStateChanged(state);
                    }
                };
                albumsNC.getLdState().observeForever(albumsNCStateObserver);
                albumsNC.updateAlbumList();
            } else {
                if (albumsNC != null) {
                    albumList.removeSource(albumsNC.getLiveDataAlbumList());
                    albumsNC.getLdState().removeObserver(albumsNCStateObserver);
                    albumsNC = null;
                }
            }
        });
    }

    private void sortAlbumList() {
        //sort by date
        albumViewModels.sort((lhs, rhs) -> {
            if (rhs.getDate() == null) {
                return -1;
            }
            if (lhs.getDate() == null) {
                return 1;
            }
            return rhs.getDate().compareTo(lhs.getDate());
        });
        albumList.setValue(albumViewModels);
    }

    private void updateAlbumList() {

        //check new albums in list
        for (Album a: albums.getAlbumList()) {
            //check if exists in list
            boolean albumExists = false;
            for (AlbumViewModel avm: albumViewModels) {
                if (a.getId().equals(avm.getId())) {
                    albumExists = true;
                    //check if album is present in VM
                    if (!avm.hasLocalAlbum()) {
                        avm.setAlbum(a);
                        a.getLdDate().observeForever(date -> sortAlbumList());
                    }
                }
            }
            if (!albumExists) {
                albumViewModels.add(new AlbumViewModel(getApplication(),a));
                a.getLdDate().observeForever(date -> sortAlbumList());
            }
        }
        if (albumsNC != null) {
            //check new albumNC in list
            for (AlbumNC a : albumsNC.getAlbumList()) {
                //check if exists in list
                boolean albumExists = false;
                for (AlbumViewModel avm : albumViewModels) {
                    if (a.getId().equals(avm.getId())) {
                        albumExists = true;
                        //check if albumNC is present in VM
                        if (!avm.hasNCAlbum()) {
                            avm.setAlbumNC(a);
                            a.getLdDate().observeForever(date -> sortAlbumList());
                        }
                    }
                }
                if (!albumExists) {
                    albumViewModels.add(new AlbumViewModel(getApplication(), a));
                    a.getLdDate().observeForever(date -> sortAlbumList());
                }
            }
        }
        //check if some album are deleted
        for (AlbumViewModel avm: albumViewModels) {
            if ((avm.getAlbum()!=null) && (!albums.isInAlbumList(avm.getAlbum().getId()))) {
                avm.setAlbum(null);
            }
            if (albumsNC != null) {
                if ((avm.getAlbumNC()!=null) && (!albumsNC.isInAlbumList(avm.getAlbumNC().getId()))) {
                    avm.setAlbumNC(null);
                }
            }
            if ((avm.getAlbum() == null) && (avm.getAlbumNC() == null))  {
                    albumViewModels.remove(avm);
            }
        }
        sortAlbumList();
    }

    public LiveData<List<AlbumViewModel>> getAlbumList() {
        return albumList;
    }

    public AlbumViewModel getAlbum(String albumPath) {
        for (AlbumViewModel a: albumViewModels) {
            if (a.hasLocalAlbum()) {
                if (a.getAlbumPath().equals(albumPath)) {
                    return a;
                }
            }
        }
        return null;
    }

    public AlbumViewModel getAlbum(UUID id) {
        for (AlbumViewModel a: albumViewModels) {
            if (a.getId().equals(id)) {
                    return a;
            }
        }
        return null;
    }

    public void deleteLocalAlbum(AlbumViewModel albumViewModel) {
        if (albumViewModel.hasLocalAlbum()) {
            albums.deleteAlbum(albumViewModel.getAlbum());
        }
    }

    public void refresh() {
        if (albums != null) {
            albums.updateAlbumList();
        }
        if (albumsNC != null) {
            albumsNC.updateAlbumList();
        }
    }
}
