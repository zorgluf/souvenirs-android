package fr.nuage.souvenirs.model.nc;

import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import fr.nuage.souvenirs.model.Album;
import fr.nuage.souvenirs.model.Page;
import fr.nuage.souvenirs.viewmodel.utils.FixedDownloadFileRemoteOperation;
import fr.nuage.souvenirs.viewmodel.utils.NCCleanAlbum;
import fr.nuage.souvenirs.viewmodel.utils.NCCreateAlbum;
import fr.nuage.souvenirs.viewmodel.utils.NCCreatePage;
import fr.nuage.souvenirs.viewmodel.utils.NCDeletePage;
import fr.nuage.souvenirs.viewmodel.utils.NCDeleteShare;
import fr.nuage.souvenirs.viewmodel.utils.NCGetAlbum;
import fr.nuage.souvenirs.viewmodel.utils.NCGetAssetProbe;
import fr.nuage.souvenirs.viewmodel.utils.NCMovePage;
import fr.nuage.souvenirs.viewmodel.utils.NCPostAlbum;

public class AlbumNC {
    
    public static final int STATE_NOT_LOADED = 0;
    public static final int STATE_OK = 1;
    public static final int STATE_ERROR = 2;

    private OwnCloudClient ncClient;
    private UUID id;
    private String name;
    private MutableLiveData<String> ldName = new MutableLiveData<String>();
    private Date date;
    private MutableLiveData<Date> ldDate = new MutableLiveData<Date>();
    private Date lastEditDate;
    private MutableLiveData<Date> ldLastEditDate = new MutableLiveData<Date>();
    private String albumImage;
    private Date pagesLastEditDate;
    private MutableLiveData<Date> ldPageLastEditDate = new MutableLiveData<Date>();
    private ArrayList<PageNC> pages = new ArrayList<>();
    private boolean isShared = false;
    private MutableLiveData<Boolean> ldIsShared = new MutableLiveData<>();
    private String shareToken;
    private int state;
    private MutableLiveData<Integer> ldState = new MutableLiveData<>();

    public AlbumNC(OwnCloudClient ncClient, @NonNull UUID id) {
        this.ncClient = ncClient;
        this.id = id;
        setState(STATE_NOT_LOADED);
    }


    /**
     * create album in NC. not on ui thread.
     * @param ncClient
     * @param id
     * @return
     */
    public static AlbumNC create(OwnCloudClient ncClient , @NonNull UUID id) {
        AlbumNC albumNC = new AlbumNC(ncClient,id);
        RemoteOperationResult result = new NCCreateAlbum(albumNC).execute(ncClient);
        if (!result.isSuccess()) {
            albumNC.setState(STATE_ERROR);
            Log.i(AlbumNC.class.getName(),"Error on nextcloud album creation "+id.toString());
            return null;
        }
        albumNC.setState(STATE_OK);
        return albumNC;
    }

