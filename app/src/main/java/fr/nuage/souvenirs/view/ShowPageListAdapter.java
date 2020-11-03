package fr.nuage.souvenirs.view;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.ShowItemPageListBinding;
import fr.nuage.souvenirs.databinding.TextElementViewShowBinding;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class ShowPageListAdapter extends RecyclerView.Adapter<ShowPageListAdapter.ViewHolder> {
    private ArrayList<PageViewModel> mPages = new ArrayList<PageViewModel>();
    private Fragment mFragment;



    public class ViewHolder extends RecyclerView.ViewHolder {
        private ShowItemPageListBinding mBinding;
        public ViewHolder(ShowItemPageListBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
        public void bind(PageViewModel page) {
            mBinding.setPage(page);
            mBinding.executePendingBindings();
        }
    }

    public ShowPageListAdapter(LiveData<ArrayList<PageViewModel>> myDataset, Fragment fragment) {
        if (myDataset.getValue() != null) {
            mPages = myDataset.getValue();
        }
        mFragment = fragment;
    }

    public void updateList(ArrayList<PageViewModel> newList) {
        if (newList != null) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PageDiffUtilCallback(mPages, newList));
            mPages = newList;
            diffResult.dispatchUpdatesTo(this);
        }

    }


    @NonNull
    @Override
    public ShowPageListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ShowItemPageListBinding binding = DataBindingUtil.inflate(inflater, R.layout.show_item_page_list,parent,false);
        binding.setLifecycleOwner(mFragment);
        return new ShowPageListAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowPageListAdapter.ViewHolder holder, int position) {
        PageViewModel page = mPages.get(position);
        //listen to elements changes
        page.getElements().observe(mFragment, new Observer<ArrayList<ElementViewModel>>() {
            @Override
            public void onChanged(@Nullable ArrayList<ElementViewModel> elementViewModels) {
                //remove all view
                ViewGroup layout = holder.itemView.findViewById(R.id.page_layout);
                layout.removeAllViewsInLayout();
                //rebuild layout
                if (elementViewModels != null) {
                    LayoutInflater inflater = LayoutInflater.from(layout.getContext());
                    for (final ElementViewModel e : elementViewModels) {
                        if (e.getClass() == TextElementViewModel.class) {
                            //load xml layout and bind data
                            TextElementViewShowBinding binding = DataBindingUtil.inflate(inflater, R.layout.text_element_view_show,layout,false);
                            binding.setLifecycleOwner(mFragment);
                            binding.setElement((TextElementViewModel) e);
                            binding.executePendingBindings();
                            layout.addView(binding.getRoot());
                        } else if (e.getClass() == ImageElementViewModel.class || e.getClass() == PaintElementViewModel.class) {
                            //load xml layout and bind data
                            ImageElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.image_element_view,layout,false);
                            binding.setLifecycleOwner(mFragment);
                            binding.setElement((ImageElementViewModel) e);
                            binding.executePendingBindings();
                            //do not listen to click if paintelement
                            if (e.getClass() != PaintElementViewModel.class) {
                                binding.imageImageview.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        //open view intent
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_VIEW);
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        Uri imUri = FileProvider.getUriForFile(mFragment.getContext(), mFragment.getContext().getPackageName() + ".provider", new File(((ImageElementViewModel) e).getImagePath().getValue()));
                                        intent.setDataAndType(imUri, "image/*");
                                        mFragment.getActivity().startActivity(intent);
                                    }
                                });
                            }
                            layout.addView(binding.getRoot());
                        } else {
                            //unknown element : display default view
                            inflater.inflate(R.layout.unknown_element_view,layout,true);
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

}
