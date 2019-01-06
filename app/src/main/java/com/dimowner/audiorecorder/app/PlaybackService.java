package com.dimowner.audiorecorder.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.dimowner.audiorecorder.ARApplication;
import com.dimowner.audiorecorder.ColorMap;
import com.dimowner.audiorecorder.R;
import com.dimowner.audiorecorder.app.main.MainActivity;
import com.dimowner.audiorecorder.audio.player.PlayerContract;
import com.dimowner.audiorecorder.exception.AppException;
import com.dimowner.audiorecorder.util.FileUtil;

import timber.log.Timber;

public class PlaybackService extends Service {

	private final static String CHANNEL_NAME = "Default";
	private final static String CHANNEL_ID = "com.dimowner.audiorecorder.NotificationId";

	public static final String ACTION_START_PLAYBACK_SERVICE = "ACTION_START_PLAYBACK_SERVICE";

	public static final String ACTION_STOP_PLAYBACK_SERVICE = "ACTION_STOP_PLAYBACK_SERVICE";

	public static final String ACTION_PAUSE_PLAYBACK = "ACTION_PAUSE_PLAYBACK";

	public static final String ACTION_PLAY_NEXT = "ACTION_PLAY_NEXT";

	public static final String ACTION_PLAY_PREV = "ACTION_PLAY_PREV";

	public static final String ACTION_CLOSE = "ACTION_CLOSE";

	public static final String EXTRAS_KEY_RECORD_NAME = "record_name";

	private static final int NOTIF_ID = 101;
	private NotificationCompat.Builder builder;
	private NotificationManager notificationManager;
	private RemoteViews remoteViewsSmall;
	private RemoteViews remoteViewsBig;
	private Notification notification;
	private String recordName = "";

	private PlayerContract.Player audioPlayer;
	private ColorMap colorMap;

	public PlaybackService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		audioPlayer = ARApplication.getInjector().provideAudioPlayer();
		colorMap = ARApplication.getInjector().provideColorMap();
		PlayerContract.PlayerCallback playerCallback = new PlayerContract.PlayerCallback() {
				@Override public void onPreparePlay() {}
				@Override public void onPlayProgress(final long mills) {}
				@Override public void onStopPlay() {}
				@Override public void onSeek(long mills) {}
				@Override public void onError(AppException throwable) {}

				@Override
				public void onStartPlay() {
					onStartPlayback();
				}

				@Override
				public void onPausePlay() {
					onPausePlayback();
				}
			};

		this.audioPlayer.addPlayerCallback(playerCallback);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.hasExtra(EXTRAS_KEY_RECORD_NAME)) {
				recordName = intent.getStringExtra(EXTRAS_KEY_RECORD_NAME);
			}

			String action = intent.getAction();
			if (action != null && !action.isEmpty()) {
				switch (action) {
					case ACTION_START_PLAYBACK_SERVICE:
						Timber.v("ACTION_START_PLAYBACK_SERVICE");
						startForegroundService();
						break;
					case ACTION_STOP_PLAYBACK_SERVICE:
						Timber.v("ACTION_STOP_PLAYBACK_SERVICE");
						stopForegroundService();
						break;
					case ACTION_PAUSE_PLAYBACK:
						Timber.v("ACTION_PAUSE_PLAYBACK");
						audioPlayer.playOrPause();
						break;
					case ACTION_CLOSE:
						Timber.v("ACTION_CLOSE");
						audioPlayer.stop();
						stopForegroundService();
						break;
					case ACTION_PLAY_NEXT:
						Timber.v("ACTION_PLAY_NEXT");
						break;
					case ACTION_PLAY_PREV:
						Timber.v("ACTION_PLAY_PREV");
						if (audioPlayer.isPlaying()) {
							audioPlayer.pause();
						}
						break;
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void startForegroundService() {
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(CHANNEL_ID, CHANNEL_NAME);
		}

		remoteViewsSmall = new RemoteViews(getPackageName(), R.layout.layout_play_notification_small);
		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_pause, getPendingSelfIntent(getApplicationContext(), ACTION_PAUSE_PLAYBACK));
		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_close, getPendingSelfIntent(getApplicationContext(), ACTION_CLOSE));
