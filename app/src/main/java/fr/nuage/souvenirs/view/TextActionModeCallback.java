package fr.nuage.souvenirs.view;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.ActionMode;

import androidx.lifecycle.Observer;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class TextActionModeCallback implements ActionMode.Callback {

    private TextElementViewModel textElementViewModel;
    private Observer<Boolean> textIsSelectedObserver;

    public TextActionModeCallback(TextElementViewModel textElementViewModel) {
        this.textElementViewModel = textElementViewModel;
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_edit_page_select_text, menu);
        //subscribe to element selection
        textIsSelectedObserver = isSelected -> {
            if (isSelected.equals(false)) {
                mode.finish();
            }
        };
        textElementViewModel.getIsSelected().observeForever(textIsSelectedObserver);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu_text_delete:
                textElementViewModel.delete();
                mode.finish();
                return true;
            case R.id.action_menu_text_move_toprevious:
                textElementViewModel.moveToPreviousPage();
                mode.finish();
                return true;
            case R.id.action_menu_text_move_tonext:
                textElementViewModel.moveToNextPage();
                mode.finish();
                return true;
            default:
                return false;
        }

    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        textElementViewModel.getIsSelected().removeObserver(textIsSelectedObserver);
        textElementViewModel.setSelected(false);
    }
}
