# Dungeon KUrawler

Dungeon KUrawler is an ongoing Java-based grid dungeon game developed for the COMP302 Software Engineering project. The project focuses on applying object-oriented design principles, logical architecture, and design patterns rather than only gameplay features.

## Project Structure

The system follows a layered architecture with clear separation of responsibilities:

- **Model** → Core game entities, map, hero, enemies, inventory, items  
- **View** → GUI windows, panels, dialogs, rendering components  
- **Controller / Engine** → Input handling, movement logic, interactions, game flow  

Packages:

- `model`
- `view`
- `engine`
- `main`

## Design Patterns & Principles

- **Controller (GRASP)** — `PlayerModeController`, `InteractionController`  
- **Factory Pattern** — `EnemyFactory` for enemy instantiation  
- **Observer Pattern** — `GameStateListener` for UI updates  
- **Model-View Separation** — Game logic in `model`/`engine`, rendering in `view`

## Implemented Functionalities

### Grid & Collision Engine
- Hero moves tile-by-tile on the grid  
- Hero stops when colliding with walls  
- Hero can move over collectible items  

### 3x3 Interaction Logic
- Action Menu appears only when the hero is adjacent to an interactable object (8 surrounding tiles)

### Inventory System
- Items can be picked from the map using the **TAKE** action  
- Picked items are transferred into the 2x4 inventory grid  
- Items disappear from the ground after pickup  

### Stat Binding
- Hero stats (such as HP / Energy) update through in-game actions and are reflected in the UI  

### Enemy Spawn & AI State
- Enemies spawn periodically  
- Basic AI state detection switches between **Roaming** and **Chasing**  

## Technologies

- Java  
- Maven  
- Swing GUI  

## Team Workflow

The project was developed collaboratively using GitHub with branching, commits, merges, and issue-based task management.

## Run

```bash
mvn clean install
mvn exec:java
