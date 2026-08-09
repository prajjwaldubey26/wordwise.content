import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import SoftCard from '../components/ui/SoftCard';
import ProgressRing from '../components/ui/ProgressRing';
import PastelIconButton from '../components/ui/PastelIconButton';
import TypePill from '../components/ui/TypePill';
import QuoteCard from '../components/ui/QuoteCard';
import CoachMascot from '../components/ui/CoachMascot';

const DAILY_GOAL = 5;

function greetingForHour(hour) {
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

function timeChip(hour) {
  if (hour < 12) return { label: 'Morning focus', icon: '☀', detail: 'A fresh window for drafts' };
  if (hour < 17) return { label: 'Afternoon flow', icon: '☁', detail: 'Keep the momentum gentle' };
  return { label: 'Evening wind-down', icon: '☾', detail: 'Review and polish slowly' };
}

function formatWhen(value) {
  if (!value) return '';
  return new Date(value).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [recent, setRecent] = useState(null);
  const hour = new Date().getHours();
  const greeting = greetingForHour(hour);
  const chip = timeChip(hour);
  const firstName = user?.name?.split(' ')[0] || 'friend';

  useEffect(() => {
    client
      .get('/dashboard')
      .then((r) => setStats(r.data))
      .catch(() =>
        setStats({ generations: 0, plagiarismChecks: 0, chapterSummaries: 0, averageSimilarity: 0 })
      );

    Promise.all([
      client.get('/content/history').catch(() => ({ data: [] })),
      client.get('/plagiarism/history').catch(() => ({ data: [] })),
      client.get('/chapters/history').catch(() => ({ data: [] })),
    ]).then(([generations, checks, chapters]) => {
      const items = [
        ...(generations.data || []).map((item) => ({
          id: `g-${item.id}`,
          type: 'generation',
          title: item.prompt,
          when: item.createdAt,
          icon: '✦',
        })),
        ...(checks.data || []).map((item) => ({
          id: `c-${item.id}`,
          type: 'check',
          title: `${item.verdict} · ${item.score}%`,
          when: item.createdAt,
          icon: '✓',
        })),
        ...(chapters.data || []).map((item) => ({
          id: `s-${item.id}`,
          type: 'summary',
          title: item.filename,
          when: item.createdAt,
          icon: '▤',
        })),
      ]
        .sort((a, b) => new Date(b.when) - new Date(a.when))
        .slice(0, 5);
      setRecent(items);
    });
  }, []);

  const progress = useMemo(() => {
    if (!stats) return { done: 0, percent: 0 };
    const done = Math.min(
      DAILY_GOAL,
      (stats.generations || 0) + (stats.plagiarismChecks || 0) + (stats.chapterSummaries || 0)
    );
    return { done, percent: Math.round((done / DAILY_GOAL) * 100) };
  }, [stats]);

  if (!stats || recent === null) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" />
      </div>
    );
  }

  return (
    <>
      <SoftCard className="dash-hero">
        <div className="dash-hero-copy">
          <h1 className="dash-greeting">
            {greeting}, <span className="name">{firstName}</span>!
          </h1>
          <p className="dash-subtext">
            Ready for a calm, focused session? Pick a quick action and make something thoughtful today.
          </p>
        </div>
        <div className="dash-mascot-wrap">
          <CoachMascot />
        </div>
      </SoftCard>

      <SoftCard className="progress-card">
        <ProgressRing percent={progress.percent} label="Daily activity progress" />
        <div className="progress-card-meta">
          <h3>
            {progress.done}/{DAILY_GOAL} Tasks
          </h3>
          <p>Content generations, originality checks, and summaries toward today’s gentle goal.</p>
          <div className="linear-progress" aria-hidden="true">
            <i style={{ width: `${progress.percent}%` }} />
          </div>
          <p className="progress-caption">Keep going!</p>
        </div>
      </SoftCard>

      <section className="quick-actions">
        <div className="quick-actions-head">
          <h2>Quick Actions</h2>
          <Link className="section-link" to="/generate">
            Edit
          </Link>
        </div>
        <div className="quick-actions-grid">
          <PastelIconButton to="/chat" icon="◎" label="Chat" tone="lavender" />
          <PastelIconButton to="/generate" icon="✦" label="New Draft" tone="yellow" />
          <PastelIconButton to="/chapters" icon="▤" label="Summarize" tone="blue" />
          <PastelIconButton to="/plagiarism" icon="✓" label="Originality" tone="green" />
        </div>
      </section>

      <SoftCard className="recent-card">
        <div className="section-heading px-3 pt-3 mb-0">
          <h2>Recent work</h2>
          <Link className="section-link" to="/history">
            See all
          </Link>
        </div>
        <div className="px-3 pb-2">
          {recent.length ? (
            recent.map((item) => (
              <Link key={item.id} className="recent-row" to="/history">
                <span className="recent-icon" aria-hidden="true">
                  {item.icon}
                </span>
                <div className="recent-body">
                  <h4>{item.title}</h4>
                  <p>{formatWhen(item.when)}</p>
                </div>
                <TypePill type={item.type} />
              </Link>
            ))
          ) : (
            <p className="text-muted py-4 text-center mb-0">Your recent drafts and checks will show up here.</p>
          )}
        </div>
      </SoftCard>

      <div className="dash-widgets">
        <SoftCard className="time-widget">
          <div className="time-icon" aria-hidden="true">
            {chip.icon}
          </div>
          <h3>{chip.label}</h3>
          <p>{chip.detail}</p>
        </SoftCard>
        <QuoteCard seed={(user?.name || '').length + hour} />
      </div>
    </>
  );
}
