package fr.nuage.souvenirs.view;

import static fr.nuage.souvenirs.view.helpers.ElementMoveDragListener.SWITCH_DRAG;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.Scene;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.appbar.AppBarLayout;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentEditPageBinding;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class EditPageFragment extends Fragment {

    private static final int ACTIVITY_ADD_IMAGE = 10;
    private static final int ACTIVITY_ADD_PHOTO = 11;
    private static final int ACTIVITY_ADD_AUDIO = 12;

    private static final String DIALOG_CHANGE_STYLE_PAGE = "DIALOG_CHANGE_STYLE_PAGE";

    private PageViewModel pageVM;
    private AlbumViewModel albumVM;
    private int activityScrollStatus;
    private ElementViewModel actionModeElement = null;
    private File pendingPhotoFile;
    private final MutableLiveData<Integer> switchPageDelta = new MutableLiveData<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switchPageDelta.postValue(0);
        //load album path in args
        if (getArguments() != null) {
            String albumPath = EditPageFragmentArgs.fromBundle(getArguments()).getAlbumPath();
            String pageId = EditPageFragmentArgs.fromBundle(getArguments()).getPageId();
            //load view model
            albumVM = new ViewModelProvider(requireActivity(),new AlbumListViewModelFactory(requireActivity().getApplication())).get(AlbumListViewModel.class).getAlbum(albumPath);
            pageVM = albumVM.getPage(UUID.fromString(pageId));
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

        //set title
        requireActivity().setTitle(R.string.edit_page_title);
        //listen to page switch
        switchPageDelta.observe(getViewLifecycleOwner(), integer -> {
            if (integer == 1) {
                moveToNext();
            }
            if (integer == -1) {
                moveToPrev();
            }
        });
        //listen to pages changes
        albumVM.getLdPages().observe(getViewLifecycleOwner(), pageViewModels -> {
            //build new view
            ((ViewGroup) requireView()).removeAllViews();
            ((ViewGroup) requireView()).addView(createView(getLayoutInflater(), (ViewGroup) getView()));
        });

        return new LinearLayout(Objects.requireNonNull(container).getContext());

    }

    private View createView(@NonNull LayoutInflater inflater, ViewGroup container) {
        //inflateview
        FragmentEditPageBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_page, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setPage(pageVM);
        binding.executePendingBindings();

        pageVM.getLdEditMode().postValue(true);
        binding.pageViewEdit.setPageViewModel(pageVM);

        //listen for audiomode change to change menu
        pageVM.getLdAudioMode().observe(getViewLifecycleOwner(), audioMode -> {
            getActivity().invalidateOptionsMenu();
        });

        binding.mainLayout.setOnClickListener(view -> {
            //if we recieve click, means no element has catch it : off page click, unselect all
            if (pageVM.getLdElements().getValue() != null) {
                for (ElementViewModel e : pageVM.getLdElements().getValue()) {
                    e.setSelected(false);
                }
            }
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

        //set prev page
        PageViewModel prevPage = albumVM.getPrevPage(pageVM);
        if (prevPage == null) {
            binding.pageViewPrev.setVisibility(View.GONE);
        } else {
            binding.pageViewPrev.setVisibility(View.VISIBLE);
            prevPage.getLdEditMode().postValue(false);
            binding.pageViewPrev.setPageViewModel(prevPage);
            binding.pageViewPrev.setOnClickListener(view -> switchPageDelta.postValue(-1));
            //set drag event if an element is moved to this page
            binding.pageViewPrev.setOnDragListener((v, event) -> {
                String dragType = (String)event.getLocalState();
                if (dragType.equals(SWITCH_DRAG)) {
                    //handle switch elements drag action
                    int action = event.getAction();
                    switch(action) {
                        case DragEvent.ACTION_DRAG_STARTED:
                        case DragEvent.ACTION_DRAG_EXITED:
                            v.setAlpha((float)0.5);
                            return true;
                        case DragEvent.ACTION_DRAG_ENTERED:
                        case DragEvent.ACTION_DRAG_ENDED:
                            v.setAlpha(1);
                            return true;
                        case DragEvent.ACTION_DRAG_LOCATION:
                            return true;
                        case DragEvent.ACTION_DROP:
                            // Gets the page id to move
                            ClipData.Item item = event.getClipData().getItemAt(0);
                            UUID oriElementUUID = UUID.fromString((String)item.getText());
                            ElementViewModel oriElementViewModel = pageVM.getElement(oriElementUUID);
                            if (oriElementViewModel != null) {
                                oriElementViewModel.moveToPreviousPage();
                            }
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            });
        }

        //set next page
        PageViewModel nextPage = albumVM.getNextPage(pageVM);
        binding.pageViewNext.setPageViewModel(nextPage);
        if (nextPage == null) {
            binding.pageViewNext.setOnClickListener(view -> {
                albumVM.createPage(albumVM.getPosition(pageVM)+1);
                switchPageDelta.postValue(1);
            });
        } else {
            nextPage.getLdEditMode().postValue(false);
            binding.pageViewNext.setOnClickListener(view -> switchPageDelta.postValue(1));
            //set drag event if an element is moved to this page
            binding.pageViewNext.setOnDragListener((v, event) -> {
                String dragType = (String)event.getLocalState();
                if (dragType.equals(SWITCH_DRAG)) {
                    //handle switch elements drag action
                    int action = event.getAction();
                    switch(action) {
                        case DragEvent.ACTION_DRAG_STARTED:
                        case DragEvent.ACTION_DRAG_EXITED:
                            v.setAlpha((float)0.5);
                            return true;
                        case DragEvent.ACTION_DRAG_ENTERED:
                        case DragEvent.ACTION_DRAG_ENDED:
                            v.setAlpha(1);
                            return true;
                        case DragEvent.ACTION_DRAG_LOCATION:
                            return true;
                        case DragEvent.ACTION_DROP:
                            // Gets the page id to move
                            ClipData.Item item = event.getClipData().getItemAt(0);
                            UUID oriElementUUID = UUID.fromString((String)item.getText());
                            ElementViewModel oriElementViewModel = pageVM.getElement(oriElementUUID);
                            if (oriElementViewModel != null) {
                                oriElementViewModel.moveToNextPage();
                            }
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            });
        }

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_page, menu);

        //set logic to add image or video
        MenuItem addImageItem = menu.findItem(R.id.edit_page_add_image);
        addImageItem.setOnMenuItemClickListener(menuItem -> {
            //test alternative
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            String[] mimetypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, ACTIVITY_ADD_IMAGE);
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

        if (pageVM.getLdAudioMode().getValue() == PageViewModel.AUDIO_MODE_NONE) {
            //disable remove audio
            menu.removeItem(R.id.edit_page_audio_remove);
            //set logic to add audio file
            MenuItem addAudioItem = menu.findItem(R.id.edit_page_audio);
            addAudioItem.setOnMenuItemClickListener(menuItem -> {
                //test alternative
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                startActivityForResult(intent, ACTIVITY_ADD_AUDIO);
                return true;
            });
            //set logic to add audio silence
            MenuItem addAudioStopItem = menu.findItem(R.id.edit_page_audio_stop);
            addAudioStopItem.setOnMenuItemClickListener(menuItem -> {
                pageVM.addAudio(null,null);
                return true;
            });
        } else {
            //disable audio add
            menu.removeItem(R.id.edit_page_audio);
            menu.removeItem(R.id.edit_page_audio_stop);
            //set logic to remove audio
            MenuItem addAudioRemoveItem = menu.findItem(R.id.edit_page_audio_remove);
            addAudioRemoveItem.setOnMenuItemClickListener(menuItem -> {
                pageVM.removeAudio();
                return true;
            });
        }

        //set logic to add text
        MenuItem addTextItem = menu.findItem(R.id.edit_page_add_text);
        addTextItem.setOnMenuItemClickListener(menuItem -> {
            pageVM.addText();
            return true;
        });

        //set logic draw mode
        MenuItem drawItem = menu.findItem(R.id.edit_page_draw);
        drawItem.setOnMenuItemClickListener(menuItem -> {
            //start paint mode
            pageVM.startPaintMode();
            return true;
        });

        //set logic to change style
        MenuItem changeStyle = menu.findItem(R.id.edit_page_change_style);
        changeStyle.setOnMenuItemClickListener(menuItem -> {
            SelectPageStyleFragment.OnSelectPageStyleListener selectPageStyleListener = style -> {
                PageBuilder pageBuilder = (albumVM.getDefaultStyle().equals(Album.STYLE_TILE)) ? new TilePageBuilder() : new PageBuilder();
                pageBuilder.applyStyle(style,pageVM.getPage());
            };
            //launch select style dialog
            SelectPageStyleDialogFragment dialog = SelectPageStyleDialogFragment.newInstance(selectPageStyleListener,pageVM.getNbImage(),pageVM.getNbText(),albumVM.getDefaultStyle());
            dialog.show(getParentFragmentManager(),DIALOG_CHANGE_STYLE_PAGE);
            return true;
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_ADD_IMAGE:
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
                        InputStream input = PageBuilder.getInputStreamFromUri(requireActivity().getContentResolver(), uri);
                        String mime = requireActivity().getContentResolver().getType(uri);
                        //extract name and size
                        String displayName = null;
                        int size = 0;
                        Cursor cursor = getActivity().getContentResolver()
                                .query(uri, null, null, null, null, null);
                        try {
                            if (cursor != null && cursor.moveToFirst()) {
                                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                displayName = cursor.getString(nameIndex);
                                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                                size = 0;
                                if (!cursor.isNull(sizeIndex)) {
                                    size = cursor.getInt(sizeIndex);
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                        if (mime.startsWith("image")) {
                            pageVM.addImage(input,mime,displayName,size);
                        } else {
                            pageVM.addVideo(input,mime,displayName,size);
                        }
                    }
                }
                break;
            case ACTIVITY_ADD_AUDIO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri audioUri = data.getData();
                    String mime = requireActivity().getContentResolver().getType(audioUri);
                    InputStream input = PageBuilder.getInputStreamFromUri(requireActivity().getContentResolver(), audioUri);
                    pageVM.addAudio(input,mime);
                }
                break;
            case ACTIVITY_ADD_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    pageVM.addImage(pendingPhotoFile);
                }
        }


    }

    private void moveToNext() {
        PageViewModel nextPage = albumVM.getNextPage(pageVM);
        PageViewModel prevPage = albumVM.getPrevPage(pageVM);
        if (nextPage != null) {
            //build transition
            TransitionSet transition = new TransitionSet();
            Transition tMove = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move);
            tMove.addTarget(pageVM.getId().toString());
            tMove.addTarget(nextPage.getId().toString());
            transition.addTransition(tMove);
            if (prevPage != null) {
                Transition fade = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.fade);
                fade.addTarget(prevPage.getId().toString());
                transition.addTransition(fade);
            }
            //change main page
            pageVM = nextPage;
            //build new view
            View nextView = createView(getLayoutInflater(), (ViewGroup) getView());
            //change view
            Scene destScene = new Scene((ViewGroup) requireView(),nextView);
            TransitionManager.go(destScene,transition);
        }
    }

    private void moveToPrev() {
        PageViewModel prevPage = albumVM.getPrevPage(pageVM);
        PageViewModel nextPage = albumVM.getNextPage(pageVM);
        if (prevPage != null) {
            //build transition
            TransitionSet transition = new TransitionSet();
            Transition tMove = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move);
            tMove.addTarget(pageVM.getId().toString());
            tMove.addTarget(prevPage.getId().toString());
            transition.addTransition(tMove);
            if (nextPage != null) {
                Transition fade = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.fade);
                fade.addTarget(nextPage.getId().toString());
                transition.addTransition(fade);
            }
            //change main page
            pageVM = prevPage;
            //build new view
            View nextView = createView(getLayoutInflater(), (ViewGroup) getView());
            //change view
            Scene destScene = new Scene((ViewGroup) requireView(),nextView);
            TransitionManager.go(destScene,transition);
        }

    }
}
