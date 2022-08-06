package fr.nuage.souvenirs.view;


import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.AudioElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;

public class AudioActionModeCallback implements ActionMode.Callback {

    private final AudioElementViewModel audioElementViewModel;
    private Observer<Boolean> audioIsSelectedObserver;

    public AudioActionModeCallback(AudioElementViewModel audioElementViewModel) {
        this.audioElementViewModel = audioElementViewModel;
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_edit_page_select_audio, menu);
        menu.findItem(R.id.action_menu_audio_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        //subscribe to element selection
        audioIsSelectedObserver = isSelected -> {
            if (isSelected.equals(false)) {
                mode.finish();
            }
        };
        audioElementViewModel.getIsSelected().observeForever(audioIsSelectedObserver);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu_audio_delete:
                audioElementViewModel.delete();
                mode.finish();
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        audioElementViewModel.getIsSelected().removeObserver(audioIsSelectedObserver);
        audioElementViewModel.setSelected(false);
    }
}
