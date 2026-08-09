import { useEffect, useState } from 'react';
import { Alert, Col, Row, Spinner } from 'react-bootstrap';
import { Bar, BarChart, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import SoftCard from '../components/ui/SoftCard';
import ProgressRing from '../components/ui/ProgressRing';

const colors = ['#3DBE6C', '#E6E1FA'];

export default function Reports() {
  const { user } = useAuth();
  const [personal, setPersonal] = useState(null);
  const [admin, setAdmin] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    client
      .get('/dashboard')
      .then((r) => setPersonal(r.data))
      .catch((e) => setError(errorMessage(e)));
    if (user?.role === 'ADMIN') {
      client
        .get('/reports/admin')
        .then((r) => setAdmin(r.data))
        .catch((e) => setError(errorMessage(e)));
    }
  }, [user?.role]);

  if (!personal) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" />
      </div>
    );
  }

  const activity = [
    { name: 'Drafts', value: personal.generations },
    { name: 'Checks', value: personal.plagiarismChecks },
    { name: 'Summaries', value: personal.chapterSummaries },
  ];
  const planData = admin
    ? [
        { name: 'Free', value: admin.freeUsers },
        { name: 'Pro', value: admin.proUsers },
      ]
    : [];

  const originality = Math.min(personal.averageSimilarity || 0, 100);

  return (
    <>
      <div className="page-intro">
        <p className="eyebrow">Reports & analytics</p>
        <h1>A clear view of your work.</h1>
        <p className="intro-copy">
          Track your activity with soft, readable charts — and for admins, a compact platform overview.
        </p>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Row className="g-4">
        <Col lg={7}>
          <SoftCard className="chart-card">
            <div className="card-body">
              <h2>Personal activity</h2>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={activity}>
                  <XAxis dataKey="name" tick={{ fill: '#8A94A6', fontSize: 12 }} />
                  <YAxis allowDecimals={false} tick={{ fill: '#8A94A6', fontSize: 12 }} />
                  <Tooltip />
                  <Bar dataKey="value" radius={[10, 10, 0, 0]} fill="#3DBE6C" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </SoftCard>
        </Col>
        <Col lg={5}>
          <SoftCard className="chart-card">
            <div className="card-body">
              <p className="eyebrow">Originality trend</p>
              <h2>Average similarity</h2>
              <div className="reports-ring-wrap">
                <ProgressRing percent={originality} label="Average similarity score" />
                <p className="text-muted mb-0">
                  Your average similarity across saved checks. Lower typically means more original wording.
                </p>
              </div>
              <div className="metric-line">
                <span>0%</span>
                <i style={{ width: `${originality}%` }} />
                <span>100%</span>
              </div>
            </div>
          </SoftCard>
        </Col>

        {user?.role === 'ADMIN' && (
          <>
            <Col lg={7}>
              <SoftCard className="chart-card">
                <div className="card-body">
                  <h2>Platform totals</h2>
                  <div className="admin-total-grid">
                    <span>
                      <b>{admin?.users}</b> users
                    </span>
                    <span>
                      <b>{admin?.generations}</b> drafts
                    </span>
                    <span>
                      <b>{admin?.plagiarismChecks}</b> checks
                    </span>
                    <span>
                      <b>{admin?.chapterSummaries}</b> summaries
                    </span>
                  </div>
                </div>
              </SoftCard>
            </Col>
            <Col lg={5}>
              <SoftCard className="chart-card">
                <div className="card-body">
                  <h2>Subscription mix</h2>
                  <ResponsiveContainer width="100%" height={210}>
                    <PieChart>
                      <Pie data={planData} dataKey="value" nameKey="name" innerRadius={55} outerRadius={80}>
                        {planData.map((entry, index) => (
                          <Cell key={entry.name} fill={colors[index]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </SoftCard>
            </Col>
          </>
        )}
      </Row>
    </>
  );
}
