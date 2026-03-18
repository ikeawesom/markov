# Markov Model Text Generator

A Java implementation of a **Markov Model** for probabilistic text generation, supporting both **character-level** and **word-level** modes. Built as part of a university project for Data Structures and Algorithms.

---

## What is a Markov Model?

A Markov Model captures the frequency of a token (character or word) appearing after a specific preceding sequence of tokens, called a **kgram**. The order `k` of the model determines the length of that preceding sequence.

For example, given the text:

```
the cat sat on the mat the cat ate the rat
```

A **word-level Markov Model of order 2** would learn that after `"the cat"`, the word `"sat"` appears once and `"ate"` appears once — so each has a 50% probability of being the next word. This can then be used to generate new, statistically plausible text.

---

## Project Structure

```
.
├── src/
│   ├── MarkovModel.java          # Original character-level implementation
│   ├── MarkovModel.java   # Extended implementation (char + word mode)
│   └── TextGenerator.java        # Reads a file, builds the model, generates text
├── aesop.txt              # Sample texts
├── Alice.txt
├── funny.txt
├── hamlet.txt
└── README.md
```

---

## Features

- **Character-level generation** — predicts the next character given the previous `k` characters
- **Word-level generation** — predicts the next word given the previous `k` words, producing more coherent output on larger texts
- **Seeded random generation** — fully reproducible output with the same seed
- **Efficient lookups** — `getFrequency(kgram)` runs in O(k) using a dedicated count table
- **Pluggable corpus** — works with any plain text file

---

## Getting Started

### Prerequisites

- Java 11 or higher
- `javac` and `java` on your PATH

### Compile

From the root directory:

```bash
javac *.java
```

### Run

```
java [-cp bin] TextGenerator <order> <length> <filename> [mode]
```

| Argument   | Description                                         |
| ---------- | --------------------------------------------------- |
| `order`    | Length of the kgram in tokens (characters or words) |
| `length`   | Number of tokens to generate (including the seed)   |
| `filename` | Path to the input text file                         |
| `mode`     | Optional: `char` (default) or `word`                |

### Examples

**Character mode** — order 6, generate 500 characters:

```bash
java -cp bin TextGenerator 6 500 funny.txt
```

**Word mode** — order 2, generate 80 words:

```bash
java -cp bin TextGenerator 2 80 funny.txt word
```

**Word mode** — order 3, generate 60 words:

```bash
java -cp bin TextGenerator 3 60 funny.txt word
```

---

## Sample Output

With `funny.txt` at order 3 (word mode):

> _"Jeeves drifted in carrying the morning tea. I say drifted because that is the sort of woman who makes strong men weak and weak men lie down and put pillows over their faces. She has opinions, and the stories have a tendency to get complicated at precisely the moments when simplicity would be most appreciated."_

---

## Tuning the Order

| Order (char) | Effect                                          |
| ------------ | ----------------------------------------------- |
| 2–4          | Garbled but vaguely English-sounding            |
| 5–8          | Sweet spot (novel but coherent)                 |
| 10+          | Output starts to mirror the source text closely |

| Order (word) | Effect                                                |
| ------------ | ----------------------------------------------------- |
| 1            | Grammatically fragmented                              |
| 2–3          | Sweet spot — coherent sentences with surprising turns |
| 4+           | Output closely follows the source text                |

---

## API Reference

For full API documentation, see [API.md](API.md).

---

## Implementation Notes

**Data structure:** `HashMap<String, LinkedHashMap<String, Integer>>`

- Outer key: kgram string
- Inner map: next token → frequency count
- `LinkedHashMap` preserves insertion order for deterministic word-mode sampling

**Why a separate `kgramCount` table?**
Without it, `getFrequency(kgram)` would need to iterate the inner map and sum all frequencies - O(number of distinct tokens). By maintaining a separate count, it becomes a single O(k) hash lookup.

**Character sampling** follows ASCII order (1–255) for reproducibility. A random integer in `[0, N-1]` is drawn, then characters are iterated in order with their frequencies subtracted until the value goes negative.

**Word sampling** follows insertion order (first encountered in source text), combined with a fixed seed for reproducible output.

---

## License

This project is for educational purposes.
