import React from 'react';
import { Building2, ClipboardList, Home, LayoutDashboard, ListChecks, LogIn, LogOut, Radar, Search, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';
import { LanguageSwitcher } from './LanguageSwitcher';
import { NotificationDropdown } from '../notifications/NotificationDropdown';

const NavButton = ({ active, icon: Icon, children, onClick }) => (
  <button onClick={onClick} className={`nav-button ${active ? 'nav-button-active' : ''}`}>
    <Icon size={16} strokeWidth={1.8} />
    <span>{children}</span>
  </button>
);

export const Header = () => {
  const { currentUser, switchRole, logout, ROLES, isDemoMode } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  const isCitizen = currentUser?.role === ROLES.CITIZEN;
  const isOfficerOrAdmin = currentUser?.role === ROLES.OFFICER || currentUser?.role === ROLES.ADMIN;
  // On the command-center shell the left sidebar handles all navigation,
  // so we suppress the top-bar nav links to avoid a duplicate double-nav.
  const isCommandCenter = location.pathname.startsWith('/command-center');

  return (
    <header className="app-header">
      {/* Official Government Top Bar with Indian Tricolour Stripe */}
      <div className="gov-topbar">
        <div className="gov-topbar-inner">
          <span className="gov-title">
            <span className="gov-seal" aria-hidden="true">🏛️</span> Government of Puducherry • Smart City Municipal Services
          </span>
          <span className="gov-helpline">
            Civic Helpline: <strong>1913</strong> | Emergency: <strong>112</strong>
          </span>
        </div>
        <div className="gov-tricolour-stripe" aria-hidden="true" />
      </div>

      <div className="app-header-inner">
        <button className="brand-lockup" onClick={() => navigate('/')} aria-label="Nirvaha home">
          <span className="brand-mark"><Building2 size={18} /></span>
          <span>
            <strong>NIRVAHA</strong>
            <small>{t('nav.workspace')}</small>
          </span>
        </button>

        <nav className="primary-nav" aria-label="Primary navigation">
          {/* Portal home link always visible */}
          <NavButton 
            active={location.pathname === '/'} 
            icon={Home} 
            onClick={() => navigate('/')}
          >
            Portal
          </NavButton>

          {/* Hide nav items on command-center – sidebar handles navigation there */}
          {!isCommandCenter && isCitizen && (
            <>
              <NavButton active={location.pathname === '/citizen/report'} icon={ClipboardList} onClick={() => navigate('/citizen/report')}>{t('nav.reportIssue')}</NavButton>
              <NavButton active={location.pathname === '/citizen/my-reports'} icon={ListChecks} onClick={() => navigate('/citizen/my-reports')}>{t('nav.myReports')}</NavButton>
            </>
          )}

          {!isCommandCenter && isOfficerOrAdmin && (
            <>
              <NavButton active={location.pathname === '/command-center'} icon={LayoutDashboard} onClick={() => navigate('/command-center')}>{t('nav.commandCenter')}</NavButton>
              <NavButton active={location.pathname === '/command-center/queue'} icon={ListChecks} onClick={() => navigate('/command-center/queue')}>{t('nav.priorityQueue')}</NavButton>
              <NavButton active={location.pathname === '/command-center/anomalies'} icon={Radar} onClick={() => navigate('/command-center/anomalies')}>{t('nav.integrityRadar')}</NavButton>
            </>
          )}

          {!isCommandCenter && !currentUser && (
            <>
              <NavButton active={location.pathname === '/citizen/report'} icon={ClipboardList} onClick={() => navigate('/citizen/report')}>{t('nav.reportIssue')}</NavButton>
              <NavButton active={false} icon={Search} onClick={() => { navigate('/'); setTimeout(() => { const el = document.getElementById('track-section'); el?.scrollIntoView({ behavior: 'smooth' }); }, 50); }}>Track Status</NavButton>
            </>
          )}
        </nav>

        <div className="header-tools">
          <LanguageSwitcher />

          {currentUser ? (
            <div className="flex items-center gap-2">
              <NotificationDropdown />
              <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold bg-[#174f4a]/10 text-[#174f4a] border border-[#174f4a]/20">
                {currentUser.role}
              </span>
              <button 
                onClick={logout} 
                className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold text-[#596765] hover:text-[#b94d36] hover:bg-[#f0f2ee] rounded border border-transparent transition"
                title="Sign out"
                aria-label="Sign out"
              >
                <LogOut size={14} />
                <span className="hidden md:inline">Sign Out</span>
              </button>
            </div>
          ) : (
            <button
              onClick={() => navigate('/login')}
              className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-bold text-white bg-[#174f4a] hover:bg-[#123e3a] rounded shadow-sm transition"
            >
              <LogIn size={14} />
              <span>Sign In</span>
            </button>
          )}

          {isDemoMode && <div className="role-switcher" aria-label="Demo role preview">
            <button className={isCitizen ? 'role-active' : ''} onClick={() => { switchRole(ROLES.CITIZEN); navigate('/citizen/report'); }}>{t('nav.citizen')}</button>
            <button className={currentUser?.role === ROLES.OFFICER ? 'role-active' : ''} onClick={() => { switchRole(ROLES.OFFICER); navigate('/command-center'); }}>{t('nav.officer')}</button>
            <button className={currentUser?.role === ROLES.ADMIN ? 'role-active' : ''} onClick={() => { switchRole(ROLES.ADMIN); navigate('/command-center'); }} aria-label={t('nav.admin')}><ShieldCheck size={15} /></button>
          </div>}
        </div>
      </div>
    </header>
  );
};
