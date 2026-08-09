import { useState } from 'react';
import { Alert, Button, Card, Col, Row } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import client, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import PageIntro from '../components/PageIntro';

const free = [
  'Offline mock AI generator',
  'PDF summary & 5-question quiz',
  'Local similarity checks',
  'Personal history',
];
const pro = [...free, 'Priority cloud AI providers', 'Advanced writing workflows', 'Pro badge on your workspace'];

function Plan({ name, price, features, action, accent }) {
  return (
    <Card className={`price-card ${accent}`}>
      <Card.Body className="p-4 p-md-5">
        <p className="eyebrow">{name.toUpperCase()}</p>
        <h2>
          {price}
          <small>{price === '₹0' ? ' forever' : ' / month'}</small>
        </h2>
        <p className="price-copy">
          {name === 'Free'
            ? 'Everything needed for a complete capstone demo.'
            : 'Extra room for your professional writing workflow.'}
        </p>
        <ul>
          {features.map((feature) => (
            <li key={feature}>✓ {feature}</li>
          ))}
        </ul>
        {action}
      </Card.Body>
    </Card>
  );
}

function loadRazorpay() {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

export default function Pricing() {
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const upgrade = async () => {
    setBusy(true);
    setError('');
    try {
      const ready = await loadRazorpay();
      if (!ready) {
        throw new Error('Could not load Razorpay Checkout. Check your network and try again.');
      }

      const { data } = await client.post('/payments/create-order');
      const options = {
        key: data.keyId,
        amount: data.amount,
        currency: data.currency,
        name: 'WordWise',
        description: data.description || 'WordWise Pro',
        order_id: data.orderId,
        prefill: {
          name: user?.name || '',
          email: user?.email || '',
        },
        theme: { color: '#3DBE6C' },
        handler: async (response) => {
          try {
            const confirm = await client.post('/payments/confirm', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            updateUser(confirm.data.user);
            navigate('/payment-success');
          } catch (err) {
            setError(errorMessage(err));
            setBusy(false);
          }
        },
        modal: {
          ondismiss: () => setBusy(false),
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', (event) => {
        setError(event?.error?.description || 'Payment failed. Try again with a Razorpay test card.');
        setBusy(false);
      });
      rzp.open();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <>
      <PageIntro eyebrow="SIMPLE, FAIR PRICING" title="Choose your workspace.">
        The Free plan contains every locally-demoable feature. Pro checkout uses Razorpay test mode when configured.
      </PageIntro>
      {error && <Alert variant="danger">{error}</Alert>}
      <Row className="g-4 justify-content-center">
        <Col lg={5}>
          <Plan
            name="Free"
            price="₹0"
            features={free}
            accent="free"
            action={
              <Button variant="outline-success" disabled>
                Current plan
              </Button>
            }
          />
        </Col>
        <Col lg={5}>
          <Plan
            name="Pro"
            price="₹499"
            features={pro}
            accent="pro"
            action={
              <Button
                className="primary-button"
                onClick={upgrade}
                disabled={busy || user?.subscriptionPlan === 'PRO'}
              >
                {user?.subscriptionPlan === 'PRO'
                  ? 'Pro is active'
                  : busy
                    ? 'Opening Razorpay…'
                    : 'Upgrade to Pro'}
              </Button>
            }
          />
        </Col>
      </Row>
    </>
  );
}
