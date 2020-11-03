package fr.nuage.souvenirs.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.EditItemPageListBinding;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.TextElementViewShowBinding;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class EditPageListAdapter extends RecyclerView.Adapter<EditPageListAdapter.ViewHolder> implements EditItemTouchHelper.ItemTouchHelperAdapter {
    private ArrayList<PageViewModel> mPages = new ArrayList<PageViewModel>();
    private EditAlbumFragment mFragment;
    private RecyclerView mRecyclerView;
    private ActionMode mImageActionMode;


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

    public EditPageListAdapter(EditAlbumFragment fragment) {
        mFragment = fragment;
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
        binding.setLifecycleOwner(mFragment);
        return new EditPageListAdapter.ViewHolder(binding);
    }


    @Override
    public void onBindViewHolder(@NonNull EditPageListAdapter.ViewHolder holder, int position) {
        PageViewModel page = mPages.get(position);
        holder.bind(page,(EditAlbumFragment) this.mFragment);
        //listen to elements changes
        page.getElements().observe(mFragment, new Observer<ArrayList<ElementViewModel>>() {
            @Override
            public void onChanged(@Nullable ArrayList<ElementViewModel> elementViewModels) {
                //remove all view
                FrameLayout layout = holder.itemView.findViewById(R.id.page_layout);
                layout.removeAllViewsInLayout();
                //rebuild layout
                if (elementViewModels != null) {
                    LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
                    for (final ElementViewModel e : elementViewModels) {
                        if (e.getClass() == TextElementViewModel.class) {
                            //load xml layout and bind data
                            TextElementViewShowBinding binding = DataBindingUtil.inflate(inflater, R.layout.text_element_view_show,layout,false);
                            layout.addView(binding.getRoot());
                            binding.setLifecycleOwner(mFragment);
                            binding.setElement((TextElementViewModel) e);
                            binding.executePendingBindings();
                        } else if (e.getClass() == ImageElementViewModel.class || e.getClass() == PaintElementViewModel.class) {
                            ImageElementViewModel ei = (ImageElementViewModel)e;
                            //load xml layout and bind data
                            ImageElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.image_element_view,layout,false);
                            layout.addView(binding.getRoot());
                            binding.setLifecycleOwner(mFragment);
                            binding.setElement(ei);
                            binding.executePendingBindings();
                        } else {
                            //unknown element : display default view
                            inflater.inflate(R.layout.unknown_element_view,layout,true);
                            ImageView unknownImage = layout.findViewById(R.id.unknown_imageview);
                        }
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
