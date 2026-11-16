package com.tuneacademy.backend.scoring;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Turns a WAV file into an AudioClip. Only accepts 16-bit mono PCM,
 * which is what our recording page produces.
 */
public final class WavReader {

    private WavReader() {} // only a static method; nothing to create

    public static AudioClip read(InputStream in) throws IOException {
        // Java's built-in audio library reads the WAV header for us. It needs to peek
        // at the start of the stream to check the file type, which BufferedInputStream allows.
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(in))) {
            AudioFormat format = audio.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                    || format.getSampleSizeInBits() != 16
                    || format.getChannels() != 1) {
                throw new IllegalArgumentException("Expected a 16-bit mono WAV recording, got: " + format);
            }
            int sampleRate = Math.round(format.getSampleRate()); // from the file; never assumed
            return new AudioClip(toFloats(audio.readAllBytes(), format.isBigEndian()), sampleRate);
        } catch (UnsupportedAudioFileException e) {
            throw new IllegalArgumentException("That file isn't a WAV recording.", e);
        }
    }

    // Each sample is 2 bytes holding a whole number from -32768 to 32767.
    // Join the two bytes into that number, then divide so it's between -1 and 1.
    private static float[] toFloats(byte[] bytes, boolean bigEndian) {
        float[] samples = new float[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            byte first = bytes[2 * i];
            byte second = bytes[2 * i + 1];
            byte high = bigEndian ? first : second; // WAV files are normally little-endian: low byte first
            byte low = bigEndian ? second : first;
            short value = (short) ((high << 8) | (low & 0xFF));
            samples[i] = value / 32768f;
        }
        return samples;
    }
}
