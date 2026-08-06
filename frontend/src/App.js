import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Container } from 'react-bootstrap';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppNavbar from './components/AppNavbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ContentGenerator from './pages/ContentGenerator';
import ChapterSummaryQuiz from './pages/ChapterSummaryQuiz';
import PlagiarismChecker from './pages/PlagiarismChecker';
import History from './pages/History';
import Pricing from './pages/Pricing';
import PaymentSuccess from './pages/PaymentSuccess';
import Reports from './pages/Reports';

function PrivateLayout() { return <><AppNavbar /><main className="main-content"><Container><Routes>
  <Route path="/dashboard" element={<Dashboard />} /><Route path="/generate" element={<ContentGenerator />} /><Route path="/chapters" element={<ChapterSummaryQuiz />} />
  <Route path="/plagiarism" element={<PlagiarismChecker />} /><Route path="/history" element={<History />} /><Route path="/pricing" element={<Pricing />} />
  <Route path="/payment-success" element={<PaymentSuccess />} /><Route path="/reports" element={<Reports />} /><Route path="*" element={<Navigate to="/dashboard" replace />} />
</Routes></Container></main></>; }
export default function App() { return <AuthProvider><BrowserRouter><Routes>
  <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
  <Route element={<ProtectedRoute />}><Route path="/*" element={<PrivateLayout />} /></Route>
</Routes></BrowserRouter></AuthProvider>; }
