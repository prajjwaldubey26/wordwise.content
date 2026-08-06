import { useEffect, useState } from 'react';
import { Alert, Card, Col, Row, Spinner } from 'react-bootstrap';
import { Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import PageIntro from '../components/PageIntro';

const colors = ['#2e6a5d', '#d88a63'];
export default function Reports() {
  const { user } = useAuth(); const [personal, setPersonal] = useState(null); const [admin, setAdmin] = useState(null); const [error, setError] = useState('');
  useEffect(() => { client.get('/dashboard').then((r) => setPersonal(r.data)).catch((e) => setError(errorMessage(e))); if (user?.role === 'ADMIN') client.get('/reports/admin').then((r) => setAdmin(r.data)).catch((e) => setError(errorMessage(e))); }, [user?.role]);
  if (!personal) return <div className="text-center py-5"><Spinner animation="border" /></div>;
  const activity = [{ name: 'Drafts', value: personal.generations }, { name: 'Checks', value: personal.plagiarismChecks }, { name: 'Summaries', value: personal.chapterSummaries }];
  const planData = admin ? [{ name: 'Free', value: admin.freeUsers }, { name: 'Pro', value: admin.proUsers }] : [];
  return <><PageIntro eyebrow="REPORTS & ANALYTICS" title="A clear view of your work.">Track your own activity and, for admins, a compact overview of the platform.</PageIntro>{error && <Alert variant="danger">{error}</Alert>}<Row className="g-4"><Col lg={7}><Card className="chart-card"><Card.Body><h2>Personal activity</h2><ResponsiveContainer width="100%" height={280}><BarChart data={activity}><XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="value" radius={[7, 7, 0, 0]} fill="#2e6a5d" /></BarChart></ResponsiveContainer></Card.Body></Card></Col><Col lg={5}><Card className="chart-card"><Card.Body><p className="eyebrow">ORIGINALITY TREND</p><h2>{personal.averageSimilarity}%</h2><p className="text-muted">Your average similarity score across all saved checks. Lower typically means more original wording.</p><div className="metric-line"><span>0%</span><i style={{ width: `${Math.min(personal.averageSimilarity, 100)}%` }} /><span>100%</span></div></Card.Body></Card></Col>{user?.role === 'ADMIN' && <><Col lg={7}><Card className="chart-card"><Card.Body><h2>Platform totals</h2><div className="admin-total-grid"><span><b>{admin?.users}</b> users</span><span><b>{admin?.generations}</b> drafts</span><span><b>{admin?.plagiarismChecks}</b> checks</span><span><b>{admin?.chapterSummaries}</b> summaries</span></div></Card.Body></Card></Col><Col lg={5}><Card className="chart-card"><Card.Body><h2>Subscription mix</h2><ResponsiveContainer width="100%" height={210}><PieChart><Pie data={planData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={80}>{planData.map((entry, index) => <Cell key={entry.name} fill={colors[index]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></Card.Body></Card></Col></>}</Row></>;
}
