package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.FragmentEditAlbumBinding;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;

public class EditAlbumFragment extends Fragment implements SelectPageStyleFragment.OnSelectPageStyleListener {

    private static final String DIALOG_CHANGE_STYLE = "DIALOG_CHANGE_STYLE";

    private EditPageListAdapter editPageListAdapter;
    private AlbumViewModel albumVM;
    private String albumPath;
    private UUID initialPageFocusId;
    private int colNb = 1;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //inflateview
        FragmentEditAlbumBinding binding = DataBindingUtil.inflate(inflater,R.layout.fragment_edit_album,container,false);
        binding.setFragment(this);

        //set recyclerview
        RecyclerView pageListRecyclerView = binding.pageList;
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), colNb);
        pageListRecyclerView.setLayoutManager(mLayoutManager);

        //fill recyclerview
        editPageListAdapter =  new EditPageListAdapter(this);
        pageListRecyclerView.setAdapter(editPageListAdapter);

        //add touch helper to move pages
        EditItemTouchHelper editItemTouchHelper = new EditItemTouchHelper(editPageListAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(editItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(pageListRecyclerView);
        //listen to album view model
        AlbumListViewModel albumListViewModel = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class);
        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
        albumListViewModel.getAlbumList().observe(lifecycleOwner, new Observer<List<AlbumViewModel>>() {
            @Override
            public void onChanged(List<AlbumViewModel> albumViewModels) {
                AlbumViewModel albumVM = albumListViewModel.getAlbum(albumPath);
                if (albumVM != null) {
                    setAlbumVM(albumVM);
                    albumVM.getPages().observe(lifecycleOwner, new Observer<ArrayList<PageViewModel>>() {
                        @Override
                        public void onChanged(@Nullable ArrayList<PageViewModel> PageViewModels) {
                            editPageListAdapter.updateList(PageViewModels);
                        }
                    });
                    getActivity().setTitle(albumVM.getName().getValue());
                    albumVM.getName().observe(lifecycleOwner, new Observer<String>() {
                        @Override
                        public void onChanged(@Nullable String s) {
                            getActivity().setTitle(albumVM.getName().getValue());
                        }
                    });
                    if (initialPageFocusId != null) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((RecyclerView)getView().findViewById(R.id.page_list)).scrollToPosition(albumVM.getPosition(initialPageFocusId));
                            }
                        },200L);
                    }
                    albumVM.getFocusPageId().observe(lifecycleOwner, new Observer<UUID>() {
                        @Override
                        public void onChanged(@Nullable UUID id) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (id != null) {
                                        ((RecyclerView)getView().findViewById(R.id.page_list)).scrollToPosition(albumVM.getPosition(id));
                                    }
                                }
                            },200L);
                        }
                    });
                }
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

    public void onSwitchStyle(PageViewModel p) {
        //save page
        lastOperationPage = p;
        //launch select style dialog
        SelectPageStyleDialogFragment dialog = SelectPageStyleDialogFragment.newInstance(this);
        dialog.show(getFragmentManager(),DIALOG_CHANGE_STYLE);
    }

    public void openPageEdition(View v) {
        String pageId = (String)v.getTag();
        String albumPath = getAlbumVM().getAlbumPath();
        EditAlbumFragmentDirections.ActionNavAlbumEditToEditPageFragment action = EditAlbumFragmentDirections.actionNavAlbumEditToEditPageFragment(albumPath,pageId);
        Navigation.findNavController(getView()).navigate(action);
    }


    @Override
    public void onStyleSelected(int style) {
        //check which dialog was launched
        Fragment dialogChangeStyle = getFragmentManager().findFragmentByTag(DIALOG_CHANGE_STYLE);
        if (dialogChangeStyle != null) {
            //dismiss dialog
            DialogFragment df = (DialogFragment) dialogChangeStyle;
            df.dismiss();
            //switch style
            albumVM.switchStyle(lastOperationPage,style);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_album, menu);

        //set logic to edit album title
        MenuItem editItem = menu.findItem(R.id.edit_title_edit_album);
        ActionAlbumEditNameView actionAlbumEditNameView = (ActionAlbumEditNameView) editItem.getActionView();
        editItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                actionAlbumEditNameView.setName(albumVM.getName().getValue());
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                albumVM.setName(actionAlbumEditNameView.getName());
                return true;
            }
        });
        //set listener to add page
        MenuItem addPageItem = menu.findItem(R.id.add_page_edit_album);
        addPageItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                onAddPage(albumVM.getPage(-1));
                return true;
            }
        });
        //set listener to switch display
        MenuItem displayColunmItem = menu.findItem(R.id.display_column_edit_album);
        displayColunmItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //change columns
                colNb = (colNb == 1) ? 2 : 1;
                //change layout of recyclerview
                RecyclerView pageListRecyclerView = getView().findViewById(R.id.page_list);
                RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), colNb);
                pageListRecyclerView.setLayoutManager(mLayoutManager);
                getActivity().invalidateOptionsMenu();
                return true;
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if (colNb != 1) {
            menu.findItem(R.id.display_column_edit_album).setIcon(R.drawable.ic_view_one_column_24dp);
        } else {
            menu.findItem(R.id.display_column_edit_album).setIcon(R.drawable.ic_view_column_black_24dp);
        }
    }


    public AlbumViewModel getAlbumVM() { return albumVM; }
}
