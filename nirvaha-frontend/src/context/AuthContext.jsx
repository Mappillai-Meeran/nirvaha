import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services';

const AuthContext = createContext(null);

export const ROLES = {
  CITIZEN: 'CITIZEN',
  OFFICER: 'OFFICER',
  ADMIN: 'ADMIN',
};

const DEMO_ACCOUNTS = {
  [ROLES.OFFICER]: { email: 'officer@puducherry.gov.in', password: 'Officer123!' },
  [ROLES.ADMIN]: { email: 'admin@puducherry.gov.in', password: 'Admin123!' },
  [ROLES.CITIZEN]: { email: 'citizen@puducherry.gov.in', password: 'Citizen123!' },
};

export const AuthProvider = ({ children }) => {
  const isDemoMode = import.meta.env.DEV && import.meta.env.VITE_DEMO_MODE !== 'false';
  const [jwtToken, setJwtToken] = useState(localStorage.getItem('nirvaha_jwt') || null);
  const [currentUser, setCurrentUser] = useState(null);

  const login = async (email, password) => {
    const res = await authService.login({ email, password });
    if (res && res.token) {
      localStorage.setItem('nirvaha_jwt', res.token);
      setJwtToken(res.token);
      setCurrentUser(res.user);
    }
    return res;
  };

  const logout = () => {
    localStorage.removeItem('nirvaha_jwt');
    setJwtToken(null);
    setCurrentUser(null);
  };

  useEffect(() => {
    // Attempt automatic profile sync if token exists
    if (jwtToken) {
      authService.getMe()
        .then((user) => {
          setCurrentUser(user);
        })
        .catch((err) => {
          console.warn('Authentication token expired or invalid:', err);
          logout();
          if (isDemoMode) {
            login(DEMO_ACCOUNTS[ROLES.OFFICER].email, DEMO_ACCOUNTS[ROLES.OFFICER].password).catch(() => {});
          }
        });
    } else if (isDemoMode) {
      // Auto-authenticate as default officer in dev demo mode so live API calls don't fail with 401 Unauthorized
      login(DEMO_ACCOUNTS[ROLES.OFFICER].email, DEMO_ACCOUNTS[ROLES.OFFICER].password).catch((err) => {
        console.warn('Initial demo auto-login failed:', err);
      });
    }
  }, [jwtToken]);

  // Demo role-switcher helper (Maintained for presentation & manual testing compatibility)
  const switchRole = async (newRole) => {
    if (!isDemoMode) return;
    const creds = DEMO_ACCOUNTS[newRole];
    if (creds) {
      try {
        await login(creds.email, creds.password);
      } catch (err) {
        console.warn(`Failed demo auto-login for ${newRole}:`, err);
      }
    }
  };

  return (
    <AuthContext.Provider value={{ currentUser, jwtToken, login, logout, switchRole, ROLES, isDemoMode }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
