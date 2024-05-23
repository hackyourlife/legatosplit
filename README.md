legatosplit
===========

This tool takes a MIDI file with a polyphonic part and splits it into
individual monophonic parts by re-assigning notes to different channels.
As a result, you can process a polyphonic part and prepare it for usage
with legato patches which usually require monophonic parts.

You might still have to swap notes between channels manually, because
the tool makes no attempts to track which notes should go onto the same
track. To do this, a much more sophisticated algorithm would be
required.

If the MIDI file has multiple tracks, every track will be processed
individually. The only requirement is that a single track only contains
notes for a single instrument.


Building
--------

```sh
javac LegatoSplit.java
```


Usage
-----

```sh
java LegatoSplit input.midi output.midi
```
