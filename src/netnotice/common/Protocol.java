package netnotice.common;

/**
 * Shared protocol constants for NetNotice communication.
 * Module: EC5205 - Operating System and Network Programming
 */
public class Protocol {

    // Default connection parameters
    public static final int DEFAULT_PORT = 5000;
    public static final String DEFAULT_HOST = "127.0.0.1";

    // Client -> Server
    public static final String PREFIX_JOIN = "JOIN|";
    public static final String PREFIX_MESSAGE = "MESSAGE|";
    public static final String PREFIX_DISCONNECT = "DISCONNECT";
    public static final String PREFIX_ANSWER = "ANSWER|";           // Client submits poll answer: ANSWER|optionIndex

    // Server -> Client
    public static final String PREFIX_JOINED = "JOINED|";
    public static final String PREFIX_ANNOUNCEMENT = "ANNOUNCEMENT|";
    public static final String PREFIX_ERROR = "ERROR|";
    public static final String PREFIX_POLL = "POLL|";               // Server sends poll: POLL|question|opt1|opt2|...
    public static final String PREFIX_POLL_RESULT = "POLL_RESULT|"; // Live results: POLL_RESULT|count1|count2|...
    public static final String PREFIX_POLL_END = "POLL_END|";       // Poll ended: POLL_END|count1|count2|...
}
