package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "c595cacbe6da2057da0c2dfb1f726c1b";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s")
            .build();

    public byte[] getUrlBytes(String specUrl) throws IOException{
        URL url = new URL(specUrl);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try{
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + " with: " + specUrl);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                byteArrayOutputStream.write(buffer,0,bytesRead);
            }
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String specUrl) throws IOException{
        return new String(getUrlBytes(specUrl));
    }

    private List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG,"Recieved JSON: " + jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            parseItems(items,jsonObject);
        }catch (IOException e){
            Log.e(TAG,"Failed to fetch items",e);
        }catch (JSONException e){
            Log.e(TAG,"Failed to fetch json object",e);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)throws IOException,JSONException{
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }
    }

    private String buildUrl(String method,String query){
        Uri.Builder builder = ENDPOINT.buildUpon()
                .appendQueryParameter("method",method);
        if(method.equals(SEARCH_METHOD)){
            builder.appendQueryParameter("text",query);
        }
        return builder.build().toString();
    }

    public List<GalleryItem> fetchRecentItems(){
        String url = buildUrl(FETCH_RECENTS_METHOD,null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD,query);
        return downloadGalleryItems(url);
    }
}
