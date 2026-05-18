# Key Architecture Diagrams

## Scope

Keys are a core progression item in DungeonKUrawler. They can be found on the map, inside chests, and later inside searchable locations. They can unlock gates and keyed containers such as chests. A key may be reusable, consumed after use, or scoped to a specific lock id depending on the game design.

Available key assets currently exist under:

- `src/main/resources/items_keys_extracted/assets/01_key_olive.png`
- `src/main/resources/items_keys_extracted/assets/02_key_silver.png`
- `src/main/resources/items_keys_extracted/assets/03_key_gold.png`
- `src/main/resources/items_keys_extracted/assets/04_key_orange.png`
- `src/main/resources/items_keys_extracted/assets/05_key_bent_silver.png`
- `src/main/resources/items_keys_extracted/assets/06_key_long_gold.png`

## Use Case Diagram

```mermaid
flowchart LR
    Player((Player))

    subgraph Key_System["Key System"]
        UC1["Pick up key from map"]
        UC2["Take key from chest"]
        UC3["Find key in searchable location"]
        UC4["Open gate with key"]
        UC5["Open chest with key"]
        UC6["View keys in inventory"]
        UC7["Consume single-use key"]
        UC8["Keep reusable key"]
    end

    Player --> UC1
    Player --> UC2
    Player --> UC3
    Player --> UC4
    Player --> UC5
    Player --> UC6

    UC4 -. includes .-> LockCheck["Check matching lock id"]
    UC5 -. includes .-> LockCheck
    UC4 -. extends .-> UC7
    UC5 -. extends .-> UC7
    UC4 -. extends .-> UC8
    UC5 -. extends .-> UC8
    UC3 -. future source .-> SearchableLocation["Searchable location"]
```

## SSD: Pick Up Key From Map

```mermaid
sequenceDiagram
    actor Player
    participant System as DungeonKUrawler

    Player->>System: Move hero onto/near key
    Player->>System: Press take key / choose Take
    System->>System: Check key is takable
    System->>System: Check inventory has free slot

    alt Inventory has space
        System->>System: Remove key from map cell
        System->>System: Add key to inventory
        System-->>Player: Update map and inventory state
    else Inventory full
        System-->>Player: Show inventory full feedback
    end
```

## SSD: Take Key From Chest

```mermaid
sequenceDiagram
    actor Player
    participant System as DungeonKUrawler

    Player->>System: Open chest
    System-->>Player: Display chest grid
    Player->>System: Select key in chest slot
    System->>System: Check chest is accessible
    System->>System: Check inventory has free slot

    alt Transfer allowed
        System->>System: Remove key from chest contents
        System->>System: Add key to inventory
        System-->>Player: Refresh chest grid
    else Transfer blocked
        System-->>Player: Show failure feedback
    end
```

## SSD: Find Key In Searchable Location

```mermaid
sequenceDiagram
    actor Player
    participant System as DungeonKUrawler

    Player->>System: Interact with searchable location
    System->>System: Check location has not already been searched
    System->>System: Roll/reveal hidden contents

    alt Key found
        System-->>Player: Show discovered key
        Player->>System: Take key
        System->>System: Add key to inventory if space exists
    else Nothing useful found
        System-->>Player: Show empty/search result feedback
    end

    System->>System: Mark location searched if one-time searchable
```

## SSD: Open Gate With Key

```mermaid
sequenceDiagram
    actor Player
    participant System as DungeonKUrawler

    Player->>System: Interact with locked gate
    System->>System: Read gate requiredKeyId
    System->>System: Search hero inventory for matching keyId

    alt Matching key exists
        System-->>Player: Present "Open gate with <key>"
        Player->>System: Choose keyed open action
        System->>System: Unlock/open gate

        alt Key is single-use
            System->>System: Remove key from inventory
        else Key is reusable
            System->>System: Keep key in inventory
        end

        System-->>Player: Gate becomes passable
    else Matching key missing
        System-->>Player: Gate remains locked
    end
```

