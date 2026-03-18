import java.util.Random;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

/**
 * MarkovModel — supports both character-level and word-level Markov Models
 *
 * CHARACTER MODE: kgram = a string of k characters. The model predicts the
 *   next character (ASCII 1–255) given the previous k characters
 *
 * WORD MODE: kgram = a sequence of k words joined by a single space. The model
 *   predicts the next word given the previous k words. This tends to produce
 *   more grammatically coherent text. (hopefully...)
 *
 * Both modes share the same internal data structure:
 *   HashMap<String, LinkedHashMap<String, Integer>> model
 *   where the outer key is the kgram string, and the inner map stores
 *   each possible next token (char or word), i.e. its frequency count.
 *
 * A separate HashMap<String, Integer> kgramCount tracks the total number of
 * occurrences of each kgram, giving O(k) getFrequency(kgram) lookups.
 */
public class MarkovModel {


    /** Determines whether the model operates on individual characters or words. */
    public enum TokenType {
        CHAR,   // character-level model (original behaviour)
        WORD    // word-level model (new)
    }

    /**
     * NEW WORD-LEVEL FEATURES
     */

    /** Delimiter used to join word tokens when building word-mode kgrams. */
    private static final String WORD_DELIMITER = " ";

    /**  Special token representing "no word" in word mode. */
    public static final String NOWORD = "";

    // Use this to generate random numbers as needed
    private final Random generator = new Random();

    // This is a special symbol to indicate no character
    public static final char NOCHARACTER = (char) 0;

    private final int order;

    /** Whether this model operates on characters or words. */
    private final TokenType tokenType;

    /**
     * Data Structure Idea
     * Outer key: kgram string (k chars joined, or k words space-joined)
     * Inner map: next token (frequency count)
     *
     * LinkedHashMap is used for the inner map so that word-mode iteration
     * follows insertion order (words seen earlier come first)
     */
    private HashMap<String, LinkedHashMap<String, Integer>> model;

    /** Total occurrence count of each kgram */
    private HashMap<String, Integer> kgramCount;

    /**
     * Creates a character-level Markov Model of the given order.
     *
     * @param order the kgram length (number of characters)
     * @param seed  seed for the pseudorandom number generator
     */
    public MarkovModel(int order, long seed) {
        this(order, seed, TokenType.CHAR);
    }

    /**
     * Creates a Markov Model of the given order and token type.
     *
     * @param order     the kgram length (in characters or words depending on type)
     * @param seed      seed for the pseudorandom number generator
     * @param tokenType CHAR for character-level, WORD for word-level
     */
    public MarkovModel(int order, long seed, TokenType tokenType) {
        if (order < 1) throw new IllegalArgumentException("Order must be at least 1");
        this.order     = order;
        this.tokenType = tokenType;
        this.model      = new HashMap<>();
        this.kgramCount = new HashMap<>();
        generator.setSeed(seed);
    }

    /**
     * Builds the Markov Model based on the specified text string based on word/char-level
     */
    public void initializeText(String text) {
        // Always reset so repeated calls start fresh.
        model      = new HashMap<>();
        kgramCount = new HashMap<>();

        if (tokenType == TokenType.CHAR) {
            initializeChar(text);
        } else {
            initializeWord(text);
        }
    }

    /**
     * Returns the number of times the specified kgram appeared in the text.
     *
     * @param kgram the kgram to look up
     * @throws IllegalArgumentException if kgram has wrong length/token count
     */
    public int getFrequency(String kgram) throws IllegalArgumentException {
        validateKgram(kgram);
        return kgramCount.getOrDefault(kgram, 0);
    }

    /**
     * Returns the number of times the character c appears immediately after the specified kgram.
     *
     * @param kgram the kgram to look up
     * @param c     the character whose follow-frequency we want (CHAR mode)
     * @throws IllegalArgumentException if kgram has wrong length
     */
    public int getFrequency(String kgram, char c) throws IllegalArgumentException {
        return getFrequencyToken(kgram, String.valueOf(c));
    }

    /**
     * Word-mode method overload: returns frequency of a word following the kgram.
     *
     * @param kgram the kgram to look up
     * @param word  the word whose follow-frequency we want (WORD mode)
     */
    public int getFrequency(String kgram, String word) {
        return getFrequencyToken(kgram, word);
    }

    /**
     * Generates the next character from the Markov Model.
     * Return NOCHARACTER if the kgram is not in the table, or if there is no
     * valid character following the kgram.
     *
     * @param kgram the current k-character
     * @return next character, or NOCHARACTER if kgram not in model
     */
    public char nextCharacter(String kgram) throws IllegalArgumentException {
        validateKgram(kgram);

        int total = getFrequency(kgram);
        if (total == 0) return NOCHARACTER;

        // Pick a random slot in [0, total-1]
        int rand = generator.nextInt(total);

        // Iterate through ASCII characters in order (1–255)
        // Subtract frequencies until rand goes negative, i.e. that character wins the weighted draw
        for (char c = 1; c <= 255; c++) {
            int freq = getFrequency(kgram, c);
            if (freq == 0) continue;
            rand -= freq;
            if (rand < 0) return c;
        }

        // Should be unreachable if model is consistent
        return NOCHARACTER;
    }

