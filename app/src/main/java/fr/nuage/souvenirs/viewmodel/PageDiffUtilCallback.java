package fr.nuage.souvenirs.viewmodel;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class PageDiffUtilCallback extends DiffUtil.Callback {

    private ArrayList<PageViewModel> oldPages;
    private ArrayList<PageViewModel> newPages;

    public PageDiffUtilCallback(ArrayList<PageViewModel> oldPages, ArrayList<PageViewModel> newPages) {
        this.oldPages = oldPages;
        this.newPages = newPages;
    }

    @Override
    public int getOldListSize() {
        return oldPages.size();
    }

    @Override
    public int getNewListSize() {
        return newPages.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPages.get(oldItemPosition).getId().equals(newPages.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ArrayList<ElementViewModel> oldElements = oldPages.get(oldItemPosition).getLdElements().getValue();
        ArrayList<ElementViewModel> newElements = newPages.get(newItemPosition).getLdElements().getValue();
        if (newElements == null) {
            return oldElements == null;
        }
        if (oldElements.size() != newElements.size()) {
            return false;
        }
        for (int i=0;i<oldElements.size();i++) {
            if (! oldElements.get(i).getId().equals(newElements.get(i).getId())) {
                return false;
            }
        }
        return true;
    }
}
