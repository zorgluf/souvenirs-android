package fr.nuage.souvenirs.view;


import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;

public class ImageActionModeCallback implements ActionMode.Callback {

    private ImageElementViewModel imageElementViewModel;
    private Observer<Boolean> imageIsSelectedObserver;

    public ImageActionModeCallback(ImageElementViewModel imageElementViewModel) {
        this.imageElementViewModel = imageElementViewModel;
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_edit_page_select_image, menu);
        //subscribe to element selection
        imageIsSelectedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSelected) {
                if (isSelected.equals(false)) {
                    mode.finish();
                }
            }
        };
        imageElementViewModel.getIsSelected().observeForever(imageIsSelectedObserver);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu_image_delete:
                imageElementViewModel.delete();
                mode.finish();
                return true;
            case R.id.action_menu_image_ratio:
                imageElementViewModel.switchTransformType();
                return true;
            case R.id.action_menu_image_move_toprevious:
                imageElementViewModel.moveToPreviousPage();
                mode.finish();
                return true;
            case R.id.action_menu_image_move_tonext:
                imageElementViewModel.moveToNextPage();
                mode.finish();
                return true;
            case R.id.action_menu_image_setasalbum:
                imageElementViewModel.setAsAlbumImage();
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        imageElementViewModel.getIsSelected().removeObserver(imageIsSelectedObserver);
        imageElementViewModel.setSelected(false);
    }
}
