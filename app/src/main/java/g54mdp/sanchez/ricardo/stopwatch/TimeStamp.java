package g54mdp.sanchez.ricardo.stopwatch;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.concurrent.TimeUnit;

/**
 * As I could not find a class which could get a time from milliseconds, I created this one
 */
public class TimeStamp implements Parcelable
{
    //region Fields
    public int Day;
    public int Hour;
    public int Minute;
    public int Second;
    public int Millisecond;
    //endregion

    //region Constructors
    public TimeStamp()
    {
        this.Day = 0;
        this.Hour = 0;
        this.Minute = 0;
        this.Second = 0;
        this.Millisecond = 0;
    }

    public TimeStamp(long millisecond)
    {
        long days = TimeUnit.MILLISECONDS.toDays(millisecond);
        this.Day = (int) days;
        this.Hour = (int) (TimeUnit.MILLISECONDS.toHours(millisecond) % TimeUnit.DAYS.toHours(1));
        this.Minute = (int) (TimeUnit.MILLISECONDS.toMinutes(millisecond) % TimeUnit.HOURS.toMinutes(1));
        this.Second = (int) (TimeUnit.MILLISECONDS.toSeconds(millisecond) % TimeUnit.MINUTES.toSeconds(1));
        this.Millisecond = (int) (millisecond % TimeUnit.SECONDS.toMillis(1));
    }

    public TimeStamp(Parcel parcel)
    {
        int[] array = new int[5];
        parcel.readIntArray(array);
        this.Day = array[0];
        this.Hour = array[1];
        this.Minute = array[2];
        this.Second = array[3];
        this.Millisecond = array[4];
    }
    //endregion

    //region Methods used to parcel the information such that can be send it through the local broadcast

    static final Creator<TimeStamp> CREATOR = new Creator<TimeStamp>()
    {
        @Override
        public TimeStamp createFromParcel(Parcel source)
        {
            return new TimeStamp(source);
        }

        @Override
        public TimeStamp[] newArray(int size)
        {
            return new TimeStamp[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeIntArray(new int[]{Day, Hour, Minute, Second, Millisecond});
    }
    //endregion

    public String toString(boolean showMilliseconds)
    {
        return this.Hour + ":" + this.Minute + ":" + this.Second + (showMilliseconds ? "." + this.Millisecond : "");
    }
}
