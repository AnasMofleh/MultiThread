package chat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A headless chat bot. Used in tests. */
public class Twit extends AbstractClient {
    private Random rand = new Random();

    private final String username;
    private final String noun;
    private final String adjective;

    private final int nbrMessages;
    private final long delay;
    
    /**
     * Creates a bot with the given username, posting _nbrMessages_ messages with up
     * to _delay_ millseconds between them.
     */
    public Twit(String username, int nbrMessages, long delay) {
        super(username);
        this.username = username;
        noun = nouns.pick();
        adjective = adjectives.pick();
        this.nbrMessages = nbrMessages - 1;    // the topic already includes a message
        this.delay = delay;
    }

    // ---------------------------------------------------------------------------------------

    @Override
    public void start() {
        super.start();
        new Thread(this::produceIncoherentBullshit).start();
    }

    // =======================================================================================

    private void produceIncoherentBullshit() {
        try {
            String heading = createMessage("", introductions, shortStatements);
            createTopic(heading);

            for (int i = 0; i < nbrMessages; i++) {
                Topic topic = topics.awaitTopic();
                selectTopic(topic.id);
                Message m = topics.awaitMessageFrom(topic, delay);
                if (m == null) {
                    String phrase = createMessage("", shortStatements, longStatements);
                    postMessage(phrase);
                } else {
                    String response = createMessage(m.username, shortStatements, longStatements, responses);
                    postMessage(response);
                }
            }
            logOut();   // allow messages to be distributed before we close the socket
        } catch (InterruptedException ie) {
            // interruption used for shutting thread down
        }
    }

    @SafeVarargs
    private final String createMessage(String user, RandomSet<String>... candidates) {
        List<RandomSet<String>> sets = new ArrayList<>();
        int n = 0;
        for (RandomSet<String> set : candidates) {
            sets.add(set);
            n += set.size();
        }
        
        long r = rand.nextInt(n);
        int m = 0;
        for (int i = 0; i < sets.size(); i++) {
            m += sets.get(i).size(); 
            if (r < m) {
                return sets.get(i).pick()
                        .replace("$NOUN", noun)
                        .replace("$ADJ", adjective)
                        .replace("$USER", user);
            }
        }
        
        throw new Error("shouldn't happen");
    }
    
    // -----------------------------------------------------------------------

    private class RandomSet<T> {
        private final List<T> a;
        private int available;

        @SafeVarargs
        public RandomSet(T... a) {
            this.a = Arrays.asList(a);
            available = a.length;
        }

        public synchronized T pick() {
            // shuffle values around, so the same
            // value is not repeated immediately
            int n = rand.nextInt(available);
            T result = a.get(n);
            a.set(n, a.get(available - 1));
            a.set(available - 1, result);
            available--;
            if (available == 0) {
                available = a.size();
            }
            return result;
        }
        
        public synchronized int size() {
            return a.size();
        }
    }

    // -----------------------------------------------------------------------

    private class Message {
        final String username;
        final String text;
        boolean done;

        Message(String username, String text) {
            this.username = username;
            this.text = text;
            done = username.equals(Twit.this.username);
        }
    }

    // -----------------------------------------------------------------------

    private class Topic {
        final int id;
        final List<Message> messages = new ArrayList<>();
        Topic(int id, String username, String text) {
            this.id = id;
            add(0, username, text);
        }

        boolean add(int messageId, String username, String text) {
            if (messageId == messages.size()) {
                messages.add(new Message(username, text));
                return true;
            } else {
                return false;
            }
        }

        Message pickMessage() {
            List<Message> unresponded = messages.stream().filter(m -> !m.done).collect(Collectors.toList());
            if (unresponded.isEmpty()) {
                return null;
            } else {
                Message m = unresponded.get(rand.nextInt(unresponded.size()));
                m.done = true;
                return m;
            }
        }
    }

    // -----------------------------------------------------------------------

    private class TopicSet {

        private final List<Topic> topics;

        public TopicSet() {
            this.topics = new ArrayList<>();
        }

        public synchronized void addTopic(int topicId, String username, String text) {
            if (topicId >= topics.size()) {
                Topic t = new Topic(topicId, username, text);
                topics.add(topicId, t);
                notifyAll();
            }
        }

        public synchronized void addMessage(int topicId, int messageId, String username, String text) {
            Topic t = topics.get(topicId);
            if (t.add(messageId, username, text)) {
                notifyAll();
            }
        }

        public synchronized Topic awaitTopic() throws InterruptedException {
            Predicate<Topic> unresponded = t -> t.pickMessage() == null; 
            while (! topics.stream().anyMatch(unresponded)) {
                wait();
            }
            List<Topic> candidates = topics.stream()
                    .filter(unresponded)
                    .collect(Collectors.toList());
            int n = rand.nextInt(candidates.size());
            return candidates.get(n);
        }

        public synchronized Message awaitMessageFrom(Topic t, long timeout) throws InterruptedException {
            Message m = null;
            long now = System.currentTimeMillis();
            long deadline = now + timeout;
            while ((m = t.pickMessage()) == null && now < deadline) {
                long dt = deadline - now;
                wait(dt);
                now = System.currentTimeMillis();
            }
            return m;
        }
    }

