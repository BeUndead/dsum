# DSum Timer application

This is a first pass at making a visual DSum timer application, which can be used
live while playing the game (rather than pre-calculated charts).

![AppFull.png](images/DSumVF.png)

## Usage

A quick video shows the main usage:

1. [Scyther in Safari](https://drive.google.com/file/d/1a_D5PaYan9_6bSNX4HQiEpOfHb4vZFix/view?usp=sharing)

These show the basic use of the application.

### Core keys

- **[Space]** — You pressed this when the **battle wipe ends** (sync as closely as you can to the moment the encounter actually starts / the wipe animation finishes; the videos are the best guide).
- **[1]–[9], [0]** — When you dismiss **“Got away safely!”**, press the digit for the **slot you actually got** (**0** = slot 10). This **calibrates** the wheel: the timer infers where the DSum value was at encounter time and starts rotating from there in overworld (count-down) mode.

#### Optional adjustment keys (after you are used to the basics):

- **[Delete]** — **Clear calibration**: forget the calibrated slot, cancel an in-progress battle transition, clear suggested-slot highlights (until you calibrate again), and go back to pure overworld rotation. Does not change the current needle angle, game, route, lead level, Pikachu-lead flag, target slots, Yellow modifier, or outer-cycle band setting.

## Calibration in more detail

### 1. Choose what you are hunting

Use the **encounter slot toggles** under the wheel (or the compact strip) to select **one or more target slots**. Selected targets pulse on the ring. The timer will highlight when **any** of those slots intersect the **uncertainty wedge** (see below).

### 2. Start a calibration encounter

Enter a wild battle normally. When the wipe ends, press **Space**.

The wheel switches to **in-battle** behaviour: DSum advances at the in-battle rate while the needle is fixed at the top.

### 3. Finish on “Got away safely!”

Run from the encounter. When the message appears and you clear it, press the number key for the **slot you just saw** (**1**–**9**, **0** for slot 10).

From that moment the app:

1. Computes where the DSum **must have been** at encounter generation from the slot midpoint, your time in battle, and the **route’s** encounter data (see **Lead level** below for animation-length correction).
2. Centers the **uncertainty wedge** on the needle: a translucent band on the wheel whose width comes from **how wide the calibrated slot is**, **how long you stayed in battle**.
3. Returns to **overworld** rotation (DSum counting down between battles).

For other battle end types, there are buttons which (while 'In Battle' mode) you can press to indicate the battle exit type.

- [T] = Pokemon captured and joined your party (no nickname).
- [N] = Pokemon captured and joined your party (nicknamed).
- [B] = Pokemon captured and sent to PC.
- [R] = Pokemon ran (Safari Zone exclusive).

For each of these, press the button at any time while 'In Battle' mode; and continue to press the slot number that you encountered, when clearing trhe final text box.

### 4. Hunting

When **any** selected **target** overlaps the wedge, the UI treats that as “good to search”: background tint. **Suggested** slots are a separate hint; **targets** are drawn with an extra **green** highlight on top so your goal stays obvious even when several slots in the chain are highlighted in amber.

If your first calibration is a **very wide** slot, the wedge can be huge. A useful approach is to calibrate roughly on that slot, aim at any of the smaller slots, so that the large wedge covers multiple slots.  Then, you are likely to encounter a much smaller window on your next encounter.

### 5. Notes

The 'Lead Level' is only relevant if you are 3 or more levels lower than some Pokemon on the route.  This causes a different entry animation, so takes a different amount of time.

The 'threshold' is what likelihood of your targets appearing do you want the app to turn green on.  The default is 0.1 (or 10%) since (for example) certain slots can **never** go over this value (Slot 10 in the Safari Zone, for instance).


## Yellow

Support for Yellow Version is pending.