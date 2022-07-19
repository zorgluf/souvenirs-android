package fr.nuage.souvenirs.model.nc;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NCAPI {

        String mApiEndpoint = "/index.php/apps/souvenirs/apiv2/";

        @GET("album")
        Call<List<String>> getAlbums(@Query("page") int page);

        @GET("album/{id}")
        Call<APIProvider.AlbumResp> getAlbum(@Path("id") String id);

        @GET("album/{id}/full")
        Call<APIProvider.AlbumResp> getAlbumFull(@Path("id") String id);

        @PUT("album/{id}")
        Call<APIProvider.AlbumResp> createAlbum(@Path("id") String id);

        @DELETE("album/{id}")
        Call<String> deleteAlbum(@Path("id") String id);

        @POST("album/{id}")
        Call<String> modifyAlbum(@Path("id") String id, @Body APIProvider.AlbumResp albumResp);

        @GET("album/{id}/clean")
        Call<String> cleanAlbum(@Path("id") String id);

        @GET("album/{id}/assetprobe/{asset_path}")
        Call<APIProvider.AssetProbeResult> AssetProbe(@Path("id") String id, @Path("asset_path") String assetPath);

        @GET("album/{id}/assetsearch")
        Call<APIProvider.AssetSearchResult> AssetSearch(@Path("id") String id, @Query("asset") String asset,
                                                        @Query("asset_name") String assetName, @Query("asset_size") int asset_size);

        @PUT("album/{id}/page/{page_pos}")
        Call<String> createPage(@Path("id") String id, @Path("page_pos") int pagePos, @Body APIProvider.PageResp page);

        @POST("album/{id}/page/{page_id}")
        Call<String> modifyPage(@Path("id") String id, @Path("page_id") String pageId, @Body APIProvider.PageResp page);

        @DELETE("album/{id}/page/{page_id}")
        Call<String> deletePage(@Path("id") String id, @Path("page_id") String pageId);

        @POST("album/{id}/page/{page_id}/pos/{page_pos}")
        Call<String> movePage(@Path("id") String id, @Path("page_id") String pageId, @Path("page_pos") int pagePos);

        @POST("share")
        Call<String> createShare(@Field("albumId") String albumId);

        @DELETE("share/{token}")
        Call<String> deleteShare(@Path("token") String token);
}
