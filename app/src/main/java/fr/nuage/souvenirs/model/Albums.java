package fr.nuage.souvenirs.model;

import android.content.SharedPreferences;
import android.os.FileObserver;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.SettingsActivity;

public class Albums {

    private String albumListPath;
    private MutableLiveData<ArrayList<Album>> ldAlbumList = new MutableLiveData<ArrayList<Album>>();
    private ArrayList<Album> albumList = new ArrayList<>();
    private static Albums mAlbums;

    private Albums(String albumsPath) {
        if (! new File(albumsPath).exists()) {
            new File(albumsPath).mkdirs();
        }
        this.albumListPath = albumsPath;
        updateAlbumList();
    }

    public static Albums getInstance(String albumsPath) {
        if (mAlbums == null) {
            mAlbums = new Albums(albumsPath);
        }
        //trick to load album list on album creation
        mAlbums.updateAlbumList();
        return mAlbums;
    }

    public static Albums getInstance() {
        return mAlbums;
    }

    public void updateAlbumList() {
        File albumsDir = new File(this.albumListPath);
        if (albumsDir.listFiles() != null) {
            //check for deleted albums
            for (Iterator<Album> it=albumList.iterator(); it.hasNext();) {
                Album album = it.next();
                boolean exists = false;
                for (File f : albumsDir.listFiles()) {
                    if (album.getAlbumPath().equals(f.getPath())) {
                        exists = true;
                    }
                }
                if (!exists) {
                    albumList.remove(album);
                }
            }
            //check for new or updated albums
            for (File f : albumsDir.listFiles()) {
                if (f.isDirectory() && (Album.exists(f.getPath()))) {
                    Album album = getAlbum(f.getPath());
                    if (album == null) {
                        albumList.add(new Album(f.getPath()));
                    }
                }
            }
        }
        //sort by date
        albumList.sort((lhs, rhs) -> {
            if (rhs.getDate() == null) {
                return -1;
            }
            if (lhs.getDate() == null) {
                return 1;
            }
            return rhs.getDate().compareTo(lhs.getDate());
        });
        setAlbumList(albumList);
    }

    public void setAlbumList(ArrayList<Album> al) {
        albumList = al;
        ldAlbumList.postValue(albumList);
    }

    public ArrayList<Album> getAlbumList() {
        return albumList;
    }

    public MutableLiveData<ArrayList<Album>> getLiveDataAlbumList() {
        return ldAlbumList;
    }

    public boolean isInAlbumList(UUID id) {
        for (Album a : albumList) {
            if (a.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public Album getAlbum(String albumPath) {
        for (Album a : albumList) {
            if (a.getAlbumPath().equals(albumPath)) {
                return a;
            }
        }
        return null;
    }

    public Album getAlbum(UUID id) {
        for (Album a : albumList) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    /**
     * create empty album
     * @return
     */
    public Album createAlbum(UUID albumId) {
        //set album date as rep
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE);
        String albumPath = new File(albumListPath,formater.format(new Date())).getPath();
        //create directory
        new File(albumPath).mkdir();
        //create Album object
        Album newAlbum = new Album(albumPath);
        //set id
        newAlbum.setID(albumId);
        //set empty pages
        newAlbum.setPages(new ArrayList<>());
        //set date
        newAlbum.setDate(new Date());
        //update livedata
        newAlbum.updateAllLiveDataObject();
        //update list
        albumList.add(newAlbum);
        setAlbumList(albumList);
        return newAlbum;
    }

    public Album createAlbum() {
        return createAlbum(UUID.randomUUID());
    }

    public boolean deleteAlbum(Album album) {
        if (getAlbum(album.getId())!=null) {
            album.delete();
            albumList.remove(album);
            setAlbumList(albumList);
            return true;
        } else {
            return false;
        }
    }

}
