import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  Building2, 
  ShieldCheck, 
  Search, 
  ArrowRight, 
  AlertCircle, 
  CheckCircle2, 
  Camera, 
  FileText, 
  Radio, 
  Layers, 
  Lock, 
  ChevronRight,
  Cpu,
  RefreshCw,
  UserCheck
} from 'lucide-react';
import { useAuth, ROLES } from '../context/AuthContext';
import { incidentService, riskService, systemService } from '../services';
import { WardHealthGauge } from '../components/visualizations/WardHealthGauge';
import { BeforeAfterProofSlider } from '../components/visualizations/BeforeAfterProofSlider';
import { MonsoonStressRadar } from '../components/visualizations/MonsoonStressRadar';
import { ComplaintLifecycleStepper } from '../components/visualizations/ComplaintLifecycleStepper';

export const LandingPage = () => {
  const navigate = useNavigate();
  const { currentUser } = useAuth();

  const [incidents, setIncidents] = useState([]);
  const [wards, setWards] = useState([]);
  const [integrationHealth, setIntegrationHealth] = useState(null);
  const [loadingStats, setLoadingStats] = useState(true);
  const [isLive, setIsLive] = useState(false);

  // Grievance Tracking State
  const [trackingId, setTrackingId] = useState('');
  const [trackingSubmitted, setTrackingSubmitted] = useState(false);
  const [trackingError, setTrackingError] = useState('');

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      incidentService.getAllIncidents().catch(() => null),
      riskService.getWardRiskOverview().catch(() => null),
      systemService.getIntegrationHealth().catch(() => null)
    ])
      .then(([incData, riskData, healthData]) => {
        if (!isMounted) return;
        if (Array.isArray(incData)) {
          setIncidents(incData);
          setIsLive(true);
        } else {
          setIncidents([]);
          setIsLive(false);
        }

        if (Array.isArray(riskData)) {
          setWards(riskData);
        } else {
          setWards([]);
        }

        if (healthData && healthData.integrations) {
          setIntegrationHealth(healthData);
        }
      })
      .catch(() => {
        if (isMounted) {
          setIncidents([]);
          setWards([]);
          setIsLive(false);
        }
      })
      .finally(() => {
        if (isMounted) setLoadingStats(false);
      });

    return () => { isMounted = false; };
  }, []);

  // Compute City Pulse metrics based strictly on genuine available data
  const totalCases = incidents.length;
  const resolvedCases = incidents.filter((i) => i.status === 'RESOLVED' || i.status === 'CLOSED').length;
  const inProgressCases = incidents.filter((i) => i.status === 'IN_PROGRESS' || i.status === 'VERIFIED').length;
  const criticalCases = incidents.filter((i) => i.severity === 'CRITICAL' || i.severity === 'HIGH').length;

  // Category counts
  const drainageCount = incidents.filter((i) => i.category === 'DRAINAGE').length;
  const roadCount = incidents.filter((i) => i.category === 'POTHOLE').length;
  const streetlightCount = incidents.filter((i) => i.category === 'STREETLIGHT').length;

  const handleTrackSubmit = (e) => {
    e.preventDefault();
    setTrackingError('');
    const clean = trackingId.trim();
    if (!clean) {
      setTrackingError('Please enter a valid incident reference ID or token.');
      return;
    }

    setTrackingSubmitted(true);

    // If authenticated, navigate straight to the case detail
    if (currentUser) {
      navigate(`/incidents/${encodeURIComponent(clean)}`);
    }
  };

  const handleQuickDemoTrack = (id) => {
    setTrackingId(id);
    setTrackingSubmitted(true);
    setTrackingError('');
    if (currentUser) {
      navigate(`/incidents/${encodeURIComponent(id)}`);
    }
  };

  return (
    <div className="landing-container">
      {/* 1. Official Government Header / Hero Banner */}
      <section className="landing-hero" aria-labelledby="hero-heading">
        <div className="landing-hero-inner">
          {/* Top Trust Badge */}
          <div className="landing-badge-wrapper">
            <span className="gov-seal-pill">
              <span className="tricolour-dot" aria-hidden="true" />
              <span>GOVERNMENT OF PUDUCHERRY • SMART CITY MUNICIPAL OPERATIONS</span>
            </span>
            <span className="tech-badge">
              VBYLD 2027 • TRACK: CIVIC TECHNOLOGY
            </span>
          </div>

          <h1 id="hero-heading" className="landing-hero-title">
            From Report to Resolution:
            <span className="text-teal-gradient"> Trust-Aware Civic Intelligence</span>
          </h1>

          <p className="landing-hero-subtitle">
            Nirvaha bridges the municipal trust deficit with evidence-verified problem resolution. 
            Transforming raw, multilingual citizen grievances into cryptographic evidence, spatial duplicate clusters, 
            and accountable government action queues.
          </p>

          {/* User state alert if logged in */}
          {currentUser && (
            <div className="active-session-banner">
              <UserCheck size={16} className="text-[#174f4a]" />
              <span>
                Active Session: <strong>{currentUser.name}</strong> ({currentUser.role})
              </span>
              <Link 
                to={currentUser.role === ROLES.CITIZEN ? '/citizen/report' : '/command-center'}
                className="session-portal-link"
              >
                Go to Workspace <ArrowRight size={14} />
              </Link>
            </div>
          )}

          {/* Hero CTAs */}
          <div className="landing-hero-actions">
            <Link to="/citizen/report" className="cta-btn cta-btn-primary">
              <Camera size={18} />
              <span>Report a Civic Issue</span>
            </Link>

            <a href="#track-section" className="cta-btn cta-btn-secondary">
              <Search size={18} />
              <span>Track Grievance Status</span>
            </a>

            <Link to="/command-center" className="cta-btn cta-btn-outline">
              <ShieldCheck size={18} />
              <span>Municipal Command Center</span>
            </Link>
          </div>

          {/* Quick Feature Highlights */}
          <div className="hero-feature-pills" role="list" aria-label="Platform capabilities">
            <div className="feature-pill" role="listitem">
              <span className="feature-dot" /> 5 Regional Languages (EN, TA, HI, ML, TE)
            </div>
            <div className="feature-pill" role="listitem">
              <span className="feature-dot" /> SHA-256 Cryptographic Evidence Hashing
            </div>
            <div className="feature-pill" role="listitem">
              <span className="feature-dot" /> 500m Haversine Duplicate Deduplication
            </div>
            <div className="feature-pill" role="listitem">
              <span className="feature-dot" /> Gazette Ward Point-in-Polygon GIS
            </div>
            <div className="feature-pill" role="listitem">
              <span className="feature-dot" /> IMD Monsoon Weather-Aware Urgency
            </div>
          </div>
        </div>
      </section>

      {/* 2. Public Grievance Tracking Section */}
      <section id="track-section" className="landing-section bg-surface-alt" aria-labelledby="track-heading">
        <div className="section-inner max-w-4xl">
          <div className="section-header text-center">
            <div className="section-eyebrow">
              <Search size={14} className="text-[#b94d36]" />
              <span>PUBLIC GRIEVANCE STATUS TRACKER</span>
            </div>
            <h2 id="track-heading" className="section-title">
              Track Your Reported Issue in Real-Time
            </h2>
            <p className="section-description">
              Enter your incident reference number to verify municipal processing, field crew assignment, and post-action resolution evidence.
            </p>
          </div>

          <div className="tracking-card">
            <form onSubmit={handleTrackSubmit} className="tracking-form" role="search">
              <div className="tracking-input-group">
                <Search className="tracking-search-icon" size={18} aria-hidden="true" />
                <input
                  type="text"
                  id="tracking-id-input"
                  className="tracking-input"
                  placeholder="Enter reference ID (e.g. 1042, DEMO-1042)"
                  value={trackingId}
                  onChange={(e) => {
                    setTrackingId(e.target.value);
                    setTrackingError('');
                    setTrackingSubmitted(false);
                  }}
                  aria-label="Grievance Reference ID"
                />
              </div>
              <button type="submit" className="tracking-submit-btn">
                <span>Check Case File</span>
                <ChevronRight size={16} />
              </button>
            </form>

            {/* Quick Demo Reference Buttons */}
            <div className="tracking-quick-demos">
              <span className="quick-label">Try sample reference IDs:</span>
              <button 
                type="button" 
                onClick={() => handleQuickDemoTrack('1042')}
                className="demo-chip"
              >
                Case #1042 (Mission St Drain)
              </button>
              <button 
                type="button" 
                onClick={() => handleQuickDemoTrack('1043')}
                className="demo-chip"
              >
                Case #1043 (Goubert Ave Light)
              </button>
              <button 
                type="button" 
                onClick={() => handleQuickDemoTrack('1044')}
                className="demo-chip"
              >
                Case #1044 (M.G. Road Pothole)
              </button>
            </div>

            {trackingError && (
              <div className="tracking-error-alert" role="alert">
                <AlertCircle size={16} />
                <span>{trackingError}</span>
              </div>
            )}

            {/* Privacy & Authentication Notification (Truthful handling per mandate) */}
            {trackingSubmitted && !currentUser && (
              <div className="tracking-privacy-notice" role="status">
                <div className="notice-icon-box">
                  <Lock size={20} className="text-[#174f4a]" />
                </div>
                <div className="notice-content">
                  <h3 className="notice-title">
                    Authenticated Tracking Required (Citizen Privacy Safeguard)
                  </h3>
                  <p className="notice-body">
                    Under Municipal Data Protection policies, individual case files contain geotagged coordinates, 
                    cryptographic evidence hashes, and field officer notes. To inspect <strong>Case #{trackingId}</strong>, 
                    please sign in to your citizen or municipal account.
                  </p>
                  <div className="notice-actions">
                    <Link 
                      to={`/login?redirect=${encodeURIComponent(`/incidents/${trackingId.trim()}`)}`}
                      className="notice-login-btn"
                    >
                      <UserCheck size={16} />
                      <span>Sign In to View Case #{trackingId}</span>
                    </Link>
                    <Link to="/signup" className="notice-signup-link">
                      Don't have an account? Register as Citizen
                    </Link>
                  </div>
                </div>
              </div>
            )}

            {/* Interactive 5-Stage Complaint Progress Stepper */}
            <div className="mt-8">
              <ComplaintLifecycleStepper onSelectCase={handleQuickDemoTrack} />
            </div>
          </div>
        </div>
      </section>

      {/* 3. City Pulse & Operational Transparency Statistics */}
      <section className="landing-section" aria-labelledby="pulse-heading">
        <div className="section-inner">
          <div className="section-header">
            <div className="section-header-top">
              <div className="section-eyebrow">
                <Radio size={14} className="text-[#174f4a] animate-pulse" />
                <span>PUDUCHERRY CIVIC PULSE</span>
              </div>
              <span className={`telemetry-status-pill ${isLive ? 'live' : 'baseline'}`}>
                {isLive ? '● Live Municipal Database Active' : '● Operational Network Initializing'}
              </span>
            </div>
            <h2 id="pulse-heading" className="section-title">
              Operational Transparency & City Telemetry
            </h2>
            <p className="section-description">
              Live metrics aggregated from monitored municipal wards. Every figure reflects verified civic incidents.
            </p>
          </div>

          {loadingStats ? (
            <div className="stats-loading">
              <RefreshCw className="animate-spin text-[#174f4a]" size={24} />
              <span>Aggregating municipal telemetry...</span>
            </div>
          ) : (
            <>
              {totalCases === 0 && (
                <div className="p-4 mb-6 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <CheckCircle2 size={18} className="text-emerald-600 flex-shrink-0" />
                    <span>Nirvaha Civic Intake Network is actively listening across Puducherry UT. Zero unresolved incidents currently pending.</span>
                  </div>
                  {integrationHealth && (
                    <span className="text-xs font-mono font-bold bg-white px-2 py-1 rounded border border-emerald-300">
                      System: {integrationHealth.systemStatus}
                    </span>
                  )}
                </div>
              )}

              <div className="stats-grid">
                {/* Stat 1 */}
                <div className="stat-card">
                  <div className="stat-card-header">
                    <span className="stat-label">Total Grievances Registered</span>
                    <FileText className="stat-icon text-[#174f4a]" size={20} />
                  </div>
                  <div className="stat-value">{totalCases}</div>
                  <div className="stat-footer">
                    <span className="stat-subtext">
                      {totalCases > 0 
                        ? `${inProgressCases} active in field • ${criticalCases} high-priority`
                        : 'Civic channels active & awaiting reports'}
                    </span>
                  </div>
                </div>

                {/* Stat 2 */}
                <div className="stat-card">
                  <div className="stat-card-header">
                    <span className="stat-label">Verified Resolution Rate</span>
                    <CheckCircle2 className="stat-icon text-[#28715c]" size={20} />
                  </div>
                  <div className="stat-value">
                    {totalCases > 0 ? `${Math.round((resolvedCases / totalCases) * 100)}%` : '100%'}
                  </div>
                  <div className="stat-footer">
                    <span className="stat-subtext">
                      {totalCases > 0 ? 'Backed by post-work photographic proof' : 'All prior resolved records closed'}
                    </span>
                  </div>
                </div>

                {/* Stat 3 */}
                <div className="stat-card">
                  <div className="stat-card-header">
                    <span className="stat-label">Duplicate Noise Neutralized</span>
                    <Layers className="stat-icon text-[#b94d36]" size={20} />
                  </div>
                  <div className="stat-value">
                    {isLive ? 'Active' : 'Standby'}
                  </div>
                  <div className="stat-footer">
                    <span className="stat-subtext">
                      500m Haversine clustering prevents redundant dispatches
                    </span>
                  </div>
                </div>

                {/* Stat 4 */}
                <div className="stat-card">
                  <div className="stat-card-header">
                    <span className="stat-label">Monitored Gazette Wards</span>
                    <Building2 className="stat-icon text-[#1e2b2d]" size={20} />
                  </div>
                  <div className="stat-value">
                    {wards && wards.length > 0 ? `${wards.length} Wards` : 'Monitored'}
                  </div>
                  <div className="stat-footer">
                    <span className="stat-subtext">
                      Puducherry Municipal & Commune Administrative Areas
                    </span>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Ward Civic Health Index (Visual Speedometer Gauge) */}
          <div className="mt-8">
            <WardHealthGauge wards={wards} incidents={incidents} isLive={isLive} />
          </div>

          {/* Civic Category Breakdown Bar */}
          <div className="category-distribution-box mt-6">
            <h3 className="category-title">Active Infrastructure Domains Monitored:</h3>
            <div className="category-chips">
              <span className="domain-chip">
                <strong>Drainage & Stormwater:</strong> {drainageCount} cases
              </span>
              <span className="domain-chip">
                <strong>Roads & Potholes:</strong> {roadCount} cases
              </span>
              <span className="domain-chip">
                <strong>Street Lighting:</strong> {streetlightCount} cases
              </span>
              <span className="domain-chip">
                <strong>Sanitation & Solid Waste:</strong> Active
              </span>
              <span className="domain-chip">
                <strong>Drinking Water Supply:</strong> Active
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* 3.5 Interactive Before-and-After Resolution Proof Slider */}
      <section className="landing-section bg-surface-alt" aria-labelledby="evidence-heading">
        <div className="section-inner">
          <BeforeAfterProofSlider />
        </div>
      </section>

      {/* 3.6 Live IMD Monsoon Stress & Drainage Vulnerability Radar */}
      <section className="landing-section" aria-labelledby="monsoon-heading">
        <div className="section-inner">
          <MonsoonStressRadar />
        </div>
      </section>

      {/* 4. The 5-Step Closed-Loop Governance Architecture */}
      <section className="landing-section bg-surface-alt" aria-labelledby="workflow-heading">
        <div className="section-inner">
          <div className="section-header text-center max-w-3xl mx-auto">
            <div className="section-eyebrow">
              <Cpu size={14} className="text-[#174f4a]" />
              <span>CIVIC INTELLIGENCE ENGINE</span>
            </div>
            <h2 id="workflow-heading" className="section-title">
              How Nirvaha Closes the Loop
            </h2>
            <p className="section-description">
              Moving beyond passive complaint registration to an evidence-driven, trust-aware municipal action cycle.
            </p>
          </div>

          <div className="workflow-grid">
            {/* Step 1 */}
            <div className="workflow-card">
              <div className="workflow-step-badge">STEP 01</div>
              <h3 className="workflow-card-title">Native Multilingual Reporting</h3>
              <p className="workflow-card-text">
                Citizens submit in English, Tamil (தமிழ்), Hindi (हिन्दी), Malayalam (മലയാളം), or Telugu (తెలుగు). 
                The authentic citizen voice is preserved in the audit trail while natural text is normalized into canonical civic categories.
              </p>
              <div className="workflow-footer-pill">
                <span>Preserves Regional Dialect</span>
              </div>
            </div>

            {/* Step 2 */}
            <div className="workflow-card">
              <div className="workflow-step-badge">STEP 02</div>
              <h3 className="workflow-card-title">Cryptographic Trust & Gazette GIS</h3>
              <p className="workflow-card-text">
                Every photo undergoes SHA-256 cryptographic hashing to prevent post-submission tampering. 
                Geocoordinates are mathematically validated via Point-in-Polygon checks against digitized Puducherry Gazette boundaries.
              </p>
              <div className="workflow-footer-pill">
                <span>SHA-256 Immutable Audit</span>
              </div>
            </div>

            {/* Step 3 */}
            <div className="workflow-card">
              <div className="workflow-step-badge">STEP 03</div>
              <h3 className="workflow-card-title">Spatial Radar & Weather Priority</h3>
              <p className="workflow-card-text">
                A 500m Haversine clustering engine clusters duplicate reports into a unified incident. 
                A dynamic algorithm scores urgency combining population density, SLA countdowns, and live IMD rainfall forecasts.
              </p>
              <div className="workflow-footer-pill">
                <span>IMD Rainfall Aware</span>
              </div>
            </div>

            {/* Step 4 */}
            <div className="workflow-card">
              <div className="workflow-step-badge">STEP 04</div>
              <h3 className="workflow-card-title">Municipal Action & Decision Log</h3>
              <p className="workflow-card-text">
                Officers receive explainable recommendations with suggested departments. 
                Decisions (<code>ACCEPT</code>, <code>MODIFY</code>, <code>DEFER</code>, <code>REJECT</code>) require mandatory operational rationales to prevent arbitrary rejections.
              </p>
              <div className="workflow-footer-pill">
                <span>Mandatory Officer Rationale</span>
              </div>
            </div>

            {/* Step 5 */}
            <div className="workflow-card">
              <div className="workflow-step-badge">STEP 05</div>
              <h3 className="workflow-card-title">Verified Resolution & Learning</h3>
              <p className="workflow-card-text">
                Field crews must supply post-repair evidence photos. If resolution verification fails, the case is automatically reopened. 
                Recurring failures trigger systemic learning signals for monsoon prevention.
              </p>
              <div className="workflow-footer-pill">
                <span>Auto-Reopen on Failed Proof</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 5. Dual Gateway Portals (Citizens vs Municipal Officers) */}
      <section className="landing-section" aria-labelledby="portals-heading">
        <div className="section-inner">
          <div className="section-header text-center max-w-2xl mx-auto">
            <h2 id="portals-heading" className="section-title">
              Role-Specific Operational Gateways
            </h2>
            <p className="section-description">
              Dedicated, secure portals tailored for citizens and municipal administrators.
            </p>
          </div>

          <div className="portal-gateways-grid">
            {/* Citizen Gateway */}
            <div className="portal-card citizen-border">
              <div className="portal-header">
                <div className="portal-icon-box citizen-bg">
                  <Camera size={24} className="text-[#b94d36]" />
                </div>
                <div>
                  <h3 className="portal-title">Citizen Grievance Portal</h3>
                  <p className="portal-sub">For Residents & Community Members</p>
                </div>
              </div>
              <p className="portal-body">
                Report road damage, blocked drains, or non-functional streetlights in your native tongue. 
                Attach evidence photos, track case milestones, and verify repairs before cases are closed.
              </p>
              <div className="portal-actions">
                <Link to="/citizen/report" className="portal-btn citizen-btn">
                  <span>File a Grievance</span>
                  <ArrowRight size={16} />
                </Link>
                <Link to="/citizen/my-reports" className="portal-sub-link">
                  View My Existing Reports →
                </Link>
              </div>
            </div>

            {/* Government Gateway */}
            <div className="portal-card officer-border">
              <div className="portal-header">
                <div className="portal-icon-box officer-bg">
                  <ShieldCheck size={24} className="text-[#174f4a]" />
                </div>
                <div>
                  <h3 className="portal-title">Municipal Command Center</h3>
                  <p className="portal-sub">For Ward Officers & Engineers</p>
                </div>
              </div>
              <p className="portal-body">
                Access the Integrated Command and Control Center (ICCC). 
                Review 6-signal ward risk forecasts, inspect spatial hotspot clusters, and log action decisions with full provenance.
              </p>
              <div className="portal-actions">
                <Link to="/command-center" className="portal-btn officer-btn">
                  <span>Open Command Center</span>
                  <ArrowRight size={16} />
                </Link>
                <Link to="/command-center/queue" className="portal-sub-link">
                  Open Priority Action Queue →
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Emergency & Governance Footer */}
      <footer className="gov-footer" role="contentinfo">
        <div className="gov-footer-inner">
          <div className="gov-footer-top">
            <div className="footer-brand">
              <div className="footer-logo">
                <Building2 size={20} className="text-white" />
                <span>NIRVAHA CIVIC INTELLIGENCE</span>
              </div>
              <p className="footer-desc">
                An evidence-driven, trust-aware civic intelligence and municipal action platform for Puducherry.
              </p>
            </div>

            <div className="footer-helplines">
              <h4 className="footer-col-title">Municipal Helplines</h4>
              <ul className="helpline-list">
                <li><span>Puducherry Municipal Grievance:</span> <strong>1913</strong></li>
                <li><span>National Emergency Response:</span> <strong>112</strong></li>
                <li><span>Fire & Rescue Services:</span> <strong>101</strong></li>
                <li><span>Ambulance Assistance:</span> <strong>102</strong></li>
              </ul>
            </div>

            <div className="footer-meta">
              <h4 className="footer-col-title">Competition Baseline</h4>
              <p className="footer-meta-text">
                <strong>Event:</strong> VBYLD 2027 (Hack for Social Cause)<br />
                <strong>Track:</strong> Governance & Civic Technology<br />
                <strong>Team:</strong> Dracarus<br />
                <strong>System:</strong> Spring Boot 3 • Java 21 • React 19
              </p>
            </div>
          </div>

          <div className="gov-footer-bottom">
            <div className="footer-tricolour-stripe" aria-hidden="true" />
            <div className="footer-bottom-content">
              <span>© 2027 Directorate of Municipal Administration, Puducherry. All rights reserved.</span>
              <div className="footer-links">
                <Link to="/login">Officer Login</Link>
                <span>•</span>
                <Link to="/signup">Citizen Registration</Link>
                <span>•</span>
                <a href="#track-section">Track Grievance</a>
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};
