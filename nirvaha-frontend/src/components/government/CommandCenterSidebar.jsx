import React from 'react';
import { 
  Map, 
  Zap, 
  FileText, 
  ShieldAlert, 
  CloudRain, 
  Layers, 
  Shuffle, 
  Database, 
  Sparkles, 
  LogOut, 
  ChevronRight,
  BarChart3,
  ShieldCheck,
  Clock
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

/**
 * CommandCenterSidebar
 * 
 * Persistent Left Command Sidebar inspired by Sentinella Maps.
 * Styled in dark forest emerald (#102D27) with deep green active states (#17483B).
 * Organizes municipal command into:
 * 1. OPERATIONS
 * 2. INTELLIGENCE & ANALYSIS
 * 3. GOVERNANCE & AUDIT
 */
export const CommandCenterSidebar = ({
  activeNav = 'map',
  onSelectNav,
  actionCount = 0,
  incidentCount = 0,
  anomalyCount = 0,
  signalCount = 0,
  onOpenJudgeDemo,
  onOpenProvenance,
  isSimulationActive = false,
  isMobileOpen = false,
  onCloseMobile
}) => {
  const { currentUser, logout } = useAuth();

  const handleNavClick = (id) => {
    onSelectNav(id);
    if (onCloseMobile) onCloseMobile();
  };

  const navItemClass = (id) => `
    w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold transition
    ${activeNav === id 
      ? 'bg-[#17483B] text-white shadow-xs' 
      : 'text-[#A3B8B2] hover:bg-[#153B34] hover:text-white'
    }
  `;

  return (
    <>
      {/* Mobile Backdrop */}
      {isMobileOpen && (
        <div 
          className="fixed inset-0 bg-black/60 z-40 md:hidden backdrop-blur-xs"
          onClick={onCloseMobile}
        />
      )}

      <aside className={`
        ${isMobileOpen ? 'fixed inset-y-0 left-0 z-50 flex' : 'hidden md:flex'} 
        w-64 bg-[#102D27] text-white flex-col justify-between shrink-0 border-r border-[#153B34] h-full
      `}>
        {/* Top Header / Branding + Nav — grows, nav scrolls internally */}
        <div className="flex flex-col flex-1 min-h-0">
          <div className="p-4 border-b border-[#153B34]">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-[#17483B] border border-[#236353] flex items-center justify-center text-white shadow-sm">
                  <ShieldCheck size={18} className="text-[#34D399]" />
                </div>
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-sm tracking-wide text-white">NIRVAHA ICCC</span>
                  </div>
                  <span className="text-[10px] text-[#A3B8B2] block leading-tight">
                    Territorial Command & Control
                  </span>
                </div>
              </div>

              {isMobileOpen && (
                <button 
                  onClick={onCloseMobile}
                  className="md:hidden text-[#A3B8B2] hover:text-white p-1 rounded"
                >
                  ✕
                </button>
              )}
            </div>

          {/* Operational Heartbeat Pill */}
          <div className="mt-3 flex items-center justify-between px-2 py-1 rounded bg-[#0A1E1A] border border-[#153B34] text-[10px]">
            <span className="flex items-center gap-1.5 text-[#34D399] font-bold">
              <span className="w-1.5 h-1.5 rounded-full bg-[#34D399] animate-pulse" />
              SYSTEM OPERATIONAL
            </span>
            <span className="text-[#A3B8B2] font-mono">PUDUCHERRY</span>
          </div>
        </div>

        {/* Navigation Categories */}
        <div className="p-3 space-y-4 overflow-y-auto flex-1 min-h-0">
          {/* Group 1: OPERATIONS */}
          <div className="space-y-1">
            <span className="px-3 text-[10px] font-bold uppercase tracking-wider text-[#688A81] block mb-1.5">
              Operação / Operations
            </span>

            <button
              type="button"
              onClick={() => handleNavClick('map')}
              className={navItemClass('map')}
            >
              <span className="flex items-center gap-2.5">
                <Map size={15} />
                <span>Territorial Risk Map</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#A3B8B2] font-mono">
                GIS
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('recommendations')}
              className={navItemClass('recommendations')}
            >
              <span className="flex items-center gap-2.5">
                <Zap size={15} className="text-[#FBBF24]" />
                <span>Priority Action Queue</span>
              </span>
              {actionCount > 0 && (
                <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#B94D36] text-white font-bold">
                  {actionCount}
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('all-incidents')}
              className={navItemClass('all-incidents')}
            >
              <span className="flex items-center gap-2.5">
                <FileText size={15} />
                <span>All Open Incidents</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#A3B8B2] font-mono">
                {incidentCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('integrity')}
              className={navItemClass('integrity')}
            >
              <span className="flex items-center gap-2.5">
                <ShieldAlert size={15} className="text-[#34D399]" />
                <span>Integrity QA Radar</span>
              </span>
              {anomalyCount > 0 && (
                <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#C58A32] text-white font-bold">
                  {anomalyCount}
                </span>
              )}
            </button>
          </div>

          {/* Group 2: INTELLIGENCE & ANALYSIS */}
          <div className="space-y-1 pt-2 border-t border-[#153B34]">
            <span className="px-3 text-[10px] font-bold uppercase tracking-wider text-[#688A81] block mb-1.5">
              Análise / Intelligence
            </span>

            <button
              type="button"
              onClick={() => handleNavClick('jurisdiction-matrix')}
              className={navItemClass('jurisdiction-matrix')}
            >
              <span className="flex items-center gap-2.5">
                <Shuffle size={15} />
                <span>Inter-Agency Matrix</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#A3B8B2] font-mono">
                PWD/LAD
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('duplicate-equity')}
              className={navItemClass('duplicate-equity')}
            >
              <span className="flex items-center gap-2.5">
                <Layers size={15} />
                <span>Duplicate & Equity</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#A3B8B2] font-mono">
                {signalCount}
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('simulator')}
              className={navItemClass('simulator')}
            >
              <span className="flex items-center gap-2.5">
                <CloudRain size={15} className={isSimulationActive ? 'text-amber-400' : ''} />
                <span>Monsoon Simulator</span>
              </span>
              {isSimulationActive && (
                <span className="text-[9px] px-1.5 py-0.2 rounded bg-amber-500 text-black font-black animate-pulse">
                  SIM
                </span>
              )}
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('historical-rainfall')}
              className={navItemClass('historical-rainfall')}
            >
              <span className="flex items-center gap-2.5">
                <BarChart3 size={15} className="text-[#34D399]" />
                <span>Historical Rainfall</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#34D399] font-mono">
                ERA5
              </span>
            </button>
          </div>

          {/* Group 3: GOVERNANCE & DEMO */}
          <div className="space-y-1 pt-2 border-t border-[#153B34]">
            <span className="px-3 text-[10px] font-bold uppercase tracking-wider text-[#688A81] block mb-1.5">
              Governança & Auditoria
            </span>

            <button
              type="button"
              onClick={() => handleNavClick('executive-cockpit')}
              className={navItemClass('executive-cockpit')}
            >
              <span className="flex items-center gap-2.5">
                <BarChart3 size={15} />
                <span>Executive KPI Cockpit</span>
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleNavClick('sla-tracking')}
              className={navItemClass('sla-tracking')}
            >
              <span className="flex items-center gap-2.5">
                <Clock size={15} className="text-[#34D399]" />
                <span>Statutory SLA Engine</span>
              </span>
              <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#0A1E1A] text-[#34D399] font-mono font-bold">
                SLA
              </span>
            </button>

            <button
              type="button"
              onClick={onOpenProvenance}
              className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold text-[#A3B8B2] hover:bg-[#153B34] hover:text-white transition"
            >
              <span className="flex items-center gap-2.5">
                <Database size={15} className="text-[#34D399]" />
                <span>Data Provenance Registry</span>
              </span>
            </button>

            <button
              type="button"
              onClick={onOpenJudgeDemo}
              className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-bold bg-[#8B5E14]/60 hover:bg-[#A87219] text-[#FDE68A] transition border border-[#C58A32]/40"
            >
              <span className="flex items-center gap-2.5">
                <Sparkles size={15} className="text-[#FBBF24]" />
                <span>Judge Demo Mode</span>
              </span>
              <ChevronRight size={13} />
            </button>
          </div>
        </div>
      </div>

      {/* Bottom Profile / Quick Session */}
      <div className="p-3 border-t border-[#153B34] bg-[#0A1E1A]">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-7 h-7 rounded-full bg-[#17483B] border border-[#236353] flex items-center justify-center text-xs font-bold text-white shrink-0">
              {currentUser?.name ? currentUser.name.charAt(0).toUpperCase() : 'O'}
            </div>
            <div className="min-w-0">
              <span className="block text-xs font-bold text-white truncate">
                {currentUser?.name || 'Municipal Officer'}
              </span>
              <span className="block text-[10px] text-[#A3B8B2] truncate font-mono">
                {currentUser?.role || 'OFFICER'}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={logout}
            className="p-1.5 text-[#A3B8B2] hover:text-[#B94D36] hover:bg-[#153B34] rounded transition"
            title="Sign out"
          >
            <LogOut size={14} />
          </button>
        </div>
      </div>
    </aside>
    </>
  );
};
