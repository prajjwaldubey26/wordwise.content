import { Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PaymentSuccess() {
  const { user } = useAuth();
  const isPro = user?.subscriptionPlan === 'PRO';

  return (
    <div className="payment-result">
      <Card>
        <Card.Body className="p-5 text-center">
          {isPro ? (
            <>
              <div className="result-symbol">✓</div>
              <p className="eyebrow">PAYMENT CONFIRMED</p>
              <h2>Welcome to Pro.</h2>
              <p className="text-muted">
                Your Razorpay payment succeeded and your workspace has been upgraded. You now have the Pro plan badge
                on your account.
              </p>
              <Link className="btn primary-button" to="/dashboard">
                Go to dashboard
              </Link>
            </>
          ) : (
            <>
              <div className="result-symbol error">!</div>
              <h2>No active Pro plan yet.</h2>
              <p className="text-muted">
                If you just paid with Razorpay, return to Pricing and complete checkout again, or contact support if
                the amount was deducted.
              </p>
              <Link className="btn primary-button" to="/pricing">
                Return to pricing
              </Link>
            </>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}
