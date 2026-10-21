import React, { useEffect, useRef } from 'react';

/**
 * Reusable feedback toast component.
 * Props:
 *  - type: 'error' | 'success' | 'info'
 *  - message: string – text to display
 *  - onClose: () => void – callback when toast is dismissed (auto or manual)
 *  - duration: number – auto-dismiss time in ms (default 5000)
 *
 * Uses role="alert" with assertive announcement for errors, and
 * role="status" with polite announcement for routine status feedback.
 */
export const FeedbackToast = ({ type = 'info', message, onClose, duration = 5000 }) => {
  const onCloseRef = useRef(onClose);

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => {
      onCloseRef.current?.();
    }, duration);
    return () => clearTimeout(timer);
  }, [message, type, duration]);

  const typeClasses = {
    error: 'bg-[#fff1ef] border-[#d89a90] text-[#943827] ',
    success: 'bg-[#f0fdf4] border-[#a3d9a5] text-[#26734d] ',
    info: 'bg-[#f5f9ff] border-[#a3c4e9] text-[#1e4a8b] ',
  }[type] || 'bg-[#f5f9ff] border-[#a3c4e9] text-[#1e4a8b] ';

  const role = type === 'error' ? 'alert' : 'status';
  const ariaLive = type === 'error' ? 'assertive' : 'polite';

  return (
    <div
      role={role}
      aria-live={ariaLive}
      aria-atomic="true"
      className={`feedback-toast ${typeClasses}border p-3 rounded-md shadow-md flex items-start gap-2 max-w-xs`}
    >
      <span className="font-bold flex-shrink-0">{type.toUpperCase()}</span>
      <span className="flex-1">{message}</span>
      <button
        type="button"
        aria-label="Dismiss notification"
        onClick={() => onCloseRef.current?.()}
        className="ml-2 text-current hover:opacity-80 focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-1 rounded cursor-pointer"
      >
        ✕
      </button>
    </div>
  );
};

