package g54mdp.sanchez.ricardo.stopwatch;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

/**
 * This class will keep the counting and the main operations
 * The logic about the stopwatch is here apart from the service
 */
public class Stopwatch
{
    volatile long startTime;
    volatile long totalTime;
    volatile long previousTotalTime;
    volatile State state;
    volatile boolean isUsingNotification;
    List<Long> laps;

    Stopwatch()
    {
        //When the stopwatch is stopped, delete the laps and update the state
        laps = new ArrayList<>();
        state = State.Stopped;
    }

    ArrayList<String> CreateNewLap()
    {
        laps.add(totalTime);
        return GetCurrentLaps();
    }

    void Start()
    {
        state = Stopwatch.State.Active;
        startTime = SystemClock.elapsedRealtime();
    }

    void Increment()
    {
        totalTime = previousTotalTime + (SystemClock.elapsedRealtime() - startTime);
    }

    void Pause()
    {
        previousTotalTime = totalTime;
        state = State.Paused;
    }

    TimeStamp GetTotalTimeStamp()
    {
        return new TimeStamp(totalTime);
    }

    public ArrayList<String> GetCurrentLaps()
    {
        //Return the list of laps in a printable format.
        ArrayList<String> values = new ArrayList<>();

        for(int r = 0; r < laps.size(); r++)
        {
            TimeStamp previousValue = new TimeStamp(laps.get(r) - ((r == 0) ? 0 : laps.get(r - 1)));
            TimeStamp currentTime = new TimeStamp(laps.get(r));
            values.add("# " + (r + 1) + "   " + previousValue.toString(true) + "   " + currentTime.toString(true));
        }

        return values;
    }

    public enum State
    {
        Active,
        Paused,
        Stopped
    }

}
