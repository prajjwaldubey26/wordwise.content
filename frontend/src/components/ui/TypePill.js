const LABELS = {
  generation: 'Draft',
  check: 'Check',
  summary: 'Summary',
};

export default function TypePill({ type = 'generation' }) {
  const key = LABELS[type] ? type : 'generation';
  return <span className={`type-pill ${key}`}>{LABELS[key]}</span>;
}
