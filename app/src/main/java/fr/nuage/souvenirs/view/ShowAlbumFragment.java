package fr.nuage.souvenirs.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.AudioPlayer;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.ShareAlbumAsyncTask;

public class ShowAlbumFragment extends Fragment {

    private static final int WRITE_REQUEST = 1;

    private ShowPageListAdapter pageListAdapter;
    private RecyclerView pageListRecyclerView;
    private AlbumViewModel albumVM;
    private AudioPlayer audioPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load album path in args
        String albumPath = ShowAlbumFragmentArgs.fromBundle(getArguments()).getAlbumPath();

        //load view model
        albumVM = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class).getAlbum(albumPath);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        //set transparant appbar if on land orientation (dirty)
        int orientation = getActivity().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (getActivity().getClass().equals(AlbumListActivity.class)) {
                ((AlbumListActivity)getActivity()).transparentAppbar(true);
            }
        }
        //init audio player
        audioPlayer = new AudioPlayer(albumVM);
        pageListRecyclerView.setOnScrollChangeListener(audioPlayer);
    }

    @Override
    public void onStop() {
        //stop audio player
        audioPlayer.stop();
        //restore activity scrolling
        if (getActivity().getClass().equals(AlbumListActivity.class)) {
            ((AlbumListActivity)getActivity()).transparentAppbar(false);
        }
        super.onStop();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //inflateview
        View v = inflater.inflate(R.layout.fragment_show_album,container,false);

        //set recyclerview
        pageListRecyclerView = v.findViewById(R.id.page_list);

        //fill recyclerview
        pageListAdapter =  new ShowPageListAdapter(albumVM.getLdPages(),this,albumVM);
        pageListRecyclerView.setAdapter(pageListAdapter);
        albumVM.getLdPages().observe(getViewLifecycleOwner(), new Observer<ArrayList<PageViewModel>>() {
            @Override
            public void onChanged(@Nullable ArrayList<PageViewModel> PageViewModels) {
                pageListAdapter.updateList(PageViewModels);
            }
        });
        albumVM.getName().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                //change title toolbar handling
                getActivity().setTitle(albumVM.getName().getValue());
            }
        });

        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_album_show:
                //get position of scrolling
                LinearLayoutManager layoutManager = (LinearLayoutManager)pageListRecyclerView.getLayoutManager();
                PageViewModel firstVisiblePage = albumVM.getPage(layoutManager.findFirstCompletelyVisibleItemPosition());
                if (firstVisiblePage != null) {
                    albumVM.setFocusPage(firstVisiblePage);
                }
                ShowAlbumFragmentDirections.ActionNavAlbumShowToAlbumEdit action = ShowAlbumFragmentDirections.actionNavAlbumShowToAlbumEdit(albumVM.getAlbumPath(),null);
                Navigation.findNavController(getView()).navigate(action);
                return true;
            case R.id.export_to_pdf:
                //check for user permissions to write
                if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(),R.string.ask_write_perm_toast,Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_REQUEST);
                    return true;
                }
                //start activity to choose resolution
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.pick_resolution)
                        .setItems(R.array.resolution_array, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                int resolution = 72;
                                switch (which) {
                                    case 0:
                                        resolution = 72;
                                        break;
                                    case 1:
                                        resolution = 300;
                                        break;
                                }
                                ShowAlbumFragmentDirections.ActionNavAlbumShowToPdfPrepareAlbumFragment action = ShowAlbumFragmentDirections.actionNavAlbumShowToPdfPrepareAlbumFragment(albumVM.getAlbumPath(),resolution);
                                Navigation.findNavController(getView()).navigate(action);
                            }
                        })
                        .create().show();
                return true;
            case R.id.share_via_nextcloud:
                ShareAlbumAsyncTask shareAlbumAsyncTask = new ShareAlbumAsyncTask(getContext(),albumVM);
                shareAlbumAsyncTask.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_show_album, menu);
        if (!albumVM.hasNCAlbum()) {
            MenuItem shareMenu = menu.findItem(R.id.share_via_nextcloud);
            shareMenu.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //refresh page list in case of edit
        //albumVM.update();
    }

}
