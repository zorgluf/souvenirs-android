package fr.nuage.souvenirs.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.EditItemPageListBinding;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.PaintElementViewBinding;
import fr.nuage.souvenirs.databinding.TextElementViewBinding;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class EditPageListAdapter extends RecyclerView.Adapter<EditPageListAdapter.ViewHolder> implements EditItemTouchHelper.ItemTouchHelperAdapter {
    private AlbumViewModel albumViewModel;
    private ArrayList<PageViewModel> mPages = new ArrayList<PageViewModel>();
    private EditAlbumFragment mFragment;
    private RecyclerView mRecyclerView;
    private ActionMode mImageActionMode;

    public void setAlbum(AlbumViewModel albumVM) {
        this.albumViewModel = albumVM;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private EditItemPageListBinding mBinding;
        public ViewHolder(EditItemPageListBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
        public void bind(PageViewModel page, EditAlbumFragment fragment) {
            mBinding.setPage(page);
            mBinding.setFragment(fragment);
            mBinding.executePendingBindings();
        }
    }

    public EditPageListAdapter(EditAlbumFragment fragment, AlbumViewModel albumViewModel) {
        mFragment = fragment;
        this.albumViewModel = albumViewModel;
    }

    public void updateList(ArrayList<PageViewModel> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PageDiffUtilCallback(mPages, newList));
        mPages = newList;
        diffResult.dispatchUpdatesTo(this);
    }


    @NonNull
    @Override
    public EditPageListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        EditItemPageListBinding binding = DataBindingUtil.inflate(inflater, R.layout.edit_item_page_list,parent,false);
        binding.setLifecycleOwner(mFragment.getViewLifecycleOwner());
        return new EditPageListAdapter.ViewHolder(binding);
    }


    @Override
    public void onBindViewHolder(@NonNull EditPageListAdapter.ViewHolder holder, int position) {
        PageViewModel page = mPages.get(position);
        holder.bind(page, this.mFragment);
        //listen to elements changes
        page.getLdElements().observe(mFragment.getViewLifecycleOwner(), elementViewModels -> {
            //remove all view
            ConstraintLayout layout = holder.itemView.findViewById(R.id.page_layout);
            layout.removeAllViewsInLayout();
            //rebuild layout
            if (elementViewModels != null) {
                LayoutInflater inflater = LayoutInflater.from(layout.getContext());
                LifecycleOwner lifecycleOwner = mFragment.getViewLifecycleOwner();
                for (final ElementViewModel e : elementViewModels) {
                    if (e.getClass() == TextElementViewModel.class) {
                        //load xml layout and bind data
                        TextElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.text_element_view,layout,false);
                        binding.setLifecycleOwner(lifecycleOwner);
                        binding.setElement((TextElementViewModel) e);
                        binding.setAlbum(albumViewModel);
                        binding.executePendingBindings();
                        layout.addView(binding.getRoot());
                    } else if (e.getClass() == PaintElementViewModel.class) {
                        PaintElementViewModel ep = (PaintElementViewModel)e;
                        //load xml layout and bind data
                        PaintElementViewBinding elBinding = DataBindingUtil.inflate(inflater, R.layout.paint_element_view, layout,false);
                        elBinding.setLifecycleOwner(lifecycleOwner);
                        elBinding.setElement(ep);
                        elBinding.executePendingBindings();
                        layout.addView(elBinding.getRoot());
                    } else if (e instanceof ImageElementViewModel) {
                        ImageElementViewModel ei = (ImageElementViewModel)e;
                        //load xml layout and bind data
                        ImageElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.image_element_view, layout,false);
                        binding.setLifecycleOwner(lifecycleOwner);
                        binding.setElement(ei);
                        binding.setAlbum(albumViewModel);
                        layout.addView(binding.getRoot());
                        binding.executePendingBindings();
                    } else if (e.getClass() == AudioElementViewModel.class) {
                        continue;
                    } else {
                            //unknown element : display default view
                            inflater.inflate(R.layout.unknown_element_view,layout,true);
                            ImageView unknownImage = layout.findViewById(R.id.unknown_imageview);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPages.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onItemMove(int from, int to) {
        mFragment.getAlbumVM().movePage(from,to);
    }

}
