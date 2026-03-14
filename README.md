# DSum Timer application

This is a first pass at making a visual, DSum timer application, which can be used
live while playing the game (rather than pre-calculated charts).

## Usage

The video [here](https://drive.google.com/file/d/1Q7juBDOXhaetI1LQ6yf_H_A3MFWIgQsQ/view?usp=sharing) shows
the basic usage of the application.

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
