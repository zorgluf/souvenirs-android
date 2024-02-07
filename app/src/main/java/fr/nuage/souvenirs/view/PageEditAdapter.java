package fr.nuage.souvenirs.view;

import static fr.nuage.souvenirs.view.helpers.ElementMoveDragListener.MOVE_DRAG;

import android.content.ClipData;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.view.helpers.EditItemTouchHelper;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;

public class PageEditAdapter extends RecyclerView.Adapter<PageEditAdapter.ViewHolder> implements EditItemTouchHelper.ItemTouchHelperAdapter {

    private ArrayList<PageViewModel> pages;
    private final AlbumViewModel albumViewModel;
    private final Fragment fragment;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final PageView pageView;

        public ViewHolder(View view) {
            super(view);
            pageView = view.findViewById(R.id.pageview);
        }

        public PageView getPageView() {
            return pageView;
        }
    }

    public PageEditAdapter(AlbumViewModel albumViewModel, Fragment fragment, ArrayList<PageViewModel> pages) {
        super();
        if (pages == null) {
            this.pages = new ArrayList<>();
        } else {
            this.pages = pages;
        }
        this.albumViewModel = albumViewModel;
        this.fragment = fragment;
    }

    public void setPages(@NonNull ArrayList<PageViewModel> pageViewModels) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PageDiffUtilCallback(pages, pageViewModels));
        pages = pageViewModels;
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.page_adapter, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        PageViewModel pageViewModel = pages.get(position);
        PageView pageView = viewHolder.getPageView();
        pageView.setPageViewModel(pageViewModel);
        //set drag listener (element move)
        pageView.setOnDragListener((v, event) -> {
            String dragType = (String)event.getLocalState();
            if (dragType.equals(MOVE_DRAG)) {
                //handle switch elements drag action
                int action = event.getAction();
                switch(action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setAlpha((float)0.5);
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        v.setAlpha(1);
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        return true;
                    case DragEvent.ACTION_DROP:
                        // Gets the element id to move
                        ClipData.Item item = event.getClipData().getItemAt(0);
                        UUID oriElementUUID = UUID.fromString((String)item.getText());
                        albumViewModel.moveElementToPage(oriElementUUID,pageViewModel);
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });
        //change focus on page click
        pageView.setOnClickListener(view -> albumViewModel.setFocusPage(pageViewModel));
        //set view as selected if page in edit frame
        albumViewModel.getFocusPageId().observe(fragment.getViewLifecycleOwner(),(uuid -> {
            if (pageViewModel.getId().equals(uuid)) {
                pageView.setAlpha(1);
            } else {
                pageView.setAlpha(0.4f);
            }
        }));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    @Override
    public void onItemMove(int from, int to) {
        albumViewModel.movePage(from,to);
    }
}

