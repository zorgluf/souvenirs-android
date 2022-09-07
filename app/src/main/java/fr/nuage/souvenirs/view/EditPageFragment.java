package fr.nuage.souvenirs.view;

import static fr.nuage.souvenirs.view.helpers.Div.getNameAndSizeFromUri;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentEditPageBinding;
import fr.nuage.souvenirs.model.AudioElement;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.view.helpers.Div;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class EditPageFragment extends Fragment {

    private static final int ACTIVITY_ADD_PHOTO = 11;
    private static final int ACTIVITY_ADD_FILE = 13;

    private static final String DIALOG_CHANGE_STYLE_PAGE = "DIALOG_CHANGE_STYLE_PAGE";

    private AlbumViewModel albumVM;
    private int activityScrollStatus;
    private ElementViewModel actionModeElement = null;
    private File pendingPhotoFile;
    private int audioMode = PageViewModel.AUDIO_MODE_NONE;
    private PageEditAdapter pageEditAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load album path in args
        if (getArguments() != null) {
            String albumPath = EditPageFragmentArgs.fromBundle(getArguments()).getAlbumPath();
            String pageId = EditPageFragmentArgs.fromBundle(getArguments()).getPageId();
            //load view model
            albumVM = new ViewModelProvider(requireActivity(),new AlbumListViewModelFactory(requireActivity().getApplication())).get(AlbumListViewModel.class).getAlbum(albumPath);
            PageViewModel pageVM = albumVM.getPage(UUID.fromString(pageId));
            //set focus on that page
            albumVM.setFocusPage(pageVM);
        }

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            pendingPhotoFile = (File) savedInstanceState.getSerializable("pendingPhotoFile");
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("pendingPhotoFile", pendingPhotoFile);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //remove title
        requireActivity().setTitle("");

        //load layout
        FragmentEditPageBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_page, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        pageEditAdapter = new PageEditAdapter(albumVM,getParentFragment(),albumVM.getLdPages().getValue());
        binding.pageRecycler.setAdapter(pageEditAdapter);
        //add touch helper to move pages
        EditItemTouchHelper editItemTouchHelper = new EditItemTouchHelper(pageEditAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(editItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(binding.pageRecycler);

        //add new page creation logic
        binding.addPageEditPage.setOnClickListener((view -> {
            Page page = albumVM.createPage(albumVM.getPosition(albumVM.getFocusPage())+1);
            albumVM.setFocusPage(page.getId());
        }));
        //add selection logic on audio
        binding.audioImageView.setOnClickListener((view -> {
            albumVM.getFocusPage().getAudioElementViewModel().setSelected(true);
        }));
        binding.mainLayout.setOnClickListener(view -> {
            //if we recieve click, means no element has catch it : off page click, unselect all
            binding.getPage().unselectAll();
        });

        //listen to pages list change
        albumVM.getLdPages().observe(getViewLifecycleOwner(), pages -> {
            pageEditAdapter.setPages(pages);
        });
        //listen to page change
        albumVM.getFocusPageId().observe(getViewLifecycleOwner(), uuid -> {
            //clean old page events
            if (binding.getPage() != null) {
                PageViewModel oldPage = binding.getPage();
                oldPage.unselectAll();
                oldPage.getLdAudioMode().removeObservers(getViewLifecycleOwner());
                oldPage.getLdPaintMode().removeObservers(getViewLifecycleOwner());
                oldPage.getLdElements().removeObservers(getViewLifecycleOwner());
            }
            //set new page on UI
            PageViewModel pageVM = albumVM.getPage(uuid);
            binding.setPage(pageVM);
            binding.pageViewEdit.setPageViewModel(pageVM);
            //binding.executePendingBindings();
            if (pageVM != null) {
                //listen for audiomode change to change menu
                pageVM.getLdAudioMode().observe(getViewLifecycleOwner(), audioModeChange -> {
                    audioMode = audioModeChange;
                    requireActivity().invalidateOptionsMenu();
                });
                //listen to elements changes
                pageVM.getLdElements().observe(getViewLifecycleOwner(), elementViewModels -> {
                    //set observers
                    if (elementViewModels != null) {
                        for (ElementViewModel e : elementViewModels) {
                            if (e.getClass() == TextElementViewModel.class) {
                                TextElementViewModel et = (TextElementViewModel) e;
                                //subscribe to selection
                                e.getIsSelected().observe(getViewLifecycleOwner(),(isSelected)-> {
                                    if (isSelected) {
                                        if (!et.equals(actionModeElement)) {
                                            requireActivity().startActionMode(new TextActionModeCallback(et));
                                            actionModeElement = et;
                                        }
                                    } else {
                                        if (et.equals(actionModeElement)) {
                                            actionModeElement = null;
                                        }
                                    }
                                });
                            } else if (e instanceof ImageElementViewModel) {
                                ImageElementViewModel ei = (ImageElementViewModel) e;
                                //subscribe to selection
                                ei.getIsSelected().observe(getViewLifecycleOwner(),(isSelected)-> {
                                    if (isSelected) {
                                        if (!ei.equals(actionModeElement)) {
                                            requireActivity().startActionMode(new ImageActionModeCallback(ei));
                                            actionModeElement = ei;
                                        }
                                    } else {
                                        if (ei.equals(actionModeElement)) {
                                            actionModeElement = null;
                                        }
                                    }
                                });
                            } else if (e instanceof AudioElementViewModel) {
                                AudioElementViewModel ea = (AudioElementViewModel) e;
                                //subscribe to selection
                                ea.getIsSelected().observe(getViewLifecycleOwner(),(isSelected)-> {
                                    if (isSelected) {
                                        if (!ea.equals(actionModeElement)) {
                                            requireActivity().startActionMode(new AudioActionModeCallback(ea));
                                            actionModeElement = ea;
                                        }
                                    } else {
                                        if (ea.equals(actionModeElement)) {
                                            actionModeElement = null;
                                        }
                                        binding.audioImageView.setSelected(false);
                                    }
                                });
                            }
                        }
                    }
                });
                //listen to paint mode
                pageVM.getLdPaintMode().observe(getViewLifecycleOwner(), isPaintMode -> {
                    if (isPaintMode) {
                        //activate submenu
                        requireActivity().startActionMode(new PaintActionModeCallback(requireActivity().getSupportFragmentManager(),pageVM, pageVM.getPaintElement()));
                    }
                });
                //scroll on recyclerview
                //FIXME : dirty fix with delay : pbm when pages are first set on adapter
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    binding.pageRecycler.smoothScrollToPosition(albumVM.getPosition(uuid));
                }, 100);

            }
        });

        return binding.getRoot();

    }

    @Override
    public void onStart() {
        super.onStart();
        //dirty trick to disable toolbar scrolling for this fragment only
        this.activityScrollStatus = ((AppBarLayout.LayoutParams) requireActivity().findViewById(R.id.toolbar).getLayoutParams()).getScrollFlags();
        ((AppBarLayout.LayoutParams) requireActivity().findViewById(R.id.toolbar).getLayoutParams()).setScrollFlags(0);
    }

    @Override
    public void onStop() {
        //restore activity scrolling
        ((AppBarLayout.LayoutParams) requireActivity().findViewById(R.id.toolbar).getLayoutParams()).setScrollFlags(this.activityScrollStatus);
        super.onStop();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_page, menu);

        //set logic to add file (audio, image, video)
        MenuItem addFileItem = menu.findItem(R.id.edit_page_add_file);
        addFileItem.setOnMenuItemClickListener(menuItem -> {
            //test alternative
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            String[] mimetypes = {"image/*", "video/*", "audio/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, ACTIVITY_ADD_FILE);
            return true;
        });

        //set logic to add photo
        MenuItem addPhotoItem = menu.findItem(R.id.edit_page_add_photo);
        addPhotoItem.setOnMenuItemClickListener(menuItem -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                pendingPhotoFile = albumVM.createEmptyDataFile("image/jpeg");
                // Continue only if the File was successfully created
                if (pendingPhotoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName()+".provider",
                            pendingPhotoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, ACTIVITY_ADD_PHOTO);
                }
            } else {
                Toast.makeText(getContext(),R.string.toast_no_camera,Toast.LENGTH_LONG).show();
            }

            return true;
        });

        //set logic to add audio silence
        MenuItem addAudioStopItem = menu.findItem(R.id.edit_page_audio_stop);
        addAudioStopItem.setOnMenuItemClickListener(menuItem -> {
            albumVM.getFocusPage().addAudio(null, null);
            return true;
        });
        if (audioMode == PageViewModel.AUDIO_MODE_NONE) {
            //disable remove audio
            addAudioStopItem.getIcon().setAlpha(255);
        } else {
            //disable audio add
            addAudioStopItem.setEnabled(false);
            addAudioStopItem.getIcon().setAlpha(128);
        }

        //set logic to add text
        MenuItem addTextItem = menu.findItem(R.id.edit_page_add_text);
        addTextItem.setOnMenuItemClickListener(menuItem -> {
            albumVM.getFocusPage().addText();
            return true;
        });

        //set logic draw mode
        MenuItem drawItem = menu.findItem(R.id.edit_page_draw);
        drawItem.setOnMenuItemClickListener(menuItem -> {
            //start paint mode
            albumVM.getFocusPage().startPaintMode();
            return true;
        });

        //set logic to change style
        MenuItem changeStyle = menu.findItem(R.id.edit_page_change_style);
        changeStyle.setOnMenuItemClickListener(menuItem -> {
            SelectPageStyleFragment.OnSelectPageStyleListener selectPageStyleListener = style -> {
                TilePageBuilder pageBuilder = new TilePageBuilder();
                pageBuilder.applyStyle(style,albumVM.getFocusPage().getPage());
            };
            //launch select style dialog
            SelectPageStyleDialogFragment dialog = SelectPageStyleDialogFragment.newInstance(selectPageStyleListener,
                    albumVM.getFocusPage().getNbImage(),albumVM.getFocusPage().getNbText());
            dialog.show(getParentFragmentManager(),DIALOG_CHANGE_STYLE_PAGE);
            return true;
        });

        //set logic to delete page
        MenuItem deletePage = menu.findItem(R.id.edit_page_delete);
        deletePage.setOnMenuItemClickListener(menuItem -> {
            int focusPagePos = albumVM.getPosition(albumVM.getFocusPage().getId());
            albumVM.getFocusPage().delete();
            if (albumVM.getSize() == 1) {
                albumVM.setFocusPage((PageViewModel) null);
            } else {
                if (focusPagePos == 0) {
                    albumVM.setFocusPage(albumVM.getPage(1));
                } else {
                    albumVM.setFocusPage(albumVM.getPage(focusPagePos-1));
                }
            }
            return true;
        });

        //make overflow menu icons visible
        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_ADD_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Uri> uris = new ArrayList<>();
                    ClipData clipdata = data.getClipData();
                    if (clipdata == null) {
                        uris.add(data.getData());
                    } else {
                        //handle multiple uri
                        for (int i = 0; i < clipdata.getItemCount(); i++) {
                            uris.add(clipdata.getItemAt(i).getUri());
                        }
                    }
                    for (Uri uri: uris) {
                        InputStream input = TilePageBuilder.getInputStreamFromUri(requireActivity().getContentResolver(), uri);
                        String mime = requireActivity().getContentResolver().getType(uri);
                        //extract name and size
                        Div.NameSize nameSize = getNameAndSizeFromUri(uri,getActivity().getContentResolver());
                        if (mime.startsWith("image")) {
                            albumVM.getFocusPage().addImage(input,mime,nameSize.name,nameSize.size);
                        } else if (mime.startsWith("video")) {
                            albumVM.getFocusPage().addVideo(input,mime,nameSize.name,nameSize.size);
                        } else if (mime.startsWith("audio")) {
                            albumVM.getFocusPage().addAudio(input,mime);
                        }
                    }
                }
                break;
            case ACTIVITY_ADD_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    albumVM.getFocusPage().addImage(pendingPhotoFile);
                }
        }


    }

}
