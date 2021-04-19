package fr.nuage.souvenirs.viewmodel;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.Guideline;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import fr.nuage.souvenirs.AlbumListActivity;
import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Element;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.model.TextElement;

public class ExportPdfAsyncTask extends AsyncTask<Void, Integer, Integer> {

    private Context context;
    private Album album;
    private String outPdfFilePath;
    private int notificationId = new Random().nextInt();
    private int resolution;
    private NotificationCompat.Builder nBuilder;

    public ExportPdfAsyncTask(Context context, Album album, String outPdfFilePath, int resolution) {
        this.context = context;
        this.album = album;
        this.outPdfFilePath = outPdfFilePath;
        this.resolution = resolution;
    }


    @Override
    protected Integer doInBackground(Void... voids) {
        LayoutInflater inflater = LayoutInflater.from(context);
        PrintAttributes printAttributes = new PrintAttributes.Builder()
                .setMediaSize(new PrintAttributes.MediaSize("0","0",(int)(30*1000/2.54),(int)(30*1000/2.54)))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setResolution(new PrintAttributes.Resolution("0","0",resolution,resolution))
                .build();
        PrintedPdfDocument document = new PrintedPdfDocument(context,printAttributes);
        //create pages
        for (int i=0;i<album.getPages().size();i++) {
            Page page = album.getPages().get(i);
            //PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pdfWidth,pdfHeight,i+1).create();
            PdfDocument.Page pagePdf = document.startPage(i);
            FrameLayout layout = new FrameLayout(context);
            //inflate view for elements
            if (page.getElements() != null) {
                for (Element e : page.getElements()) {
                    //create elements views
                    View eRootView;
                    if (e.getClass().equals(TextElement.class)) {
                        eRootView = inflater.inflate(R.layout.text_element_view_show,layout,false);
                        TextView textview = eRootView.findViewById(R.id.text_element);
                        textview.setText(((TextElement) e).getText());
                    } else if (e.getClass().equals(ImageElement.class)) {
                        eRootView = inflater.inflate(R.layout.image_element_view,layout,false);
                        String imPath = ((ImageElement) e).getImagePath();
                        if (! imPath.equals("")) {
                            ImageView imageview = eRootView.findViewById(R.id.image_imageview);
                            Bitmap imBitmap = BitmapFactory.decodeFile(imPath);
                            int imRotation = getDegreeRotationFromExif(imPath);
                            if (imRotation > 0 && imBitmap != null) {
                                Matrix m = new Matrix();
                                m.setRotate(imRotation);
                                Bitmap tmp = Bitmap.createBitmap(imBitmap, 0, 0, imBitmap.getWidth(), imBitmap.getHeight(), m, true);
                                if (tmp != null) {
                                    imBitmap.recycle();
                                    imBitmap = tmp;
                                }
                            }
                            imageview.setImageBitmap(imBitmap);
                        }
                    } else {
                        eRootView = inflater.inflate(R.layout.unknown_element_view,layout,false);
                    }
                    Guideline guideline_left = eRootView.findViewById(R.id.guideline_left);
                    guideline_left.setGuidelinePercent(e.getLeft().floatValue()/100);
                    Guideline guideline_right = eRootView.findViewById(R.id.guideline_right);
                    guideline_right.setGuidelinePercent(e.getRight().floatValue()/100);
                    Guideline guideline_top = eRootView.findViewById(R.id.guideline_top);
                    guideline_top.setGuidelinePercent(e.getTop().floatValue()/100);
                    Guideline guideline_bottom = eRootView.findViewById(R.id.guideline_bottom);
                    guideline_bottom.setGuidelinePercent(e.getBottom().floatValue()/100);
                    layout.addView(eRootView);
                }
            }
            //set view to the size of pdf
            int pdfWidth = pagePdf.getInfo().getPageWidth();
            int pdfHeight = pagePdf.getInfo().getPageWidth();
            int measureWidth = View.MeasureSpec.makeMeasureSpec(pdfWidth,View.MeasureSpec.EXACTLY);
            int measureHeight = View.MeasureSpec.makeMeasureSpec(pdfHeight,View.MeasureSpec.EXACTLY);
            layout.measure(measureWidth,measureHeight);
            layout.layout(0,0,pdfWidth,pdfHeight);
            //rescale all images to actual display size
            for (int j=0;j < layout.getChildCount();j++) {
                ImageView imageview = layout.getChildAt(j).findViewById(R.id.image_imageview);
                if (imageview != null) {
                    BitmapDrawable imageDrawable = (BitmapDrawable) imageview.getDrawable();
                    if (imageDrawable != null) {
                        float ratioView = imageview.getWidth()/imageview.getHeight();
                        float ratioBitmap = (float)imageDrawable.getIntrinsicWidth()/(float)imageDrawable.getIntrinsicHeight();
                        float targetWidth = imageview.getWidth()*resolution/72; //fix for printpdfdocument bug on resolution
                        float targetHeight = imageview.getHeight()*resolution/72; //fix for printpdfdocument bug on resolution
                        if (ratioBitmap>ratioView) {
                            targetHeight = targetWidth/ratioBitmap;
                        } else {
                            targetWidth = targetHeight*ratioBitmap;
                        }
                        Bitmap imBitmap = getResizedBitmap(imageDrawable.getBitmap(),(int)targetWidth,(int)targetHeight);
                        imageview.setImageBitmap(imBitmap);
                    }
                }
            }
            //draw layout view on page
            layout.draw(pagePdf.getCanvas());
            document.finishPage(pagePdf);
            //load UI
            publishProgress(i);
        }
        // write the document to file
        try {
            FileOutputStream out = new FileOutputStream(new File(outPdfFilePath));
            document.writeTo(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // close the document
        document.close();
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        nBuilder.setProgress(album.getPages().size(), progress[0], false);
        NotificationManagerCompat.from(context).notify(notificationId,nBuilder.build());
    }

    @Override
    protected void onPreExecute() {
        //show progress bar in notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        nBuilder = new NotificationCompat.Builder(context, AlbumListActivity.CHANNEL_ID);
        nBuilder.setContentTitle(context.getString(R.string.notification_pdf_creation_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_file_download_black_24dp);
        nBuilder.setProgress(album.getPages().size(), 0, false);
        notificationManager.notify(notificationId, nBuilder.build());
    }

    @Override
    protected void onPostExecute(Integer results) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_VIEW);
        Uri pdfUri = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", new File(outPdfFilePath));
        shareIntent.setDataAndType(pdfUri, "application/pdf");
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, shareIntent, 0);
        nBuilder.setContentText(context.getString(R.string.notification_pdf_creation_text_end))
                .setProgress(0,0,false)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_check_black_24dp);
        NotificationManagerCompat.from(context).notify(notificationId,nBuilder.build());
    }

    private static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    private static int getDegreeRotationFromExif(String imPath){
        int imRotation = ExifInterface.ORIENTATION_UNDEFINED;
        try {
            ExifInterface exif = new ExifInterface(imPath);
            int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (exifRotation) {
                case ExifInterface.ORIENTATION_ROTATE_90: {
                    imRotation = 90;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_180: {
                    imRotation = 180;
                    break;
                }
                case ExifInterface.ORIENTATION_ROTATE_270: {
                    imRotation = 270;
                    break;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return imRotation;
    }
}
