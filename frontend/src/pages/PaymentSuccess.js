import { useEffect, useState } from 'react';
import { Alert, Card, Spinner } from 'react-bootstrap';
import { Link, useSearchParams } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function PaymentSuccess() {
  const [params] = useSearchParams(); const sessionId = params.get('session_id'); const [state, setState] = useState({ loading: true, error: '' }); const { updateUser } = useAuth();
  useEffect(() => { if (!sessionId) { setState({ loading: false, error: 'No Stripe Checkout session was supplied.' }); return; } client.post(`/payments/confirm?sessionId=${encodeURIComponent(sessionId)}`).then(({ data }) => { updateUser(data.user); setState({ loading: false, error: '' }); }).catch((err) => setState({ loading: false, error: errorMessage(err) })); }, [sessionId, updateUser]);
  return <div className="payment-result"><Card><Card.Body className="p-5 text-center">{state.loading ? <><Spinner animation="border" className="mb-3" /><h2>Confirming your payment…</h2></> : state.error ? <><div className="result-symbol error">!</div><h2>We couldn’t confirm the payment.</h2><Alert variant="danger">{state.error}</Alert><Link to="/pricing">Return to pricing</Link></> : <><div className="result-symbol">✓</div><p className="eyebrow">PAYMENT CONFIRMED</p><h2>Welcome to Pro.</h2><p className="text-muted">Your workspace has been upgraded. You now have the Pro plan badge on your account.</p><Link className="btn primary-button" to="/dashboard">Go to dashboard</Link></>}</Card.Body></Card></div>;
}
