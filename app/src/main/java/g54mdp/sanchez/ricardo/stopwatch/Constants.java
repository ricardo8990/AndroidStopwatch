package g54mdp.sanchez.ricardo.stopwatch;

/**
 * This class will contain all constants defined in the code in order to make it easier to apply
 * any new change
 */
public class Constants
{
    //region Constants used in the MainActivity

    public static String BundleLapsString = "laps";
    public static String ShareContentType = "text/plain";
    public static String FlagToReplaceTime = "{totalTime}";
    public static String LogDTag = "g54mdp";

    //endregion

    //region Constants used by the Service

    public static int NotificationId = 1;
    public static String BroadcastActionUpdateTime = "g54mdp.sanchez.ricardo.stopwatch.sendTimeViaBroadcast";
    public static String BroadcastActionUpdateAction = "g54mdp.sanchez.ricardo.stopwatch.updateAction";
    public static String BroadCastMessageTime = "time";
    public static String BroadcastMessageAction = "action";

    //endregion
}
