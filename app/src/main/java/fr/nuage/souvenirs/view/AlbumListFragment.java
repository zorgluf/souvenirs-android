package fr.nuage.souvenirs.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.SettingsActivity;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class AlbumListFragment extends Fragment implements AlbumsRecyclerViewAdapter.OnListFragmentInteractionListener {

    private AlbumListViewModel albumsVM;
    private AlbumsRecyclerViewAdapter mAdapter;
    private int activityScrollStatus = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlbumListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //load view model
        albumsVM = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class);
        mAdapter = new AlbumsRecyclerViewAdapter(albumsVM.getAlbumList().getValue(), this, true);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_album_list, container, false);

        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        if (recyclerView instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(mAdapter);
            albumsVM.getAlbumList().observe(getViewLifecycleOwner(), new Observer<List<AlbumViewModel>>() {
                @Override
                public void onChanged(@Nullable List<AlbumViewModel> albumViewModels) {
                    mAdapter.updateList(albumViewModels);
                }
            });
        }

        //set action on floating button
        FloatingActionButton addAlbumActionButton = view.findViewById(R.id.addAlbum);
        addAlbumActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CreateAlbumDialogFragment().show(getFragmentManager(),null);
            }
        });

        //set action on refresh
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.albumlist_swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            albumsVM.refresh();
            swipeRefreshLayout.setRefreshing(false);
        });

        //set title
        getActivity().setTitle(R.string.app_name);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //dirty trick to disable toolbar scrolling for this fragment only
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            this.activityScrollStatus = ((AppBarLayout.LayoutParams)toolbar.getLayoutParams()).getScrollFlags();
            ((AppBarLayout.LayoutParams)((Toolbar) getActivity().findViewById(R.id.toolbar)).getLayoutParams()).setScrollFlags(0);
        }
        //refresh list
        albumsVM.refresh();
    }

    @Override
    public void onStop() {
        //restore activity scrolling
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            ((AppBarLayout.LayoutParams)toolbar.getLayoutParams()).setScrollFlags(this.activityScrollStatus);
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        //mAdapter.updateList(albumsVM.getAlbumList().getValue());
    }

    @Override
    public void onListFragmentInteraction(AlbumViewModel album, boolean editModeSelected, boolean delSelected) {
        if (!album.hasLocalAlbum()) {
            //no actions if no local album
            return;
        }
        if (editModeSelected) {
            AlbumListFragmentDirections.ActionNavAlbumListToNavAlbumEdit action = AlbumListFragmentDirections.actionNavAlbumListToNavAlbumEdit(album.getAlbumPath(),null);
            Navigation.findNavController(getView()).navigate(action);
        } else if (delSelected) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.delete_album)
                    .setMessage(getString(R.string.delete_album_message)+" : "+album.getName().getValue())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            albumsVM.deleteLocalAlbum(album);
                        }})
                    .setNegativeButton(android.R.string.no, null).create().show();
        } else {
            AlbumListFragmentDirections.ActionNavAlbumListToNavAlbumShow action = AlbumListFragmentDirections.actionNavAlbumListToNavAlbumShow(album.getAlbumPath());
            Navigation.findNavController(getView()).navigate(action);
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_album_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
