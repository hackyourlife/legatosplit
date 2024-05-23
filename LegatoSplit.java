import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class LegatoSplit {
	private Sequence seq;
	private Sequence out;

	public LegatoSplit() {
		seq = null;
	}

	public void load(Sequence seq) {
		this.seq = seq;
	}

	public void process() throws InvalidMidiDataException {
		out = new Sequence(seq.getDivisionType(), seq.getResolution(), seq.getTracks().length);

		for(Track track : seq.getTracks()) {
			process(track, out.createTrack());
		}
	}

	private void process(Track track, Track result) throws InvalidMidiDataException {
		// active notes
		boolean[] active = new boolean[128];
		int[] channels = new int[128];
		int channelmap = 0;
		int polyphony = 0;
		int maxpolyphony = 0;

		for(int i = 0; i < track.size(); i++) {
			MidiEvent evt = track.get(i);
			MidiMessage msg = evt.getMessage();
			if(msg instanceof ShortMessage) {
				ShortMessage m = (ShortMessage) msg;
				switch(m.getCommand()) {
					case ShortMessage.NOTE_ON: {
						int key = m.getData1();
						int velocity = m.getData2();
						int channel = 0;
						for(int j = 0; j < 16; j++) {
							if((channelmap & (1 << j)) == 0) {
								channel = j;
								break;
							}
						}
						if(velocity == 0) {
							if(!active[key]) {
								throw new RuntimeException("More NOTE_OFF than NOTE_ON detected");
							}
							channel = channels[key];
							active[key] = false;
							channelmap &= ~(1 << channel);
							polyphony--;
						} else {
							if(active[key]) {
								throw new RuntimeException("Note already on");
							}
							active[key] = true;
							channels[key] = channel;
							channelmap |= 1 << channel;
							polyphony++;
							if(polyphony > maxpolyphony) {
								maxpolyphony = polyphony;
							}
						}
						ShortMessage adjusted = new ShortMessage(m.getStatus(), channel, m.getData1(), m.getData2());
						result.add(new MidiEvent(adjusted, evt.getTick()));
						break;
					}
					case ShortMessage.NOTE_OFF: {
						int key = m.getData1();
						int channel = channels[key];
						if(!active[key]) {
							throw new RuntimeException("More NOTE_OFF than NOTE_ON detected");
						}
						active[key] = false;
						channelmap &= ~(1 << channel);
						polyphony--;
						ShortMessage adjusted = new ShortMessage(m.getStatus(), channel, m.getData1(), m.getData2());
						result.add(new MidiEvent(adjusted, evt.getTick()));
						break;
					}
				}
			} else {
				result.add(evt);
			}
		}

		System.out.println("Max polyphony for track: " + maxpolyphony);
	}

	public void write(File file) throws IOException {
		MidiSystem.write(out, 1, file);
	}

	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.out.println("LegatoSplit in.midi out.midi");
			System.exit(1);
		}

		LegatoSplit split = new LegatoSplit();
		split.load(MidiSystem.getSequence(new File(args[0])));
		split.process();
		split.write(new File(args[1]));
	}
}