    public void load() {
        AlbumNC that = this;
        //make async web api requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                RemoteOperationResult result = new NCGetAlbum(that).execute(ncClient);
                if (!result.isSuccess()) {
                    Log.i(getClass().getName(),"Error on fetching nextcloud album "+id.toString());
                    setState(STATE_ERROR);
                    return;
                } else {
                    setState(STATE_OK);
                }
            }
        }).start();
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

    public void loadFromJson(JSONObject albumJSON) {
        try {
            if (albumJSON.has("name")) {
                    setName(albumJSON.getString("name"));
            }
            if (albumJSON.has("date")) {
                try {
                    setDate(new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).parse(albumJSON.getString("date")));
                } catch (ParseException e) {
                    Log.w(getClass().getName(),"Error in parsing date in json",e);
                }
            }
            if (albumJSON.has("lastEditDate")) {
                try {
                    setLastEditDate(new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).parse(albumJSON.getString("lastEditDate")));
                } catch (ParseException e) {
                    Log.w(getClass().getName(),"Error in parsing date in json",e);
                }
            }
            if (albumJSON.has("pages")) {
                ArrayList<PageNC> pages = new ArrayList<>();
                Object jsonPage = albumJSON.get("pages");
                if (jsonPage instanceof JSONArray) {
                    for (int i=0;i<((JSONArray)jsonPage).length();i++) {
                        PageNC p = PageNC.fromJSON((JSONObject)((JSONArray)jsonPage).get(i),this);
                        pages.add(p);
                    }
                }
                setPages(pages);
            }
            if (albumJSON.has("pagesLastEditDate")) {
                try {
                    setPagesLastEditDate(new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).parse(albumJSON.getString("pagesLastEditDate")));
                } catch (ParseException e) {
                    Log.w(getClass().getName(),"Error in parsing date in json",e);
                }
            }
            if (albumJSON.has("albumImage")) {
                setAlbumImage(albumJSON.getString("albumImage"));
            }
            if (albumJSON.has("isShared")) {
                setIsShared(albumJSON.getBoolean("isShared"));
            } else {
                setIsShared(false);
            }
            if (albumJSON.has("shareToken")) {
                shareToken = albumJSON.getString("shareToken");
            }
        } catch (JSONException e) {
            Log.i(getClass().getName(),e.toString(),e);
        }
    }

    private void setIsShared(boolean isShared) {
        this.isShared = isShared;
        ldIsShared.postValue(isShared);
    }

    public JSONObject toJSONInfos() {
        JSONObject out = new JSONObject();
        try {
            if (name != null) { out.put("name",name);  }
            if (date != null) { out.put("date", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(date)); }
            if (getLastEditDate() != null) { out.put("lastEditDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getLastEditDate())); }
            if (getPagesLastEditDate() != null) { out.put("pagesLastEditDate", new SimpleDateFormat("yyyyMMddHHmmss", Locale.FRANCE).format(getPagesLastEditDate())); }
            if (albumImage != null) { out.put("albumImage", albumImage); }
        } catch (JSONException e) {
            Log.e(getClass().getName(),"Error in toJSON function.",e);
        }
        return out;
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
        RemoteOperationResult result = new NCPostAlbum(this).execute(ncClient);
        if (!result.isSuccess()) {
            Log.i(getClass().getName(),"Error on posting nextcloud album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
        return true;
    }

    public LiveData<Date> getLdPageLastEditDate() {
        return ldPageLastEditDate;
    }

    public void onChange() {
    }

    public boolean delPage(PageNC pageNC) {
        RemoteOperationResult result = new NCDeletePage(this,pageNC).execute(getNcClient());
        if (result.isSuccess()) {
            //update local object
            ArrayList<PageNC> tmp = pages;
            tmp.remove(pageNC);
            pageNC.clear();
            setPages(tmp);
            return true;
        } else {
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

    public OwnCloudClient getNcClient() {
        return ncClient;
    }

    public boolean createPage(PageNC pageNC, int index, String localAlbumPath) {
        if (!pageNC.pushAssets(localAlbumPath,this)) {
            return false;
        }
        RemoteOperationResult result = new NCCreatePage(this,pageNC,index).execute(getNcClient());
        if (result.isSuccess()) {
            return true;
        } else {
            Log.i(getClass().getName(),"Error on creating nextcloud page in album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }

    public boolean clean() {
        RemoteOperationResult result = new NCCleanAlbum(this).execute(getNcClient());
        if (result.isSuccess()) {
            return true;
        } else {
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
        RemoteOperationResult result = new NCGetAssetProbe(this,assetPath).execute(getNcClient());
        if (result.isSuccess()) {
            Log.d(getClass().getName(),String.format("Asset %1$s already present.",assetPath));
        } else {
            Log.d(getClass().getName(),String.format("Asset %1$s not present.",assetPath));
            if (result.getSingleData() != null) {
                //get path to push asset
                String path = (String)result.getSingleData();
                if (path.equals("")) {
                    Log.i(getClass().getName(),"Nextcloud response incomplete");
                    setState(STATE_ERROR);
                    return false;
                }
                //get local asset file path
                String localPath = new File(localAlbumPath,assetPath).getPath();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(localPath));
                //push asset
                String modTimestamp = ((Long)(new File(localPath).lastModified()/1000)).toString();
                RemoteOperationResult fileUploadResult = new UploadFileRemoteOperation(localPath, path, mimeType,modTimestamp).execute(getNcClient());
                if (fileUploadResult.isSuccess()) {
                    Log.d(getClass().getName(),String.format("Asset %1$s uploaded.",assetPath));
                } else {
                    Log.i(getClass().getName(),String.format("Error in upload of asset %1$s",assetPath));
                    setState(STATE_ERROR);
                    return false;
                }
            } else {
                Log.i(getClass().getName(),String.format("Error on asset probe request for %1$s",assetPath));
                setState(STATE_ERROR);
                return false;
            }
        }
        return true;
    }

    public boolean pullAsset(String localAlbumPath, String assetPath) {
        //test if asset does not exist locally
        if (! new File(localAlbumPath,assetPath).exists()) {
            RemoteOperationResult result = new NCGetAssetProbe(this, assetPath).execute(getNcClient());
            if (result.isSuccess()) {
                String fullAssetPath = (String) result.getSingleData();
                Log.d(getClass().getName(), String.format("Asset %1$s already at %2$s.", assetPath, fullAssetPath));
                if (!fullAssetPath.equals("")) {
                    //pull file
                    String destLocalPath = new File(localAlbumPath, Album.DATA_DIR).getPath();
                    RemoteOperationResult fileDownload = new FixedDownloadFileRemoteOperation(fullAssetPath, destLocalPath).execute(getNcClient());
                    if (fileDownload.isSuccess()) {
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
            RemoteOperationResult result = new NCDeleteShare(shareToken).execute(getNcClient());
            if (result.isSuccess()) {
                setIsShared(false);
                setShareToken(null);
                return true;
            } else {
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


    private void setState(int state) {
        this.state = state;
        ldState.postValue(state);
    }

    public boolean movePage(PageNC pageNC, int pos) {
        RemoteOperationResult result = new NCMovePage(this,pageNC,pos).execute(getNcClient());
        if (result.isSuccess()) {
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
            Log.i(getClass().getName(),"Error on moving nextcloud page in album "+id.toString());
            setState(STATE_ERROR);
            return false;
        }
    }
}
