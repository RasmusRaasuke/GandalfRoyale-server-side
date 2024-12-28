# iti0301-2024

## Idea

This is a battle royale game that we wrote in java using Libgdx.

## Trailer

[![Trailer](https://img.youtube.com/vi/BQvvgYg9uYE/0.jpg)](https://www.youtube.com/watch?v=BQvvgYg9uYE)

## Client side

Our server side is in another [repository](https://github.com/RasmusRaasuke/GandalfRoyale-client-side).

## Gandalf Royale

Players start empty handed spread around the map. Players have three inventory slots and they can collect different magic weapons and they can also switch between them or drop them. Different weapons have different purposes, some deal damage and some have other uses. The play area shrinks over time until only one player is alive, at which point the game ends. The player can be in the red zone, but will take damage over time.
There are also AI pumpkins roaming around the map (randomized movement), they will follow the player when the player enters their proximity (players will take damage, when the pumpkin is close enough). Pumpkins drop gold coins when killed. These coins can be used to buy even better weapons and to restore health.

Each player has three resources: **health**, **mana** and **gold**.

**Health** - Visible to all nearby. Every hit decreases health and potions can be bought and used to regenerate health.

**Mana** - Visible to all nearby. Every spell has a different mana cost. Mana regenerates automatically. Used to limit spell casting.

**Gold** - Visible to only the player. Dropped by goblins when killed. Can be used to buy items from the shop.

### Installation and starting

**Server Side:** <br>
Server side is up and running on TalTech-s server. So you don't have to run it locally.

But if you want to run it locally then here is guide:
1. Clone this repository [GandalfRoyale-server-side](https://github.com/RasmusRaasuke/GandalfRoyale-server-side)
2. If you don't have Gradle plugin, download it
3. Make sure you have server file as module in project structure, if it is not you should make it a module
4. Make sure the project is running with Java 21 and Language level 21 (SDK default should work also). You can do that in File -> Project Structure -> Project -> SDK and Language level
5. Open Project view and go to iti0301-2024-server -> src -> main -> java -> ee.taltech.server -> GameServer
6. Run this file and you should have working server side

**Client Side:** <br>
1. Clone this repository [GandalfRoyale-client-side](https://github.com/RasmusRaasuke/GandalfRoyale-client-side)
2. If you don't have Gradle plugin, download it
3. Load project as Gradle project
4. Make sure the project is running with Java 21 and Language level 21 (SDK default should work also). You can do that in Project Structure -> Project
5. From the file tree go to: desktop -> src\[main\] -> ee.taltech.gandalf -> DesktopLauncher
6. Run DesktopLauncher and you should have working game instance.

Have fun :P

### How to play and game controlls

Please note: This game is still under development, so you might encounter some bugs.

1. Start your adventure - Click the "Play" button on the main screen.
2. Host a game - Click the "Create Game" button in the top right corner. Choose a catchy name for your lobby, and others will see it appear in the lobby list.
3. Ready, set, play! - You can start the game with at least 2 players.
4. Change inventory slots - Inventory slots can be changed with scroll wheel or '1', '2', '3' buttons. Highlighted slot is currently selected.

After 15 seconds all player will be put to random spots, also items and mobs will spawn.

5. Pick up items - Items can be picked up with 'F' button when player stands on the item.
6. Fire away! - Click the left mouse button when an item is selected and then you can shoot spells.
7. Drop items - Items can be dropped with 'F' button when item is in the currently selected slot.
