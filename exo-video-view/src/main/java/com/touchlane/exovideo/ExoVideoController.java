package com.touchlane.exovideo;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public final class ExoVideoController {

    private final Context mContext;
    private final Window mWindow;

    private SimpleExoPlayer mPlayer;
    private int mPlaybackState;

    private PlayerConnection mPlayerConnection;

    public ExoVideoController(Context context) {
        mContext = context;
        mWindow = ((Activity) context).getWindow();
    }

    public void init() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);
        mPlayer.addVideoListener(mVideoListener);
        mPlayer.addListener(mEventListener);
    }

    PlayerConnection connectToPlayer(Uri uri, Listener listener) {
        closePlayerConnection();
        mPlaybackState = mPlayer.getPlaybackState();
        mPlayerConnection = new PlayerConnection(uri, listener);
        return mPlayerConnection;
    }

    private MediaSource createMediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext,
                Util.getUserAgent(mContext, "com.touchlane.exovideo"));
        return new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(),
                null, null);
    }

    private void closePlayerConnection() {
        if (mPlayerConnection != null) {
            mPlayerConnection.close();
        }
    }

    public void release() {
        closePlayerConnection();
        mPlayer.release();
        mPlayer = null;
    }

    private void keepScreenOn() {
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void dontKeepScreenOn() {
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private final SimpleExoPlayer.VideoListener mVideoListener =
            new SimpleExoPlayer.VideoListener() {

                @Override
                public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                        float pixelWidthHeightRatio) {
                    if (mPlayerConnection != null) {
                        mPlayerConnection.mListener.onVideoSizeChanged(width, height);
                    }
                }

                @Override
                public void onRenderedFirstFrame() {

                }

            };

    private final Player.EventListener mEventListener = new SimpleEventListener() {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            boolean playbackStateChanged = mPlaybackState != playbackState;
            mPlaybackState = playbackState;

            if (playbackStateChanged && playbackState == Player.STATE_ENDED) {
                if (mPlayerConnection != null) {
                    mPlayerConnection.mListener.onVideoEnded();
                }
            }
        }

    };

    interface Listener {
        void onVideoEnded();

        void onVideoSizeChanged(int width, int height);

        void onPlayerConnectionClosed();
    }

    class PlayerConnection {

        private boolean mClosed = false;

        private final Uri mUri;
        private final Listener mListener;

        private Surface mSurface;

        PlayerConnection(Uri uri, Listener listener) {
            mUri = uri;
            mListener = listener;
            mPlayer.prepare(createMediaSource(mUri));
        }

        Uri getUri() {
            return mUri;
        }

        private void assertNotClosed() {
            if (mClosed) {
                throw new IllegalStateException("this connection was closed");
            }
        }

        void setSurfaceTexture(SurfaceTexture surfaceTexture) {
            assertNotClosed();
            releaseSurface();
            if (surfaceTexture != null) {
                mSurface = new Surface(surfaceTexture);
                mPlayer.setVideoSurface(mSurface);
            }
        }

        private void releaseSurface() {
            if (mSurface != null) {
                mPlayer.setVideoSurface(null);
                mSurface.release();
                mSurface = null;
            }
        }

        void play() {
            assertNotClosed();
            mPlayer.setPlayWhenReady(true);
            keepScreenOn();
        }

        boolean isPlaying() {
            assertNotClosed();
            return mPlayer.getPlayWhenReady();
        }

        void pause() {
            assertNotClosed();
            mPlayer.setPlayWhenReady(false);
            dontKeepScreenOn();
        }

        void seekTo(long position) {
            assertNotClosed();
            mPlayer.seekTo(position);
        }

        long getCurrentPosition() {
            return mPlayer.getCurrentPosition();
        }

        void close() {
            assertNotClosed();
            mClosed = true;
            releaseSurface();
            mPlayer.stop();
            mPlayerConnection = null;
            dontKeepScreenOn();
            mListener.onPlayerConnectionClosed();
        }

    }

}
