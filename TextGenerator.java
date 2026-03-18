import java.io.FileInputStream;

/**
 * This class is used to generated text using a Markov Model
 * Supports both character/word-level generation.
 */
public class TextGenerator {

    // For testing, we will choose different seeds
    private static long seed;

    // Sets the random number generator seed
    public static void setSeed(long s) {
        seed = s;
    }

    /**
     * Reads a file and builds a character-level MarkovModel.
     * Behaviour is identical to the original buildModel() — no changes needed
     * for existing CHAR-mode callers.
     *
     * @param order the order of the Markov Model
     * @param fileName the name of the file to read
     * @param model the Markov Model to build
     * @return the first {@code order} characters of the file to be used as the seed text
     */
    public static String buildModel(int order, String fileName, MarkovModel model) {
        // Get ready to parse the file.
        // StringBuffer is used instead of String as appending character to String is slow
        StringBuilder text = new StringBuilder();

        // Loop through the text
        try {
            FileInputStream inputStream = new FileInputStream(fileName);

            // Determine the size of the file, in bytes
            int fileSize = inputStream.available();

            // Read in the file, one character at a time.
            for (int i = 0; i < fileSize; i++) {
                // Read a character
                char c = (char) inputStream.read();
                text.append(c);
            }

            // Make sure that length of input text is longer than requested Markov order
            if (text.length() < order) {
                System.out.println("Text is shorter than specified Markov Order.");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Problem reading file " + fileName + ".");
            return null;
        }
        // Build Markov Model of order from text
        model.initializeText(text.toString());
        return model.extractSeed(text.toString());
    }

    /**
     * Same as buildModel() but for WORD-mode models.
     * The seed returned is the first order words of the file joined with a space " ".
     *
     * @param order the order of the Markov Model
     * @param fileName the name of the file to read
     * @param model the Markov Model to build
     * @return the first {@code order} words of the file to be used as the seed string,
     * or null if the file cannot be read or has too few words
     */
    public static String buildModelWord(int order, String fileName, MarkovModel model) {
        // Get ready to parse the file.
        // StringBuffer is used instead of String as appending character to String is slow
        StringBuilder text = new StringBuilder();

        // Loop through the text
        try {
            FileInputStream inputStream = new FileInputStream(fileName);

            // Determine the size of the file, in bytes
            int fileSize = inputStream.available();

            // Read in the file, one character at a time.
            for (int i = 0; i < fileSize; i++) {
                // Read a character
                char c = (char) inputStream.read();
                text.append(c);
            }

            // Make sure that length of input text is at least order words.
            String[] words = text.toString().trim().split("\\s+");
            if (words.length < order) {
                System.out.println("Text has fewer words than specified Markov Order.");
                return null;
            }
        } catch (Exception e) {
            System.out.println("Problem reading file " + fileName + ".");
            return null;
        }

        // Build Markov Model of order from text
        model.initializeText(text.toString());
        return model.extractSeed(text.toString());
    }

    /**
     * generateText outputs to stdout text of the specified length based on the specified seedText
     * using the given Markov Model.
     *
     * @param model the Markov Model to use
     * @param seedText the initial kgram used to generate text
     * @param order the order of the Markov Model
     * @param length the length of the text to generate
     */
    public static void generateText(MarkovModel model, String seedText, int order, int length) {
        // Use the first order characters of the text as the starting string
        StringBuffer kgram = new StringBuffer();
        kgram.append(seedText);

        // Generate length characters
        char charToAppend;
        int outLength = kgram.length();
        while (outLength < length) {
            // Get the next character from kgram sequence. The kgram sequence to use
            // is the sequence starting from ith position.
            charToAppend = model.nextCharacter(kgram.substring(outLength - order));

            // If there is no next character, restart generation with initial kgram value which
            // Starts from 0th position.
            if (charToAppend != MarkovModel.NOCHARACTER) {
                kgram.append(charToAppend);
                outLength++;
            } else {
                // This prefix has never appeared in the text.
                // Give up?
                System.out.println(kgram);
                return;
            }
        }

        // Output the generated characters, not including the initial seed.
        System.out.println(kgram);
    }

    /**
     * Same as generateText() but operates on words rather than characters.
     *
     * @param model the word-level MarkovModel to use
     * @param seedText the initial k-word seed (words joined by space " ")
     * @param order the order of the Markov Model (in words)
     * @param length total number of words to output (including seed words)
     */
    public static void generateTextWord(MarkovModel model, String seedText,
                                        int order, int length) {
        // Split seed into individual words
        String[] seedWords = seedText.trim().split("\\s+");
        StringBuilder output = new StringBuilder(seedText);
        int wordCount = seedWords.length;

        while (wordCount < length) {
            // Extract the last order words as the current kgram
            String[] allWords = output.toString().trim().split("\\s+");
            int total = allWords.length;

            // Build kgram from the last k words
            StringBuilder kgramBuilder = new StringBuilder(allWords[total - order]);
            for (int i = total - order + 1; i < total; i++) {
                kgramBuilder.append(" ").append(allWords[i]);
            }
            String kgram = kgramBuilder.toString();

            String nextWord = model.nextWord(kgram);

            if (!nextWord.equals(MarkovModel.NOWORD)) {
                output.append(" ").append(nextWord);
                wordCount++;
            } else {
                // This prefix has never appeared in the text.
                // Give up?
                System.out.println(output);
                return;
            }
        }

        // Output the generated words, not including the initial seed.
        System.out.println(output);
    }

    /**
     * The main routine.  Takes 3-4 arguments:
     * args[0]: the order of the Markov Model
     * args[1]: the length of the text to generate
     * args[2]: the filename for the input text
     * args[3]: (optional) "word" for word mode, defaults to "char"
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: TextGenerator <order> <length> <filename> [char|word]");
            return;
        }

        // Get the input:
        int order    = Integer.parseInt(args[0]);
        int length   = Integer.parseInt(args[1]);
        String fileName = args[2];

        // Read optional mode argument which defaults to CHAR
        boolean wordMode = args.length >= 4 && args[3].equalsIgnoreCase("word");

        if (wordMode) {
            // Create the model for words
            MarkovModel model = new MarkovModel(order, seed, MarkovModel.TokenType.WORD);
            String seedText = buildModelWord(order, fileName, model);
            if (seedText == null) return;

            // Generate text
            generateTextWord(model, seedText, order, length);

        } else {
            // Create the model for characters
            MarkovModel model = new MarkovModel(order, seed);
            String seedText = buildModel(order, fileName, model);
            if (seedText == null) return;

            // Generate text
            generateText(model, seedText, order, length);
        }
    }
}