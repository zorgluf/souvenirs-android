package fr.nuage.souvenirs.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
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
        //get albums path
        File albumsPath = new File(application.getApplicationContext().getExternalFilesDir(null),"albums");
        if (!albumsPath.exists()) {
            albumsPath.mkdirs();
        }
        //load albums
        albums = Albums.getInstance(albumsPath.getPath());
        //albumViewModels = new CopyOnWriteArrayList<>(new ArrayList<>());
        albumViewModels = new ArrayList<>();
        //build livedata and load album list
        updateAlbumList();
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
                    //remove albumNC ref
                    for(Iterator<AlbumViewModel> iter = albumViewModels.iterator(); iter.hasNext();) {
                        AlbumViewModel albumViewModel = iter.next();
                        albumViewModel.setAlbumNC(null);
                        if (albumViewModel.getAlbum() == null) {
                            iter.remove();
                        }
                    }
                    //remove albumsNC model
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
            for(int i = 0; i<albumViewModels.size(); i++) {
                AlbumViewModel avm = albumViewModels.get(i);
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
            for (AlbumNC a : new ArrayList<>(albumsNC.getAlbumList())) {
                //check if exists in list
                boolean albumExists = false;
                Iterator<AlbumViewModel> itAVM = albumViewModels.iterator();
                while (itAVM.hasNext()) {
                    AlbumViewModel avm = itAVM.next();
                    if (a.getId().equals(avm.getId())) {
                        albumExists = true;
                        //check if albumNC is present in VM
                        if (!avm.hasNCAlbum()) {
                            avm.setAlbumNC(a);
                        }
                    }
                }
                if (!albumExists) {
                    albumViewModels.add(new AlbumViewModel(getApplication(), a));
                }
                if (!a.getLdDate().hasObservers()) {
                    a.getLdDate().observeForever(date -> sortAlbumList());
                }
            }
        }
        //check if some album are deleted
        ArrayList<AlbumViewModel> aVM = new ArrayList<>(albumViewModels);
        for(int i = 0; i<aVM.size(); i++) {
            AlbumViewModel avm = aVM.get(i);
            if ((avm.getAlbum()!=null) && (!albums.isInAlbumList(avm.getAlbum().getId()))) {
                albumViewModels.get(i).setAlbum(null);
            }
            if (albumsNC != null) {
                if ((avm.getAlbumNC()!=null) && (!albumsNC.isInAlbumList(avm.getAlbumNC().getId()))) {
                    albumViewModels.get(i).setAlbumNC(null);
                }
            }
            if ((avm.getAlbum() == null) && (avm.getAlbumNC() == null))  {
                    albumViewModels.remove(i);
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

    public void deleteAlbum(AlbumViewModel album) {
        if (album.hasNCAlbum()) {
            albumsNC.deleteAlbum(album.getAlbumNC());
        }
        deleteLocalAlbum(album);
    }
}
