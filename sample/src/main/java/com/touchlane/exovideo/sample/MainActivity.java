package com.touchlane.exovideo.sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.touchlane.exovideo.ExoVideoController;
import com.touchlane.exovideo.ExoVideoView;

public class MainActivity extends AppCompatActivity {

    private static final int NUMBER_OF_ASSET_VIDEOS = 5;
    private static final int SAMPLE_LIST_LENGTH = 100;

    private ExoVideoController mExoVideoController;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mExoVideoController = new ExoVideoController(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.videos);
        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new VideosAdapter());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mExoVideoController.init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mExoVideoController.release();
    }

    private static class VideoViewHolder extends RecyclerView.ViewHolder {

        private ExoVideoView mExoVideoView;
        private ImageView mPlayButton;

        VideoViewHolder(View view, ExoVideoController exoVideoController,
                ExoVideoView.ThumbnailProvider thumbnailProvider) {
            super(view);
            mPlayButton = (ImageView) view.findViewById(R.id.btn_play);
            mExoVideoView = (ExoVideoView) view.findViewById(R.id.exo_video);
            mExoVideoView.setExoVideoController(exoVideoController);
            mExoVideoView.setThumbnailProvider(thumbnailProvider);

            mExoVideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mExoVideoView.isPlaying()) {
                        setPlayButtonVisible(true);
                        mExoVideoView.pause();
                    } else {
                        setPlayButtonVisible(false);
                        mExoVideoView.play();
                    }
                }
            });

            mExoVideoView.setVideoEndListener(new ExoVideoView.VideoEndListener() {
                @Override
                public void onVideoEnded() {
                    setPlayButtonVisible(true);
                }

                @Override
                public void onPlayerDisconnected() {
                    setPlayButtonVisible(true);
                }
            });

        }

        void setPlayButtonVisible(boolean visible) {
            mPlayButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }

        void setVideoSource(Uri uri) {
            mExoVideoView.setSource(uri);
        }
    }

    private class VideosAdapter extends RecyclerView.Adapter<VideoViewHolder> {

        @Override
        public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.item_video, parent, false);
            return new VideoViewHolder(view, mExoVideoController,
                    mThumbnailProvider);
        }

        @Override
        public void onBindViewHolder(VideoViewHolder holder, int position) {
            int videoNumber = position % NUMBER_OF_ASSET_VIDEOS + 1;
            Uri uri = Uri.parse("asset:///video_" + videoNumber + ".mp4");
            holder.setVideoSource(uri);
        }

        @Override
        public int getItemCount() {
            return SAMPLE_LIST_LENGTH;
        }
    }

    private ExoVideoView.ThumbnailProvider mThumbnailProvider =
            new ExoVideoView.ThumbnailProvider() {
                @Override
                public void provideThumbnail(ImageView imageView, Uri uri) {
                    // see MyApplication for Picasso configuration
                    Picasso.with(MainActivity.this).load(uri).into(imageView);
                }
            };

}
