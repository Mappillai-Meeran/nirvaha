import React from 'react';
import { useLanguage } from '../../context/LanguageContext';
import { Globe } from 'lucide-react';

export const LanguageSwitcher = ({ className = '' }) => {
  const { language, setLanguage, supportedLanguages } = useLanguage();

  return (
    <div className={`inline-flex items-center text-xs ${className}`}>
      <label className="language-select">
        <Globe className="w-3.5 h-3.5" />
        <select
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className="bg-transparent font-semibold focus:outline-none cursor-pointer"
          aria-label="Select Interface Language"
        >
          {supportedLanguages.map((lang) => (
            <option key={lang.code} value={lang.code}>
              {lang.nativeName} ({lang.code})
            </option>
          ))}
        </select>
      </label>
    </div>
  );
};
