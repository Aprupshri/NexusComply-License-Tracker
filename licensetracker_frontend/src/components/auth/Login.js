import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import logo from '../../utils/no-background_1-removebg-preview.png'
import { useAuth } from '../../context/AuthContext';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import 'bootstrap/dist/css/bootstrap.min.css';

const Login = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await login(username, password);
            navigate('/dashboard');
        } catch (err) {
            setError(err.response?.data?.message || 'Invalid username or password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ 
            minHeight: '100vh', 
            background: 'linear-gradient(135deg,hsl(193, 79.60%, 71.20%) 0%,rgb(24, 101, 225) 100%)',
            display: 'flex',
            alignItems: 'center'
        }}>
            <Container>
                <Row className="justify-content-center">
                    <Col md={5}>
                        <Card className="shadow-lg border-0">
                            <Card.Body className="p-5">
                                <div className="text-center mb-4">
                                    <img src={logo} className="App-logo" alt="logo" style={{ width: '50px', height: '60px' }}  /> 
                                    <h2 className="fw-bold text-dark mt-3">Telecom License Tracker</h2>
                                    <p className="text-muted">Sign in to your account</p>
                                </div>

                                {error && (
                                    <Alert variant="danger" dismissible onClose={() => setError('')}>
                                        <i className="bi bi-exclamation-circle me-2"></i>
                                        {error}
                                    </Alert>
                                )}

                                <Form onSubmit={handleSubmit}>
                                    <Form.Group className="mb-3">
                                        <Form.Label>
                                            <i className="bi bi-person me-2"></i>
                                            Username
                                        </Form.Label>
                                        <Form.Control
                                            type="text"
                                            placeholder="Enter username"
                                            value={username}
                                            onChange={(e) => setUsername(e.target.value)}
                                            required
                                            disabled={loading}
                                            size="lg"
                                        />
                                    </Form.Group>

                                    <Form.Group className="mb-4">
                                        <Form.Label>
                                            <i className="bi bi-lock me-2"></i>
                                            Password
                                        </Form.Label>
                                        <Form.Control
                                            type="password"
                                            placeholder="Enter password"
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            required
                                            disabled={loading}
                                            size="lg"
                                        />
                                    </Form.Group>
                                    <div className="d-flex justify-content-end mb-3">
                                        <Link 
                                            to="/forgot-password" 
                                            className="text-decoration-none"
                                        >
                                            <small>Forgot Password?</small>
                                        </Link>
                                    </div>
                                    <Button 
                                        variant="primary" 
                                        type="submit" 
                                        className="w-100"
                                        size="lg"
                                        disabled={loading}
                                        style={{ background: 'linear-gradient(135deg,rgb(54, 25, 220) 0%,rgb(20, 118, 231) 100%)', border: 'none' }}
                                    >
                                        {loading ? (
                                            <>
                                                <Spinner
                                                    as="span"
                                                    animation="border"
                                                    size="sm"
                                                    role="status"
                                                    aria-hidden="true"
                                                    className="me-2"
                                                />
                                                Signing in...
                                            </>
                                        ) : (
                                            <>
                                                <i className="bi bi-box-arrow-in-right me-2"></i>
                                                Sign In
                                            </>
                                        )}
                                    </Button>
                                </Form>

                                <div className="text-center mt-4">
                                    <small className="text-muted">
                                        © 2025 Telecom License Tracker. All rights reserved.
                                    </small>
                                </div>
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
        </div>
    );
};

export default Login;
