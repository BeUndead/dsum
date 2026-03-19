# DSum Timer application

This is a first pass at making a visual, DSum timer application, which can be used
live while playing the game (rather than pre-calculated charts).

## Usage

There are 2 videos:
1. Yellow (catching Pidgeotto) [here](https://drive.google.com/file/d/1z5Q3yE7RRjrLV01ZeNj4sZCCd5W_POTn/view?usp=sharing)
2. Red (catching Scyther / Chansey) [here](https://drive.google.com/file/d/10rm5AnlR5OPHUpbm7NOsEQ2mCx04gw7C/view?usp=sharing)

These show the basic use of the application.

The controls are:

* [SPACE] when entering an encounter (sync as best you can with the end of the wipe animation)
* [# KEY] when clearing the 'Got away safely' message.  Use the number of the slot you encountered (1-9, 0 for 10)


### To Calibrate

Select your target slot from the drop down menu.  The wedge of the wheel will
begin to pulse to highlight the slot.

Enter a single encounter, and press [SPACE] at the end of the wipe animation.
Exit the encounter promptly, and when clearing the 'Got away safely' message, press the
number key corresponding to the slot you encountered.

The application will then proceed rotating the DSum wheel, with the calibration in mind.
It will display a whitened, 'uncertainty wedge' around the arrow at the top.  When your
target slot overlaps this uncertainty wedge, search for encounters.  Other times, stop.
The background will also go green and a quiet hum play during this time.

#### Tip

If your first calibration is a wide slot, this leaves a large uncertainty wedge.  It can be useful to use this
rough calibration, to aim for any of the smaller slots, and 'recalibrate' off one of those.
