package fr.nuage.souvenirs.model;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Album {

    public static final String DATA_DIR = "data";
    public static final String CONFFILE = "album.json";

    public static final String STYLE_FREE = "FREE";
    public static final String STYLE_TILE = "TILE";

    private String albumPath;
    private MutableLiveData<String> ldName = new MutableLiveData<String>();
    private String name;
    private MutableLiveData<ArrayList<Page>> ldPages = new MutableLiveData<ArrayList<Page>>();
    private ArrayList<Page> pages = new ArrayList<Page>();
    private Date pagesLastEditDate;
    private MutableLiveData<Date> ldPagesLastEditDate = new MutableLiveData<>();
    private Date pagesLastSyncDate;
    private Date date;
    private MutableLiveData<Date> ldDate = new MutableLiveData<>();
    private Date lastEditDate;
    private MutableLiveData<Date> ldLastEditDate = new MutableLiveData<>();
    private UUID id;
    private String albumImage;
    private MutableLiveData<String> ldAlbumImage = new MutableLiveData<>();
    private String defaultStyle = STYLE_TILE;
    private boolean unsavedModifications = false;



    public Album(String albumPath) {
        this.albumPath = albumPath;
        load();
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public static boolean exists(String albumPath) {
        return (new File(albumPath,CONFFILE).exists());
    }

    public void updateAllLiveDataObject() {
        ldName.postValue(name);
        ldPages.postValue(pages);
        ldDate.postValue(date);
        ldAlbumImage.postValue(albumImage);
    }

    public String getName(){
        return this.name;
    }

    public MutableLiveData<String> getLiveDataName(){
        return ldName;
    }

    public void setName(String name) {
        this.name = name;
        this.ldName.postValue(this.name);
        this.save();
        setLastEditDate(new Date());
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("name",name);
            json.put("id",id.toString());
            JSONArray jPages = new JSONArray();
            for( Page p: pages ) {
                jPages.put(p.toJSON());
            }
            json.put("pages",jPages);
            if (getDate() != null) {
                json.put("date", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getDate()));
            }
            if (getLastEditDate() != null) {
                json.put("lastEditDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getLastEditDate()));
            }
            if (getPagesLastEditDate() != null) {
                json.put("pagesLastEditDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getPagesLastEditDate()));
            }
            if (getPagesLastSyncDate() != null) {
                json.put("pagesLastSyncDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getPagesLastSyncDate()));
            }
            if (getAlbumImage() != null) {
                json.put("albumImage", Utils.getRelativePath(getAlbumPath(),getAlbumImage()));
            }
            json.put("defaultStyle",getDefaultStyle());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void reload() {
        if (!unsavedModifications) {
            load();
        }
    }

    public boolean load() {
        Log.d(this.getClass().toString(),"Load Album "+albumPath);
        //load album datas from file
        InputStream is = null;
        JSONObject json = null;
        try {
            is = new FileInputStream(new File(albumPath,CONFFILE));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new JSONObject(new String(buffer, "UTF-8"));
        } catch (FileNotFoundException e) {
            Log.w(this.getClass().getSimpleName(),"File "+this.albumPath+"/"+CONFFILE+" not found.");
            return false;
        } catch (IOException e) {
            Log.w(this.getClass().getSimpleName(),"Error reading file "+this.albumPath+"/"+CONFFILE);
            return false;
        } catch (JSONException e) {
            Log.w(this.getClass().getSimpleName(),"Wrong file format for "+this.albumPath+"/"+CONFFILE);
            return false;
        }

        try {
            name = json.getString("name");
            if (json.has("id")) {
                id = UUID.fromString(json.getString("id"));
            }
            if (json.has("albumImage")) {
                albumImage = json.getString("albumImage");
                if (!albumImage.startsWith("/")) {
                    albumImage = new File(getAlbumPath(),albumImage).getPath();
                }
            } else {
                albumImage = null;
            }
            if (json.has("date")) {
                try {
                    date = new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(json.getString("date"));
                } catch (Exception e) {
                    date = new Date();
                }
            } else {
                date = new Date();
            }
            if (json.has("lastEditDate")) {
                try {
                    lastEditDate = new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(json.getString("lastEditDate"));
                } catch (Exception e) {
                    lastEditDate = new Date();
                }
            } else {
                lastEditDate = new Date();
            }
            if (json.has("defaultStyle")) {
                defaultStyle = json.getString("defaultStyle");
            } else {
                defaultStyle = STYLE_FREE;
            }
            JSONArray jPages = json.getJSONArray("pages");
            ArrayList<Page> pages = new ArrayList<Page>();
            for (int i=0;i<jPages.length();i++) {
                Page p  = Page.fromJSON(jPages.getJSONObject(i),this);
                pages.add(p);
            }
            this.pages = pages;
            if (json.has("pagesLastEditDate")) {
                try {
                    pagesLastEditDate = new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(json.getString("pagesLastEditDate"));
                } catch (Exception e) {
                    pagesLastEditDate = new Date();
                }
            } else {
                pagesLastEditDate = new Date();
            }
            if (json.has("pagesLastSyncDate")) {
                try {
                    pagesLastSyncDate = new SimpleDateFormat("yyyyMMddHHmmss",Locale.FRANCE).parse(json.getString("pagesLastSyncDate"));
                } catch (Exception e) {
                    pagesLastSyncDate = new Date();
                }
            } else {
                pagesLastSyncDate = new Date();
            }
            //if no albumimage defined, set last image as image album
            if ((albumImage == null)||(albumImage.equals(""))) {
                for (Page p : getPages()) {
                    for (Element e : p.getElements()) {
                        if (e.getClass().equals(ImageElement.class)) {
                            albumImage = ((ImageElement) e).getImagePath();
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(this.getClass().getSimpleName(),"Wrong file format for "+this.albumPath);
            return false;
        }
        updateAllLiveDataObject();
        return true;
    }

    public void onChange() {
        unsavedModifications = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (unsavedModifications) {
                unsavedModifications = false;
                save();
            }
        },1000);
    }

    public void onPageChange() {
        setPagesLastEditDate(new Date());
        onChange();
    }

    public boolean save() {
        Log.d(this.getClass().toString(),"Save Album "+getId().toString());
        //si id vide = empty album due to bug : do not save !
        if (getId() == null) {
            return false;
        }

        //serialize album
        JSONObject json = this.toJSON();
        if (json == null) {
            return false;
        }

        //save conf file
        File file = new File(this.albumPath,Album.CONFFILE);
        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(json.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    /*
    if index = -1, add to end of album
     */
    public void addPage(Page page, int index) {
        addPage(page,index,true);
    }

    public void addPage(Page page, int index, boolean updatePagesLastEditPate) {
        page.setAlbumParent(this);
        if (index == -1) {
            pages.add(page);
        } else {
            pages.add(index, page);
        }
        ldPages.postValue(pages);
        if (updatePagesLastEditPate) {
            setPagesLastEditDate(new Date());
        }
        onChange();
    }

    public Page createPage(int position) {
        return createPage(position,true);
    }

    public Page createPage(int position, boolean updatePagesLastEditPate) {
        Page p = new Page();
        addPage(p,position,updatePagesLastEditPate);
        return p;
    }

    public int getIndex(Page page) {
        return getPages().indexOf(page);
    }

    public void setPages(ArrayList<Page> pages) {
        for (Page p : pages) {
            p.setAlbumParent(this);
        }
        this.pages = pages;
        this.ldPages.postValue(this.pages);
        onPageChange();
    }

    /*
    if position = -1, return last page
     */
    public Page getPage(int position) {
        if (pages.size() == 0) {
            return null;
        }
        if (position == -1) {
            return pages.get(pages.size()-1);
        }
        return pages.get(position);
    }

    public Page getPage(UUID id) {
        for (Page page: getPages()) {
            if (page.getId().equals(id)) {
                return page;
            }
        }
        return null;
    }

    public void addPages(ArrayList<Page> pages, int position) {
        ArrayList<Page> newPages = this.pages;
        newPages.addAll(position,pages);
        setPages(newPages);
    }

    public void movePage(Page page, int toIndex) {
        movePage(getIndex(page),toIndex);
    }

    public void movePage(int fromIndex, int toIndex) {
        ArrayList<Page> tmp = pages;
        Page tmpPage = tmp.get(fromIndex);
        tmp.remove(fromIndex);
        if (fromIndex > toIndex) {
            tmp.add(toIndex,tmpPage);
        } else {
            tmp.add(toIndex-1,tmpPage);
        }
        setPages(tmp);
    }

    public void delPage(Page page) {
        delPage(page,true);
    }

    public void delPage(Page page, boolean clearPage) {
        ArrayList<Page> tmp = pages;
        tmp.remove(page);
        if (clearPage) {
            page.clear();
        }
        setPages(tmp);
    }

    public ArrayList<Page> getPages() {
        ArrayList<Page> out = pages;
        if (out == null) {
            return new ArrayList<Page>();
        }
        return out;
    }

    public MutableLiveData<ArrayList<Page>> getLiveDataPages() {
        return ldPages;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void delete() {
        deleteRecursive(new File(albumPath));
    }

    public UUID getId() {
        return id;
    }



    public String getDataPath() {
        File dataFile = new File(albumPath,DATA_DIR);
        if (! dataFile.exists()) {
            dataFile.mkdirs();
        }
        return dataFile.getPath();
    }

    public void delPage(int position) {
        ArrayList<Page> tmp = getPages();
        tmp.remove(position);
        setPages(tmp);
    }

    public String getAlbumPath() {
        return albumPath;
    }


    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public Date getLastEditDate() {
        return lastEditDate;
    }

    public LiveData<Date> getLdLastEditDate() {
        return ldLastEditDate;
    }

    public LiveData<Date> getLdPageLastEditDate() {
        return ldPagesLastEditDate;
    }

    public void setDate(Date newDate) {
        date = newDate;
        ldDate.postValue(newDate);
        setLastEditDate(new Date());
        onChange();
    }

    public void setLastEditDate(Date date) {
        lastEditDate = date;
        ldLastEditDate.postValue(date);
        onChange();
    }


    public LiveData<String> getLdAlbumImage() {
        return ldAlbumImage;
    }

    public void setAlbumImage(String albumImage) {
        this.albumImage = albumImage;
        ldAlbumImage.postValue(albumImage);
        setLastEditDate(new Date());
        onChange();
    }

    public void setDefaultStyle(String style) {
        defaultStyle = style;
        setLastEditDate(new Date());
        onChange();
    }

    public String createDataFile(InputStream inputStream, String mimeType) {
        //if input null, do nothing (blank image)
        if (inputStream != null) {
            //generate file name
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String name = UUID.randomUUID().toString()+"."+ext;
            File file = new File(getDataPath(), name);
            //input is a data stream : save it to file
            try {
                OutputStream output = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
                output.close();
            } catch (FileNotFoundException e) {
                Log.w(ImageElement.class.getName(), "Impossible to save " + name + " to " + getDataPath());
            } catch (IOException e) {
                Log.w(ImageElement.class.getName(), "Impossible to save " + name + " to " + getDataPath());
            }
            return file.getPath();
        } else {
            return null;
        }
    }


    public boolean isLastPage(Page page) {
        return getIndex(page) == (getPages().size()-1);
    }

    public boolean FirstPage(Page page) {
        return getIndex(page) == 0;
    }

    public Date getPagesLastEditDate() {
        return pagesLastEditDate;
    }

    public void setPagesLastEditDate(Date pagesLastEditDate) {
        this.pagesLastEditDate = pagesLastEditDate;
        ldPagesLastEditDate.postValue(pagesLastEditDate);
        onChange();
    }

    public Date getPagesLastSyncDate() {
        return pagesLastSyncDate;
    }

    public void setPagesLastSyncDate(Date pagesLastSyncDate) {
        this.pagesLastSyncDate = pagesLastSyncDate;
        onChange();
    }

    public void setID(UUID id) {
        this.id = id;
        setLastEditDate(new Date());
        onChange();
    }

    public String getAlbumImage() {
        return this.albumImage;
    }

    public boolean hasPage(UUID id) {
        return (getPage(id)!=null);
    }

    public LiveData<Date> getLdDate() {
        return ldDate;
    }

}
