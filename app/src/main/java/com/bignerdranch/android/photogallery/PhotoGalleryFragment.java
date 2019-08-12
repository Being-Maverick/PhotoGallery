package com.bignerdranch.android.photogallery;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "Photo Gallery Fragment";
    private RecyclerView mRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    public static Intent newIntent(Context context){
        return new Intent(context,PhotoGalleryFragment.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        Handler handler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(handler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background Thread Started");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setUpAdapter();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background Thread Stopped");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem item = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG,"Query is " + query);
                QueryPreferences.setStoredQuery(getActivity(),query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG,"Query Text Changed " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else{
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private AsyncTask<Void, Void, List<GalleryItem>> updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());
        return new FetchItemsTask(query).execute();
    }
    private void setUpAdapter(){
        if(isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View view){
            super(view);
            mImageView = view.findViewById(R.id.item_type_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable){
            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            // Intent i = new Intent(Intent.ACTION_VIEW,mGalleryItem.getPhotoPageUri());
            Intent intent = PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(intent);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery,viewGroup,false);
            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
            if(mQuery == null){
                return new FlickrFetchr().fetchRecentItems();
            }
            return new FlickrFetchr().searchPhotos(mQuery);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setUpAdapter();
        }
    }
}
