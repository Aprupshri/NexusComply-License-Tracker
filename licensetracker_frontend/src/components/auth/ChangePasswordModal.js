import React, { useState } from 'react';
import { Modal, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { toast } from 'sonner';
import authService from '../../services/authService';

const ChangePasswordModal = ({ show, onHide, onPasswordChanged, isForced = false }) => {
    const [formData, setFormData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
        setError('');
    };

    const validatePassword = () => {
        if (formData.newPassword.length < 6) {
            setError('New password must be at least 6 characters long');
            return false;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            setError('New password and confirmation do not match');
            return false;
        }

        if (formData.currentPassword === formData.newPassword) {
            setError('New password must be different from current password');
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!validatePassword()) {
            return;
        }

        setLoading(true);

        try {
            await authService.changePassword(
                formData.currentPassword,
                formData.newPassword,
                formData.confirmPassword
            );

            // Update user in storage to remove password change flag
            authService.updateUserInStorage({ passwordChangeRequired: false });

            toast.success('Password changed successfully!');
            
            // Reset form
            setFormData({
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
            });

            // Call callback
            if (onPasswordChanged) {
                onPasswordChanged();
            }

            // Close modal
            if (!isForced) {
                onHide();
            }
        } catch (error) {
            console.error('Password change error:', error);
            setError(error.response?.data?.message || 'Failed to change password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Modal 
            show={show} 
            onHide={isForced ? undefined : onHide}
            backdrop={isForced ? 'static' : true}
            keyboard={!isForced}
        >
            <Modal.Header closeButton={!isForced}>
                <Modal.Title>
                    {isForced ? 'Change Password Required' : 'Change Password'}
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {isForced && (
                    <Alert variant="warning">
                        <i className="bi bi-exclamation-triangle me-2"></i>
                        <strong>Security Notice:</strong> You must change your password before continuing.
                        This is a temporary password set by the administrator.
                    </Alert>
                )}

                {error && <Alert variant="danger">{error}</Alert>}

                <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-3">
                        <Form.Label>Current Password *</Form.Label>
                        <Form.Control
                            type="password"
                            name="currentPassword"
                            value={formData.currentPassword}
                            onChange={handleChange}
                            placeholder="Enter current password"
                            required
                        />
                    </Form.Group>

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
                        />
                        <Form.Text className="text-muted">
                            Must be at least 6 characters and different from current password
                        </Form.Text>
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Confirm New Password *</Form.Label>
                        <Form.Control
                            type="password"
                            name="confirmPassword"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            placeholder="Re-enter new password"
                            required
                        />
                    </Form.Group>

                    <div className="d-grid gap-2">
                        {!isForced && (
                            <Button 
                                variant="secondary" 
                                onClick={onHide}
                                disabled={loading}
                            >
                                Cancel
                            </Button>
                        )}
                        <Button 
                            variant="primary" 
                            type="submit"
                            disabled={loading}
                        >
                            {loading ? (
                                <>
                                    <Spinner size="sm" className="me-2" />
                                    Changing Password...
                                </>
                            ) : (
                                <>
                                    <i className="bi bi-shield-check me-2"></i>
                                    Change Password
                                </>
                            )}
                        </Button>
                    </div>
                </Form>
            </Modal.Body>
        </Modal>
    );
};

export default ChangePasswordModal;
