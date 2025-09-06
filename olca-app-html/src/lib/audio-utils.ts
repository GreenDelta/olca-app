export function createAudioRecorder(): MediaRecorder | null {
  if (typeof MediaRecorder === 'undefined') {
    return null;
  }
  
  try {
    return new MediaRecorder(new MediaStream());
  } catch (error) {
    console.warn('Audio recording not supported:', error);
    return null;
  }
}

export function formatAudioDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

export function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = reader.result as string;
      resolve(result.split(',')[1]); // Remove data:audio/wav;base64, prefix
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

export function recordAudio(stream: MediaStream): Promise<Blob> & { stop: () => void } {
  const mediaRecorder = new MediaRecorder(stream);
  const chunks: BlobPart[] = [];
  let resolvePromise: (blob: Blob) => void;
  let rejectPromise: (error: Error) => void;

  const promise = new Promise<Blob>((resolve, reject) => {
    resolvePromise = resolve;
    rejectPromise = reject;
  }) as Promise<Blob> & { stop: () => void };

  mediaRecorder.ondataavailable = (event) => {
    chunks.push(event.data);
  };

  mediaRecorder.onstop = () => {
    const blob = new Blob(chunks, { type: 'audio/wav' });
    resolvePromise(blob);
  };

  mediaRecorder.onerror = (error) => {
    rejectPromise(new Error('Recording failed'));
  };

  mediaRecorder.start();

  promise.stop = () => {
    mediaRecorder.stop();
  };

  return promise;
}
