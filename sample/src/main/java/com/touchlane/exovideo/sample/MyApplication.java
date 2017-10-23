package com.touchlane.exovideo.sample;

import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Picasso picasso = new Picasso.Builder(this).addRequestHandler(
                new AssetVideoRequestHandler()).build();
        Picasso.setSingletonInstance(picasso);
    }

    private class AssetVideoRequestHandler extends RequestHandler {

        @Override
        public boolean canHandleRequest(Request data) {
            return "asset".equals(data.uri.getScheme());
        }

        @Override
        public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
            // ExoPlayer accepts uris in the form "asset:///path/to/video.mp4",
            // but AssetManager only needs the relative path "path/to/video.mp4"
            String assetPath = request.uri.toString().replaceFirst("asset:///", "");

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            AssetFileDescriptor afd = getAssets().openFd(assetPath);
            mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            Bitmap bitmap = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }
}
