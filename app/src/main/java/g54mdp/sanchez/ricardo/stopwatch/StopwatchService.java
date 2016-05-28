package g54mdp.sanchez.ricardo.stopwatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

/**
 * Service created to keep the stopwatch running in the background even if the application is closed
 */
public class StopwatchService extends Service
{
    private final IBinder binder = new StopwatchBinder();
    private Stopwatch stopwatch;
    LocalBroadcastManager broadcastManager;
    BroadcastReceiver receiver;

    //region Main thread used to update the time
    /**
     * Service uses a thread to update the time, then it sends the update to the application via a
     * local broadcast or displays a notification in case the application is not running
     */
    Thread timeThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            long previousTime = 0;
            while(!Thread.interrupted())
            {
                //Give a time before the next action, otherwise it uses too much processing power
                synchronized (this)
                {
                    try
                    {
                        wait(10);
                    }catch (Exception ignored) { }
                }

                //Act according to the stopwatch state
                switch (stopwatch.state)
                {
                    case Active:
                        //Increment the stopwatch and display the notification (notification is only
                        //updated each second) or send the local broadcast
                        stopwatch.Increment();
                        if(stopwatch.isUsingNotification && stopwatch.totalTime - previousTime > 1000)
                        {
                            updateNotification(stopwatch.GetTotalTimeStamp());
                            previousTime = stopwatch.totalTime;
                        }
                        else
                            sendTimeViaBroadcast(stopwatch.GetTotalTimeStamp());
                        break;

                    case Paused:
                        stopwatch.Pause();
                        break;

                    case Stopped:
                        break;
                }
            }
        }
    });
    //endregion

    //region Override methods
    @Override
    public IBinder onBind(Intent intent)
    {
        stopForeground(true);           //When the service is bind we don't need to show the notification anymore
        return binder;                  //Even more the service wont be killed
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        //Start updating the notification
        stopwatch.isUsingNotification = true;
        //Create the notification
        Notification notification = GetNotificationBuilder(this, stopwatch.GetTotalTimeStamp()).build();
        //Start the service as a foreground such that wont be killed even if the application is
        //destroyed or swiped from the list of current applications
        startForeground(Constants.NotificationId, notification);
        super.onUnbind(intent);
        return true;
    }

    @Override
    public void onRebind(Intent intent)
    {
        stopwatch.isUsingNotification = false;
        stopForeground(true);
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;    //Sticky to create the service again if it is destroyed by the sys
    }

    @Override
    public void onCreate()
    {
        stopwatch = new Stopwatch();
        timeThread.start();         //Initialize the main thread
        broadcastManager = LocalBroadcastManager.getInstance(this);  //Manager to send the local broadcasts
        //Register the service to listen for broadcasts send by the notification buttons or widget
        receiver = new ActionReceiver();
        registerReceiver(receiver, new IntentFilter(Constants.BroadcastActionUpdateAction));
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(receiver);
        timeThread.interrupt();
    }
    //endregion

    //region Help methods

    //Method used when a broadcast is received, it performs an action over the stopwatch
    private void updateStateFromBroadcast(Action action)
    {
        switch (action)
        {
            case Start:
                this.stopwatch.Start();
                break;
            case NewLap:
                this.stopwatch.CreateNewLap();
                break;
            case Pause:
                this.stopwatch.Pause();
                updateNotification(stopwatch.GetTotalTimeStamp());
                break;
        }
    }

    //Method used to send the local broadcast to the application such that it can update the UI
    public void sendTimeViaBroadcast(TimeStamp time)
    {
        Intent intent = new Intent(Constants.BroadcastActionUpdateTime);
        intent.putExtra(Constants.BroadCastMessageTime, time);
        broadcastManager.sendBroadcast(intent);
    }

    //When the application is not running, update the notification
    private void updateNotification(TimeStamp time)
    {
        NotificationCompat.Builder builder = GetNotificationBuilder(this, time);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Constants.NotificationId, builder.build());
    }

    //It creates the notification builder used to update or create a new notification
    NotificationCompat.Builder GetNotificationBuilder(Context context, TimeStamp time)
    {
        //Notification with the main info (Icon, title and text)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stopwatch_running);
        builder.setContentTitle(time.toString(false));
        builder.setContentText(stopwatch.laps.size() > 0 ? getString(R.string.lap) + stopwatch.laps.size() : "");
        builder.setVisibility(1); //In case of Lollipop this will show the information in the lock screen

        //When the notification is clicked open the main activity, this is stored in the notification
        //As a PendingIntent
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        //If the stopwatch is running, show the pause and new lap buttons
        if(stopwatch.state == Stopwatch.State.Active)
        {
            //Create the broadcast pointing for a new lap action
            Intent newLapIntent = new Intent(Constants.BroadcastActionUpdateAction);
            newLapIntent.putExtra(Constants.BroadcastMessageAction, 1);
            PendingIntent newLapPendingIntent = PendingIntent.getBroadcast(context, 0, newLapIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.mipmap.ic_new_lap, getString(R.string.new_lap), newLapPendingIntent);

            //Create the broadcast to a pause action
            Intent pauseIntent = new Intent(Constants.BroadcastActionUpdateAction);
            pauseIntent.putExtra(Constants.BroadcastMessageAction, 2);
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.mipmap.ic_pause, getString(R.string.Pause), pausePendingIntent);
        }
        else if (stopwatch.state == Stopwatch.State.Paused)         //Otherwise, show the restart and stop buttons
        {
            //Create the PendingIntent to open the application
            Intent stopIntent = new Intent(context, MainActivity.class);
            PendingIntent stopPendingIntent = PendingIntent.getActivity(context, 2, stopIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.mipmap.ic_stop, getString(R.string.Stop), stopPendingIntent);

            //Create the broadcast to start the stopwatch again
            Intent startIntent = new Intent(Constants.BroadcastActionUpdateAction);
            startIntent.putExtra(Constants.BroadcastMessageAction, 0);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(context, 3, startIntent, PendingIntent.FLAG_ONE_SHOT);
            builder.addAction(R.mipmap.ic_start, getString(R.string.start), startPendingIntent);
        }

        return builder;
    }
    //endregion

    //region Inner classes implemented

    /**
     * This class will be used to link with the application, it contains the methods required to
     * start, pause, stop and get some information useful
     */
    public class StopwatchBinder extends Binder
    {
        void StartStopwatch()
        {
            stopwatch.Start();
        }

        void PauseStopwatch()
        {
            stopwatch.state = Stopwatch.State.Paused;
        }

        ArrayList<String> CreateLap()
        {
            return stopwatch.CreateNewLap();
        }

        public TimeStamp GetCurrentTime()
        {
            return stopwatch.GetTotalTimeStamp();
        }

        public ArrayList<String> GetCurrentLaps()
        {
            return stopwatch.GetCurrentLaps();
        }

        public Stopwatch.State GetCurrentState()
        {
            return stopwatch.state;
        }

        public void StopStopwatch()
        {
            stopwatch = new Stopwatch();
        }
    }

    /**
     * This class will receive the broadcast send by the notification buttons or the widget
     * in order to perform an action on the stopwatch (Pause, Run, New Lap)
     */
    public class ActionReceiver extends BroadcastReceiver
    {
        public ActionReceiver(){}

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Action action = Action.values()[intent.getIntExtra(Constants.BroadcastMessageAction, 0)];
            updateStateFromBroadcast(action);
        }
    }

    /**
     * Actions available to be used in the broadcast
     */
    public enum Action
    {
        Start,
        NewLap,
        Pause
    }
    //endregion
}
