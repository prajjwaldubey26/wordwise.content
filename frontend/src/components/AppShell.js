import { useEffect, useRef, useState } from 'react';
import { Container, Offcanvas } from 'react-bootstrap';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import LogoMark from './LogoMark';

const DRAWER_LINKS = [
  { to: '/dashboard', label: 'Home' },
  { to: '/chat', label: 'Chat' },
  { to: '/generate', label: 'Generate' },
  { to: '/chapters', label: 'Summary & Quiz' },
  { to: '/plagiarism', label: 'Originality' },
  { to: '/history', label: 'History' },
  { to: '/reports', label: 'Reports' },
  { to: '/pricing', label: 'Pricing' },
];

const BOTTOM_LINKS = [
  { to: '/dashboard', label: 'Home', icon: '⌂' },
  { to: '/chat', label: 'Chat', icon: '◎' },
  { to: '/generate', label: 'Create', icon: '✦' },
  { to: '/history', label: 'History', icon: '▤' },
  { to: '/reports', label: 'Reports', icon: '◈' },
];

function initials(name = '') {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return 'WW';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

export default function AppShell({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const isChat = pathname.startsWith('/chat');
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const [notifCount, setNotifCount] = useState(0);
  const accountMenuRef = useRef(null);

  useEffect(() => {
    let alive = true;
    Promise.all([
      client.get('/content/history').catch(() => ({ data: [] })),
      client.get('/plagiarism/history').catch(() => ({ data: [] })),
      client.get('/chapters/history').catch(() => ({ data: [] })),
    ]).then(([generations, checks, chapters]) => {
      if (!alive) return;
      const total =
        (generations.data?.length || 0) +
        (checks.data?.length || 0) +
        (chapters.data?.length || 0);
      setNotifCount(Math.min(total, 99));
    });
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (!accountMenuOpen) return undefined;
    const onPointerDown = (event) => {
      if (!accountMenuRef.current?.contains(event.target)) {
        setAccountMenuOpen(false);
      }
    };
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setAccountMenuOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [accountMenuOpen]);

  const leave = () => {
    setMenuOpen(false);
    setAccountMenuOpen(false);
    logout();
    navigate('/login');
  };

  const addAccount = () => {
    setMenuOpen(false);
    setAccountMenuOpen(false);
    logout();
    navigate('/register');
  };

  return (
    <div className="app-shell">
      <header className="app-topbar">
        <button
          type="button"
          className="icon-circle-btn"
          aria-label="Open menu"
          onClick={() => setMenuOpen(true)}
        >
          ☰
        </button>
        <div className="app-topbar-right">
          <button
            type="button"
            className="icon-circle-btn notif-btn"
            aria-label={`Notifications, ${notifCount} items`}
            onClick={() => navigate('/history')}
          >
            🔔
            {notifCount > 0 && <span className="notif-badge">{notifCount}</span>}
          </button>
          <div className="topbar-account-wrap" ref={accountMenuRef}>
            <button
              type="button"
              className="user-avatar user-avatar-btn"
              title={user?.name || 'Account menu'}
              aria-label="Open account menu"
              aria-haspopup="menu"
              aria-expanded={accountMenuOpen}
              onClick={() => setAccountMenuOpen((open) => !open)}
            >
              {initials(user?.name)}
            </button>
            {accountMenuOpen && (
              <div className="topbar-account-menu" role="menu">
                <div className="chat-account-menu-head">
                  <span className="chat-user-avatar" aria-hidden="true">
                    {initials(user?.name)}
                  </span>
                  <div>
                    <strong>{user?.name || 'WordWise user'}</strong>
                    <p>{user?.email || 'Signed in'}</p>
                  </div>
                </div>
                <button type="button" role="menuitem" onClick={leave}>
                  Log in with another account
                </button>
                <button type="button" role="menuitem" onClick={addAccount}>
                  Add a new account
                </button>
                <button type="button" role="menuitem" className="chat-account-logout" onClick={leave}>
                  Log out
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <Offcanvas show={menuOpen} onHide={() => setMenuOpen(false)} className="app-drawer" placement="start">
        <Offcanvas.Header closeButton>
          <Offcanvas.Title className="d-flex align-items-center gap-2">
            <LogoMark className="nav-logo" /> WordWise
          </Offcanvas.Title>
        </Offcanvas.Header>
        <Offcanvas.Body className="d-flex flex-column">
          <nav className="app-drawer-nav">
            {DRAWER_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) => `app-drawer-link${isActive ? ' active' : ''}`}
                onClick={() => setMenuOpen(false)}
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="app-drawer-footer">
            <p className="small text-muted mb-2 px-2">
              {user?.name} · {user?.subscriptionPlan || 'FREE'}
            </p>
            <button type="button" className="app-drawer-link w-100 mb-2" onClick={addAccount}>
              Add a new account
            </button>
            <button type="button" className="app-drawer-logout" onClick={leave}>
              Log out
            </button>
          </div>
        </Offcanvas.Body>
      </Offcanvas>

      <main className={`main-content${isChat ? ' main-content-chat' : ''}`}>
        {isChat ? (
          <div className="chat-page">{children}</div>
        ) : (
          <Container>{children}</Container>
        )}
      </main>

      <nav className="bottom-nav" aria-label="Primary">
        {BOTTOM_LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => `bottom-nav-item${isActive ? ' active' : ''}`}
          >
            <span className="icon" aria-hidden="true">
              {link.icon}
            </span>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
