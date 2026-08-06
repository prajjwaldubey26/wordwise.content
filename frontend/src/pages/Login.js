import { useState } from 'react';
import { Alert, Button, Card, Form } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import LogoMark from '../components/LogoMark';

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' }); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const { authenticate } = useAuth(); const navigate = useNavigate();
  const submit = async (event) => { event.preventDefault(); setError(''); setBusy(true); try { const { data } = await client.post('/auth/login', form); authenticate(data); navigate('/dashboard'); } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); } };
  return <div className="auth-page"><Card className="auth-card"><Card.Body className="p-4 p-md-5"><div className="text-center mb-4"><div className="auth-logo"><LogoMark /></div><p className="eyebrow">WELCOME BACK</p><h1>Give your best ideas a voice.</h1><p className="text-muted">Sign in to your WordWise workspace.</p></div>{error && <Alert variant="danger">{error}</Alert>}
    <Form onSubmit={submit}><Form.Group className="mb-3"><Form.Label>Email</Form.Label><Form.Control type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Form.Group><Form.Group className="mb-4"><Form.Label>Password</Form.Label><Form.Control type="password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Form.Group><Button className="w-100 primary-button" type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</Button></Form>
    <p className="text-center small mt-4 mb-0">New here? <Link to="/register">Create a free account</Link></p></Card.Body></Card></div>;
}
