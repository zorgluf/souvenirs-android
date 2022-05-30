package fr.nuage.souvenirs.model.nc;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import fr.nuage.souvenirs.model.Album;

import static fr.nuage.souvenirs.model.Album.STYLE_FREE;

public class AlbumNC {
    
    public static final int STATE_NOT_LOADED = 0;
    public static final int STATE_OK = 1;
    public static final int STATE_ERROR = 2;

    private UUID id;
    private String name;
    private final MutableLiveData<String> ldName = new MutableLiveData<>();
    private Date date;
    private final MutableLiveData<Date> ldDate = new MutableLiveData<>();
    private Date lastEditDate;
    private final MutableLiveData<Date> ldLastEditDate = new MutableLiveData<>();
    private String albumImage;
    private Date pagesLastEditDate;
    private final MutableLiveData<Date> ldPageLastEditDate = new MutableLiveData<>();
    private ArrayList<PageNC> pages = new ArrayList<>();
    private boolean isShared = false;
    private final MutableLiveData<Boolean> ldIsShared = new MutableLiveData<>();
    private String shareToken;
    private int state;
    private final MutableLiveData<Integer> ldState = new MutableLiveData<>();
    private String defaultStyle;


    public AlbumNC(@NonNull UUID id) {
        this.id = id;
        setState(STATE_NOT_LOADED);
    }


    /**
     * create album in NC. not on ui thread.
     */
    public static AlbumNC create(@NonNull UUID id) {
        APIProvider.AlbumResp albumResp;
        //create on remote
        try {
            albumResp = APIProvider.getApi().createAlbum(id.toString()).execute().body();
        } catch (IOException e) {
            Log.i(AlbumNC.class.getName(),"Error on nextcloud album creation "+id.toString());
            return null;
        }
        if (albumResp == null) {
            return null;
        }
        AlbumNC albumNC = new AlbumNC(albumResp.id);
        albumNC.load(albumResp);
        albumNC.setState(STATE_OK);
        return albumNC;
    }