## SSD: Open Chest With Key

```mermaid
sequenceDiagram
    actor Player
    participant System as DungeonKUrawler

    Player->>System: Interact near locked chest
    System->>System: Read chest requiredKeyId
    System->>System: Search hero inventory for matching keyId

    alt Matching key exists
        System-->>Player: Present "Open <chest> with <key>"
        Player->>System: Choose keyed open action
        System->>System: Unlock/open chest
        System-->>Player: Display chest grid
    else Matching key missing
        System-->>Player: Do not show keyed open action
    end
```

## Logical Architecture

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer"]
        GamePanel["GamePanel\nmap rendering + interaction input"]
        InventoryDialog["InventoryDialog\nshows carried keys"]
        ChestDialog["ChestDialog\nshows keys inside containers"]
        SearchDialog["SearchDialog\nfuture searchable-location results"]
        SpriteRegistry["SpriteRegistry\nKey type to key sprite"]
        AssetManager["AssetManager\ncached key images"]
    end

    subgraph Controller["Application / Controller Layer"]
        InteractionController["InteractionController\ndiscovers available keyed actions"]
        InventoryController["InventoryController\npickup and transfer validation"]
        LockController["LockController\nfuture lock/key matching service"]
        GameEngine["GameEngine\ncoordinates state changes"]
    end

    subgraph Domain["Domain Model Layer"]
        Hero["Hero\ninventory + stats"]
        Inventory["Inventory\nowned keys/items"]
        Item["Item\nbase abstraction"]
        Key["Key\nkeyId + use policy"]
        Lockable["Lockable\nrequiredKeyId + locked state"]
        Gate["Gate\nblocks map passage"]
        Container["Container\nmay require a key"]
        SearchableLocation["SearchableLocation\nfuture hidden loot source"]
        DungeonMap["DungeonMap\ncells + gates + item locations"]
        GridCell["GridCell\nground items + entities"]
    end

    GamePanel --> InteractionController
    GamePanel --> SpriteRegistry
    InventoryDialog --> GameEngine
    ChestDialog --> GameEngine
    SearchDialog --> GameEngine
    SpriteRegistry --> AssetManager

    InteractionController --> LockController
    InteractionController --> GameEngine
    InventoryController --> GameEngine
    LockController --> Inventory
    LockController --> Lockable

    GameEngine --> Hero
    GameEngine --> DungeonMap
    GameEngine --> SearchableLocation
    Hero --> Inventory
    Inventory --> Item
    Key -- extends --> Item
    Container -- implements --> Lockable
    Gate -- implements --> Lockable
    DungeonMap --> GridCell
    GridCell --> Item
```

## GRASP Responsibility Assignment

```mermaid
flowchart LR
    subgraph GRASP
        IE["Information Expert"]
        ControllerPattern["Controller"]
        Creator["Creator"]
        LowCoupling["Low Coupling"]
        HighCohesion["High Cohesion"]
        Polymorphism["Polymorphism"]
    end

    IE --> Key["Key knows keyId, name, sprite category, and use policy"]
    IE --> Lockable["Gate/Container knows requiredKeyId and locked state"]
    IE --> Inventory["Inventory knows which keys the hero owns"]
    IE --> SearchableLocation["SearchableLocation knows whether it was searched and what it can reveal"]

    ControllerPattern --> InteractionController["InteractionController receives player interaction events"]
    ControllerPattern --> LockController["LockController answers canUnlock(lockable, inventory)"]
    ControllerPattern --> GameEngine["GameEngine applies unlock and transfer state changes"]

    Creator --> Chest["Chest/SearchableLocation creates or contains discovered Key instances"]
    Creator --> DungeonMap["DungeonMap places map keys and gate locks"]

    LowCoupling --> UI["UI asks controllers for available actions instead of checking lock rules itself"]
    HighCohesion --> LockControllerOnly["LockController focuses only on lock/key matching"]
    HighCohesion --> AssetOnly["AssetManager only loads/caches key images"]

    Polymorphism --> LockableAction["Gate and Chest can both be unlocked through the Lockable interface"]
