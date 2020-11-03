package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import fr.nuage.souvenirs.R;

public class SelectPageStyleDialogFragment extends BottomSheetDialogFragment {

    private SelectPageStyleFragment.OnSelectPageStyleListener listener;
    private int imageFilter;
    private int textFilter;


    public SelectPageStyleDialogFragment(SelectPageStyleFragment.OnSelectPageStyleListener listener,int imageFilter, int textFilter) {
        super();
        this.listener = new SelectPageStyleFragment.OnSelectPageStyleListener() {
            @Override
            public void onStyleSelected(int style) {
                listener.onStyleSelected(style);
                dismiss();
            }
        };
        this.imageFilter = imageFilter;
        this.textFilter = textFilter;
    }

    public static SelectPageStyleDialogFragment newInstance(SelectPageStyleFragment.OnSelectPageStyleListener listener) {
        return newInstance(listener,-1,-1);
    }

    public static SelectPageStyleDialogFragment newInstance(SelectPageStyleFragment.OnSelectPageStyleListener listener,
                                                            int imageFilter, int textFilter) {
        SelectPageStyleDialogFragment dialog = new SelectPageStyleDialogFragment(listener,imageFilter,textFilter);
        return dialog;
    }


    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_select_page_style,container,false);
        SelectPageStyleFragment selectPageStyleFragment = new SelectPageStyleFragment(listener,imageFilter,textFilter);
        getChildFragmentManager().beginTransaction().add(R.id.dialog_select_layout,selectPageStyleFragment).commit();
        return dialogView;
    }

}
