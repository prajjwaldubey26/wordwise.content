import { useEffect, useState } from 'react';
import { Card, Col, Row, Spinner } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';
import PageIntro from '../components/PageIntro';
import LogoMark from '../components/LogoMark';

const actions = [{ to: '/chat', icon: '◎', title: 'Chat with AI', text: 'Ask anything and continue past conversations.' }, { to: '/generate', icon: 'wordwise', title: 'Generate content', text: 'Turn a thought into a draft.' }, { to: '/chapters', icon: '▤', title: 'Summarize a chapter', text: 'Upload a PDF and test yourself.' }, { to: '/plagiarism', icon: '✓', title: 'Check originality', text: 'Compare a draft against your corpus.' }];
export default function Dashboard() {
  const { user } = useAuth(); const [stats, setStats] = useState(null);
  useEffect(() => { client.get('/dashboard').then((r) => setStats(r.data)).catch(() => setStats({ generations: 0, plagiarismChecks: 0, chapterSummaries: 0, averageSimilarity: 0 })); }, []);
  const values = stats ? [{ label: 'Content generations', value: stats.generations, accent: 'mint' }, { label: 'Originality checks', value: stats.plagiarismChecks, accent: 'sand' }, { label: 'Chapter summaries', value: stats.chapterSummaries, accent: 'lavender' }, { label: 'Average similarity', value: `${stats.averageSimilarity}%`, accent: 'peach' }] : [];
  return <><PageIntro eyebrow="YOUR WORKSPACE" title={`Good to see you, ${user?.name?.split(' ')[0]}.`}>Pick up where you left off, or start a new piece of thoughtful work.</PageIntro>
    {!stats ? <div className="text-center py-5"><Spinner animation="border" /></div> : <><Row className="g-3 mb-5">{values.map((item) => <Col md={6} xl={3} key={item.label}><Card className={`stat-card ${item.accent}`}><Card.Body><p>{item.label}</p><strong>{item.value}</strong><span>all time</span></Card.Body></Card></Col>)}</Row>
      <div className="section-heading"><div><p className="eyebrow">QUICK START</p><h2>What are you making today?</h2></div></div><Row className="g-4">{actions.map((item) => <Col md={6} xl={3} key={item.to}><Link className="action-card" to={item.to}><span className="action-icon">{item.icon === 'wordwise' ? <LogoMark /> : item.icon}</span><h3>{item.title}</h3><p>{item.text}</p><b>Open tool <span>→</span></b></Link></Col>)}</Row></>}</>;
}