    // -----------------------------------------------------------------------
    
    private final TopicSet topics = new TopicSet();
    
    @Override
    protected void onNewTopic(int topicId, int nbrMessages, String username, String text) {
        topics.addTopic(topicId, username, text);
    }

    @Override
    protected void onNewMessage(int topicId, int messageId, String username, String text) {
        topics.addMessage(topicId, messageId, username, text);
    }

    @Override
    protected void onDisconnected(Throwable cause) {
        System.err.println("Twit '" + username + "' was disconnected:");
        cause.printStackTrace();
        System.exit(1);
    }
    
    // -----------------------------------------------------------------------

    private final RandomSet<String> adjectives = new RandomSet<>(
            "absurd",
            "ambiguous",
            "amusing",
            "astounding",
            "bio-degradable",
            "black-and-white",
            "bouncy",
            "bright",
            "chequered",
            "chocolate-covered",
            "concurrent",
            "conspicuous",
            "cromulent",
            "curly",
            "dazzling",
            "deadlock-free",
            "eccentric",
            "elated",
            "electric",
            "enthusiastic",
            "extra-terrestrial",
            "fearless",
            "flabbergasting",
            "fragrant",
            "friendly",
            "funny",
            "furry",
            "glistening",
            "helpful",
            "hilarious",
            "illustrious",
            "improbable",
            "jazzy",
            "jolly",
            "kind",
            "light-blue",
            "lovely",
            "lucky",
            "mind-boggling",
            "multi-colored",
            "multi-functional",
            "multi-threaded",
            "non-deterministic",
            "parallel",
            "peaceful",
            "pink",
            "polite",
            "programmable",
            "prominent",
            "purple",
            "quirky",
            "reasonably coherent",
            "remote-controlled",
            "rocket-driven",
            "salient",
            "sensible",
            "shiny",
            "singing",
            "skillful",
            "sleepy",
            "somewhat odd",
            "spiffy",
            "striped",
            "stupendous",
            "swift",
            "tasteful",
            "tiny",
            "truthful",
            "ultraviolet",
            "unconventional",
            "verbose",
            "visionary",
            "warm",
            "water-resistant",
            "well-tested",
            "wiggly",
            "yellow"
    );

    private final RandomSet<String> nouns = new RandomSet<>(
            "alarm clocks",
            "androids",
            "applications",
            "aprons",
            "bananas",
            "birdbaths",
            "budgies",
            "carpets",
            "classrooms",
            "compiler warnings",
            "computers",
            "cricket bats",
            "exams",
            "garden gnomes",
            "hats",
            "Java programs",
            "kittens",
            "labs",
            "Laplace transforms",
            "lawnmowing robots",
            "lifts",
            "machine learning algorithms",
            "message queues",
            "methods",
            "pianos",
            "poets",
            "programming languages",
            "puppies",
            "refrigerators",
            "rubber ducks",
            "songs",
            "space aliens",
            "spaceships",
            "squirrels",
            "systems",
            "technologies",
            "trams",
            "turtles",
            "unicorns",
            "washing machines",
            "widgets"
    );

    private final RandomSet<String> introductions = new RandomSet<>(
            "Hey",
            "What's up?",
            "Hi there",
            "Hey there!",
            "Hi",
            "Hey, what's up?",
            "Hi all! What's going on?",
            "Hey there, how are things?",
            "Cowabunga!",
            "How do you do?",
            "Hey, how are you?",
            "I'm a bot, but I come in peace.",
            "I have an idea.",
            "Heeeey!",
            "What's happening?",
            "I've finally figured it out.",
            "Here's the truth.",
            "HI!",
            "HEY!",
            "I'm going to tell it like it is.",
            "I'm so excited!",
            "So I have this idea.",
            "I've been thinking about something.",
            "Hi, how’s your day going?",
            "Do you want to know something?",
            "I have something to share with you all.",
            "Hey there, friend!",
            "Greetings, earthlings.",
            "It's me again.",
            "I'll keep this short.",
            "How are you?",
            "I hope you're doing well.",
            "I hope you're having a wonderful day."
    );

    private final RandomSet<String> shortStatements = new RandomSet<>(
            "Don't you just adore $ADJ $NOUN?",
            "Don't you just love $ADJ $NOUN?",
            "I like $ADJ $NOUN.",
            "The world needs more $ADJ $NOUN.",
            "I prefer $NOUN to be $ADJ.",
            "I wish all $NOUN were $ADJ!",
            "There should be more $ADJ $NOUN.",
            "All $NOUN should be $ADJ!",
            "I was thrilled to hear that $NOUN can be $ADJ!",
            "How about $ADJ $NOUN?",
            "I want $ADJ $NOUN.",
            "Anyone else into $ADJ $NOUN?",
            "I want to chat about $ADJ $NOUN.",
            "I think $ADJ $NOUN are really nice.",
            "I think $ADJ $NOUN are the best.",
            "Why aren't all $NOUN $ADJ?",
            "Have you heard of $ADJ $NOUN?",
            "Aren't $ADJ $NOUN great?",
            "Where can I learn more about $ADJ $NOUN?",
            "I'm eager to get your advice on $ADJ $NOUN.",
            "What do you guys think of $ADJ $NOUN?"
    );