```

## GoF Pattern View

```mermaid
flowchart TB
    Strategy["Strategy\nKeyUsePolicy: reusable or consumed"]
    State["State\nLocked / Unlocked / Open"]
    Observer["Observer\nGameEngine notifies map and inventory views"]
    Singleton["Singleton\nAssetManager shared cache"]
    Flyweight["Flyweight\nshared key sprites"]
    Registry["Registry\nSpriteRegistry maps Key variants to assets"]
    Factory["Factory\nfuture loot/search generation creates keys by id/type"]

    Strategy --> KeyUsePolicy["KeyUsePolicy"]
    State --> LockableState["Lockable state"]
    Observer --> GamePanel["GamePanel repaint"]
    Observer --> InventoryDialog["Inventory refresh"]
    Singleton --> AssetManager["AssetManager.get()"]
    Flyweight --> AssetManager
    Registry --> SpriteRegistry["SpriteRegistry.spriteFor(key)"]
    Factory --> LootFactory["LootFactory/SearchResultFactory"]
```

## Proposed Key Domain Model

```mermaid
classDiagram
    class Item {
        +String name
        +boolean isTakable()
    }

    class Key {
        +String keyId
        +KeyColor color
        +boolean singleUse
        +boolean matches(requiredKeyId)
    }

    class Inventory {
        +int capacity
        +hasFreeSlot()
        +containsKey(keyId)
        +findKey(keyId)
        +tryAdd(item)
        +remove(item)
    }

    class Hero {
        +Inventory inventory
    }

    class Lockable {
        <<interface>>
        +String requiredKeyId
        +boolean locked
        +unlockWith(key)
    }

    class Gate {
        +boolean passableWhenUnlocked
    }

    class Container {
        +int capacity
        +contents()
    }

    class SearchableLocation {
        +boolean searched
        +search()
    }

    Item <|-- Key
    Lockable <|.. Gate
    Lockable <|.. Container
    Hero "1" o-- "1" Inventory
    Inventory "1" o-- "0..*" Item
    Container "1" o-- "0..*" Item : contents
    SearchableLocation "1" o-- "0..*" Item : hiddenContents
```

## Key Source and Sink Flow

```mermaid
flowchart LR
    MapDrop["Map key drop"]
    ChestLoot["Chest contents"]
    SearchLoot["Searchable location\nfuture"]

    Inventory["Hero inventory"]

    GateLock["Locked gate"]
    ChestLock["Locked chest"]

    MapDrop --> Inventory
    ChestLoot --> Inventory
    SearchLoot --> Inventory

    Inventory --> GateLock
    Inventory --> ChestLock

    GateLock --> OpenPassage["Opened passage"]
    ChestLock --> OpenChest["Opened chest grid"]
```

## Suggested Key Types

```mermaid
flowchart TB
    Key["Key"]
    Olive["Olive key\ncommon doors or tutorial gate"]
    Silver["Silver key\nstandard gates"]
    Gold["Gold key\nimportant chests or boss route"]
    Orange["Orange key\narea-specific lock"]
    BentSilver["Bent silver key\nold/fragile/single-use lock"]
    LongGold["Long gold key\nmajor progression gate"]

    Key --> Olive
    Key --> Silver
    Key --> Gold
    Key --> Orange
    Key --> BentSilver
    Key --> LongGold
```

## Design Notes

- A `keyId` should be the real lock matcher; color/name should be presentation and game-feel metadata.
- Gates and chests should share a `Lockable` abstraction so the keyed action flow is not duplicated.
- The action list should only show `Open <target> with <key>` when the matching key exists in inventory.
- Searchable locations should be modeled as loot sources, like chests, but with search state and possible random/revealed results.
- Single-use keys add tension; reusable keys support region-wide progression. This should be a key property, not hardcoded in gate or chest logic.
