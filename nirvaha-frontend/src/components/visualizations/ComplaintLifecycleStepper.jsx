import React, { useState } from 'react';
import { 
  Clock, 
  Shield
} from 'lucide-react';

/**
 * ComplaintLifecycleStepper
 * 
 * Interactive 5-Stage Municipal Redressal Milestone Tracker.
 * 1. Complaint Submitted (Voice preserved)
 * 2. Officer & Department Assigned (Gazette routing)
 * 3. Field Work in Progress (Crew dispatched)
 * 4. Resolution Evidence Submitted (Cryptographic photo)
 * 5. Resolution Verified / Reopened (Algorithmic & Citizen audit)
 * 
 * Protects citizen privacy by sanitizing personal phone numbers and officer details.
 */
export const ComplaintLifecycleStepper = ({ onSelectCase: _onSelectCase }) => {
  const [selectedCaseId, setSelectedCaseId] = useState('1042');

  // Curated representative cases demonstrating diverse states of the 5-stage pipeline
  const casesData = {
    '1042': {
      id: '1042',
      title: 'Mission Street Storm Drain Overflow Clearance',
      category: 'DRAINAGE',
      ward: 'Ward 05 (V.O.C. Nagar)',
      currentStep: 5, // Fully verified closed
      status: 'VERIFIED',
      slaStatus: 'COMPLETED_ON_TIME',
      slaTargetHours: 48,
      slaElapsedHours: 26,
      timeline: [
        {
          step: 1,
          name: 'Grievance Registered',
          date: '12 Oct 2026, 09:15 AM',
          details: 'Reported in Tamil via mobile web. Raw voice normalized to DRAINAGE. SHA-256 evidence registered.',
          actor: 'Citizen (Masked ID: CIT-***82)',
          completed: true
        },
        {
          step: 2,
          name: 'Department & Ward Assigned',
          date: '12 Oct 2026, 09:30 AM',
          details: 'Point-in-Polygon GIS matched to Gazette Ward 05. Assigned to PWD Stormwater Division.',
          actor: 'Automated Gazette GIS Engine',
          completed: true
        },
        {
          step: 3,
          name: 'Field Operations In Progress',
          date: '12 Oct 2026, 01:15 PM',
          details: 'Excavation crew and suction tanker dispatched to Mission Street canal inlet.',
          actor: 'Field Crew Team A (Lead: Officer #108)',
          completed: true
        },
        {
          step: 4,
          name: 'Post-Repair Evidence Uploaded',
          date: '13 Oct 2026, 11:30 AM',
          details: 'Excavated silt photo uploaded with GPS geotag (11.9351° N, 79.8301° E). SHA-256: 8f4343...aa4',
          actor: 'Junior Engineer (Field Supervisor)',
          completed: true
        },
        {
          step: 5,
          name: 'Resolution Verified & Sign-Off',
          date: '13 Oct 2026, 11:45 AM',
          details: 'Algorithmic proximity matched (14m delta). Case closed under 72-hour citizen verification window.',
          actor: 'Nirvaha Trust Engine & Citizen Sign-Off',
          completed: true,
          outcome: 'VERIFIED'
        }
      ]
    },
    '1044': {
      id: '1044',
      title: 'M.G. Road School Junction Pothole Bitumen Patch',
      category: 'POTHOLE',
      ward: 'Ward 18 (Orleanpet)',
      currentStep: 3, // In progress
      status: 'IN_PROGRESS',
      slaStatus: 'ON_TRACK',
      slaTargetHours: 36,
      slaElapsedHours: 14,
      timeline: [
        {
          step: 1,
          name: 'Grievance Registered',
          date: '12 Oct 2026, 06:20 AM',
          details: 'Reported in Hindi. High severity tag applied due to school pedestrian zone.',
          actor: 'Citizen (Masked ID: CIT-***41)',
          completed: true
        },
        {
          step: 2,
          name: 'Department & Ward Assigned',
          date: '12 Oct 2026, 07:05 AM',
          details: 'Assigned to Roads & Infrastructure Maintenance Wing. Priority Score: 88/100.',
          actor: 'Priority Scoring Service',
          completed: true
        },
        {
          step: 3,
          name: 'Field Operations In Progress',
          date: '12 Oct 2026, 10:45 AM',
          details: 'Cold-mix asphalt batch allocated. Caution cones placed around crater.',
          actor: 'Road Patching Unit #4',
          completed: true,
          active: true
        },
        {
          step: 4,
          name: 'Post-Repair Evidence Uploaded',
          date: 'Pending',
          details: 'Awaiting field crew upload of completed road compaction photo.',
          actor: 'Awaiting Field Upload',
          completed: false
        },
        {
          step: 5,
          name: 'Resolution Verified & Sign-Off',
          date: 'Pending',
          details: 'Proximity and photo verification pending completion of repair.',
          actor: 'Trust Engine Audit',
          completed: false
        }
      ]
    },
    '1043': {
      id: '1043',
      title: 'Goubert Avenue Beach Light Pole #42 Inspection',
      category: 'STREETLIGHT',
      ward: 'Ward 11 (Goubert Nagar)',
      currentStep: 5, // Reopened/Manual review
      status: 'MANUAL_REVIEW',
      slaStatus: 'APPROACHING_DEADLINE',
      slaTargetHours: 48,
      slaElapsedHours: 42,
      timeline: [
        {
          step: 1,
          name: 'Grievance Registered',
          date: '10 Oct 2026, 08:10 PM',
          details: 'Reported via web portal. Night photo uploaded showing dark carriageway.',
          actor: 'Citizen (Masked ID: CIT-***99)',
          completed: true
        },
        {
          step: 2,
          name: 'Department & Ward Assigned',
          date: '11 Oct 2026, 09:00 AM',
          details: 'Assigned to Municipal Electrical Division for ballast & LED fixture check.',
          actor: 'Municipal Dispatcher',
          completed: true
        },
        {
          step: 3,
          name: 'Field Operations In Progress',
          date: '11 Oct 2026, 03:30 PM',
          details: 'High-reach bucket truck dispatched to Goubert Avenue.',
          actor: 'Electrical Team #2',
          completed: true
        },
        {
          step: 4,
          name: 'Post-Repair Evidence Uploaded',
          date: '12 Oct 2026, 02:15 PM',
          details: 'Replacement photo uploaded from 48m distance. GPS divergence flagged.',
          actor: 'Field Contractor',
          completed: true
        },
        {
          step: 5,
          name: 'Flagged for Supervisory Inspection',
          date: '12 Oct 2026, 02:30 PM',
          details: 'Confidence score (68%) below automated closure threshold. Dispatched for physical officer audit.',
          actor: 'Integrity Anomaly Engine',
          completed: true,
          outcome: 'MANUAL_REVIEW'
        }
      ]
    }
  };

  const activeCase = casesData[selectedCaseId] || casesData['1042'];

  return (
    <div className="bg-white rounded-xl border border-[#d8ddda] p-6 shadow-sm">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-5 border-b border-[#e5e8e6]">
        <div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-[#174f4a]/10 text-[#174f4a]">
              <Clock size={13} />
              SLA & TRANSPARENCY
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
              5-Stage Redressal Architecture
            </span>
          </div>
          <h3 className="text-xl font-bold text-[#1e2b2d] mt-1.5">
            Interactive Five-Stage Complaint Progress Stepper
          </h3>
          <p className="text-xs text-[#596765] mt-0.5">
            Trace the complete lifecycle of a reported grievance with verified timestamps and accountable actors.
          </p>
        </div>

        {/* Demo Case Switcher */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-[#596765]">Inspect Case:</span>
          <div className="flex items-center gap-1 bg-[#f0f4f3] p-1 rounded-lg border border-[#cfdad7]">
            {Object.keys(casesData).map((id) => (
              <button
                key={id}
                type="button"
                onClick={() => setSelectedCaseId(id)}
                className={`px-2.5 py-1 text-xs font-bold rounded-md transition ${
                  selectedCaseId === id 
                    ? 'bg-[#174f4a] text-white shadow-xs' 
                    : 'text-[#394644] hover:text-[#174f4a]'
                }`}
              >
                #{id}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Case Header Details & SLA Bar */}
      <div className="my-5 p-4 rounded-xl bg-[#fafafa] border border-[#e5e8e6]">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-bold px-2 py-0.5 bg-[#e5e8e6] text-[#1e2b2d] rounded">
                CASE #{activeCase.id}
              </span>
              <span className="text-xs font-bold text-[#174f4a]">{activeCase.category}</span>
              <span className="text-xs text-[#66716e]">• {activeCase.ward}</span>
            </div>
            <h4 className="text-sm font-bold text-[#1e2b2d] mt-1">
              {activeCase.title}
            </h4>
          </div>

          {/* SLA Clock Indicator */}
          <div className="flex items-center gap-3 self-start md:self-center">
            <div className="text-right text-xs">
              <div className="text-[11px] text-[#66716e]">Municipal SLA Buffer</div>
              <strong className="text-[#1e2b2d]">
                {activeCase.slaElapsedHours}h elapsed / {activeCase.slaTargetHours}h standard
              </strong>
            </div>

            <div className={`px-3 py-1.5 rounded-lg text-xs font-bold border flex items-center gap-1.5 ${
              activeCase.slaStatus === 'COMPLETED_ON_TIME'
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                : activeCase.slaStatus === 'ON_TRACK'
                ? 'bg-sky-50 text-sky-800 border-sky-300'
                : 'bg-amber-50 text-amber-800 border-amber-300'
            }`}>
              <Clock size={13} />
              <span>{activeCase.slaStatus.replace(/_/g, ' ')}</span>
            </div>
          </div>
        </div>
      </div>

      {/* 5-Stage Stepper Visualization */}
      <div className="relative py-4">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 relative z-10">
          {activeCase.timeline.map((step) => {
            const isCompleted = step.completed && !step.active;
            const isActive = step.active;

            return (
              <div 
                key={step.step}
                className={`flex flex-col justify-between p-3.5 rounded-xl border transition ${
                  isActive 
                    ? 'bg-[#174f4a]/5 border-[#174f4a] shadow-sm ring-1 ring-[#174f4a]'
                    : isCompleted
                    ? 'bg-white border-[#d8ddda]'
                    : 'bg-[#fafafa] border-[#e8ebe9] opacity-70'
                }`}
              >
                <div>
                  {/* Step Number & Badge */}
                  <div className="flex items-center justify-between mb-2">
                    <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                      isCompleted 
                        ? 'bg-emerald-600 text-white' 
                        : isActive 
                        ? 'bg-[#174f4a] text-white animate-pulse' 
                        : 'bg-[#e5e8e6] text-[#66716e]'
                    }`}>
                      {isCompleted ? '✓' : step.step}
                    </span>

                    <span className="text-[10px] font-mono text-[#66716e]">
                      STAGE 0{step.step}
                    </span>
                  </div>

                  {/* Title */}
                  <h5 className="text-xs font-bold text-[#1e2b2d] leading-snug">
                    {step.name}
                  </h5>

                  {/* Date/Time */}
                  <div className="text-[10px] text-[#66716e] mt-1 flex items-center gap-1 font-mono">
                    <Clock size={10} />
                    <span>{step.date}</span>
                  </div>

                  {/* Operational Details */}
                  <p className="text-[11px] text-[#596765] mt-2 leading-relaxed">
                    {step.details}
                  </p>
                </div>

                {/* Accountable Actor (Sanitized) */}
                <div className="mt-3 pt-2.5 border-t border-[#f0f2ee] text-[10px] text-[#66716e]">
                  <span className="block font-semibold text-[#1e2b2d]">Accountable Actor:</span>
                  <span className="truncate block" title={step.actor}>{step.actor}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Citizen Privacy Safeguard Note */}
      <div className="mt-4 p-3 bg-[#f8faf9] rounded-lg border border-[#e1e8e5] flex items-center justify-between text-xs text-[#596765]">
        <div className="flex items-center gap-2">
          <Shield size={14} className="text-[#174f4a]" />
          <span>
            <strong>Citizen Privacy Shield Active:</strong> Personal mobile numbers and residential house numbers are automatically redacted from public milestones in compliance with the Digital Personal Data Protection Act.
          </span>
        </div>
      </div>
    </div>
  );
};
