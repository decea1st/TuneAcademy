import { useEffect, useRef, useState } from "react";
import { encodeWav, toMono } from "@/lib/wav";

/**
 * Records the microphone and hands back a 16-bit mono WAV, ready to upload.
 * Stops by itself after `maxSeconds`, and trims to exactly that, so the backend's
 * 30-second limit can't be tripped by a timer firing a few milliseconds late.
 */
export function useRecorder(maxSeconds: number) {
  const [recording, setRecording] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [wav, setWav] = useState<Blob | null>(null);
  const [error, setError] = useState<string | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const autoStopRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  async function start() {
    setError(null);
    setWav(null);

    let stream: MediaStream;
    try {
      // The browser's "call clean-up" is all off: it would muffle or reshape the notes we score.
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false,
          channelCount: 1,
        },
      });
    } catch {
      setError("Microphone access denied. Allow mic permission and try again.");
      return;
    }
    streamRef.current = stream;

    const chunks: Blob[] = [];
    const recorder = new MediaRecorder(stream);
    recorderRef.current = recorder;
    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data);
    };
    recorder.onstop = async () => {
      stream.getTracks().forEach((t) => t.stop()); // turns the mic light off
      try {
        // The browser records compressed audio (webm/ogg). Decode it back to raw samples, then save as WAV.
        const ctx = new AudioContext();
        const audio = await ctx.decodeAudioData(await new Blob(chunks).arrayBuffer());
        void ctx.close();
        const samples = toMono(audio).subarray(0, maxSeconds * audio.sampleRate);
        setWav(encodeWav(samples, audio.sampleRate));
      } catch {
        setError("Couldn't process the recording. Try again.");
      }
    };

    recorder.start();
    setSeconds(0);
    setRecording(true);
    autoStopRef.current = setTimeout(stop, maxSeconds * 1000);
  }

  function stop() {
    if (autoStopRef.current) clearTimeout(autoStopRef.current);
    if (recorderRef.current?.state === "recording") recorderRef.current.stop();
    setRecording(false);
  }

  // The on-screen seconds counter.
  useEffect(() => {
    if (!recording) return;
    const timer = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => clearInterval(timer);
  }, [recording]);

  // Leaving the page mid-recording releases the mic.
  useEffect(
    () => () => {
      if (autoStopRef.current) clearTimeout(autoStopRef.current);
      if (recorderRef.current?.state === "recording") recorderRef.current.stop();
      streamRef.current?.getTracks().forEach((t) => t.stop());
    },
    [],
  );

  return { recording, seconds, wav, error, start, stop };
}
