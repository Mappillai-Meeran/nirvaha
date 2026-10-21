import React, { useEffect, useRef, useState } from 'react';
import { Camera, RotateCcw, Video, X } from 'lucide-react';

export const CameraCapture = ({ onCapture }) => {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);
  const [error, setError] = useState(null);

  const stopCamera = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  };

  useEffect(() => () => stopCamera(), []);

  const openCamera = async () => {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
      streamRef.current = stream;
      setIsOpen(true);
      requestAnimationFrame(() => { if (videoRef.current) videoRef.current.srcObject = stream; });
    } catch (cameraError) {
      setError(cameraError.name === 'NotAllowedError' ? 'Camera access was not allowed. You can still choose a photo from your device.' : 'We could not start the camera. Please choose a photo from your device.');
    }
  };

  const capture = () => {
    const video = videoRef.current;
    if (!video?.videoWidth) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      if (!blob) return;
      onCapture(new File([blob], `nirvaha-capture-${Date.now()}.jpg`, { type: 'image/jpeg' }));
      stopCamera();
      setIsOpen(false);
    }, 'image/jpeg', 0.9);
  };

  return (
    <div className="space-y-2">
      <button type="button" onClick={openCamera} className="location-button"><Camera size={14} />Use camera</button>
      {error && <p className="text-xs text-[#943827]">{error}</p>}
      {isOpen && <div className="camera-capture" role="dialog" aria-label="Capture evidence photo">
        <div className="camera-toolbar"><span><Video size={15} /> Camera preview</span><button type="button" onClick={() => { stopCamera(); setIsOpen(false); }} aria-label="Close camera"><X size={17} /></button></div>
        <video ref={videoRef} autoPlay playsInline muted className="camera-video" />
        <div className="camera-actions"><button type="button" onClick={capture}><Camera size={17} />Capture photo</button><button type="button" onClick={() => { stopCamera(); setIsOpen(false); }}><RotateCcw size={16} />Cancel</button></div>
      </div>}
    </div>
  );
};
