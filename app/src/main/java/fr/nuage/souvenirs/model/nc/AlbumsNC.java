package fr.nuage.souvenirs.model.nc;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.util.ArrayList;
import java.util.UUID;

import fr.nuage.souvenirs.viewmodel.utils.NCGetAlbumList;

public class AlbumsNC {

    public static final int STATE_NOT_LOADED = 0;
    public static final int STATE_OK = 1;
    public static final int STATE_ERROR = 2;

    private static AlbumsNC albums;
    private OwnCloudClient ncClient;
    private MutableLiveData<ArrayList<AlbumNC>> ldAlbumList = new MutableLiveData<ArrayList<AlbumNC>>();
    private ArrayList<AlbumNC> albumList = new ArrayList<>();
    private int state;
    private MutableLiveData<Integer> ldState = new MutableLiveData<>();

    private AlbumsNC(OwnCloudClient ncClient) {
        this.ncClient = ncClient;
        setState(STATE_NOT_LOADED);
        //updateAlbumList();
    }

    public static AlbumsNC getInstance(OwnCloudClient ncClient) {
        if (albums == null) {
            albums = new AlbumsNC(ncClient);
        }
        return albums;
    }

    public void updateAlbumList() {
        //make async web api requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                RemoteOperationResult result = new NCGetAlbumList().execute(ncClient);
                if (!result.isSuccess()) {
                    Log.i(getClass().getName(),"Error on fetching nextcloud album list.");
                    setState(STATE_ERROR);
                    return;
                }
                setState(STATE_OK);
                ArrayList<AlbumNC> newAlbumList = new ArrayList<>();
                for (Object albumId: result.getData()) {
                    try {
                        AlbumNC albumNC = new AlbumNC(ncClient,(UUID) albumId);
                        albumNC.load();
                        newAlbumList.add(albumNC);
                    } catch (Exception e) {
                        Log.w(getClass().getName(),"Wrong album id "+albumId);
                    }
                }
                setAlbumList(newAlbumList);
            }
        }).start();
    }

    public void setAlbumList(ArrayList<AlbumNC> al) {
        albumList = al;
        ldAlbumList.postValue(albumList);
    }

    public ArrayList<AlbumNC> getAlbumList() {
        return albumList;
    }

    public MutableLiveData<ArrayList<AlbumNC>> getLiveDataAlbumList() {
        return ldAlbumList;
    }

    public boolean isInAlbumList(UUID id) {
        for (AlbumNC a : albumList) {
            if (a.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public LiveData<Integer> getLdState() {
        return ldState;
    }

    public int getState() {
        return state;
    }


    private void setState(int state) {
        this.state = state;
        ldState.postValue(state);
    }
}
