package fr.nuage.souvenirs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import fr.nuage.souvenirs.model.TilePageBuilder;
import fr.nuage.souvenirs.view.AlbumsRecyclerViewAdapter;
import fr.nuage.souvenirs.view.EditAlbumFragmentArgs;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;

public class AddImageToAlbumActivity extends AppCompatActivity implements AlbumsRecyclerViewAdapter.OnListFragmentInteractionListener{

    public static final String IMAGE_SHARE = "fr.nuage.souvenirs.AddImageToAlbumActivity.IMAGE_SHARE";
    private ArrayList<Uri> imageUris;
    private ArrayList<Uri> videoUris;
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
            } else if (type.startsWith("video/")) {
                videoUris = new ArrayList<>();
                videoUris.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                Collections.reverse(imageUris);
            } else if (type.startsWith("video/")) {
                videoUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                Collections.reverse(videoUris);
            }
        }
        //load view model
        albumsVM = new ViewModelProvider(this,new AlbumListViewModelFactory(getApplication())).get(AlbumListViewModel.class);
        mAdapter = new AlbumsRecyclerViewAdapter(albumsVM.getAlbumList().getValue(), this, false, true);

        //pre-select album
        if (extra.containsKey(Intent.EXTRA_SHORTCUT_ID)) {
            String albumId = extra.getString(Intent.EXTRA_SHORTCUT_ID);
            AlbumViewModel album = albumsVM.getAlbum(UUID.fromString(albumId));
            onListFragmentInteraction(album,false,false);
        } else {
            //set content view
            setContentView(R.layout.activity_add_image_to_album);
            //link to adapter
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(mAdapter);
            albumsVM.getAlbumList().observe(this, albumViewModels -> mAdapter.updateList(albumViewModels));
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
        TilePageBuilder pageBuilder = new TilePageBuilder();
        pageBuilder.create(album,-1,imageUris, null, videoUris);

        //dismiss progress
        dialog.dismiss();
        //start edit activity
        EditAlbumFragmentArgs editAlbumFragmentArgs = new EditAlbumFragmentArgs.Builder(album.getAlbumPath(),album.getAlbum().getPage(-1).getId().toString()).build();
        new NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_main)
                .setDestination(R.id.nav_album_edit)
                .setArguments(editAlbumFragmentArgs.toBundle())
                .setComponentName(fr.nuage.souvenirs.AlbumListActivity.class)
                .createTaskStackBuilder().startActivities();
        finish();
    }


}
