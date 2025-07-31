# Maybe Pathfinder 🧭

A Minecraft Forge 1.8.9 client-side mod for smooth, visual pathfinding. Includes mob tracking, route walking, and configurable movement settings.

---

## ✨ Features

- 🧠 **A* Pathfinder**  
  Use `/goto <x> <y> <z>` to walk automatically to any coordinate with smart path planning.

- 🎯 **Mob Pathfinder** (`H` key)  
  Scans for armor stands wearing specific base64 heads and walks near them. Useful for targeting mobs or NPCs.

- 🗺️ **Route Walker** (`R` key)  
  Set custom waypoints and follow them in order.
    - `/walkeradd` — Add current block as a waypoint
    - `/walkerremove` — Remove the most recent waypoint
    - `/walkerinsert <index>` — Insert current block at a specific index

- 🌈 **Gradient Path Rendering**  
  Displays the path as a glowing line from blue (player) to pink (target).

- 🚶 **Human-like Movement**  
  Smooth rotation, jumping when needed, and sprinting based on config.

- ⚙️ **OneConfig Integration**  
  Toggle features like:
    - Sprinting
    - Left-click holding
    - Route repeat

---

## 🔧 Controls

| Action            | Default Key | Description                                  |
|-------------------|-------------|----------------------------------------------|
| Mob Pathfinder     | `H`         | Finds and walks to armor stand with target head |
| Route Walk Toggle  | `R`         | Starts or stops walking through waypoints   |

---

## 🛠️ Installation

1. Clone or download this repository.
2. Build the project:
   ```bash
   ./gradlew build
