import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import authService from '../../services/authService';

const ResetPassword = () => {
    const [searchParams] = useSearchParams();
    const [formData, setFormData] = useState({
        newPassword: '',
        confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [validating, setValidating] = useState(true);
    const [tokenValid, setTokenValid] = useState(false);
    const [token, setToken] = useState('');
    
    const navigate = useNavigate();

    useEffect(() => {
        const tokenParam = searchParams.get('token');
        if (!tokenParam) {
            toast.error('Invalid reset link');
            navigate('/login');
            return;
        }
        
        setToken(tokenParam);
        validateToken(tokenParam);
    }, [searchParams, navigate]);

    const validateToken = async (tokenParam) => {
        setValidating(true);
        try {
            const response = await authService.validateResetToken(tokenParam);
            setTokenValid(response.valid);
            
            if (!response.valid) {
                toast.error('Reset link has expired or is invalid');
            }
        } catch (error) {
            console.error('Token validation error:', error);
            setTokenValid(false);
            toast.error('Invalid reset link');
        } finally {
            setValidating(false);
        }
    };

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const validateForm = () => {
        if (formData.newPassword.length < 6) {
            toast.error('Password must be at least 6 characters long');
            return false;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            toast.error('Passwords do not match');
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setLoading(true);

        try {
            await authService.resetPassword(
                token,
                formData.newPassword,
                formData.confirmPassword
            );

            toast.success('Password reset successfully!');
            
            setTimeout(() => {
                navigate('/login');
            }, 1500);
        } catch (error) {
            console.error('Reset password error:', error);
            toast.error(error.response?.data?.message || 'Failed to reset password');
        } finally {
            setLoading(false);
        }
    };

    if (validating) {
        return (
            <div className="min-vh-100 d-flex align-items-center justify-content-center" style={{ backgroundColor: '#f8f9fa' }}>
                <div className="text-center">
                    <Spinner animation="border" variant="primary" />
                    <p className="mt-3">Validating reset link...</p>
                </div>
            </div>
        );
    }

    if (!tokenValid) {
        return (
            <div className="min-vh-100 d-flex align-items-center" style={{ backgroundColor: '#f8f9fa' }}>
                <Container>
                    <Row className="justify-content-center">
                        <Col md={6} lg={5}>
                            <Card className="shadow-lg border-0">
                                <Card.Body className="p-5 text-center">
                                    <i className="bi bi-x-circle text-danger" style={{ fontSize: '4rem' }}></i>
                                    <h3 className="mt-3 mb-2">Invalid Reset Link</h3>
                                    <p className="text-muted mb-4">
                                        This password reset link is invalid or has expired.
                                    </p>
                                    <div className="d-grid gap-2">
                                        <Button 
                                            variant="primary"
                                            onClick={() => navigate('/forgot-password')}
                                        >
                                            Request New Link
                                        </Button>
                                        <Button 
                                            variant="outline-secondary"
                                            onClick={() => navigate('/login')}
                                        >
                                            Back to Login
                                        </Button>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </Container>
            </div>
        );
    }

    return (
        <div className="min-vh-100 d-flex align-items-center" style={{ backgroundColor: '#f8f9fa' }}>
            <Container>
                <Row className="justify-content-center">
                    <Col md={6} lg={5}>
                        <Card className="shadow-lg border-0">
                            <Card.Body className="p-5">
                                <div className="text-center mb-4">
                                    <i className="bi bi-key text-primary" style={{ fontSize: '3rem' }}></i>
                                    <h3 className="mt-3 mb-2">Reset Password</h3>
                                    <p className="text-muted">Enter your new password</p>
                                </div>

                                <Form onSubmit={handleSubmit}>
                                    <Form.Group className="mb-3">
                                        <Form.Label>New Password *</Form.Label>
                                        <Form.Control
                                            type="password"
                                            name="newPassword"
                                            value={formData.newPassword}
                                            onChange={handleChange}
                                            placeholder="Minimum 6 characters"
                                            required
                                            minLength={6}
                                            size="lg"
                                        />
                                        <Form.Text className="text-muted">
                                            Must be at least 6 characters long
                                        </Form.Text>
                                    </Form.Group>

                                    <Form.Group className="mb-4">
                                        <Form.Label>Confirm New Password *</Form.Label>
                                        <Form.Control
                                            type="password"
                                            name="confirmPassword"
                                            value={formData.confirmPassword}
                                            onChange={handleChange}
                                            placeholder="Re-enter new password"
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
                                                    Resetting...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    Reset Password
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
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Container>
        </div>
    );
};

export default ResetPassword;
