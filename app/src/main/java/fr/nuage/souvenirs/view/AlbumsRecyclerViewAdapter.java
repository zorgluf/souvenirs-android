package fr.nuage.souvenirs.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentAlbumInListBinding;
import fr.nuage.souvenirs.viewmodel.AlbumDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;


public class AlbumsRecyclerViewAdapter extends RecyclerView.Adapter<AlbumsRecyclerViewAdapter.ViewHolder> {

    private List<AlbumViewModel> mValues;
    private final OnListFragmentInteractionListener mListener;
    private boolean mEditable = false;
    private boolean mOnlyLocalAlbums = false;

    public AlbumsRecyclerViewAdapter(List<AlbumViewModel> albums, OnListFragmentInteractionListener listener) {
        this(albums,listener,false,false);
    }

    public AlbumsRecyclerViewAdapter(List<AlbumViewModel> albums, OnListFragmentInteractionListener listener, boolean editable) {
        this(albums,listener,editable,false);
    }

    public AlbumsRecyclerViewAdapter(List<AlbumViewModel> albums, OnListFragmentInteractionListener listener, boolean editable, boolean onlyLocalAlbums) {
        mValues = new ArrayList<>();
        if (albums != null) {
            updateList(albums);
        }
        mListener = listener;
        mEditable = editable;
        mOnlyLocalAlbums = onlyLocalAlbums;
    }

    public void updateList(@NonNull List<AlbumViewModel> newList) {
        ArrayList<AlbumViewModel> filteredAlbums = new ArrayList<>();
        for (AlbumViewModel albumViewModel: newList) {
            if (mOnlyLocalAlbums && !albumViewModel.hasLocalAlbum()) {
                continue;
            }
            filteredAlbums.add(albumViewModel);
        }
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AlbumDiffUtilCallback(mValues, filteredAlbums));
        diffResult.dispatchUpdatesTo(this);
        mValues = filteredAlbums;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        FragmentAlbumInListBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_album_in_list,parent,false);
        binding.setLifecycleOwner((AppCompatActivity)parent.getContext());
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        AlbumViewModel albumVM = mValues.get(position);
        //albumVM.update();
        holder.binding.setAlbum(albumVM);
        holder.binding.albumCard.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onListFragmentInteraction(albumVM,false, false);
            }
        });
        holder.binding.editInAlbumListButton.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onListFragmentInteraction(albumVM,true,false);
            }
        });
        holder.binding.delButton.setOnClickListener(v -> {
            if (null != mListener) {
                mListener.onListFragmentInteraction(albumVM,false,true);
            }
        });
        holder.binding.SharedImageView.setOnClickListener(v -> new DeleteShareDialogFragment(albumVM).show(((AppCompatActivity)v.getContext()).getSupportFragmentManager(),null));
        if (!mEditable) {
            holder.binding.albumLayout.removeView(holder.binding.editInAlbumListButton);
            holder.binding.albumLayout.removeView(holder.binding.delButton);
            holder.binding.albumLayout.removeView(holder.binding.SharedImageView);
            holder.binding.albumLayout.removeView(holder.binding.albumNextcloud);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public FragmentAlbumInListBinding binding;

        public ViewHolder(FragmentAlbumInListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(AlbumViewModel album, boolean editModeSelected, boolean delSelected);
    }


}
