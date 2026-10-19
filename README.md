# Scratch for Java - Code the Classics

Reimplementations of the games from *Code the Classics Volume 1* and *Code the Classics Volume 2* by Raspberry Pi Press, using [Scratch for Java](https://scratch4j.openpatch.org/) 6.3.0:

Volume 1:

- Boing
- Cavern
- Myriapod
- Substitute Soccer
- Infinite Bunner

Volume 2:

- Avenger
- Beat Streets
- Eggzy
- Kinetix
- Leading Edge

Download the scratch4j 6.3.0 jar files (https://scratch4j.openpatch.org/download) for your operating system and put them into the +libs folder.

This repo and setup works in VSCode and in BlueJ. For Intellij or Eclipse you need to configure the path to the libraries yourself.

Run the `main` method of each game's `Game` class to start it (for example `boing.Game`).

## Keeping the examples simple

Use stages, sprites, costumes, sounds and `run()` directly. Keep helpers specific to their game, and use library features before adding another abstraction.

To compile and run from the repository root on Linux or macOS:

```sh
mkdir -p build
javac -cp '+libs/*' -d build */*.java
java -cp '+libs/*:build' boing.Game
```

On Windows, use `;` instead of `:` in the runtime classpath.

## Assets Attribution

All sound files and images are adapted from *Code the Classics Volume 1* and *Code the Classics Volume 2* by Russell Barnes, Raspberry Pi Press.  
Licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).  
These assets are non-commercial and must be shared under the same license.
