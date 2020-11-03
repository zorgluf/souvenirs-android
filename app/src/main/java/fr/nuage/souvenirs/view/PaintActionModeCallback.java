package fr.nuage.souvenirs.view;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.ActionMode;

import androidx.fragment.app.FragmentManager;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;

public class PaintActionModeCallback implements ActionMode.Callback, ColorPickerDialogFragment.ColorPickerListener {

    private PaintElementViewModel paintElementViewModel;
    private PageViewModel pageViewModel;
    private FragmentManager fragmentManager;
    private MenuItem penMenuItem;
    private MenuItem eraserMenuItem;
    private int penColor;

    public PaintActionModeCallback(FragmentManager fragmentManager, PageViewModel pageViewModel, PaintElementViewModel paintElementViewModel) {
        this.paintElementViewModel = paintElementViewModel;
        this.pageViewModel = pageViewModel;
        this.fragmentManager = fragmentManager;
    }


    @Override
    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_edit_page_select_paint, menu);
        penMenuItem = menu.findItem(R.id.action_menu_paint_pen);
        eraserMenuItem = menu.findItem(R.id.action_menu_paint_eraser);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
        return false;
    }

    public Drawable getPenIcon() {
        Drawable icon;
        if (penMenuItem.isChecked()) {
            penMenuItem.setIcon(R.drawable.menu_item_paint_pen_checked);
        } else {
            penMenuItem.setIcon(R.drawable.ic_brush_black_24dp);
        }
        icon = penMenuItem.getIcon();
        icon.setColorFilter(penColor, PorterDuff.Mode.SRC_ATOP);
        return icon;
    }

    @Override
    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu_paint_eraser:
                eraserMenuItem.setChecked(true);
                penMenuItem.setChecked(false);
                penMenuItem.setIcon(getPenIcon());
                eraserMenuItem.setIcon(R.drawable.menu_item_paint_eraser_checked);
                paintElementViewModel.setToolSelected(PaintElementViewModel.TOOL_ERASER);
                break;
            case R.id.action_menu_paint_pen:
                if (item.isChecked()) {
                    ColorPickerDialogFragment colorPickerDialogFragment = new ColorPickerDialogFragment();
                    colorPickerDialogFragment.setListener(this);
                    colorPickerDialogFragment.show(fragmentManager,"");
                } else {
                    penMenuItem.setChecked(true);
                    eraserMenuItem.setIcon(R.drawable.ic_erase_black_24dp);
                    eraserMenuItem.setChecked(false);
                    paintElementViewModel.setToolSelected(PaintElementViewModel.TOOL_PEN);
                }
                break;
            case R.id.action_menu_paint_delete:
                paintElementViewModel.delete();
                mode.finish();
                return true;
            default:
                return false;
        }
        penMenuItem.setIcon(getPenIcon());
        return true;
    }

    @Override
    public void onDestroyActionMode(android.view.ActionMode mode) {
        pageViewModel.setPaintMode(false);
    }

    @Override
    public void onColorPicked(int color) {
        penColor = color;
        penMenuItem.setIcon(getPenIcon());
        paintElementViewModel.setColor(color);
    }
}
