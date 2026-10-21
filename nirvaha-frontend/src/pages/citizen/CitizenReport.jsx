import React, { useState } from 'react';
import { FeedbackToast } from '../../components/ui/FeedbackToast';
import { Camera, CheckCircle2, FileText, LocateFixed, MapPin, Send } from 'lucide-react';
import { incidentService, duplicateService, evidenceService } from '../../services';
import { LocationPickerMap } from '../../components/map/LocationPickerMap';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { CameraCapture } from '../../components/camera/CameraCapture';

const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const CATEGORIES = ['DRAINAGE', 'POTHOLE', 'GARBAGE', 'STREETLIGHT', 'WATER_SUPPLY', 'OTHER'];

export const CitizenReport = () => {
  const { language, setLanguage, supportedLanguages, t, getCategoryLabel } = useLanguage();
  const { currentUser } = useAuth();
  const [formData, setFormData] = useState({ title: '', category: 'DRAINAGE', description: '', address: 'Mission Street, Puducherry', latitude: 11.935, longitude: 79.83, severity: 'HIGH', originalLanguage: language });
  const [evidenceFile, setEvidenceFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [duplicateWarnings, setDuplicateWarnings] = useState([]);
  const [createdIncident, setCreatedIncident] = useState(null);
  const [error, setError] = useState(null);



  const updateLocation = (latitude, longitude) => setFormData((current) => ({ ...current, latitude: Number(latitude.toFixed(6)), longitude: Number(longitude.toFixed(6)) }));

  const handleLocationDetect = () => {
    if (!navigator.geolocation) {
      setError('Location services are not available in this browser. You can place the pin on the map instead.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => updateLocation(position.coords.latitude, position.coords.longitude),
      () => setError('We could not access your location. Please place the pin on the map.'),
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setDuplicateWarnings([]);
    try {
      const incident = await incidentService.createIncident({ ...formData, reporterId: currentUser?.id, originalLanguage: language });
      setCreatedIncident(incident);
      if (evidenceFile && incident.id) {
        const evidence = new FormData();
        evidence.append('file', evidenceFile);
        evidence.append('evidenceType', 'PHOTO');
        evidence.append('description', 'Initial citizen report image');
        evidence.append('latitude', formData.latitude.toString());
        evidence.append('longitude', formData.longitude.toString());
        await evidenceService.uploadEvidence(incident.id, evidence);
      }
      try {
        const matches = await duplicateService.findDuplicates(incident.id);
        setDuplicateWarnings(Array.isArray(matches) ? matches : (matches?.matches || []));
      } catch (duplicateError) {
        console.warn('Duplicate check did not complete:', duplicateError);
      }
    } catch (submitError) {
      setError(submitError.message || t('report.errorMsg'));
    } finally {
      setSubmitting(false);
    }
  };

  if (createdIncident) {
    return (
      <div className="report-page">
        <div className="report-intro"><span className="eyebrow">{t('report.eyebrow')}</span><h1>{t('report.successTitle')}</h1><p>{t('report.successMsg')}</p></div>
        <section className="form-surface" aria-live="polite">
          <div className="flex items-start gap-3 border-b border-[#d8ddda] pb-5">
            <CheckCircle2 className="mt-0.5 text-[#28715c]" size={28} />
            <div><strong className="text-base">{t('report.reference')} #{createdIncident.id}</strong><p className="mt-1 text-sm text-[#66716e]">{t('report.successMsg')}</p></div>
          </div>
          <dl className="mt-5 grid gap-4 text-sm md:grid-cols-2">
            <div><dt className="text-xs font-bold uppercase tracking-wide text-[#73807d]">{t('report.originalLanguage')}</dt><dd className="mt-1 font-semibold">{createdIncident.originalLanguage || language}</dd></div>
            <div><dt className="text-xs font-bold uppercase tracking-wide text-[#73807d]">{t('report.normalizedMeaning')}</dt><dd className="mt-1 font-semibold">{createdIncident.category}</dd></div>
            <div className="md:col-span-2"><dt className="text-xs font-bold uppercase tracking-wide text-[#73807d]">{t('report.originalText')}</dt><dd className="mt-1 leading-relaxed">{createdIncident.originalDescription || createdIncident.description}</dd></div>
          </dl>
          {duplicateWarnings.length > 0 && (
            <FeedbackToast
              type="info"
              message={t('report.similarReportsText')}
              onClose={() => setDuplicateWarnings([])}
            />
          )}
          <button className="primary-action max-w-xs" onClick={() => { setCreatedIncident(null); setDuplicateWarnings([]); }}>{t('report.submitAnother')}</button>
        </section>
      </div>
    );
  }

  return (
    <div className="report-page">
      <div className="report-intro"><span className="eyebrow">{t('report.eyebrow')}</span><h1>{t('report.title')}</h1><p>{t('report.subtitle')}</p></div>
      <form onSubmit={handleSubmit} className="report-layout">
        <section className="form-surface">
          <h2 className="section-heading"><FileText size={17} className="text-[#b94d36]" /> {t('form.details')}</h2>
          <div className="form-grid">
            <div>
              <label className="field-label" htmlFor="report-language">{t('report.languageSelectionLabel')}</label>
              <select id="report-language" className="field-control" value={language} onChange={(event) => setLanguage(event.target.value)}>
                {supportedLanguages.map((item) => <option key={item.code} value={item.code}>{item.nativeName} - {item.name}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label" htmlFor="category">{t('report.categoryLabel')}</label>
              <select id="category" className="field-control" value={formData.category} onChange={(event) => setFormData({ ...formData, category: event.target.value })}>
                {CATEGORIES.map((category) => <option key={category} value={category}>{getCategoryLabel(category)}</option>)}
              </select>
            </div>
            <div className="full">
              <label className="field-label" htmlFor="title">{t('report.issueTitleLabel')}</label>
              <input id="title" className="field-control" required value={formData.title} onChange={(event) => setFormData({ ...formData, title: event.target.value })} placeholder={t('report.issueTitlePlaceholder')} />
            </div>
            <div className="full">
              <label className="field-label" htmlFor="description">{t('report.descriptionLabel')}</label>
              <textarea id="description" className="field-control" required value={formData.description} onChange={(event) => setFormData({ ...formData, description: event.target.value })} placeholder={t('report.descriptionPlaceholder')} />
            </div>
            <div className="full">
              <label className="field-label">{t('report.severityLabel')}</label>
              <div className="severity-options">{SEVERITIES.map((severity) => <button key={severity} type="button" className={`severity-option ${formData.severity === severity ? 'selected' : ''}`} onClick={() => setFormData({ ...formData, severity })}>{t(`report.${severity.toLowerCase()}Severity`)}</button>)}</div>
            </div>
            <div className="full">
              <label className="field-label" htmlFor="address">{t('report.locationLabel')}</label>
              <input id="address" className="field-control" required value={formData.address} onChange={(event) => setFormData({ ...formData, address: event.target.value })} placeholder={t('report.locationPlaceholder')} />
            </div>
            <div className="full">
              <label className="field-label" htmlFor="evidence-upload">{t('report.evidenceLabel')}</label>
              <div className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-center"><label className="photo-dropzone" htmlFor="evidence-upload"><Camera size={22} className="text-[#b94d36]" /><span><strong>{evidenceFile ? evidenceFile.name : t('report.addPhoto')}</strong><small>{t('report.photoHint')}</small></span></label><CameraCapture onCapture={setEvidenceFile} /></div>
              <input id="evidence-upload" className="hidden" type="file" accept="image/*" onChange={(event) => setEvidenceFile(event.target.files?.[0] || null)} />
            </div>
          </div>
          {error && (
            <FeedbackToast
              type="error"
              message={error}
              onClose={() => setError(null)}
            />
          )}
          <button className="primary-action" type="submit" disabled={submitting}><Send size={17} />{submitting ? t('report.submitting') : t('report.submitBtn')}</button>
        </section>

        <aside className="location-surface">
          <div className="location-surface-header"><div><h2>{t('report.pinLocation')}</h2><p>{t('report.locationHint')}</p></div><button className="location-button" type="button" onClick={handleLocationDetect}><LocateFixed size={14} />{t('report.useMyLocation')}</button></div>
          <LocationPickerMap latitude={formData.latitude} longitude={formData.longitude} onChange={updateLocation} />
          <div className="location-coordinates"><div><span>{t('form.latitude')}</span><strong>{formData.latitude.toFixed(6)}</strong></div><div><span>{t('form.longitude')}</span><strong>{formData.longitude.toFixed(6)}</strong></div></div>
          <div className="mx-[18px] mb-[18px] flex items-center gap-2 rounded-md bg-[#f1f4f0] p-3 text-xs text-[#53625f]"><MapPin size={15} className="shrink-0 text-[#174f4a]" />{t('form.routingHint')}</div>
        </aside>
      </form>
    </div>
  );
};
