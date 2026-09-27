import { createContext, useContext, useEffect, useMemo, useState } from 'react';

const DEFAULT_LANGUAGE = 'en';

const LanguageContext = createContext(null);

function detectBrowserLanguage() {
  if (typeof navigator === 'undefined') {
    return DEFAULT_LANGUAGE;
  }

  const browserLanguages = Array.isArray(navigator.languages) && navigator.languages.length > 0
    ? navigator.languages
    : [navigator.language || navigator.userLanguage || DEFAULT_LANGUAGE];

  const preferredLanguage = String(browserLanguages[0] || DEFAULT_LANGUAGE).toLowerCase();

  return preferredLanguage.startsWith('fr') ? 'fr' : 'en';
}

export function LanguageProvider({ children }) {
  const [language, setDetectedLanguage] = useState(detectBrowserLanguage);

  useEffect(() => {
    const applyBrowserLanguage = () => {
      setDetectedLanguage(detectBrowserLanguage());
    };

    applyBrowserLanguage();
    window.addEventListener('languagechange', applyBrowserLanguage);

    return () => {
      window.removeEventListener('languagechange', applyBrowserLanguage);
    };
  }, []);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = 'ltr';

    try {
      localStorage.removeItem('publicLanguage');
    } catch {
      // localStorage can be unavailable in some private browsing contexts.
    }
  }, [language]);

  const value = useMemo(
    () => ({
      language,
      isFrench: language === 'fr',
      isEnglish: language === 'en',

      // Kept only for backward compatibility with older components.
      // The public website now follows the browser language automatically.
      setLanguage: () => {},
      toggleLanguage: () => {},
    }),
    [language],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const context = useContext(LanguageContext);

  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider.');
  }

  return context;
}
