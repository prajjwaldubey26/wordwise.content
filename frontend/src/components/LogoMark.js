export default function LogoMark({ className = '' }) {
  return <svg className={`logo-mark ${className}`} viewBox="0 0 32 32" role="img" aria-label="WordWise logo">
    <path d="M4.5 6.5 9.5 23 16 10.5 22.5 23 27.5 6.5" fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3.2" />
    <path d="m21.5 7 4.9-3.4 2.1 2.2-4.7 3.7-3.3.8z" fill="currentColor" opacity=".92" />
    <path d="m25.7 4.1 1.9 2" fill="none" stroke="#f8f6ef" strokeLinecap="round" strokeWidth="1.1" />
  </svg>;
}
