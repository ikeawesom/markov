# API Reference - MarkovModel

Full documentation for all public methods in `MarkovModel`. For setup and usage, see [README.md](README.md).

---

## Table of Contents

- [Enums](#enums)
- [Constants](#constants)
- [Constructors](#constructors)
- [Model Building](#model-building)
- [Frequency Queries](#frequency-queries)
- [Text Generation](#text-generation)
- [Utilities](#utilities)

---

## Enums

### `TokenType`

Determines whether the model operates on characters or words.

```java
public enum TokenType {
    CHAR,   // character-level model (default)
    WORD    // word-level model
}
```

| Value  | Description                                                                    |
| ------ | ------------------------------------------------------------------------------ |
| `CHAR` | kgram = a string of `k` characters. Predicts the next character (ASCII 1–255). |
| `WORD` | kgram = `k` words space-joined. Predicts the next word.                        |

---

## Constants

### `NOCHARACTER`

```java
public static final char NOCHARACTER = (char) 0;
```

Returned by `nextCharacter()` when no next character exists - i.e., the kgram was not found in the model, or only appeared at the very end of the text. Equivalent to ASCII NULL.

---

### `NOWORD`

```java
public static final String NOWORD = "";
```

Returned by `nextWord()` when no next word exists. Analogous to `NOCHARACTER` for word mode.

---

## Constructors

### `MarkovModel(int order, long seed)`

Creates a **character-level** Markov Model of the given order. Drop-in replacement for the original `MarkovModel(int, long)` constructor - all existing code calling this signature continues to work unchanged.

```java
MarkovModel model = new MarkovModel(6, 42L);
```

| Parameter | Type   | Description                                                          |
| --------- | ------ | -------------------------------------------------------------------- |
| `order`   | `int`  | kgram length in characters. Must be >= 1.                            |
| `seed`    | `long` | Seed for the pseudorandom number generator. Same seed = same output. |

---

### `MarkovModel(int order, long seed, TokenType tokenType)`

Creates a Markov Model of the given order and token type. Use this constructor when word-level generation is desired.

```java
MarkovModel model = new MarkovModel(2, 42L, TokenType.WORD);
```

| Parameter   | Type        | Description                                        |
| ----------- | ----------- | -------------------------------------------------- |
| `order`     | `int`       | kgram length in tokens. Must be >= 1.              |
| `seed`      | `long`      | Seed for the pseudorandom number generator.        |
| `tokenType` | `TokenType` | `CHAR` for character-level, `WORD` for word-level. |

**Throws:** `IllegalArgumentException` if `order < 1`.

---

## Model Building

### `void initializeText(String text)`

Builds (or rebuilds) the Markov Model from the given source text. Resets all internal state - safe to call multiple times on the same object.

```java
model.initializeText("the cat sat on the mat the cat ate the rat");
```

| Parameter | Type     | Description                    |
| --------- | -------- | ------------------------------ |
| `text`    | `String` | The source text to learn from. |

**CHAR mode:** Slides a window of `k+1` characters across the text. NULL characters (ASCII 0) are stripped before processing.

**WORD mode:** Splits on whitespace (`\\s+`), filters empty tokens, then slides a window of `k+1` words across the resulting array.

**Complexity:** O(N) where N = number of tokens in `text`.

---

## Frequency Queries

### `int getFrequency(String kgram)`

Returns the total number of times `kgram` appears in the text, counting only occurrences where at least one token follows (i.e., the final kgram in the text is excluded).

```java
int freq = model.getFrequency("the cat"); // e.g. returns 2
```

| Parameter | Type     | Description                                                                   |
| --------- | -------- | ----------------------------------------------------------------------------- |
| `kgram`   | `String` | The kgram to look up. Must have the correct length/word count for this model. |

**Returns:** Count >= 0. Returns `0` if the kgram never appeared.

**Throws:** `IllegalArgumentException` if kgram has wrong length (CHAR) or wrong word count (WORD).

**Complexity:** O(k) - single hash lookup, where hashing the kgram string takes O(k).

---

### `int getFrequency(String kgram, char c)`

Returns the number of times character `c` appears immediately after `kgram`. For use in CHAR mode.

```java
int freq = model.getFrequency("ga", 'g'); // e.g. returns 4
```

| Parameter | Type     | Description                                        |
| --------- | -------- | -------------------------------------------------- |
| `kgram`   | `String` | The kgram context.                                 |
| `c`       | `char`   | The character whose follow-frequency is requested. |

**Returns:** Count >= 0. Returns `0` if kgram not in model or `c` never followed it.

**Throws:** `IllegalArgumentException` if kgram has wrong length.

**Complexity:** O(k).

---

### `int getFrequency(String kgram, String word)`

Returns the number of times `word` appears immediately after `kgram`. For use in WORD mode.

```java
int freq = model.getFrequency("the cat", "sat"); // e.g. returns 1
```

| Parameter | Type     | Description                                   |
| --------- | -------- | --------------------------------------------- |
| `kgram`   | `String` | The kgram context (k words space-joined).     |
| `word`    | `String` | The word whose follow-frequency is requested. |

**Returns:** Count >= 0. Returns `0` if kgram not in model or `word` never followed it.

**Throws:** `IllegalArgumentException` if kgram has wrong word count.

**Complexity:** O(k).

---

## Text Generation

### `char nextCharacter(String kgram)`

Returns a randomly sampled next character, weighted by the model's probability distribution for the given kgram.

```java
char next = model.nextCharacter("ga"); // e.g. returns 'g' with probability 4/5
```

Sampling follows ASCII order (1–255) for reproducibility: a random integer in `[0, N-1]` is drawn (where N = `getFrequency(kgram)`), then characters are walked in ASCII order with each frequency subtracted until the value goes negative.

| Parameter | Type     | Description                      |
| --------- | -------- | -------------------------------- |
| `kgram`   | `String` | The current k-character context. |

**Returns:** Next character, or `NOCHARACTER` if kgram not in model.

**Throws:** `IllegalArgumentException` if kgram has wrong length.

---

### `String nextWord(String kgram)`

Returns a randomly sampled next word, weighted by the model's probability distribution for the given kgram. For use in WORD mode.

```java
String next = model.nextWord("the cat"); // e.g. returns "sat" or "ate"
```

Words are sampled in insertion order (order first encountered in the source text). Combined with a fixed seed, this gives fully reproducible output.

| Parameter | Type     | Description                                      |
| --------- | -------- | ------------------------------------------------ |
| `kgram`   | `String` | The current k-word context (words space-joined). |

**Returns:** Next word, or `NOWORD` (`""`) if kgram not in model.

**Throws:** `IllegalArgumentException` if kgram has wrong word count.

---

## Utilities

### `String extractSeed(String text)`

Returns the appropriate seed string to begin text generation, extracted from the same source text passed to `initializeText()`.

```java
String seed = model.extractSeed(text); // "the ca" in CHAR mode (order 6)
                                        // "the cat" in WORD mode (order 2)
```

**CHAR mode:** Returns `text.substring(0, order)` - the first `k` characters.

**WORD mode:** Returns the first `k` words of the text, space-joined.

| Parameter | Type     | Description                                                |
| --------- | -------- | ---------------------------------------------------------- |
| `text`    | `String` | The full source text (same as passed to `initializeText`). |

**Returns:** Seed string of the correct format for this model's token type.

**Throws:** `IllegalArgumentException` if text has fewer tokens than `order`.

---

## Error Handling Summary

| Method                        | Throws                     | Condition                       |
| ----------------------------- | -------------------------- | ------------------------------- |
| Constructor                   | `IllegalArgumentException` | `order < 1`                     |
| `getFrequency(kgram)`         | `IllegalArgumentException` | Wrong kgram length / word count |
| `getFrequency(kgram, char)`   | `IllegalArgumentException` | Wrong kgram length              |
| `getFrequency(kgram, String)` | `IllegalArgumentException` | Wrong kgram word count          |
| `nextCharacter(kgram)`        | `IllegalArgumentException` | Wrong kgram length              |
| `nextWord(kgram)`             | `IllegalArgumentException` | Wrong kgram word count          |
| `extractSeed(text)`           | `IllegalArgumentException` | Text too short for order        |
