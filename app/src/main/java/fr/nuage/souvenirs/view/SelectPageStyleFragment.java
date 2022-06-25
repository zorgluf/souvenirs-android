package fr.nuage.souvenirs.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.PageBuilder;
import fr.nuage.souvenirs.model.TilePageBuilder;

public class SelectPageStyleFragment extends Fragment {

    private static final int mCols = 3;

    public interface OnSelectPageStyleListener {
        void onStyleSelected(int style);
    }

    private OnSelectPageStyleListener listener;
    private int imageFilter;
    private int textFilter;
    private String albumStyle;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SelectPageStyleFragment() {
    }

    public SelectPageStyleFragment(OnSelectPageStyleListener listener,int imageFilter, int textFilter, String albumStyle) {
        this.listener = listener;
        this.imageFilter = imageFilter;
        this.textFilter = textFilter;
        this.albumStyle = albumStyle;
    }

    public SelectPageStyleFragment(OnSelectPageStyleListener listener) {
        this(listener,-1,-1, Album.STYLE_FREE);
    }




    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_page_style,container,false);
        ConstraintLayout previewGrid = view.findViewById(R.id.preview_layout);

        //create grid
        int rowPos = 0;
        int colPos = 0;
        int[] prevRowIds = new int[mCols];
        prevRowIds[0] = R.id.preview_layout;
        ConstraintSet cs = new ConstraintSet();
        cs.clone(previewGrid);
        float[] weights = new float[mCols];
        Arrays.fill(weights,(float)1);

        //init pagebuilder
        PageBuilder pageBuilder;
        if (albumStyle.equals(Album.STYLE_TILE)) {
            pageBuilder = new TilePageBuilder();
        } else {
            pageBuilder = new PageBuilder();
        }
        for (int i=0;i<pageBuilder.getPageStyleMap().length;i++) {
            //filter style
            if (!pageBuilder.isStyleFitted(i,imageFilter,textFilter)) {
                continue;
            }
            //gen view and add to parent
            View pageView = pageBuilder.genPreview(i,previewGrid,inflater);
            pageView.setId(View.generateViewId());
            final int j = i;
            pageView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStyleSelected(j);
                }
            });
            previewGrid.addView(pageView);
            //calc constraints
            cs.constrainWidth(pageView.getId(),ConstraintSet.MATCH_CONSTRAINT);
            cs.constrainHeight(pageView.getId(),ConstraintSet.MATCH_CONSTRAINT);
            cs.setDimensionRatio(pageView.getId(),"1:1");
            if (colPos == 0) {
                cs.connect(pageView.getId(),ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP);
            } else {
                cs.connect(pageView.getId(),ConstraintSet.TOP,prevRowIds[rowPos%mCols],ConstraintSet.BOTTOM);
            }
            //before loop
            prevRowIds[rowPos%mCols] = pageView.getId();
            rowPos += 1;
            if (rowPos >= mCols) {
                rowPos = 0;
                colPos += 1;
                cs.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                        prevRowIds, weights, ConstraintSet.CHAIN_SPREAD);
            }
        }
        if (rowPos != 0) {
            //fill with empty view
            while (rowPos < mCols) {
                View pageView = new View(getContext());
                pageView.setId(View.generateViewId());
                previewGrid.addView(pageView);
                cs.constrainWidth(pageView.getId(),ConstraintSet.MATCH_CONSTRAINT);
                cs.constrainHeight(pageView.getId(),ConstraintSet.MATCH_CONSTRAINT);
                cs.setDimensionRatio(pageView.getId(),"1:1");
                prevRowIds[rowPos%mCols] = pageView.getId();
                rowPos += 1;
            }
            cs.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                    ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                    prevRowIds,weights, ConstraintSet.CHAIN_SPREAD);
        }
        if ((rowPos == 0) && (colPos == 0)) {
            //display warning
            view.findViewById(R.id.style_none_text).setVisibility(View.VISIBLE);
        }
        cs.applyTo(previewGrid);
        return view;
    }

}