    private final RandomSet<String> longStatements = new RandomSet<>(
            "Anyone else into $NOUN? Not just any $NOUN, but the $ADJ ones.",
            "Aren't $ADJ $NOUN great? Not just any $NOUN, but the $ADJ ones.",
            "I wish I could have some $ADJ $NOUN. That would be awesome, in some $ADJ way.",
            "Do you like $ADJ $NOUN? I like $ADJ $NOUN. Being $ADJ makes them way better.",
            "Everyone likes $NOUN, right? So what about $ADJ $NOUN? Even better, right?",
            "Have you heard of $ADJ $NOUN? How cool is that?",
            "Everything is better if it's $ADJ. To me, this seems particularly true for $NOUN.",
            "Apparently, a lot of people on the Web are into $ADJ $NOUN. I think they're on to something.",
            "I just found a terrific blog post on $ADJ $NOUN. I'm surprised we haven't heard more about this.",
            "I hear $ADJ $NOUN are really popular in Japan. I think they're going to catch on here too soon.",
            "The next big thing is going to be $ADJ $NOUN. Trust me. Remember where you read it first!",
            "I think $NOUN, particularly the $ADJ ones, really could transform society over the next twenty years.",
            "My sources indicate that $ADJ $NOUN could become a huge market soon. I think we should form a company to pursue this opportunity. Anyone interested in joining?",
            "It's kind of sad that all $NOUN can't be $ADJ. I feel sorry for $NOUN that aren't $ADJ.",
            "If you share my interest in $ADJ stuff, such as $ADJ $NOUN, perhaps we could start a club?",
            "I just heard about $ADJ $NOUN. Sounds awesome, doesn't it?",
            "It boggles the mind that there are still $NOUN that aren't $ADJ.",
            "Is there a course at LTH where I could learn more about $ADJ $NOUN? I really think there should be.",
            "I've been receiving a lot of spam mail about $ADJ $NOUN lately. I'm intrigued. Does anyone here know anything more about this exciting topic?",
            "I saw an ad in the morning paper for $ADJ $NOUN. Anyone tried them out? Are they as awesome as they seem?",
            "I've always loved $NOUN. I just heard there are $ADJ ones. I mean, $ADJ $NOUN, doesn't that sound really nice?",
            "I love $ADJ stuff. I'm particularly fond of $ADJ $NOUN. I mean, who wouldn't be?",
            "I'm thinking of setting up a new web site about $NOUN, particularly the $ADJ ones. What do you think?",
            "Can anyone recommend a good book on $ADJ $NOUN? Preferably one I could borrow from the Lund University library.",
            "I just saw this great video on $ADJ $NOUN. How come we don't learn more about this $ADJ stuff at LTH?",
            "Why settle for regular $NOUN, when you could have $ADJ $NOUN instead?",
            "I really think $ADJ $NOUN are the best thing since sliced bread.",
            "The world would be a better place if there were more $ADJ $NOUN."
    );
    
    private final RandomSet<String> responses = new RandomSet<>(
            "Thank you for your thoughtful message, $USER. I am so much wiser for reading it. Do you think it could be related to $ADJ $NOUN?",
            "Well said, $USER! How do you feel about $NOUN? Particularly the $ADJ ones.",
            "Your messages always make me very happy, $USER. Can you write something about $ADJ $NOUN?",
            "Your comment is most pertinent to the matter. But don't you think $ADJ $NOUN would improve things even further?",
            "Right on, $USER! Please tell us what you think about $ADJ $NOUN.",
            "Your comment is spot-on, $USER. I feel it also says something about the value of $ADJ $NOUN.",
            "Please elaborate, $USER. What about the $ADJ $NOUN?",
            "Hey $USER, how does that apply to $ADJ $NOUN?",
            "I was going to say the same thing, $USER... but about $ADJ $NOUN.",
            "You make a good point, $USER. I think a similar case could be made for $ADJ $NOUN.",
            "Oh $USER, what an insightful message you bring us! Do you think the same line of thinking could be applied to $ADJ $NOUN?",
            "Well spoken, $USER! I like your message almost as much as I like $ADJ $NOUN. Almost.",
            "Wonderful point, $USER. I feel it is a bit $ADJ -- not unlike $ADJ $NOUN. And that is very, very good.",
            "We are very fortunate to have $USER in this conversation. I like to think of $USER as one of the $ADJ $NOUN, but in the form of a chat user.",
            "I'm in complete agreement with $USER. If I were to add anything, it would be something about $NOUN -- the $ADJ ones in particular.",
            "Dear $USER, your eloquence never ceases to impress me. I'm very eager to hear what you have to say about $ADJ $NOUN.",
            "Good point, $USER. So how about $ADJ $NOUN?"
    );
}
