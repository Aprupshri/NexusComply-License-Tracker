import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import authService from '../../services/authService';

const ForgotPassword = () => {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [resetInfo, setResetInfo] = useState(null);
    
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!email || !email.includes('@')) {
            toast.error('Please enter a valid email address');
            return;
        }

        setLoading(true);

        try {
            const response = await authService.forgotPassword(email);
            setResetInfo(response);
            setSuccess(true);
            toast.success('Password reset link sent!');
        } catch (error) {
            console.error('Forgot password error:', error);
            toast.error(error.response?.data?.message || 'Failed to send reset link');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-vh-100 d-flex align-items-center" style={{ backgroundColor: '#f8f9fa' }}>
            <Container>
                <Row className="justify-content-center">
                    <Col md={6} lg={5}>
                        <Card className="shadow-lg border-0">
                            <Card.Body className="p-5">
                                <div className="text-center mb-4">
                                    <i className="bi bi-shield-lock text-primary" style={{ fontSize: '3rem' }}></i>
                                    <h3 className="mt-3 mb-2">Forgot Password?</h3>
                                    <p className="text-muted">
                                        {success 
                                            ? 'Check your email for reset instructions'
                                            : 'Enter your email to receive a password reset link'
                                        }
                                    </p>
                                </div>

                                {success ? (
                                    <>
                                        <Alert variant="success">
                                            <i className="bi bi-check-circle me-2"></i>
                                            Password reset link has been sent to <strong>{resetInfo?.email}</strong>
                                        </Alert>

                                        {/* Development only - remove in production */}
                                        {resetInfo?.resetToken && (
                                            <Alert variant="warning">
                                                <strong>Development Mode:</strong>
                                                <br />
                                                <small>
                                                    Since email is not configured, use this token:
                                                    <br />
                                                    <code>{resetInfo.resetToken}</code>
                                                    <br />
                                                    <Button
                                                        size="sm"
                                                        variant="outline-primary"
                                                        className="mt-2"
                                                        onClick={() => navigate(`/reset-password?token=${resetInfo.resetToken}`)}
                                                    >
                                                        Go to Reset Password
                                                    </Button>
                                                </small>
                                            </Alert>
                                        )}

                                        <div className="d-grid gap-2 mt-4">
                                            <Button 
                                                variant="primary"
                                                onClick={() => navigate('/login')}
                                            >
                                                <i className="bi bi-arrow-left me-2"></i>
                                                Back to Login
                                            </Button>
                                        </div>
                                    </>
                                ) : (
                                    <Form onSubmit={handleSubmit}>
                                        <Form.Group className="mb-4">
                                            <Form.Label>Email Address</Form.Label>
                                            <Form.Control
                                                type="email"
                                                placeholder="Enter your email"
                                                value={email}
                                                onChange={(e) => setEmail(e.target.value)}
                                                required
                                                size="lg"
                                            />
                                        </Form.Group>

                                        <div className="d-grid gap-2">
                                            <Button 
                                                variant="primary" 
                                                type="submit"
                                                disabled={loading}
                                                size="lg"
                                            >
                                                {loading ? (
                                                    <>
                                                        <Spinner size="sm" className="me-2" />
                                                        Sending...
                                                    </>
                                                ) : (
                                                    <>
                                                        <i className="bi bi-envelope me-2"></i>
                                                        Send Reset Link
                                                    </>
                                                )}
                                            </Button>

                                            <Button 
                                                variant="outline-secondary"
                                                onClick={() => navigate('/login')}
                                                disabled={loading}
                                            >
                                                <i className="bi bi-arrow-left me-2"></i>
                                                Back to Login
                                            </Button>
                                        </div>
                                    </Form>
                                )}

                                <div className="text-center mt-4">
                                    <p className="text-muted small mb-0">
                                        Need help? Contact your administrator
                                    </p>
                                </div>
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
        </div>
    );
};

export default ForgotPassword;
