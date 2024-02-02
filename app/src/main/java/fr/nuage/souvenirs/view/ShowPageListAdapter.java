package fr.nuage.souvenirs.view;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
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

import fr.nuage.souvenirs.PanoViewerActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.ImagePanoElementViewBinding;
import fr.nuage.souvenirs.databinding.ShowItemPageListBinding;
import fr.nuage.souvenirs.databinding.TextElementViewShowBinding;
import fr.nuage.souvenirs.databinding.VideoElementViewBinding;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;
import fr.nuage.souvenirs.viewmodel.VideoElementViewModel;

public class ShowPageListAdapter extends RecyclerView.Adapter<ShowPageListAdapter.ViewHolder> {
    private ArrayList<PageViewModel> mPages = new ArrayList<>();
    private Fragment mFragment;
    private AlbumViewModel albumViewModel;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ShowItemPageListBinding mBinding;
        public ViewHolder(ShowItemPageListBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }
        public void bind(AlbumViewModel album) {
            mBinding.setAlbum(album);
            mBinding.executePendingBindings();
        }
    }

    public ShowPageListAdapter(LiveData<ArrayList<PageViewModel>> myDataset, Fragment fragment, AlbumViewModel albumViewModel) {
        if (myDataset.getValue() != null) {
            mPages = myDataset.getValue();
        }
        mFragment = fragment;
        this.albumViewModel = albumViewModel;
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
        holder.bind(albumViewModel);

        PageViewModel page = mPages.get(position);
        //listen to elements changes
        page.getLdElements().observe(mFragment, new Observer<ArrayList<ElementViewModel>>() {
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
                        } else if (e instanceof VideoElementViewModel) {
                            //load xml layout and bind data
                            VideoElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.video_element_view,layout,false);
                            binding.setLifecycleOwner(mFragment);
                            binding.setElement((VideoElementViewModel) e);
                            binding.executePendingBindings();
                            ((VideoElementViewModel) e).getVideoPath().observe(mFragment, binding.imageVideoview::setVideoPath);
                            ((VideoElementViewModel) e).getIsPlaying().observe(mFragment, isPlaying -> {
                                if (isPlaying) {
                                    if (binding.imageVideoview.isPlaying()) {
                                        binding.imageVideoview.resume();
                                    } else {
                                        binding.imageVideoview.start();
                                    }
                                } else {
                                    binding.imageVideoview.pause();
                                }
                            });
                            //set on click event
                            binding.imageVideoview.setOnClickListener(view -> {
                                //open view intent on video
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Uri imUri = FileProvider.getUriForFile(mFragment.getContext(), mFragment.getContext().getPackageName() + ".provider", new File(((VideoElementViewModel) e).getVideoPath().getValue()));
                                intent.setDataAndType(imUri, "video/*");
                                mFragment.getActivity().startActivity(intent);
                            });
                            //scale to fill/crop canvas
                            ((VideoElementViewModel) e).getZoom().observe(mFragment.getViewLifecycleOwner(), integer -> {
                                if (integer == 100) {
                                    binding.imageVideoview.setOnPreparedListener(mp -> {
                                        float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                                        float screenRatio = binding.imageVideoview.getWidth() / (float)
                                                binding.imageVideoview.getHeight();
                                        float scaleX = videoRatio / screenRatio;
                                        if (scaleX >= 1f) {
                                            binding.imageVideoview.setScaleX(scaleX);
                                            binding.imageVideoview.setScaleY(scaleX);
                                        } else {
                                            binding.imageVideoview.setScaleX(1f / scaleX);
                                            binding.imageVideoview.setScaleY(1f / scaleX);
                                        }
                                    });
                                } else {
                                    binding.imageVideoview.setOnPreparedListener(null);
                                }
                            });
                            layout.addView(binding.getRoot());
                        } else if (e instanceof ImageElementViewModel) {
                            //if pano, get special layout
                            if (((ImageElementViewModel) e).getIsPano()) {
                                //load xml layout and bind data
                                ImagePanoElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.image_pano_element_view,layout,false);
                                binding.setLifecycleOwner(mFragment);
                                binding.setElement((ImageElementViewModel) e);
                                binding.executePendingBindings();
                                ((ImageElementViewModel)e).getImagePath().observe(binding.getLifecycleOwner(), path -> {
                                    binding.imagePanoview.setPanoUri(Uri.fromFile(new File(path)));
                                });
                                //do not listen to click if paintelement
                                if (e.getClass() != PaintElementViewModel.class) {
                                    binding.imagePanoview.setOnClickListener(view -> {
                                        //open PanoViewerActivity
                                        Intent intent = new Intent();
                                        intent.setClass(mFragment.getContext().getApplicationContext(), PanoViewerActivity.class);
                                        intent.setAction(Intent.ACTION_SEND);
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        Uri imUri = FileProvider.getUriForFile(mFragment.getContext(), mFragment.getContext().getPackageName() + ".provider", new File(((ImageElementViewModel) e).getImagePath().getValue()));
                                        intent.setType(ImageElement.GOOGLE_PANORAMA_360_MIMETYPE);
                                        intent.putExtra(Intent.EXTRA_STREAM,imUri);
                                        mFragment.getActivity().startActivity(intent);
                                    });
                                }
                                layout.addView(binding.getRoot());
                            } else {
                                //load xml layout and bind data
                                ImageElementViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.image_element_view, layout, false);
                                binding.setLifecycleOwner(mFragment);
                                binding.setElement((ImageElementViewModel) e);
                                binding.executePendingBindings();
                                //do not listen to click if paintelement
                                if (e.getClass() != PaintElementViewModel.class) {
                                    binding.imageImageview.setOnClickListener(view -> {
                                        //open view intent on image
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_VIEW);
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        Uri imUri = FileProvider.getUriForFile(mFragment.getContext(), mFragment.getContext().getPackageName() + ".provider", new File(((ImageElementViewModel) e).getImagePath().getValue()));
                                        intent.setDataAndType(imUri, "image/*");
                                        mFragment.getActivity().startActivity(intent);
                                    });
                                }
                                layout.addView(binding.getRoot());
                            }
                        } else if (e.getClass() == AudioElementViewModel.class) {
                            continue;
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
