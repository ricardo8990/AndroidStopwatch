package g54mdp.sanchez.ricardo.stopwatch;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
{
    //region Class fields
    private StopwatchService.StopwatchBinder binder = null;    //This binder is the connection with the service
    private boolean isServiceBound = false;                    //Indicates if the service has been binded otherwise would throw an exception
    private TimeStamp time;                                    //The current time showed in the interface used to update the interface
    private ArrayList<String> laps;                            //Number of laps in a printable format used to update the interface

    //It receives the messages from the service in order to update the numbers in the interface
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //When a value is received from the service, update the time field and the numbers in the user interface
            time = intent.getParcelableExtra(Constants.BroadCastMessageTime);
            updateNumbers();
        }
    };

    //Connection used to bind the service
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        /**
         * Method called when the service is bounded, we need to update the interface according to
         * the state of the stopwatch, number of laps and the current count.
         */
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            binder = (StopwatchService.StopwatchBinder) service;
            isServiceBound = true;

            //Update the numbers in the interface
            time = binder.GetCurrentTime();
            updateNumbers();
            //Update the listView which contains the laps
            laps = binder.GetCurrentLaps();
            updateLaps();
            //Update the buttons showed to the user according to the state of the stopwatch
            Stopwatch.State state = binder.GetCurrentState();
            switch (state)
            {
                case Active:
                    showButtons(false, true, false, true, false);
                    break;
                case Paused:
                    showButtons(true, false, true, false, true);
                    break;
                case Stopped:
                    showButtons(true, false, false, false, false);
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            binder = null;
            isServiceBound = false;
        }
    };
    //endregion

    //region Override methods
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        laps = new ArrayList<>();
        //Recover the list of laps in case the orientation changed and the list was stored in the Bundle
        if(savedInstanceState != null)
        {
            laps = savedInstanceState.getStringArrayList(Constants.BundleLapsString);
            updateLaps();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        //Start the service such that it is not going to be closed when the application is destroyed
        Intent intent = new Intent(this, StopwatchService.class);
        startService(intent);

        //Bind to the service, in case this was already running it will update the interface in the service connection
        this.bindService(new Intent(this, StopwatchService.class), serviceConnection, 0);
        //Register to the local broadcast to start receiving the numbers from the service
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(Constants.BroadcastActionUpdateTime));
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        //In case the user exit the application and the stopwatch is stopped, then stop the service
        //We only need it when there is a task running
        if(isServiceBound && binder.GetCurrentState() == Stopwatch.State.Stopped)
        {
            Intent intent = new Intent(this, StopwatchService.class);
            stopService(intent);
        }

        //Always unbind the service such that it is not going to throw an exception when trying to bind again next time
        unbindService(serviceConnection);
        isServiceBound = false;

        //Unregister the local broadcast, we do not need it anymore at this point
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        //The only value we need to keep is the number of laps due to the numbers are updated
        //By the service via the local broadcast
        outState.putStringArrayList(Constants.BundleLapsString, laps);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Methods used by the buttons
    public void startStopWatch(View view)
    {
        if(!isServiceBound)
        {
            Log.d(Constants.LogDTag, "Service was not bound");
            return;
        }
        binder.StartStopwatch();
        showButtons(false, true, false, true, false);
    }

    public void pauseStopwatch(View view)
    {
        if(!isServiceBound)
        {
            Log.d(Constants.LogDTag, "Service was not bound");
            return;
        }

        binder.PauseStopwatch();
        showButtons(true, false, true, false, true);
    }

    public void stopStopwatch(View view)
    {
        if(!isServiceBound)
        {
            Log.d(Constants.LogDTag, "Service was not bound");
            return;
        }

        binder.StopStopwatch();
        //Reset the laps and time and update the interface
        laps = new ArrayList<>();
        updateLaps();
        time = new TimeStamp();
        updateNumbers();
        showButtons(true, false, false, false, false);
    }

    public void newLap(View view)
    {
        if(!isServiceBound)
        {
            Log.d(Constants.LogDTag, "Service was not bound");
            return;
        }

        laps = binder.CreateLap();
        updateLaps();
    }

    public void shareLaps(View view)
    {
        if(!isServiceBound)
        {
            Log.d(Constants.LogDTag, "Service was not bound");
            return;
        }

        //Get the current time and the laps from the service
        TimeStamp totalTime = binder.GetCurrentTime();
        ArrayList<String> laps = binder.GetCurrentLaps();
        //Create the intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(Constants.ShareContentType);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, (getString(R.string.share_title)).replace(Constants.FlagToReplaceTime, totalTime.toString(true)));
        //If there is no laps, only shows the total time
        if(laps != null && laps.size() > 0)
            shareIntent.putExtra(Intent.EXTRA_TEXT, TextUtils.join("\n", laps));
        //Send the intent managed by android to show the share window
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_head_text)));
    }
    //endregion

    //region Methods used to update the user interface

    private void updateNumbers()
    {
        //This method updates the numbers in the interface using the time field
        TextView hours = (TextView) findViewById(R.id.hours);
        TextView minutes = (TextView) findViewById(R.id.minutes);
        TextView seconds = (TextView) findViewById(R.id.seconds);
        TextView milliseconds = (TextView) findViewById(R.id.milliseconds);
        hours.setText(Integer.toString(time.Hour));
        minutes.setText(Integer.toString(time.Minute));
        seconds.setText(Integer.toString(time.Second));
        milliseconds.setText(Integer.toString(time.Millisecond));
    }

    private void updateLaps()
    {
        //Update the list of laps using the Laps field
        ListView listView = (ListView) findViewById(R.id.lapList);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_white_text, R.id.list_content, laps);
        listView.setAdapter(adapter);
    }

    private void showButtons(boolean showStart, boolean showPause, boolean showStop, boolean showLaps, boolean showShare)
    {
        //It will show or hide the buttons according to the parameters
        ImageButton start = (ImageButton) findViewById(R.id.buttonStart);
        ImageButton pause = (ImageButton) findViewById(R.id.buttonPause);
        ImageButton newLap = (ImageButton) findViewById(R.id.buttonNewLap);
        ImageButton stop = (ImageButton) findViewById(R.id.buttonStop);
        ImageButton share = (ImageButton) findViewById(R.id.buttonShare);
        start.setVisibility(showStart ? View.VISIBLE : View.GONE);
        pause.setVisibility(showPause ? View.VISIBLE : View.GONE);
        newLap.setVisibility(showLaps ? View.VISIBLE : View.GONE);
        stop.setVisibility(showStop ? View.VISIBLE : View.GONE);
        share.setVisibility(showShare ? View.VISIBLE : View.GONE);
    }
    //endregion
}