    public UUID getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
        ldName.postValue(name);
    }

    public LiveData<String> getLdName() {
        return ldName;
    }

    public void setDate(Date date) {
        this.date = date;
        ldDate.postValue(date);
    }

    public LiveData<Date> getLdDate() {
        return ldDate;
    }

    public void setLastEditDate(Date date) {
        this.lastEditDate = date;
        ldLastEditDate.postValue(date);
    }

    public Date getDate() {
        return date;
    }

    public LiveData<Date> getLdLastEditDate() {
        return ldLastEditDate;
    }

    public Date getPagesLastEditDate() {
        return pagesLastEditDate;
    }

    public void setPagesLastEditDate(Date pagesLastEditDate) {
        this.pagesLastEditDate = pagesLastEditDate;
        ldPageLastEditDate.postValue(pagesLastEditDate);
    }

    public void setPages(ArrayList<PageNC> pages) {
        this.pages = pages;
    }

    public void setIsShared(boolean isShared) {
        this.isShared = isShared;
        ldIsShared.postValue(isShared);
    }


    public Date getLastEditDate() {
        return lastEditDate;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setAlbumImage(String albumImage) {
        this.albumImage = albumImage;
    }

    public String getName() {
        return name;
    }

    public String getAlbumImage() {
        return albumImage;
    }

    /**
    save album infos to nextcloud (not the pages !)
     sync method
     */
    public boolean save() {
        try {
            APIProvider.AlbumResp albumResp = new APIProvider.AlbumResp();
            albumResp.id = getId();
            albumResp.albumImage = getAlbumImage();
            albumResp.date = getDate();
            albumResp.lastEditDate = getLastEditDate();
            albumResp.name = getName();
            albumResp.pagesLastEditDate = getPagesLastEditDate();
            albumResp.defaultStyle = getDefaultStyle();
            String result = APIProvider.getApi().modifyAlbum(getId().toString(), albumResp).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                return true;
            } else {
                throw new IOException("Error on post modifications on album");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),"Error on posting nextcloud album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }

    private String getDefaultStyle() {
        return defaultStyle;
    }

    public LiveData<Date> getLdPageLastEditDate() {
        return ldPageLastEditDate;
    }

    public boolean delPage(PageNC pageNC) {
        try {
            String result = APIProvider.getApi().deletePage(getId().toString(),pageNC.getId().toString()).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                //update local object
                ArrayList<PageNC> tmp = pages;
                tmp.remove(pageNC);
                pageNC.clear();
                setPages(tmp);
                return true;
            } else {
                throw new IOException("Error in delete page");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),"Error on deleting nextcloud page in album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }

    public boolean hasPage(UUID id) {
        return (getPage(id) != null);
    }

    public PageNC getPage(UUID id) {
        for (PageNC page : pages) {
            if (page.getId().equals(id)) {
                return page;
            }
        }
        return null;
    }

    public ArrayList<PageNC> getPages() {
        return pages;
    }

    public boolean createPage(PageNC pageNC, int index, String localAlbumPath) {
        if (!pageNC.pushAssets(localAlbumPath,this)) {
            return false;
        }
        try {
            String result = APIProvider.getApi().createPage(getId().toString(),index,pageNC.generatePageResp()).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                //update local object
                pages.add(index, pageNC);
                return true;
            } else {
                throw new IOException("Create page error");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),"Error on creating nextcloud page in album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }

    public boolean clean() {
        try {
            String result = APIProvider.getApi().cleanAlbum(getId().toString()).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                return true;
            } else {
                throw new IOException("Error on clean");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),"Error on cleaning nextcloud album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }

    public int getIndex(PageNC pageNC) {
        return getPages().indexOf(pageNC);
    }

    public LiveData<Boolean> getLdIsShared() {
        return ldIsShared;
    }

    public boolean pushAsset(String localAlbumPath, String assetPath) {
        //probe asset
        APIProvider.AssetProbeResult result;
        try {
            result = APIProvider.getApi().AssetProbe(getId().toString(),assetPath).execute().body();
        } catch (Exception e) {
            Log.i(getClass().getName(),String.format("Error on asset probe request for %1$s",assetPath),e);
            setState(STATE_ERROR);
            return false;
        }
        if (result == null) {
            Log.i(getClass().getName(),String.format("Error on asset probe request for %1$s",assetPath));
            setState(STATE_ERROR);
            return false;
        }
        //get local asset file path
        String localPath = new File(localAlbumPath,assetPath).getPath();
        if (result.status.equals("ok")) {
            Log.d(getClass().getName(),String.format("Asset %1$s already present.",assetPath));
            //check if size equal to local one
            if ((result.size == 0) || (result.size == (new File(localPath)).length())) {
                return true;
            }
            Log.d(getClass().getName(),String.format("Asset %1$s wrong size on server side, reupload.",assetPath));
        } else {
            Log.d(getClass().getName(),String.format("Asset %1$s not present.",assetPath));
        }
        //get path to push asset
        String path = result.path;
        if (path.equals("")) {
            Log.i(getClass().getName(),"Nextcloud response incomplete");
            setState(STATE_ERROR);
            return false;
        }
        //push asset
        if (Utils.uploadFile(path,localPath)) {
            Log.d(getClass().getName(),String.format("Asset %1$s uploaded.",assetPath));
        } else {
            Log.i(getClass().getName(),String.format("Error in upload of asset %1$s",assetPath));
            setState(STATE_ERROR);
            return false;
        }
        return true;
    }

    public boolean pullAsset(String localAlbumPath, String assetPath) {
        //test if asset does not exist locally
        if (! new File(localAlbumPath,assetPath).exists()) {
            //probe asset
            APIProvider.AssetProbeResult result;
            try {
                result = APIProvider.getApi().AssetProbe(getId().toString(),assetPath).execute().body();
            } catch (IOException e) {
                Log.i(getClass().getName(),String.format("Error on asset probe request for %1$s",assetPath),e);
                setState(STATE_ERROR);
                return false;
            }
            if (result == null) {
                Log.i(getClass().getName(),String.format("Error on asset probe request for %1$s",assetPath));
                setState(STATE_ERROR);
                return false;
            }
            if (result.status.equals("ok")) {
                String fullAssetPath = result.path;
                Log.d(getClass().getName(), String.format("Asset %1$s already at %2$s.", assetPath, fullAssetPath));
                if (!fullAssetPath.equals("")) {
                    //pull file
                    String destLocalPath = new File(localAlbumPath, Album.DATA_DIR).getPath();
                    if (Utils.downloadFile(fullAssetPath,destLocalPath,new File(assetPath).getName())) {
                        Log.d(getClass().getName(), String.format("Asset %1$s downloaded.", assetPath));
                    } else {
                        Log.i(getClass().getName(), String.format("Error in download of asset %1$s", assetPath));
                        setState(STATE_ERROR);
                        return false;
                    }
                } else {
                    Log.i(getClass().getName(), "Nextcloud response incomplete");
                    setState(STATE_ERROR);
                    return false;
                }
            } else {
                Log.i(getClass().getName(), String.format("Error on asset probe request for %1$s", assetPath));
                setState(STATE_ERROR);
                return false;
            }
        } else {
            Log.d(getClass().getName(), String.format("Asset %1$s already present locally.", assetPath));
        }
        return true;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    /*
    to be used in async call
     */
    public boolean deleteShare() {
        if (isShared && (shareToken != null)) {
            try {
                String result = APIProvider.getApi().deleteShare(shareToken).execute().body();
                if ((result != null) && (result.equals("OK"))) {
                    setIsShared(false);
                    setShareToken(null);
                    return true;
                } else {
                    throw new IOException("Wrong result on deleteshare");
                }
            } catch (IOException e) {
                setState(STATE_ERROR);
                return false;
            }
        }
        return false;
    }

    public LiveData<Integer> getLdState() {
        return ldState;
    }

    public int getState() {
        return state;
    }


    public void setState(int state) {
        this.state = state;
        ldState.postValue(state);
    }

    public boolean movePage(PageNC pageNC, int pos) {
        try {
            String result = APIProvider.getApi().movePage(getId().toString(),pageNC.getId().toString(),pos).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                //update local page list
                ArrayList<PageNC> tmp = pages;
                int old_pos = getIndex(pageNC);
                PageNC tmpPage = tmp.get(old_pos);
                tmp.remove(old_pos);
                if (old_pos > pos) {
                    tmp.add(pos,tmpPage);
                } else {
                    tmp.add(pos-1,tmpPage);
                }
                setPages(tmp);
                return true;
            } else {
                throw new IOException("Error on move page");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),"Error on moving nextcloud page in album "+id.toString(),e);
            setState(STATE_ERROR);
            return false;
        }
    }

    public boolean load() {
        return load(false);
    }

    public boolean load(APIProvider.AlbumResp albumResp) {
        //import fields from album POJO
        setName(albumResp.name);
        setDate(albumResp.date);
        setLastEditDate(albumResp.lastEditDate);
        setAlbumImage(albumResp.albumImage);
        setPagesLastEditDate(albumResp.pagesLastEditDate);
        setIsShared(albumResp.isShared);
        setShareToken(albumResp.shareToken);
        setDefaultStyle(albumResp.defaultStyle);
        if (albumResp.pages != null) {
            ArrayList<PageNC> pageNCArrayList = new ArrayList<>();
            for (APIProvider.PageResp page : albumResp.pages) {
                PageNC pageNC = new PageNC();
                pageNC.load(page);
                pageNCArrayList.add(pageNC);
            }
            setPages(pageNCArrayList);
        }
        return true;
    }

    public void setDefaultStyle(String defaultStyle) {
        if (defaultStyle == null) {
            this.defaultStyle = STYLE_FREE;
        } else {
            this.defaultStyle = defaultStyle;
        }
    }

    public boolean load(boolean full) {
        //reload album from nextcloud
        APIProvider.AlbumResp albumResp;
        try {
            if (full) {
                albumResp = APIProvider.getApi().getAlbumFull(id.toString()).execute().body();
            } else {
                albumResp = APIProvider.getApi().getAlbum(id.toString()).execute().body();
            }
        } catch (IOException e) {
            Log.w(getClass().getName(),"Unable to load album from nextcloud.",e);
            return false;
        }
        if (albumResp == null) {
            return false;
        }
        return load(albumResp);
    }

    public boolean pushPage(PageNC remotePage, String albumPath) {
        if (!remotePage.pushAssets(albumPath,this))  {
            return false;
        }

        //save page content
        try {
            String result = APIProvider.getApi().modifyPage(getId().toString(),remotePage.getId().toString(),remotePage.generatePageResp()).execute().body();
            if ((result != null) && (result.equals("OK"))) {
                Log.d(getClass().getName(),String.format("Page %1$s uploaded.",getId().toString()));
                return true;
            } else {
                throw new IOException("Error in page upload");
            }
        } catch (IOException e) {
            Log.i(getClass().getName(),String.format("Error in page %1$s upload.",getId().toString()));
            return false;
        }
    }
}
