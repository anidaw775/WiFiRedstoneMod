# WiFi Redstone Mod

A Minecraft Fabric mod for version 1.21.5 that adds a "Receiver" block and a "Transmitter" item for remote redstone activation.

## Features
- **Receiver Block**: Emits a redstone signal (strength 15) for 20 ticks (1 second) when activated remotely.
- **Transmitter Item**: Syncs with a Receiver block via right-click, then activates it with another right-click.
- Uses NBT to store a UUID for synchronization between Transmitter and Receiver.
- Craftable items with intuitive recipes.

## Requirements
- Minecraft 1.21.5
- Fabric Loader 0.17.9 or higher
- Fabric API 0.105.1+1.21.5
- Java 21

## Installation
1. Install Fabric Loader for Minecraft 1.21.5.
2. Download and place the Fabric API `.jar` in the `mods` folder.
3. Place the `wifiredstone-1.0.0.jar` in the `mods` folder of your Minecraft directory.
4. Launch Minecraft with the Fabric profile.

## Usage
1. **Crafting**:
   - **Receiver**: 4 iron ingots, 4 redstone, 1 stone.
     ```
     I R I
     R S R
     I R I
     ```
   - **Transmitter**: 3 iron ingots, 1 redstone, 1 ender pearl.
     ```
       E
     I R I
       I
     ```
2. **Synchronization**:
   - Hold the Transmitter item and right-click on a Receiver block to sync them.
3. **Activation**:
   - Right-click with the synced Transmitter to activate the Receiver, emitting a redstone signal for 1 second.
4. **Notes**:
   - One Transmitter syncs with one Receiver at a time.
   - Works within loaded chunks.

## Building from Source
1. Clone or download this repository.
2. Ensure JDK 21 is installed.
3. Open a terminal in the project directory and run:
   ```bash
   ./gradlew build
   ```
4. Find the compiled `.jar` in `build/libs/wifiredstone-1.0.0.jar`.

## Project Structure
```
WiFiRedstoneMod/
├── src/
│   ├── main/
│   │   ├── java/com/example/wifiredstone/
│   │   │   ├── WiFiRedstoneMod.java
│   │   │   ├── WiFiRedstoneModClient.java
│   │   ├── resources/
│   │   │   ├── assets/wifiredstone/
│   │   │   │   ├── blockstates/receiver.json
│   │   │   │   ├── models/block/receiver.json
│   │   │   │   ├── models/item/transmitter.json
│   │   │   │   ├── textures/block/receiver.png
│   │   │   │   ├── textures/item/transmitter.png
│   │   │   ├── data/wifiredstone/
│   │   │   │   ├── recipes/receiver.json
│   │   │   │   ├── recipes/transmitter.json
│   │   │   ├── fabric.mod.json
│   │   │   ├── pack.mcmeta
├── build.gradle
├── gradle.properties
├── LICENSE
├── README.md
```

## License
This mod is licensed under the MIT License. See `LICENSE` for details.

## Contributing
Feel free to submit issues or pull requests on the repository (if hosted). For questions, contact the author.

## Author
- YourName (replace with your name or pseudonym)