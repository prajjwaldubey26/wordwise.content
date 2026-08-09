const QUOTES = [
  'Small steps today become the draft you are proud of tomorrow.',
  'Clarity loves a quiet mind — write one honest sentence first.',
  'Progress is not perfect words. It is showing up again.',
  'Your best work grows from curious questions, not pressure.',
];

export default function QuoteCard({ seed = 0 }) {
  const quote = QUOTES[Math.abs(seed) % QUOTES.length];

  return (
    <div className="quote-card">
      <div className="quote-mark" aria-hidden="true">
        “
      </div>
      <p>{quote}</p>
      <svg className="quote-wave" viewBox="0 0 400 48" preserveAspectRatio="none" aria-hidden="true">
        <path
          d="M0 28 C60 8 120 48 180 28 C240 8 300 40 400 18 L400 48 L0 48 Z"
          fill="#ffffff"
        />
      </svg>
    </div>
  );
}
