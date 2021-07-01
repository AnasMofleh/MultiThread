package chat.client;

/** A headless chat client for logging results. Used in tests. */
public class ChatLog extends AbstractClient {

    private int nbrTopics = 0;
    private int nbrMessages = 0;

    // -----------------------------------------------------------------------

    private ChatLog() {
        super("logging_client");
    }

    public static void expect(int expectedNbrTopics, int expectedNbrMessages) throws InterruptedException {
        ChatLog logger = new ChatLog();
        logger.start();
        logger.awaitResults(expectedNbrTopics, expectedNbrMessages);
        logger.logOut();
    }
    
    // -----------------------------------------------------------------------
    
    // determines how often update() below is called
    private static final long REPORTING_PERIOD = 1000;
    
    /**
     * Blocks until the indicated number of topics and messages have been reported.
     * If results are delayed, a status report is printed every REPORTING_PERIOD
     * milliseconds.
     */
    private synchronized void awaitResults(int expectedNbrTopics, int expectedNbrMessages) throws InterruptedException {
        long lastUpdate = System.currentTimeMillis();

        while (nbrTopics < expectedNbrTopics || nbrMessages < expectedNbrMessages) {
            wait(REPORTING_PERIOD);
            long now = System.currentTimeMillis();
            if (now >= lastUpdate + REPORTING_PERIOD) {
                report(expectedNbrTopics, expectedNbrMessages, "WAITING");
                lastUpdate = now;
            }
        }
        report(expectedNbrTopics, expectedNbrMessages, "OK");
    }

    // -----------------------------------------------------------------------

    private void report(int expectedNbrTopics, int expectedNbrMessages, String status) {
        System.out.printf("  %5d/%-5d topics  |  %5d/%-5d messages  |  ==> %s\n",
                nbrTopics, expectedNbrTopics,
                nbrMessages, expectedNbrMessages,
                status);
    }

    // =======================================================================
    
    @Override
    protected synchronized void onNewTopic(int topicId, int nbrMessages, String username, String text) {
        nbrTopics++;
        this.nbrMessages += nbrMessages;
        notifyAll();
    }

    @Override
    protected synchronized void onNewMessage(int topicId, int messageId, String username, String text) {
        // nevermind
    }
}
