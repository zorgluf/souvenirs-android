package fr.nuage.souvenirs.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.transition.Scene;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.google.android.material.appbar.AppBarLayout;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentEditPageBinding;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.view.helpers.ViewGenerator;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

import static fr.nuage.souvenirs.view.helpers.ElementMoveDragListener.SWITCH_DRAG;

public class EditPageFragment extends Fragment implements PageView.OnSwingListener {

    private static final int ACTIVITY_ADD_IMAGE = 10;
    private static final int ACTIVITY_ADD_PHOTO = 11;

    private static final String DIALOG_CHANGE_STYLE_PAGE = "DIALOG_CHANGE_STYLE_PAGE";

    private PageViewModel pageVM;
    private AlbumViewModel albumVM;
    private int activityScrollStatus;
    private ElementViewModel actionModeElement = null;
    private File pendingPhotoFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load album path in args
        if (getArguments() != null) {
            String albumPath = EditPageFragmentArgs.fromBundle(getArguments()).getAlbumPath();
            String pageId = EditPageFragmentArgs.fromBundle(getArguments()).getPageId();
            //load view model
            albumVM = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class).getAlbum(albumPath);
            pageVM = albumVM.getPage(UUID.fromString(pageId));
            //set focus on that page
            albumVM.setFocusPage(pageVM);
        }

        setHasOptionsMenu(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //set title
        getActivity().setTitle(R.string.edit_page_title);

        return createView(inflater,container);

    }

    private View createView(@NonNull LayoutInflater inflater, ViewGroup container) {
        //inflateview
        FragmentEditPageBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_page, container, false);

        pageVM.getLdEditMode().postValue(true);
        binding.pageViewEdit.setPageViewModel(pageVM);

        binding.mainLayout.setOnClickListener(view -> {
            //if we recieve click, means no element has catch it : off page click, unselect all
            if (pageVM.getElements().getValue() != null) {
                for (ElementViewModel e : pageVM.getElements().getValue()) {
                    e.setSelected(false);
                }
            }
        });

        //listen to elements changes
        pageVM.getElements().observe(getViewLifecycleOwner(), elementViewModels -> {
            //set observers
            if (elementViewModels != null) {
                for (ElementViewModel e : elementViewModels) {
                    if (e.getClass() == TextElementViewModel.class) {
                        TextElementViewModel et = (TextElementViewModel) e;
                        //subscribe to selection
                        e.getIsSelected().observe(getViewLifecycleOwner(),(isSelected)-> {
                            if (isSelected) {
                                if (!et.equals(actionModeElement)) {
                                    getActivity().startActionMode(new TextActionModeCallback(et));
                                    actionModeElement = et;
                                }
                            } else {
                                if (et.equals(actionModeElement)) {
                                    actionModeElement = null;
                                }
                            }
                        });
                    } else if (e.getClass() == ImageElementViewModel.class) {
                        ImageElementViewModel ei = (ImageElementViewModel) e;
                        //subscribe to selection
                        ei.getIsSelected().observe(getViewLifecycleOwner(),(isSelected)-> {
                            if (isSelected) {
                                if (!ei.equals(actionModeElement)) {
                                    getActivity().startActionMode(new ImageActionModeCallback(ei));
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

        //listen swing on pageview
        binding.pageViewEdit.setOnSwingListener(this);

        //set prev page
        PageViewModel prevPage = albumVM.getPrevPage(pageVM);
        if (prevPage == null) {
            binding.pageViewPrev.setVisibility(View.GONE);
        } else {
            binding.pageViewPrev.setVisibility(View.VISIBLE);
            prevPage.getLdEditMode().postValue(false);
            binding.pageViewPrev.setPageViewModel(prevPage);
            binding.pageViewPrev.setOnClickListener(view -> {
                moveToPrev();
            });
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
        if (nextPage == null) {
            binding.pageViewNext.setVisibility(View.GONE);
        } else {
            binding.pageViewNext.setVisibility(View.VISIBLE);
            nextPage.getLdEditMode().postValue(false);
            binding.pageViewNext.setPageViewModel(nextPage);
            binding.pageViewNext.setOnClickListener(view -> {
                moveToNext();
            });
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
        this.activityScrollStatus = ((AppBarLayout.LayoutParams) getActivity().findViewById(R.id.toolbar).getLayoutParams()).getScrollFlags();
        ((AppBarLayout.LayoutParams) getActivity().findViewById(R.id.toolbar).getLayoutParams()).setScrollFlags(0);
    }

    @Override
    public void onStop() {
        //restore activity scrolling
        ((AppBarLayout.LayoutParams) getActivity().findViewById(R.id.toolbar).getLayoutParams()).setScrollFlags(this.activityScrollStatus);
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_page, menu);

        //set logic to add image
        MenuItem addImageItem = menu.findItem(R.id.edit_page_add_image);
        addImageItem.setOnMenuItemClickListener(menuItem -> {
            //open image picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
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
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                pendingPhotoFile = albumVM.createEmptyDataFile("image/jpeg");
                // Continue only if the File was successfully created
                if (pendingPhotoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(getContext(),
                            getContext().getPackageName()+".provider",
                            pendingPhotoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, ACTIVITY_ADD_PHOTO);
                }
            }

            return true;
        });

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
                    ArrayList<Uri> uris = new ArrayList<Uri>();
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
                        InputStream input = PageBuilder.getInputStreamFromUri(getActivity().getContentResolver(), uri);
                        String mime = getActivity().getContentResolver().getType(uri);
                        pageVM.addImage(input,mime);
                    }
                }
                break;
            case ACTIVITY_ADD_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    pageVM.addImage(pendingPhotoFile);
                }
        }


    }


    @Override
    public void onSwing(int direction) {
        switch (direction) {
            case PageView.SWING_DIRECTION_DOWN:
                moveToPrev();
                break;
            case PageView.SWING_DIRECTION_UP:
                moveToNext();
                break;
        }
    }

    private void moveToNext() {
        PageViewModel nextPage = albumVM.getNextPage(pageVM);
        if (nextPage != null) {
            //build transition
            TransitionSet transition = new TransitionSet();
            Transition tMove = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move);
            tMove.addTarget(pageVM.getId().toString());
            tMove.addTarget(nextPage.getId().toString());
            transition.addTransition(tMove);
            //change main page
            pageVM = nextPage;
            //build new view
            View nextView = createView(getLayoutInflater(), (ViewGroup) getView());
            //change view
            Scene destScene = new Scene((ViewGroup)getView(),nextView);
            TransitionManager.go(destScene,transition);
        }
    }

    private void moveToPrev() {
        PageViewModel prevPage = albumVM.getPrevPage(pageVM);
        if (prevPage != null) {
            //build transition
            TransitionSet transition = new TransitionSet();
            Transition tMove = TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move);
            tMove.addTarget(pageVM.getId().toString());
            tMove.addTarget(prevPage.getId().toString());
            transition.addTransition(tMove);
            //change main page
            pageVM = prevPage;
            //build new view
            View nextView = createView(getLayoutInflater(), (ViewGroup) getView());
            //change view
            Scene destScene = new Scene((ViewGroup)getView(),nextView);
            TransitionManager.go(destScene,transition);
        }

    }
}
