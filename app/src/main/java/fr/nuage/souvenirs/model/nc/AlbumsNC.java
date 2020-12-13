package fr.nuage.souvenirs.model.nc;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumsNC {

    public static final int STATE_NOT_LOADED = 0;
    public static final int STATE_OK = 1;
    public static final int STATE_ERROR = 2;

    private static AlbumsNC albums;
    private MutableLiveData<ArrayList<AlbumNC>> ldAlbumList = new MutableLiveData<ArrayList<AlbumNC>>();
    private ArrayList<AlbumNC> albumList = new ArrayList<>();
    private int state;
    private MutableLiveData<Integer> ldState = new MutableLiveData<>();

    private AlbumsNC() {
        setState(STATE_NOT_LOADED);
    }

    public static AlbumsNC getInstance() {
        if (albums == null) {
            albums = new AlbumsNC();
        }
        return albums;
    }

    public void updateAlbumList() {
        //make async web api requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> albumIds = null;
                try {
                    albumIds = APIProvider.getApi().getAlbums().execute().body();
                    if (albumIds == null) {
                        throw new IOException("Network error.");
                    }
                } catch (IOException e) {
                    Log.i(getClass().getName(),"Error on fetching nextcloud album list.",e);
                    setState(STATE_ERROR);
                    return;
                }
                setState(STATE_OK);
                ArrayList<AlbumNC> newAlbumList = new ArrayList<>();
                for (String albumId: albumIds) {
                    AlbumNC albumNC = new AlbumNC(UUID.fromString(albumId));
                    if (albumNC.load()) {
                        albumNC.setState(AlbumNC.STATE_OK);
                        newAlbumList.add(albumNC);
                    } else {
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

    public void deleteAlbum(AlbumNC albumNC) {
        APIProvider.getApi().deleteAlbum(albumNC.getId().toString()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.body().equals("OK")) {
                    updateAlbumList();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }
}
