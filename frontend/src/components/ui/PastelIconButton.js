import { Link } from 'react-router-dom';

export default function PastelIconButton({ to, icon, label, tone = 'green' }) {
  return (
    <Link className="pastel-icon-btn" to={to}>
      <span className={`pastel-icon-tile ${tone}`} aria-hidden="true">
        {icon}
      </span>
      <label>{label}</label>
    </Link>
  );
}
