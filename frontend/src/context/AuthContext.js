import { createContext, useCallback, useContext, useMemo, useState } from 'react';

const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'));
  const authenticate = useCallback((data) => { localStorage.setItem('token', data.token); localStorage.setItem('user', JSON.stringify(data.user)); setUser(data.user); }, []);
  const updateUser = useCallback((nextUser) => { localStorage.setItem('user', JSON.stringify(nextUser)); setUser(nextUser); }, []);
  const logout = useCallback(() => { localStorage.removeItem('token'); localStorage.removeItem('user'); setUser(null); }, []);
  const value = useMemo(() => ({ user, authenticate, updateUser, logout }), [user, authenticate, updateUser, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export const useAuth = () => useContext(AuthContext);
