package fr.nuage.souvenirs.view;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.databinding.ImageElementViewBinding;
import fr.nuage.souvenirs.databinding.ShowItemPageListBinding;
import fr.nuage.souvenirs.databinding.TextElementViewShowBinding;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModel;
import fr.nuage.souvenirs.viewmodel.AlbumListViewModelFactory;
import fr.nuage.souvenirs.viewmodel.AlbumViewModel;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageDiffUtilCallback;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.PaintElementViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class PdfPrepareAlbumFragment extends Fragment {

    private PageListAdapter pageListAdapter;
    private RecyclerView pageRecyclerView;
    private AlbumViewModel albumVM;
    private int resolution;
    private PrintedPdfDocument document;
    private AlertDialog waitDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //load album path in args
        String albumPath = PdfPrepareAlbumFragmentArgs.fromBundle(getArguments()).getAlbumPath();
        resolution = PdfPrepareAlbumFragmentArgs.fromBundle(getArguments()).getResolution();

        //load view model
        albumVM = new ViewModelProvider(getActivity(),new AlbumListViewModelFactory(getActivity().getApplication())).get(AlbumListViewModel.class).getAlbum(albumPath);

        //init pdf doc
        initPdfDocument();

        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.pdf_export_title);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //inflateview
        View v = inflater.inflate(R.layout.fragment_prepare_pdf,container,false);

        //set listview
        pageRecyclerView = v.findViewById(R.id.page_listview);
        int pageSize = document.getPageWidth();
        ViewGroup.LayoutParams params = pageRecyclerView.getLayoutParams();
        params.width = pageSize;
        pageRecyclerView.setLayoutParams(params);

        //fill recyclerview
        pageListAdapter =  new PageListAdapter(this);
        pageRecyclerView.setAdapter(pageListAdapter);
        albumVM.getPages().observe(getViewLifecycleOwner(), pageViewModels -> {
            pageListAdapter.updateList(pageViewModels);
            if (pageViewModels != null) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.schedule(() -> {
                    exportToPdf(pageRecyclerView);
                    waitDialog.dismiss();
                    Navigation.findNavController(container).popBackStack();
                },2, TimeUnit.SECONDS); //dirty fix : wait for adapter to populate
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pdf_export_wait)
                .setCancelable(false);
        waitDialog = builder.create();
        waitDialog.show();

        return v;
    }

    private void initPdfDocument() {
        //since PrintedPdfDocument has a fixed resolution of 72 dpi in source code, we rise the mediasize as a trick to save better image resolution
        //we start from a 30 cm square media at 72 dpi
        int mediaSize = (int)((30*1000/2.54)*resolution/72);
        PrintAttributes printAttributes = new PrintAttributes.Builder()
                .setMediaSize(new PrintAttributes.MediaSize("0","0",mediaSize,mediaSize))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
        document = new PrintedPdfDocument(getContext(),printAttributes);
    }

    private void exportToPdf(RecyclerView listView) {

        String pdfPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(),albumVM.getName().getValue()+".pdf").getPath();

        //one page per Page object
        for(int i = 0 ; i < listView.getAdapter().getItemCount(); i++) {
            int j = i;
            getActivity().runOnUiThread(() -> {
                listView.scrollToPosition(j);
            });
            try {
                Thread.sleep(2000); //dirty fix : wait for image rendering
            } catch (InterruptedException e) {
            }
            View v = listView.findViewHolderForAdapterPosition(i).itemView;
            PdfDocument.Page pagePdf = document.startPage(i);
            v.draw(pagePdf.getCanvas());
            document.finishPage(pagePdf);
        }
        // write the document to file
        try {
            FileOutputStream out = new FileOutputStream(new File(pdfPath));
            document.writeTo(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // close the document
        document.close();
        Log.i(getClass().getName(),"Export to pdf finished to "+pdfPath);

        //create notification
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_VIEW);
        Uri pdfUri = FileProvider.getUriForFile(getContext(),getContext().getApplicationContext().getPackageName() + ".provider", new File(pdfPath));
        shareIntent.setDataAndType(pdfUri, "application/pdf");
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, shareIntent, 0);
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(getContext(), AlbumListActivity.CHANNEL_ID);
        nBuilder.setContentText(getContext().getString(R.string.notification_pdf_creation_text_end))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_check_black_24dp);
        NotificationManagerCompat.from(getContext()).notify(new Random().nextInt(),nBuilder.build());
    }

    public class PageListAdapter extends RecyclerView.Adapter<PageListAdapter.ViewHolder> {

        private ArrayList<PageViewModel> mPages = new ArrayList<>();
        private Fragment mFragment;

        public PageListAdapter(Fragment fragment) {
            mFragment = fragment;
        }

        public void updateList(ArrayList<PageViewModel> pageViewModels) {
            if (pageViewModels != null) {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PageDiffUtilCallback(mPages, pageViewModels));
                mPages = pageViewModels;
                diffResult.dispatchUpdatesTo(this);
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ShowItemPageListBinding mBinding;
            public ViewHolder(ShowItemPageListBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }
            public void bind(AlbumViewModel albumViewModel) {
                mBinding.setAlbum(albumViewModel);
                mBinding.executePendingBindings();
            }
        }

        @NonNull
        @Override
        public PageListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ShowItemPageListBinding binding = DataBindingUtil.inflate(inflater, R.layout.show_item_page_list,parent,false);
            binding.setLifecycleOwner(mFragment);
            return new PageListAdapter.ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            //listen to elements changes
            PageViewModel page = mPages.get(position);
            holder.bind(albumVM);
            page.getElements().observe(mFragment, elementViewModels -> {
                ViewGroup layout = holder.itemView.findViewById(R.id.page_layout);
                layout.removeAllViewsInLayout();
                LayoutInflater inflater = LayoutInflater.from(layout.getContext());
                //build layout
                if (elementViewModels != null) {
                    for (final ElementViewModel e : elementViewModels) {
                        if (e.getClass() == TextElementViewModel.class) {
                            //load xml layout and bind data
                            TextElementViewShowBinding txtBinding = DataBindingUtil.inflate(inflater, R.layout.text_element_view_show,layout,false);
                            txtBinding.setLifecycleOwner(mFragment);
                            txtBinding.setElement((TextElementViewModel) e);
                            txtBinding.executePendingBindings();
                            layout.addView(txtBinding.getRoot());
                        } else if (e.getClass() == ImageElementViewModel.class || e.getClass() == PaintElementViewModel.class) {
                            //load xml layout and bind data
                            ImageElementViewBinding imBinding = DataBindingUtil.inflate(inflater, R.layout.image_element_view,layout,false);
                            imBinding.setLifecycleOwner(mFragment);
                            imBinding.setElement((ImageElementViewModel) e);
                            imBinding.executePendingBindings();
                            layout.addView(imBinding.getRoot());
                        } else {
                            //unknown element : display default view
                            inflater.inflate(R.layout.unknown_element_view, layout,true);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPages.size();
        }
    }

}