//		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_next, getPendingSelfIntent(getApplicationContext(), ACTION_PLAY_NEXT));
//		remoteViewsSmall.setOnClickPendingIntent(R.id.btn_prev, getPendingSelfIntent(getApplicationContext(), ACTION_PLAY_PREV));
//		remoteViewsSmall.setTextViewText(R.id.txt_playback_progress, TimeUtils.formatTimeIntervalMinSecMills(0));
		remoteViewsSmall.setTextViewText(R.id.txt_name, FileUtil.removeFileExtension(recordName));
		remoteViewsSmall.setInt(R.id.container, "setBackgroundColor", this.getResources().getColor(colorMap.getPrimaryColorRes()));

		remoteViewsBig = new RemoteViews(getPackageName(), R.layout.layout_play_notification_big);
		remoteViewsBig.setOnClickPendingIntent(R.id.btn_pause, getPendingSelfIntent(getApplicationContext(), ACTION_PAUSE_PLAYBACK));
		remoteViewsBig.setOnClickPendingIntent(R.id.btn_close, getPendingSelfIntent(getApplicationContext(), ACTION_CLOSE));
//		remoteViewsBig.setOnClickPendingIntent(R.id.btn_next, getPendingSelfIntent(getApplicationContext(), ACTION_PLAY_NEXT));
//		remoteViewsBig.setOnClickPendingIntent(R.id.btn_prev, getPendingSelfIntent(getApplicationContext(), ACTION_PLAY_PREV));
//		remoteViewsBig.setTextViewText(R.id.txt_playback_progress, TimeUtils.formatTimeIntervalMinSecMills(0));
		remoteViewsBig.setTextViewText(R.id.txt_name, FileUtil.removeFileExtension(recordName));
		remoteViewsBig.setInt(R.id.container, "setBackgroundColor", this.getResources().getColor(colorMap.getPrimaryColorRes()));

		// Create notification default intent.
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		// Create notification builder.
		builder = new NotificationCompat.Builder(this, CHANNEL_ID);

		builder.setWhen(System.currentTimeMillis());
		builder.setContentTitle(getResources().getString(R.string.app_name));
		builder.setSmallIcon(R.drawable.ic_play_circle);
		builder.setPriority(Notification.PRIORITY_MAX);
		// Make head-up notification.
		builder.setContentIntent(pendingIntent);
		builder.setCustomContentView(remoteViewsSmall);
		builder.setCustomBigContentView(remoteViewsBig);
		builder.setOngoing(true);
		builder.setOnlyAlertOnce(true);
		builder.setDefaults(0);
		builder.setSound(null);
		notification = builder.build();
		startForeground(NOTIF_ID, notification);
	}

	private void stopForegroundService() {
		audioPlayer = null;
		stopForeground(true);
		stopSelf();
	}

	protected PendingIntent getPendingSelfIntent(Context context, String action) {
		Intent intent = new Intent(context, StopPlaybackReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 10, intent, 0);
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(String channelId, String channelName) {
		NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
		if (channel == null) {
			Timber.v("Create notification channel: " + CHANNEL_ID);
			NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
			chan.setLightColor(Color.BLUE);
			chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			chan.setSound(null,null);
			chan.enableLights(false);
			chan.enableVibration(false);

			notificationManager.createNotificationChannel(chan);
		} else {
			Timber.v("Channel already exists: " + CHANNEL_ID);
		}
		return channelId;
	}

	public void onPausePlayback() {
		Timber.v("onPausePlayback");
		if (remoteViewsBig != null && remoteViewsSmall != null) {
//		remoteViewsBig.setInt(R.id.container, "setBackgroundColor", this.getResources().getColor(colorMap.getPrimaryColorRes()));
			remoteViewsBig.setImageViewResource(R.id.btn_pause, R.drawable.ic_play);
			remoteViewsSmall.setImageViewResource(R.id.btn_pause, R.drawable.ic_play);
			builder.setOngoing(false);
			notificationManager.notify(NOTIF_ID, builder.build());
		}
	}

	public void onStartPlayback() {
		Timber.v("onStartPlayback");
		if (remoteViewsBig != null && remoteViewsSmall != null) {
			remoteViewsBig.setImageViewResource(R.id.btn_pause, R.drawable.ic_pause);
			remoteViewsSmall.setImageViewResource(R.id.btn_pause, R.drawable.ic_pause);
			builder.setOngoing(true);
			notificationManager.notify(NOTIF_ID, builder.build());
		}
	}

	public static class StopPlaybackReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent stopIntent = new Intent(context, PlaybackService.class);
			stopIntent.setAction(intent.getAction());
			context.startService(stopIntent);
		}
	}
}
