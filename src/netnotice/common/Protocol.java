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

    // Server -> Client
    public static final String PREFIX_JOINED = "JOINED|";
    public static final String PREFIX_ANNOUNCEMENT = "ANNOUNCEMENT|";
    public static final String PREFIX_ERROR = "ERROR|";

    // ========================================================
    // MEMBER 3 HOOKS: Poll & Quiz Response System
    // (Reserved for Member 3 implementation)
    // ========================================================
    // public static final String PREFIX_QUESTION = "QUESTION|";
    // public static final String PREFIX_ANSWER = "ANSWER|";
    // public static final String PREFIX_RESULTS = "RESULTS|";
}
