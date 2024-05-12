package fr.nuage.souvenirs.view;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentEditAlbumBinding;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;

public class EditAlbumFragment extends Fragment {

    private static final String DIALOG_CHANGE_STYLE = "DIALOG_CHANGE_STYLE";

    private EditPageListAdapter editPageListAdapter;
    private AlbumViewModel albumVM;
    private String albumPath;
    private UUID initialPageFocusId;
    private int colNb = 3;

    private PageViewModel lastOperationPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load album path in args
        albumPath = EditAlbumFragmentArgs.fromBundle(getArguments()).getAlbumPath();
        String pageFocusId = EditAlbumFragmentArgs.fromBundle(getArguments()).getPageFocusId();
        if (pageFocusId != null) {
            initialPageFocusId = UUID.fromString(pageFocusId);
        }

        setHasOptionsMenu(true);
    }

    public void setAlbumVM(AlbumViewModel albumVM) {
        this.albumVM = albumVM;
        editPageListAdapter.setAlbum(albumVM);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //inflateview
        FragmentEditAlbumBinding binding = DataBindingUtil.inflate(inflater,R.layout.fragment_edit_album,container,false);

        //set recyclerview
        RecyclerView pageListRecyclerView = binding.pageList;
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), colNb);
        pageListRecyclerView.setLayoutManager(mLayoutManager);

        //fill recyclerview
        editPageListAdapter =  new EditPageListAdapter(this, albumVM);
        pageListRecyclerView.setAdapter(editPageListAdapter);

        //add touch helper to move pages
        EditItemTouchHelper editItemTouchHelper = new EditItemTouchHelper(editPageListAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(editItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(pageListRecyclerView);
        //listen to album view model
        AlbumListViewModel albumListViewModel = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class);
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        albumListViewModel.getAlbumList().observe(lifecycleOwner, albumViewModels -> {
            AlbumViewModel albumVM = albumListViewModel.getAlbum(albumPath);
            if (albumVM != null) {
                setAlbumVM(albumVM);
                binding.setAlbum(albumVM);
                albumVM.getLdPages().observe(lifecycleOwner, PageViewModels -> editPageListAdapter.updateList(PageViewModels));
                getActivity().setTitle(albumVM.getName().getValue());
                albumVM.getName().observe(lifecycleOwner, s -> getActivity().setTitle(albumVM.getName().getValue()));
                if (initialPageFocusId != null) {
                    new Handler().postDelayed(() -> ((RecyclerView)getView().findViewById(R.id.page_list)).scrollToPosition(albumVM.getPosition(initialPageFocusId)),200L);
                }
                albumVM.getFocusPageId().observe(lifecycleOwner, id -> new Handler().postDelayed(() -> {
                    if (id != null) {
                        ((RecyclerView)getView().findViewById(R.id.page_list)).scrollToPosition(albumVM.getPosition(id));
                    }
                },200L));
            }
        });

        return binding.getRoot();
    }

    public void onAddPage(PageViewModel previousPage) {
        Page page = albumVM.createPage(albumVM.getPosition(previousPage)+1);
        //open edit page fragment
        String pageId = page.getId().toString();
        String albumPath = getAlbumVM().getAlbumPath();
        EditAlbumFragmentDirections.ActionNavAlbumEditToEditPageFragment action = EditAlbumFragmentDirections.actionNavAlbumEditToEditPageFragment(albumPath,pageId);
        Navigation.findNavController(getView()).navigate(action);
    }


    public void openPageEdition(View v) {
        String pageId = (String)v.getTag();
        String albumPath = getAlbumVM().getAlbumPath();
        EditAlbumFragmentDirections.ActionNavAlbumEditToEditPageFragment action = EditAlbumFragmentDirections.actionNavAlbumEditToEditPageFragment(albumPath,pageId);
        Navigation.findNavController(getView()).navigate(action);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_album, menu);

        //set logic to edit album title
        MenuItem editItem = menu.findItem(R.id.edit_title_edit_album);
        editItem.setOnMenuItemClickListener(menuItem -> {
            new EditAlbumNameDialogFragment(albumVM).show(getParentFragmentManager(),null);
            return true;
        });
        //set listener to add page
        MenuItem addPageItem = menu.findItem(R.id.add_page_edit_album);
        addPageItem.setOnMenuItemClickListener(menuItem -> {
            onAddPage(albumVM.getPage(-1));
            return true;
        });
    }

    public AlbumViewModel getAlbumVM() { return albumVM; }

}
