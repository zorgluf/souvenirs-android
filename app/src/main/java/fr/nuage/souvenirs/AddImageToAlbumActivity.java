package fr.nuage.souvenirs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.view.AlbumsRecyclerViewAdapter;
import fr.nuage.souvenirs.view.EditAlbumFragmentArgs;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.utils.NCUtils;

public class AddImageToAlbumActivity extends AppCompatActivity implements AlbumsRecyclerViewAdapter.OnListFragmentInteractionListener{

    public static final String EXTRA_ALBUM = "extra_album";
    private ArrayList<Uri> imageUris;
    private AlbumListViewModel albumsVM;
    private AlbumsRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Bundle extra = intent.getExtras();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imageUris = new ArrayList<>();
                imageUris.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
        }
        //load view model
        albumsVM = new ViewModelProvider(this,new AlbumListViewModelFactory(getApplication())).get(AlbumListViewModel.class);
        mAdapter = new AlbumsRecyclerViewAdapter(albumsVM.getAlbumList().getValue(), this, false);

        //set content view
        setContentView(R.layout.activity_add_image_to_album);

        //link to adapter
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
        albumsVM.getAlbumList().observe(this, albumViewModels -> mAdapter.updateList(albumViewModels));

        //pre-select album
        if (extra.containsKey(AddImageToAlbumActivity.EXTRA_ALBUM)) {
            String albumPath = extra.getString(AddImageToAlbumActivity.EXTRA_ALBUM);
            AlbumViewModel album = albumsVM.getAlbum(albumPath);
            onListFragmentInteraction(album,false,false);
        }

    }


    @Override
    public void onListFragmentInteraction(AlbumViewModel album, boolean editMode, boolean delMode) {
        //launch progress dialog
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = alertBuilder.setCancelable(false)
                .setView(new ProgressBar(this,null,android.R.attr.progressBarStyleLarge)).create();
        dialog.show();
        //create page according to style
        PageBuilder pageBuilder = (album.getDefaultStyle().equals(Album.STYLE_TILE)) ? new TilePageBuilder() : new PageBuilder();
        pageBuilder.create(album,-1,imageUris, null);

        //dismiss progress
        dialog.dismiss();
        //start edit activity
        EditAlbumFragmentArgs editAlbumFragmentArgs = new EditAlbumFragmentArgs.Builder(album.getAlbumPath(),album.getPage(-1).getId().toString()).build();
        new NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_main)
                .setDestination(R.id.nav_album_edit)
                .setArguments(editAlbumFragmentArgs.toBundle())
                .setComponentName(fr.nuage.souvenirs.AlbumListActivity.class)
                .createTaskStackBuilder().startActivities();
        finish();
    }


}
