# Boing! – Java & Scratch4j Edition

This is my reimplementation of **Boing!**, the Pong-inspired game featured in *[Code the Classics, Volume 1](https://www.raspberrypi.org/books/code-the-classics/)*, originally developed in Python with Pygame Zero.
My version is written entirely in **Java**, using the **[Scratch4j](https://github.com/openpatch/scratch-for-java)** library.

## 📖 About the Game

Boing! is a simple but fun arcade game where you control a bat to bounce a ball back towards your opponent.
It’s a great starting point for learning the basics of game development:

* The game takes place on a **single static screen** (no scrolling).
* Only **three moving objects**: two bats (paddles) and one ball.
* **Simple AI** for the computer player — or none at all, for two-player matches.

My Java port keeps the spirit of the original but uses the Scratch4j API, making it beginner-friendly while still allowing full customization.

## 🎮 Game Modes

* **Single-player** – Battle against a simple AI opponent.
* **Two-player** – Challenge a friend on the same keyboard.

## 🕹️ Controls

| Player | Action    | Key        |
| ------ | --------- | ---------- |
| P1     | Move Up   | Up Arrow          |
| P1     | Move Down | Down Arrow          |
| P2/AI  | Move Up   | W   |
| P2/AI  | Move Down | S |

## 📚 Credits

* **Original concept & design:** *Code the Classics, Volume 1* by Raspberry Pi Press
* **Java port & Scratch4j adaptation:** Mike Barkmin
* **Scratch4j library:** https://github.com/openpatch/scratch-for-java

## 📜 License

The original *Code the Classics* source code is licensed under **CC BY-NC-SA 3.0**.
This Java reimplementation is licensed under the same terms.
