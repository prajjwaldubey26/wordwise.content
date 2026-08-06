import { useState } from 'react';
import { Alert, Button, Card, Col, Row } from 'react-bootstrap';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import PageIntro from '../components/PageIntro';

const free = ['Offline mock AI generator', 'PDF summary & 5-question quiz', 'Local similarity checks', 'Personal history']; const pro = [...free, 'Priority cloud AI providers', 'Advanced writing workflows', 'Pro badge on your workspace'];
function Plan({ name, price, features, action, accent }) { return <Card className={`price-card ${accent}`}><Card.Body className="p-4 p-md-5"><p className="eyebrow">{name.toUpperCase()}</p><h2>{price}<small>{price === '$0' ? ' forever' : ' / month'}</small></h2><p className="price-copy">{name === 'Free' ? 'Everything needed for a complete capstone demo.' : 'Extra room for your professional writing workflow.'}</p><ul>{features.map((feature) => <li key={feature}>✓ {feature}</li>)}</ul>{action}</Card.Body></Card>; }
export default function Pricing() {
  const { user } = useAuth(); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const upgrade = async () => { setBusy(true); setError(''); try { const { data } = await client.post('/payments/create-checkout-session'); window.location.assign(data.checkoutUrl); } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); } };
  return <><PageIntro eyebrow="SIMPLE, FAIR PRICING" title="Choose your workspace.">The Free plan contains every locally-demoable feature. Pro checkout uses Stripe test mode when configured.</PageIntro>{error && <Alert variant="danger">{error}</Alert>}<Row className="g-4 justify-content-center"><Col lg={5}><Plan name="Free" price="$0" features={free} accent="free" action={<Button variant="outline-success" disabled>Current plan</Button>} /></Col><Col lg={5}><Plan name="Pro" price="$9.99" features={pro} accent="pro" action={<Button className="primary-button" onClick={upgrade} disabled={busy || user?.subscriptionPlan === 'PRO'}>{user?.subscriptionPlan === 'PRO' ? 'Pro is active' : busy ? 'Opening checkout…' : 'Upgrade to Pro'}</Button>} /></Col></Row></>;
}