    /**
     * Returns the next word chosen randomly according to the model (WORD mode).
     *
     * Unlike character mode, there is no natural ASCII ordering for words.
     * Words are sampled in insertion order (order first encountered in text), which gives a
     * standard output with a fixed seed.
     *
     * @param kgram the current k-word context (words space-joined)
     * @return next word, or NOWORD if kgram not in model
     */
    public String nextWord(String kgram) throws IllegalArgumentException {
        validateKgram(kgram);

        int total = getFrequency(kgram);
        if (total == 0) return NOWORD;

        int rand = generator.nextInt(total);

        // Iterate through the inner LinkedHashMap in insertion order
        // Subtract each word's frequency until rand goes negative
        LinkedHashMap<String, Integer> followers = model.get(kgram);
        if (followers == null) return NOWORD;

        for (Map.Entry<String, Integer> entry : followers.entrySet()) {
            rand -= entry.getValue();
            if (rand < 0) return entry.getKey();
        }

        // Again, should unreachable if model is consistent
        return NOWORD;
    }

    /**
     * Returns the seed text that TextGenerator should use to start generation.
     *
     * CHAR mode: returns the first order-th characters of the text.
     * WORD mode: returns the first order-th words of the text (joined by space " ").
     *
     * Called by TextGenerator.buildModel() and buildModelWord() so
     * that both modes can extract the seed from the source text.
     *
     * @param text the full source text (same text passed to initializeText)
     * @return seed string of the correct format for this model's token type
     */
    public String extractSeed(String text) throws IllegalArgumentException {
        if (tokenType == TokenType.CHAR) {
            // First k characters
            return text.substring(0, order);
        } else {
            // First k words
            String[] words = text.trim().split("\\s+");
            if (words.length < order) {
                throw new IllegalArgumentException("Text has fewer than " + order + " words.");
            }
            return buildWordKgram(words, 0);
        }
    }

    /**
     * Builds the character-level model
     * Records kgram and next character pairs.
     * NULL characters (ASCII 0) are removed beforehand.
     */
    private void initializeChar(String text) {
        text = text.replace("\0", "");

        int N = text.length();

        // We need at least k +1 characters to form one (kgram, nextChar) pair
        for (int i = 0; i < N - order; i++) {
            String kgram   = text.substring(i, i + order);
            String nextTok = String.valueOf(text.charAt(i + order));
            recordObservation(kgram, nextTok);
        }
    }

    /**
     * Builds the word-level model by splitting text into words and
     * iterating across the word array.
     * Empty tokens from multiple spaces are filtered out.
     */
    private void initializeWord(String text) {
        // Split on any whitespace and filter out empty strings from leading/
        // trailing spaces or consecutive spaces
        String[] words = Arrays.stream(text.split("\\s+"))
                            .filter(w -> !w.isEmpty())
                            .toArray(String[]::new);

        int N = words.length;
        for (int i = 0; i < N - order; i++) {
            // Join k consecutive words with spaces
            String kgram   = buildWordKgram(words, i);
            String nextTok = words[i + order];
            recordObservation(kgram, nextTok);
        }
    }

    /**
     * Records one (kgram, nextToken) observation into the model and
     * increments the kgram's total count.
     */
    private void recordObservation(String kgram, String nextToken) {
        kgramCount.merge(kgram, 1, Integer::sum);

        // Get or create the inner frequency map for this kgram.
        // LinkedHashMap preserves insertion order for word-mode sampling.

        // NOTE: This was an interesting and useful method I learnt while building this :O
        model.computeIfAbsent(kgram, k -> new LinkedHashMap<>())
                .merge(nextToken, 1, Integer::sum);
    }

    /**
     * Frequency lookup for any token type.
     */
    private int getFrequencyToken(String kgram, String token) throws IllegalArgumentException {
        validateKgram(kgram);
        LinkedHashMap<String, Integer> followers = model.get(kgram);
        if (followers == null) return 0;
        return followers.getOrDefault(token, 0);
    }

    /**
     * Joins order consecutive words from words[] starting at index i,
     * separated by WORD_DELIMITER (in our case, a space: " ").
     */
    private String buildWordKgram(String[] words, int i) {
        StringBuilder sb = new StringBuilder(words[i]);
        for (int j = 1; j < order; j++) {
            sb.append(WORD_DELIMITER).append(words[i + j]);
        }
        return sb.toString();
    }

    /**
     * Checks that a kgram has the correct length/token count for this model.
     * CHAR mode: kgram.length() must equal order.
     * WORD mode: number of space-separated tokens must equal order.
     *
     * @throws IllegalArgumentException on mismatch
     */
    private void validateKgram(String kgram) throws IllegalArgumentException {
        if (tokenType == TokenType.CHAR) {
            if (kgram.length() != order) {
                throw new IllegalArgumentException("CHAR mode: kgram length must be " + order + ", got " + kgram.length());
            }
        } else {
            String[] tokens = kgram.split("\\s+");
            if (tokens.length != order) {
                throw new IllegalArgumentException("WORD mode: kgram must have " + order + " words, got " + tokens.length);
            }
        }
    }
}