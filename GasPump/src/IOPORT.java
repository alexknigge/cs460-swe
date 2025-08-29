/*    */

public interface IOPORT {
    /* Send a message */
    public static void send(Message msg);
    /* Get message and destory immediately */
    public static Message get();
    /* Get message but don't destory */
    public static Message read();
}

