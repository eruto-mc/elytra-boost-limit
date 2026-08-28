# Elytra Boost Limit

Turns firework boosting into a way to **gain height** instead of a way to cross the map.

Minecraft 1.20.1 / Forge. MIT.

## What it does

- A rocket used while gliding stops pushing along your line of sight and pushes **straight up**
  instead, as hard as its Flight duration says. Horizontal speed is neither added nor removed.
- On top of that, a gliding player's **sideways** speed can be capped, so a straight line across
  the world stays slow while dives and climbs stay lively.
- **Gliding itself is never touched**, and vertical movement is never limited.
- Boosting can be switched off entirely per dimension.
- A boost key **bound to a mouse button works**. Deeper and Darker's Soul Elytra boost is asked
  for from `InputEvent.Key`, which Forge fires for the keyboard only, so binding it to a mouse
  button leaves it dead; this mod polls the key once per client tick instead. See below.

## Why it works this way

Rockets are cancelled at `EntityJoinLevelEvent`, i.e. the moment they spawn — not a tick later.
Removing them after they exist still applies one tick of acceleration.

The check is `instanceof FireworkRocketEntity`, not a list of entity IDs. Other mods subclass the
vanilla rocket (Alex's Mobs' enderiophage rocket and Alex's Caves' soda bottle rocket both do),
and an ID list silently misses them. Subclasses are covered automatically.

A rocket that is not attached to a gliding player is left alone, so ordinary fireworks and
crossbow rockets behave normally.

### Boost keys bound to a mouse button

Right-click is the natural place for a boost, since it is where a firework already goes. But
Deeper and Darker sends its Soul Elytra boost request from a handler on `InputEvent.Key`, and
Forge fires that event for keyboard input only — mouse buttons arrive as `InputEvent.MouseButton`
instead. Bind the boost to a mouse button and it never happens. Elytra Slot's Deeper and Darker
integration listens to the same event, so it stops working at the same moment.

With `fixMouseBoundBoostKey` on, this mod reads the key's pressed state once per client tick and
sends the boost request on the press. Polling does not care which device the key lives on.

Two guards keep it from misfiring:

- It only acts **while the key is bound to a mouse button**. On a keyboard binding those mods'
  own handlers already work, and doing nothing here is what keeps the boost from firing twice.
- It only sends **while gliding**. Right-click is also "use item", so it is pressed constantly;
  the server checks the same condition, and this just avoids the traffic.

Both Deeper and Darker's packet and Elytra Slot's are sent, because the two look at different
places: Deeper and Darker checks the chest slot, Elytra Slot checks the Curios slot, and neither
looks at the other's. Whichever one does not apply simply does nothing.

## Config

`config/elytra_boost_limit-common.toml`:

| Key | Default | |
| - | - | - |
| `blockedDimensions` | *(empty)* | Dimensions where boosting is switched off entirely |
| `requireEmptyHands` | | Whether the boost requires free hands |
| `fixMouseBoundBoostKey` | | Poll the Soul Elytra boost key so a mouse binding works |
| `verticalOnlyBoost` | | Redirect the boost straight up instead of along the view |
| `verticalBoostPerFlightLevel` | | How much height one Flight level gives |
| `speedLimitEnabled` | | Whether the horizontal cap applies |
| `horizontalLimit` | | Cap on sideways speed while gliding |
| `divingOrClimbingLimit` | | Looser cap while descending or climbing |
| `reliefAtVerticalSpeed` | | Vertical speed at which the cap relaxes |
| `limitCatchUpRate` | | How quickly an over-speed player is brought back down |
| `exemptDimensions` | *(empty)* | Dimensions where the cap does not apply |

Every value is commented in the generated file — read it there rather than here, since defaults
may change.

## Build

```bash
export JAVA_HOME=/path/to/jdk-17      # JDK 17 ならどれでもよい
./gradlew build --no-daemon
# → build/libs/elytra_boost_limit-<version>.jar
```

## Contributing

Issues and pull requests are welcome. This mod exists because a server wanted elytras to stay
useful without making every other form of travel pointless — if your server wants a different
balance, a config key is usually the right answer, and a request for one is a fine issue.
