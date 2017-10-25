package com.touchlane.exovideo;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ExoVideoView extends FrameLayout {

    private TextureView mTextureView;
    private ImageView mThumbnail;

    private SurfaceTexture mSurfaceTexture;
    private Size mViewSize;
    private Size mVideoSize;

    private Uri mVideoUri;
    private ExoVideoController mExoVideoController;
    private ExoVideoController.PlayerConnection mPlayerConnection;

    private VideoEndListener mVideoEndListener;
    private ThumbnailProvider mThumbnailProvider;

    private boolean mHideThumbnailOnNewFrame = false;

    public ExoVideoView(@NonNull Context context) {
        this(context, null);
    }

    public ExoVideoView(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExoVideoView(@NonNull Context context,
            @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout();
    }

    private void initLayout() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        layoutInflater.inflate(R.layout.view_exo_video, this, true);
        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mThumbnail = (ImageView) findViewById(R.id.thumbnail);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                        int height) {
                    mViewSize = new Size(width, height);
                    mSurfaceTexture = surface;
                    trySetAspectRatio();
                    trySetSurfaceTexture();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                        int height) {
                    mViewSize = new Size(width, height);
                    trySetAspectRatio();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    mSurfaceTexture = null;
                    if (mPlayerConnection != null) {
                        mPlayerConnection.setSurfaceTexture(null);
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    if (mHideThumbnailOnNewFrame && mVideoSize != null) {
                        mHideThumbnailOnNewFrame = false;
                        showThumbnail(false);
                    }
                }

            };

    private ExoVideoController.Listener mExoVideoContollerListener =
            new ExoVideoController.Listener() {

                @Override
                public void onVideoEnded() {
                    if (mPlayerConnection != null) {
                        mPlayerConnection.close();
                    }
                    if (mVideoEndListener != null) {
                        mVideoEndListener.onVideoEnded();
                    }
                }

                @Override
                public void onVideoSizeChanged(int width, int height) {
                    mVideoSize = new Size(width, height);
                    trySetAspectRatio();
                }

                @Override
                public void onPlayerConnectionClosed() {
                    showThumbnail(true);
                    mPlayerConnection = null;
                    if (mVideoEndListener != null) {
                        mVideoEndListener.onPlayerDisconnected();
                    }
                }

            };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPlayerConnection != null) {
            mPlayerConnection.close();
        }
    }

    private void trySetAspectRatio() {
        if (mVideoSize != null && mViewSize != null) {
            Matrix matrix = createAspectRatioCorrectionMatrix(mViewSize, mVideoSize);
            mTextureView.setTransform(matrix);
        }
    }

    private static Matrix createAspectRatioCorrectionMatrix(Size viewSize, Size contentSize) {
        Matrix matrix = new Matrix();
        float previewRatio = (float) contentSize.getWidth() / contentSize.getHeight();
        float viewRatio = (float) viewSize.getWidth() / viewSize.getHeight();
        float scaleX, scaleY;
        float scaleFactor = previewRatio / viewRatio;
        if (scaleFactor > 1f) {
            scaleX = scaleFactor;
            scaleY = 1f;
        } else {
            scaleX = 1f;
            scaleY = 1f / scaleFactor;
        }
        matrix.setScale(scaleX, scaleY, (float) viewSize.getWidth() / 2,
                (float) viewSize.getHeight() / 2);
        return matrix;
    }

    private void showThumbnail(boolean show) {
        mThumbnail.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Set uri of a video to play.
     *
     * @param uri all supported schemes are defined here
     *            {@link com.google.android.exoplayer2.upstream.DefaultDataSource}
     *            (currently tested only with asset and file schemes)
     */
    public void setSource(Uri uri) {
        mVideoUri = uri;
        showThumbnail(true);
        if (mThumbnailProvider != null) {
            mThumbnailProvider.provideThumbnail(mThumbnail, mVideoUri);
        }
    }

    public void setExoVideoController(ExoVideoController exoVideoController) {
        mExoVideoController = exoVideoController;
    }

    /**
     * Plays the specified video.
     * <p>
     * Set {@link ExoVideoController} and source before playing.
     */
    public void play() {
        if (mPlayerConnection == null || !mPlayerConnection.getUri().equals(mVideoUri)) {
            mPlayerConnection = mExoVideoController.connectToPlayer(mVideoUri,
                    mExoVideoContollerListener);
            mVideoSize = null; // wait for new video size
            trySetSurfaceTexture();
        }
        mPlayerConnection.play();
        mHideThumbnailOnNewFrame = true;
    }

    private void trySetSurfaceTexture() {
        if (mPlayerConnection != null && mSurfaceTexture != null) {
            mPlayerConnection.setSurfaceTexture(mSurfaceTexture);
        }
    }

    public boolean isPlaying() {
        return mPlayerConnection != null && mPlayerConnection.isPlaying();
    }

    /**
     * Pauses the playback. It may be continued from the paused position with {@link #play()}.
     * <p>
     * If you don't expect the playback to be continued from the same position, you may want to call
     * {@link #stop()}.
     */
    public void pause() {
        if (mPlayerConnection != null) {
            mPlayerConnection.pause();
        }
    }

    public long getCurrentPosition() {
        if (mPlayerConnection != null) {
            return mPlayerConnection.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public void seekTo(long position) {
        if (mPlayerConnection != null) {
            mPlayerConnection.seekTo(position);
        }
    }

    /**
     * Stops the playback, resets position, and disconnects from player.
     */
    public void stop() {
        if (mPlayerConnection != null) {
            mPlayerConnection.close();
        }
    }

    public interface VideoEndListener {
        void onVideoEnded();

        void onPlayerDisconnected();
    }

    /**
     * You may set {@link VideoEndListener} to listen for video end or when another ExoVideoView
     * started playing its video and the current view lost its connection to player.
     */
    public void setVideoEndListener(VideoEndListener listener) {
        mVideoEndListener = listener;
    }

    public interface ThumbnailProvider {
        void provideThumbnail(ImageView imageView, Uri uri);
    }

    public void setThumbnailProvider(ThumbnailProvider thumbnailProvider) {
        mThumbnailProvider = thumbnailProvider;
    }

}
