export default function ProgressRing({ percent = 0, size = 96, stroke = 8, label }) {
  const value = Math.max(0, Math.min(100, Math.round(percent)));
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (value / 100) * circumference;
  const center = size / 2;

  return (
    <div className="progress-ring" style={{ width: size, height: size }} aria-label={label || `${value} percent`}>
      <svg viewBox={`0 0 ${size} ${size}`} role="img">
        <circle className="progress-ring-track" cx={center} cy={center} r={radius} strokeWidth={stroke} />
        <circle
          className="progress-ring-value"
          cx={center}
          cy={center}
          r={radius}
          strokeWidth={stroke}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
        <text className="progress-ring-label" x="50%" y="50%" dominantBaseline="central" textAnchor="middle">
          {value}%
        </text>
      </svg>
    </div>
  );
}
