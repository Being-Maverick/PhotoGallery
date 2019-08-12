package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static int MESSAGE_DOWNLOAD = 1;

    private Handler mRequestHandler;
    private ConcurrentHashMap<T,String> mConcurrentHashMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener listener){
        mThumbnailDownloadListener = listener;
    }

    private boolean mHasQuit = false;
    public ThumbnailDownloader(Handler handler){
        super(TAG);
        mResponseHandler = handler;
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG,"Got a URL: " + url);
        if(url == null){
            mConcurrentHashMap.remove(target);
            return;
        }
        mConcurrentHashMap.put(target,url);
        mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj;
                    Log.i(TAG,"Got a request for URL: " + mConcurrentHashMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mConcurrentHashMap.clear();
    }

    private void handleRequest(final T target) {
        try{
            final String url = mConcurrentHashMap.get(target);
            if (url == null){
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            Log.i(TAG,"Bitmap Created");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mConcurrentHashMap.get(target) != url || mHasQuit){
                        return;
                    }
                    mConcurrentHashMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,bitmap);
                }
            });
        }catch (IOException e){
            Log.e(TAG,"Error downloading image");
        }
    }
}
