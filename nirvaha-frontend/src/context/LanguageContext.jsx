import React, { createContext, useContext, useState, useEffect } from 'react';
import { TRANSLATIONS, SUPPORTED_LANGUAGES } from '../i18n/translations';

const LanguageContext = createContext();

export const LanguageProvider = ({ children }) => {
  const [language, setLanguageState] = useState(() => {
    const saved = localStorage.getItem('nirvaha_language');
    if (saved && SUPPORTED_LANGUAGES.some(l => l.code === saved)) {
      return saved;
    }
    return 'EN';
  });

  const setLanguage = (langCode) => {
    if (SUPPORTED_LANGUAGES.some(l => l.code === langCode)) {
      setLanguageState(langCode);
      localStorage.setItem('nirvaha_language', langCode);
    }
  };

  useEffect(() => {
    const current = SUPPORTED_LANGUAGES.find((item) => item.code === language);
    document.documentElement.lang = current?.locale || 'en-IN';
  }, [language]);

  const t = (keyPath) => {
    const keys = keyPath.split('.');
    let result = TRANSLATIONS[language];
    for (const k of keys) {
      if (result && result[k] !== undefined) {
        result = result[k];
      } else {
        result = null;
        break;
      }
    }

    if (result !== null && result !== undefined) {
      return result;
    }

    // Fallback to English
    let fallback = TRANSLATIONS.EN;
    for (const k of keys) {
      if (fallback && fallback[k] !== undefined) {
        fallback = fallback[k];
      } else {
        fallback = null;
        break;
      }
    }

    return fallback !== null && fallback !== undefined ? fallback : keyPath;
  };

  const getCategoryLabel = (categoryCode) => {
    return t(`categories.${categoryCode}`);
  };

  const getStatusLabel = (statusCode) => {
    return t(`statuses.${statusCode}`);
  };

  return (
    <LanguageContext.Provider value={{
      language,
      setLanguage,
      supportedLanguages: SUPPORTED_LANGUAGES,
      t,
      getCategoryLabel,
      getStatusLabel
    }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
};
