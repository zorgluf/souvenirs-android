package fr.nuage.souvenirs.viewmodel;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;

public class AlbumDiffUtilCallback extends DiffUtil.Callback {

    private List<AlbumViewModel> oldAlbums;
    private List<AlbumViewModel> newAlbums;

    public AlbumDiffUtilCallback(List<AlbumViewModel> oldAlbums, List<AlbumViewModel> newAlbums) {
        this.oldAlbums = (oldAlbums == null) ? new ArrayList<AlbumViewModel>() : oldAlbums;
        this.newAlbums = (newAlbums == null) ? new ArrayList<AlbumViewModel>() : newAlbums;
    }

    @Override
    public int getOldListSize() {
        return oldAlbums.size();
    }

    @Override
    public int getNewListSize() {
        return newAlbums.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldAlbums.get(oldItemPosition).getId().equals(newAlbums.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        //both lists hold the same AlbumViewModel instances, and album views refresh themselves
        //through their livedata bindings : a change event would only make the item flash
        return true;
    }
}
