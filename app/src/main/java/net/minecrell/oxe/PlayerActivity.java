/*
 * Oxe
 * Copyright (C) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.minecrell.oxe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.C.ContentType;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class PlayerActivity extends Activity implements View.OnSystemUiVisibilityChangeListener,
        PlayerControlView.VisibilityListener, View.OnClickListener, Player.EventListener {

    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";

    private DataSource.Factory dataSourceFactory;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;

    private PlayerView playerView;

    private ImageButton videoTrackButton;
    private ImageButton audioTrackButton;
    private ImageButton textTrackButton;
    private ImageButton resizeModeButton;

    private SimpleExoPlayer player;

    private DefaultTrackSelector.Parameters lastTrackSelectorParameters;
    private int lastWindow = C.INDEX_UNSET;
    private long lastPosition = C.TIME_UNSET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)));

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            this.mediaSource = createMediaSource(getIntent().getData());
        } else if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            if ("text/plain".equals(getIntent().getType())) {
                String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    this.mediaSource = createMediaSource(Uri.parse(text));
                }
            } else {
                this.mediaSource = createMediaSource(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
            }
        }

        if (this.mediaSource == null) {
            finish();
        }

        setContentView(R.layout.activity_player);

        this.playerView = findViewById(R.id.player_view);

        this.videoTrackButton = initializeButton(R.id.oxe_track_video);
        this.audioTrackButton = initializeButton(R.id.oxe_track_audio);
        this.textTrackButton = initializeButton(R.id.oxe_track_text);
        this.resizeModeButton = initializeButton(R.id.oxe_resize_mode);

        this.playerView.setControllerVisibilityListener(this);

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        if (savedInstanceState != null) {
            this.lastTrackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            this.lastWindow = savedInstanceState.getInt(KEY_WINDOW);
            this.lastPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            this.lastTrackSelectorParameters = null;
            this.lastWindow = C.INDEX_UNSET;
            this.lastPosition = C.TIME_UNSET;
        }
    }

    private ImageButton initializeButton(int id) {
        ImageButton button = findViewById(id);
        button.setOnClickListener(this);
        return button;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, this.lastTrackSelectorParameters);
        outState.putInt(KEY_WINDOW, this.lastWindow);
        outState.putLong(KEY_POSITION, this.lastPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializePlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    private void initializePlayer() {
        if (this.player != null) {
            return;
        }

        this.trackSelector = new DefaultTrackSelector();
        if (this.lastTrackSelectorParameters != null) {
            this.trackSelector.setParameters(this.lastTrackSelectorParameters);
        }

        this.player = ExoPlayerFactory.newSimpleInstance(this, this.trackSelector);
        this.player.setPlayWhenReady(true);
        this.player.addListener(this);
        this.playerView.setPlayer(this.player);

        if (this.lastWindow != C.INDEX_UNSET) {
            this.player.seekTo(this.lastWindow, this.lastPosition);
            this.player.prepare(this.mediaSource, false, false);
        } else {
            this.player.prepare(this.mediaSource, true, false);
        }

        updateTrackButtons();
    }

    private void releasePlayer() {
        if (this.player == null) {
            return;
        }

        this.lastTrackSelectorParameters = this.trackSelector.getParameters();
        this.lastWindow = this.player.getCurrentWindowIndex();
        this.lastPosition = this.player.getContentPosition();

        this.playerView.setPlayer(null);
        this.player.release();
        this.player = null;
        this.trackSelector = null;
    }

    private MediaSource createMediaSource(Uri uri) {
        if (uri == null) {
            return null;
        }

        @ContentType int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(this.dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(this.dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(this.dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(this.dataSourceFactory)
                        .createMediaSource(uri);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }


    @Override
    public void onClick(View v) {
        if (v == this.videoTrackButton || v == this.audioTrackButton || v == this.textTrackButton) {
            showTrackSelector((ImageButton) v);
        } else if (v == this.resizeModeButton) {
            this.playerView.setResizeMode((this.playerView.getResizeMode() + 1) % AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        }
    }

    private void updateTrackButtons() {
        this.videoTrackButton.setVisibility(View.GONE);
        this.audioTrackButton.setVisibility(View.GONE);
        this.textTrackButton.setVisibility(View.GONE);

        if (this.trackSelector == null) {
            return;
        }

        MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                View button = getTrackButton(mappedTrackInfo.getRendererType(i));
                if (button != null) {
                    button.setTag(i);
                    button.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private View getTrackButton(int rendererType) {
        switch (rendererType) {
            case C.TRACK_TYPE_VIDEO:
                return this.videoTrackButton;
            case C.TRACK_TYPE_AUDIO:
                return this.audioTrackButton;
            case C.TRACK_TYPE_TEXT:
                return this.textTrackButton;
            default:
                return null;
        }
    }

    private void showTrackSelector(ImageButton button) {
        if (button.getTag() == null) {
            return;
        }

        new TrackSelectionDialogBuilder(this, button.getContentDescription(),
                this.trackSelector, (int) button.getTag())
                .setShowDisableOption(true)
                .setAllowAdaptiveSelections(true)
                .build().show();
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        updateTrackButtons();
    }

    @Override
    public void onVisibilityChange(int visibility) {
        if (visibility == View.GONE) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LOW_PROFILE
            );
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            this.playerView.showController();
        }
    }

}
