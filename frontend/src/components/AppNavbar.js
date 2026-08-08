import { Container, Nav, Navbar, Badge, Button } from 'react-bootstrap';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LogoMark from './LogoMark';

export default function AppNavbar() {
  const { user, logout } = useAuth(); const navigate = useNavigate();
  const leave = () => { logout(); navigate('/login'); };
  return <Navbar expand="lg" className="app-navbar" sticky="top"><Container>
    <Navbar.Brand as={NavLink} to="/dashboard" className="brand-mark"><LogoMark className="nav-logo" /> WordWise</Navbar.Brand>
    <Navbar.Toggle aria-controls="main-nav" /><Navbar.Collapse id="main-nav">
      <Nav className="me-auto"><Nav.Link as={NavLink} to="/dashboard">Dashboard</Nav.Link><Nav.Link as={NavLink} to="/chat">Chat</Nav.Link><Nav.Link as={NavLink} to="/generate">Generate</Nav.Link><Nav.Link as={NavLink} to="/chapters">Summary & Quiz</Nav.Link><Nav.Link as={NavLink} to="/plagiarism">Originality</Nav.Link><Nav.Link as={NavLink} to="/history">History</Nav.Link><Nav.Link as={NavLink} to="/reports">Reports</Nav.Link></Nav>
      <Nav className="align-items-lg-center gap-lg-2"><span className="nav-user">Hi, {user?.name?.split(' ')[0]}</span><Badge bg={user?.subscriptionPlan === 'PRO' ? 'warning' : 'light'} text={user?.subscriptionPlan === 'PRO' ? 'dark' : 'success'} className="plan-badge">{user?.subscriptionPlan}</Badge><Button variant="link" className="logout-link" onClick={leave}>Logout</Button></Nav>
    </Navbar.Collapse>
  </Container></Navbar>;
}
