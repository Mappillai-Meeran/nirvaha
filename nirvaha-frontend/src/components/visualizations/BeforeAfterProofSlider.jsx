import React, { useState, useRef, useCallback, useEffect } from 'react';
import { 
  CheckCircle2, 
  AlertTriangle, 
  Copy, 
  Check, 
  Camera, 
  Lock, 
  Info,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';

/**
 * BeforeAfterProofSlider
 * 
 * Interactive Split-Screen Resolution Verification Slider.
 * Shows side-by-side comparison between the original citizen report photo 
 * and the municipal contractor's post-repair evidence.
 * Displays SHA-256 cryptographic fingerprints, geographic distance, and verification audit result.
 */
export const BeforeAfterProofSlider = () => {
  const [sliderPos, setSliderPos] = useState(50); // percentage 0 to 100
  const [activeCaseIdx, setActiveCaseIdx] = useState(0);
  const [copiedHash, setCopiedHash] = useState(null);
  const [containerWidth, setContainerWidth] = useState(0);
  const isDragging = useRef(false);
  const containerRef = useRef(null);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (entry) {
        const measured = Math.round(entry.contentRect.width);
        setContainerWidth((prev) => (prev !== measured ? measured : prev));
      }
    });

    observer.observe(el);

    return () => {
      observer.disconnect();
    };
  }, []);

  // Curated representative cases demonstrating the 3 verification states
  const sampleCases = [
    {
      id: '1042',
      title: 'Mission Street Storm Drain Desilting & Overflow Clearance',
      ward: 'Ward 05 (V.O.C. Nagar)',
      department: 'Public Works Department (PWD)',
      status: 'VERIFIED',
      verificationScore: 94,
      distanceDeltaMeters: 14.2,
      timeElapsedHours: 26,
      beforePhoto: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=800&q=80',
      beforeCaption: 'Citizen Report: Stormwater drain completely choked with silt & plastic debris',
      beforeHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      beforeTime: '12 Oct 2026, 09:15 AM',
      beforeCoords: '11.9350° N, 79.8300° E',
      afterPhoto: 'https://images.unsplash.com/photo-1584467735871-8e85353a8413?auto=format&fit=crop&w=800&q=80',
      afterCaption: 'PWD Field Crew Proof: Silt excavated, grates re-seated, free flow restored',
      afterHash: '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4',
      afterTime: '13 Oct 2026, 11:30 AM',
      afterCoords: '11.9351° N, 79.8301° E',
      auditSummary: 'Submission distance (14m) within 500m municipal tolerance. Proximity and payload telemetry verify on-site remediation.'
    },
    {
      id: '1044',
      title: 'M.G. Road School Junction Deep Pothole Bitumen Patch',
      ward: 'Ward 18 (Orleanpet)',
      department: 'Roads & Infrastructure Maintenance',
      status: 'VERIFIED',
      verificationScore: 88,
      distanceDeltaMeters: 8.5,
      timeElapsedHours: 18,
      beforePhoto: 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=800&q=80',
      beforeCaption: 'Citizen Report: Severe pothole at school pedestrian crossing endangering two-wheelers',
      beforeHash: 'a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8',
      beforeTime: '11 Oct 2026, 02:40 PM',
      beforeCoords: '11.9318° N, 79.8271° E',
      afterPhoto: 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?auto=format&fit=crop&w=800&q=80',
      afterCaption: 'Contractor Proof: Dense cold-mix bitumen compaction and road marking completed',
      afterHash: '3c9909afec25354d551dae21590bb26e38d53f2173b8d3dc3eee4c047e7ab1c1',
      afterTime: '12 Oct 2026, 08:30 AM',
      afterCoords: '11.9319° N, 79.8272° E',
      auditSummary: 'Proximity verified (8.5m). Road leveling matches surrounding asphalt gradient.'
    },
    {
      id: '1043',
      title: 'Goubert Avenue Beach Promenade High-Mast LED Replacement',
      ward: 'Ward 11 (Goubert Nagar)',
      department: 'Electrical & Street Lighting Department',
      status: 'MANUAL_REVIEW',
      verificationScore: 68,
      distanceDeltaMeters: 48.0,
      timeElapsedHours: 42,
      beforePhoto: 'https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=800&q=80',
      beforeCaption: 'Citizen Report: Promenade light pole #42 dark for 3 consecutive nights',
      beforeHash: 'b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5',
      beforeTime: '10 Oct 2026, 08:10 PM',
      beforeCoords: '11.9382° N, 79.8334° E',
      afterPhoto: 'https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=800&q=80',
      afterCaption: 'Field Junior Upload: Photo captured from distance with wide illumination angle',
      afterHash: '7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e',
      afterTime: '12 Oct 2026, 02:15 PM',
      afterCoords: '11.9386° N, 79.8338° E',
      auditSummary: 'GPS shift 48m flagged for supervisory review. Pole serial number requires physical sign-off.'
    }
  ];

  const currentCase = sampleCases[activeCaseIdx];

  const handleCopy = (hash, label) => {
    navigator.clipboard.writeText(hash);
    setCopiedHash(label);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  const handleMouseDown = () => {
    isDragging.current = true;
  };

  const handleMouseUp = () => {
    isDragging.current = false;
  };

  const updatePosition = useCallback((clientX) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const pct = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setSliderPos(pct);
  }, []);

  const handleMouseMove = useCallback((e) => {
    if (!isDragging.current) return;
    updatePosition(e.clientX);
  }, [updatePosition]);

  const handleTouchMove = useCallback((e) => {
    if (e.touches && e.touches[0]) {
      updatePosition(e.touches[0].clientX);
    }
  }, [updatePosition]);

  return (
    <div className="bg-white rounded-xl border border-[#d8ddda] p-6 shadow-sm">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-[#e5e8e6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#b94d36]/10 text-[#b94d36]">
              <Lock size={13} />
              IMMUTABLE AUDIT TRAIL
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
              SHA-256 Provenance Proof
            </span>
          </div>
          <h3 className="text-xl font-bold text-[#1e2b2d] mt-1.5">
            Before-and-After Resolution Verification Slider
          </h3>
          <p className="text-xs text-[#596765] mt-0.5">
            Drag the split-slider to inspect ground truth evidence before cases are officially closed.
          </p>
        </div>

        {/* Case Switcher */}
        <div className="flex items-center gap-1 bg-[#f4f5f3] p-1 rounded-lg border border-[#d8ddda] self-start sm:self-center">
          {sampleCases.map((c, idx) => (
            <button
              key={c.id}
              type="button"
              onClick={() => setActiveCaseIdx(idx)}
              className={`px-2.5 py-1 text-xs font-bold rounded-md transition ${
                activeCaseIdx === idx 
                  ? 'bg-[#174f4a] text-white shadow-xs' 
                  : 'text-[#596765] hover:text-[#1e2b2d]'
              }`}
            >
              Case #{c.id}
            </button>
          ))}
        </div>
      </div>

      {/* Case Overview Bar */}
      <div className="py-3 px-4 my-4 bg-[#f8faf9] rounded-lg border border-[#e1e8e5] flex flex-wrap items-center justify-between gap-3 text-xs">
        <div>
          <strong className="text-[#1e2b2d] font-bold text-sm block">{currentCase.title}</strong>
          <span className="text-[#66716e]">{currentCase.ward} • {currentCase.department}</span>
        </div>

        <div className="flex items-center gap-2">
          {currentCase.status === 'VERIFIED' ? (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
              <CheckCircle2 size={14} />
              VERIFIED ({currentCase.verificationScore}% Confidence)
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-300">
              <AlertTriangle size={14} />
              MANUAL SUPERVISORY REVIEW ({currentCase.verificationScore}%)
            </span>
          )}
        </div>
      </div>

      {/* Interactive Split-Screen Image Slider */}
      <div 
        ref={containerRef}
        className="relative w-full h-[360px] sm:h-[420px] rounded-xl overflow-hidden select-none border border-[#cfd6d2] shadow-inner bg-slate-900 cursor-ew-resize"
        onMouseDown={handleMouseDown}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onMouseMove={handleMouseMove}
        onTouchMove={handleTouchMove}
      >
        {/* Background Layer: "AFTER" (Post-Repair Evidence) */}
        <div className="absolute inset-0 w-full h-full">
          <img 
            src={currentCase.afterPhoto} 
            alt="Post-Repair Work" 
            className="w-full h-full object-cover"
          />
          <div className="absolute top-4 right-4 bg-emerald-900/90 text-white text-xs font-bold px-3 py-1.5 rounded-md backdrop-blur-sm shadow-md border border-emerald-500/30 flex items-center gap-1.5">
            <CheckCircle2 size={14} className="text-emerald-300" />
            <span>AFTER (Field Crew Repair)</span>
          </div>
        </div>

        {/* Foreground Layer: "BEFORE" (Clipped by slider position) */}
        <div 
          className="absolute inset-0 h-full overflow-hidden"
          style={{ width: `${sliderPos}%` }}
        >
          <img 
            src={currentCase.beforePhoto} 
            alt="Initial Citizen Complaint" 
            className="absolute inset-0 w-full h-full object-cover max-w-none"
            style={{ width: containerWidth ? `${containerWidth}px` : '100%' }}
          />
          <div className="absolute top-4 left-4 bg-rose-900/90 text-white text-xs font-bold px-3 py-1.5 rounded-md backdrop-blur-sm shadow-md border border-rose-500/30 flex items-center gap-1.5">
            <Camera size={14} className="text-rose-300" />
            <span>BEFORE (Citizen Report)</span>
          </div>
        </div>

        {/* Draggable Divider Handle */}
        <div 
          className="absolute top-0 bottom-0 w-1 bg-white shadow-[0_0_10px_rgba(0,0,0,0.5)] z-20"
          style={{ left: `${sliderPos}%` }}
        >
          <div className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-8 h-8 rounded-full bg-[#174f4a] text-white border-2 border-white shadow-lg flex items-center justify-center cursor-ew-resize">
            <div className="flex items-center -space-x-1">
              <ChevronLeft size={14} />
              <ChevronRight size={14} />
            </div>
          </div>
        </div>

        {/* Drag Hint overlay */}
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 bg-black/75 text-white/90 text-[11px] px-3 py-1 rounded-full backdrop-blur-xs pointer-events-none">
          ↔ Drag slider left/right to compare ground evidence
        </div>
      </div>

      {/* Forensic Metadata & SHA-256 Audit Panel */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
        {/* Before Metadata */}
        <div className="p-3.5 rounded-lg bg-[#fafafa] border border-[#e5e8e6] text-xs">
          <div className="flex items-center justify-between font-bold text-[#be123c] mb-2">
            <span>ORIGINAL REPORT EVIDENCE</span>
            <span className="text-[#66716e] font-normal">{currentCase.beforeTime}</span>
          </div>
          <p className="text-[#394644] text-xs mb-2 italic">"{currentCase.beforeCaption}"</p>
          <div className="space-y-1.5 font-mono text-[11px]">
            <div className="flex items-center justify-between text-[#66716e]">
              <span>Geo Coordinates:</span>
              <strong className="text-[#1e2b2d]">{currentCase.beforeCoords}</strong>
            </div>
            <div className="p-2 bg-white rounded border border-[#d8ddda] flex items-center justify-between gap-2">
              <span className="truncate text-[#596765]" title={currentCase.beforeHash}>
                SHA-256: {currentCase.beforeHash.substring(0, 20)}...
              </span>
              <button 
                type="button" 
                onClick={() => handleCopy(currentCase.beforeHash, 'before')}
                className="text-[#174f4a] hover:text-[#0f3531] p-1"
                title="Copy full cryptographic hash"
              >
                {copiedHash === 'before' ? <Check size={14} className="text-emerald-600" /> : <Copy size={14} />}
              </button>
            </div>
          </div>
        </div>

        {/* After Metadata */}
        <div className="p-3.5 rounded-lg bg-[#fafafa] border border-[#e5e8e6] text-xs">
          <div className="flex items-center justify-between font-bold text-[#15803d] mb-2">
            <span>POST-REPAIR EVIDENCE</span>
            <span className="text-[#66716e] font-normal">{currentCase.afterTime}</span>
          </div>
          <p className="text-[#394644] text-xs mb-2 italic">"{currentCase.afterCaption}"</p>
          <div className="space-y-1.5 font-mono text-[11px]">
            <div className="flex items-center justify-between text-[#66716e]">
              <span>Geo Coordinates (Delta: {currentCase.distanceDeltaMeters}m):</span>
              <strong className="text-[#1e2b2d]">{currentCase.afterCoords}</strong>
            </div>
            <div className="p-2 bg-white rounded border border-[#d8ddda] flex items-center justify-between gap-2">
              <span className="truncate text-[#596765]" title={currentCase.afterHash}>
                SHA-256: {currentCase.afterHash.substring(0, 20)}...
              </span>
              <button 
                type="button" 
                onClick={() => handleCopy(currentCase.afterHash, 'after')}
                className="text-[#174f4a] hover:text-[#0f3531] p-1"
                title="Copy full cryptographic hash"
              >
                {copiedHash === 'after' ? <Check size={14} className="text-emerald-600" /> : <Copy size={14} />}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Critical Government Truth Guardrail Callout */}
      <div className="mt-4 p-3.5 rounded-lg bg-[#f0f5f4] border border-[#cadfdc] flex items-start gap-2.5 text-xs text-[#1e2b2d]">
        <Info size={16} className="text-[#174f4a] shrink-0 mt-0.5" />
        <div className="leading-relaxed">
          <strong>Integrity Transparency Note for Municipal Officers & Judges:</strong> A cryptographic SHA-256 hash mathematically proves that 
          the photo file has not been manipulated or swapped in transit. It does <em>not</em> prove by itself that the physical work was done honestly. 
          To prevent fraudulent closures, Nirvaha cross-references the photo's submission coordinate proximity delta (within 500m municipal tolerance) and 
          mandates a 72-hour citizen sign-off window before the work order is archived.
        </div>
      </div>
    </div>
  );
};
