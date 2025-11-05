// src/components/vendors/VendorForm.jsx
import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import vendorService from '../../services/vendorService';

const VendorForm = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [formData, setFormData] = useState({
        vendorName: '',
        contactEmail: '',
        contactPhone: '',
        supportEmail: ''
    });
    const [loading, setLoading] = useState(false);
    const [validated, setValidated] = useState(false);
    
    const navigate = useNavigate();
    const { id } = useParams();
    const isEditMode = !!id;

    useEffect(() => {
        if (isEditMode) {
            fetchVendor();
        }
    }, [id]);

    const fetchVendor = async () => {
        setLoading(true);
        try {
            const data = await vendorService.getVendorById(id);
            setFormData({
                vendorName: data.vendorName,
                contactEmail: data.contactEmail || '',
                contactPhone: data.contactPhone || '',
                supportEmail: data.supportEmail || ''
            });
        } catch (error) {
            console.error('Error fetching vendor:', error);
            toast.error('Failed to fetch vendor details');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const validateForm = () => {
        if (!formData.vendorName.trim()) {
            toast.error('Vendor name is required');
            return false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        
        if (formData.contactEmail && !emailRegex.test(formData.contactEmail)) {
            toast.error('Invalid contact email format');
            return false;
        }

        if (formData.supportEmail && !emailRegex.test(formData.supportEmail)) {
            toast.error('Invalid support email format');
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validateForm()) {
            setValidated(true);
            return;
        }

        setLoading(true);

        const promise = isEditMode
            ? vendorService.updateVendor(id, formData)
            : vendorService.createVendor(formData);

        toast.promise(promise, {
            loading: isEditMode ? 'Updating vendor...' : 'Creating vendor...',
            success: () => {
                setTimeout(() => {
                    navigate('/vendors');
                }, 1000);
                return isEditMode 
                    ? 'Vendor updated successfully!'
                    : 'Vendor created successfully!';
            },
            error: (err) => {
                setLoading(false);
                return err.response?.data?.message || `Failed to ${isEditMode ? 'update' : 'create'} vendor`;
            }
        });
    };

    if (loading && isEditMode) {
        return (
            <>
                <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-4">
                        <div className="text-center py-5">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-3">Loading vendor details...</p>
                        </div>
                    </div>
                </div>
            </>
        );
    }

    return (
        <>
            <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
            <div className="d-flex">
                <Sidebar />
                <div className="flex-grow-1 p-4" style={{ backgroundColor: '#f8f9fa' }}>
                    <Container fluid>
                        {/* Header */}
                        <Row className="mb-4">
                            <Col>
                                <h2 className="fw-bold">
                                    <i className="bi bi-building me-2"></i>
                                    {isEditMode ? 'Edit Vendor' : 'Add Vendor'}
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <Button 
                                    variant="outline-secondary"
                                    onClick={() => navigate('/vendors')}
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                            </Col>
                        </Row>

                        <Card className="shadow-sm border-0">
                            <Card.Body className="p-4">
                                <Form noValidate validated={validated} onSubmit={handleSubmit}>
                                    {/* Vendor Name */}
                                    <Row>
                                        <Col md={12} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Vendor Name *</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="vendorName"
                                                    value={formData.vendorName}
                                                    onChange={handleChange}
                                                    placeholder="e.g., Cisco Systems"
                                                    required
                                                />
                                                <Form.Control.Feedback type="invalid">
                                                    Please provide a vendor name.
                                                </Form.Control.Feedback>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Contact Email & Phone */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Contact Email</Form.Label>
                                                <Form.Control
                                                    type="email"
                                                    name="contactEmail"
                                                    value={formData.contactEmail}
                                                    onChange={handleChange}
                                                    placeholder="contact@vendor.com"
                                                />
                                                <Form.Text className="text-muted">
                                                    Primary contact email
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Contact Phone</Form.Label>
                                                <Form.Control
                                                    type="tel"
                                                    name="contactPhone"
                                                    value={formData.contactPhone}
                                                    onChange={handleChange}
                                                    placeholder="+1-234-567-8900"
                                                />
                                                <Form.Text className="text-muted">
                                                    Primary contact number
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Support Email */}
                                    <Row>
                                        <Col md={12} className="mb-4">
                                            <Form.Group>
                                                <Form.Label>Support Email</Form.Label>
                                                <Form.Control
                                                    type="email"
                                                    name="supportEmail"
                                                    value={formData.supportEmail}
                                                    onChange={handleChange}
                                                    placeholder="support@vendor.com"
                                                />
                                                <Form.Text className="text-muted">
                                                    Technical support email
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Action Buttons */}
                                    <div className="d-flex flex-column flex-sm-row justify-content-end gap-2">
                                        <Button 
                                            variant="secondary"
                                            onClick={() => navigate('/vendors')}
                                            disabled={loading}
                                            className="order-2 order-sm-1"
                                        >
                                            Cancel
                                        </Button>
                                        <Button 
                                            variant="primary"
                                            type="submit"
                                            disabled={loading}
                                            className="order-1 order-sm-2"
                                        >
                                            {loading ? (
                                                <>
                                                    <Spinner size="sm" className="me-2" />
                                                    Saving...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    {isEditMode ? 'Update Vendor' : 'Add Vendor'}
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </Form>
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Password Change Modal */}
            <ChangePasswordModal
                show={showPasswordModal}
                onHide={() => setShowPasswordModal(false)}
                onPasswordChanged={() => setShowPasswordModal(false)}
                isForced={false}
            />
        </>
    );
};

export default VendorForm;
