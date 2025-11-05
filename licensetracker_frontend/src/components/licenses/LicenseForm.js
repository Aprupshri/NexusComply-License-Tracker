// src/components/licenses/LicenseForm.jsx
import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import licenseService from '../../services/licenseService';
import vendorService from '../../services/vendorService';

const LicenseForm = () => {
    const [formData, setFormData] = useState({
        licenseKey: '',
        softwareName: '',
        licenseType: 'PER_DEVICE',
        maxUsage: '',
        validFrom: '',
        validTo: '',
        region: 'BANGALORE',
        poNumber: '',
        cost: '',
        vendorId: '',
        description: ''
    });

    const [vendors, setVendors] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingVendors, setLoadingVendors] = useState(false);
    
    const navigate = useNavigate();
    const { id } = useParams();
    const isEditMode = !!id;

    useEffect(() => {
        fetchVendors();
        if (isEditMode) {
            fetchLicense();
        }
    }, [id]);

    /**
     * Fetch vendors - FIXED to handle paginated response
     */
    const fetchVendors = async () => {
        setLoadingVendors(true);
        try {
            const response = await vendorService.getAllVendors();
            
            // Check if response has content (paginated) or is array
            if (response.content && Array.isArray(response.content)) {
                setVendors(response.content);
            } else if (Array.isArray(response)) {
                setVendors(response);
            } else {
                console.error('Unexpected vendor data structure:', response);
                setVendors([]);
                toast.error('Failed to load vendors');
            }
        } catch (error) {
            console.error('Error fetching vendors:', error);
            toast.error('Failed to fetch vendors');
            setVendors([]); // Set empty array on error
        } finally {
            setLoadingVendors(false);
        }
    };

    const fetchLicense = async () => {
        setLoading(true);
        try {
            const license = await licenseService.getLicenseById(id);
            setFormData({
                licenseKey: license.licenseKey,
                softwareName: license.softwareName,
                licenseType: license.licenseType,
                maxUsage: license.maxUsage,
                validFrom: license.validFrom,
                validTo: license.validTo,
                region: license.region,
                poNumber: license.poNumber || '',
                cost: license.cost || '',
                vendorId: license.vendorId || '',
                description: license.description || ''
            });
        } catch (error) {
            toast.error('Failed to fetch license details');
            console.error('Error:', error);
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

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Validation
        if (new Date(formData.validFrom) >= new Date(formData.validTo)) {
            toast.error('Valid From date must be before Valid To date');
            return;
        }

        if (!formData.maxUsage || formData.maxUsage < 1) {
            toast.error('Max Usage must be at least 1');
            return;
        }

        setLoading(true);

        try {
            const payload = {
                ...formData,
                maxUsage: parseInt(formData.maxUsage),
                cost: formData.cost ? parseFloat(formData.cost) : null,
                vendorId: formData.vendorId ? parseInt(formData.vendorId) : null
            };

            if (isEditMode) {
                await licenseService.updateLicense(id, payload);
                toast.success('License updated successfully!');
            } else {
                await licenseService.createLicense(payload);
                toast.success('License created successfully!');
            }
            
            setTimeout(() => {
                navigate('/licenses');
            }, 1500);
        } catch (error) {
            const errorMessage = error.response?.data?.message || 'Failed to save license';
            toast.error(errorMessage);
            console.error('Error:', error);
        } finally {
            setLoading(false);
        }
    };

    const licenseTypes = [
        { value: 'PER_DEVICE', label: 'Per Device', description: 'License assigned to specific devices' },
        { value: 'PER_USER', label: 'Per User', description: 'License assigned to specific users' },
        { value: 'ENTERPRISE', label: 'Enterprise', description: 'Organization-wide license' },
        { value: 'REGION', label: 'Region', description: 'Region-based license' }
    ];

    const regions = ['BANGALORE', 'CHENNAI', 'DELHI', 'MUMBAI', 'HYDERABAD', 'KOLKATA'];

    if (loading && isEditMode) {
        return (
            <>
                <NavigationBar />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-4">
                        <div className="text-center py-5">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-3">Loading license details...</p>
                        </div>
                    </div>
                </div>
            </>
        );
    }

    return (
        <>
            <NavigationBar />
            <div className="d-flex">
                <Sidebar />
                <div className="flex-grow-1 p-4" style={{ backgroundColor: '#f8f9fa' }}>
                    <Container fluid>
                        {/* Header */}
                        <Row className="mb-4">
                            <Col>
                                <h2 className="fw-bold">
                                    <i className="bi bi-key me-2"></i>
                                    {isEditMode ? 'Edit License' : 'Add New License'}
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <Button 
                                    variant="outline-secondary"
                                    onClick={() => navigate('/licenses')}
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                            </Col>
                        </Row>

                        <Card className="shadow-sm border-0">
                            <Card.Body className="p-4">
                                <Form onSubmit={handleSubmit}>
                                    {/* License Key & Software Name */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    License Key <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="licenseKey"
                                                    value={formData.licenseKey}
                                                    onChange={handleChange}
                                                    placeholder="e.g., CISCO-IOS-XR-2024-001"
                                                    required
                                                    disabled={isEditMode}
                                                />
                                                <Form.Text className="text-muted">
                                                    <i className="bi bi-info-circle me-1"></i>
                                                    Unique license key (cannot be changed after creation)
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Software Name <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="softwareName"
                                                    value={formData.softwareName}
                                                    onChange={handleChange}
                                                    placeholder="e.g., Cisco IOS XR Advanced"
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* License Type & Max Usage */}
                                    <Row>
                                        <Col md={6} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    License Type <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Select
                                                    name="licenseType"
                                                    value={formData.licenseType}
                                                    onChange={handleChange}
                                                    required
                                                >
                                                    {licenseTypes.map(type => (
                                                        <option key={type.value} value={type.value}>
                                                            {type.label}
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                                <Form.Text className="text-muted">
                                                    {licenseTypes.find(t => t.value === formData.licenseType)?.description}
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Max Usage <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="number"
                                                    name="maxUsage"
                                                    value={formData.maxUsage}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 10"
                                                    min="1"
                                                    required
                                                />
                                                <Form.Text className="text-muted">
                                                    Maximum concurrent usages
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={12} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Region <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Select
                                                    name="region"
                                                    value={formData.region}
                                                    onChange={handleChange}
                                                    required
                                                >
                                                    {regions.map(region => (
                                                        <option key={region} value={region}>{region}</option>
                                                    ))}
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Validity Period */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Valid From <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="date"
                                                    name="validFrom"
                                                    value={formData.validFrom}
                                                    onChange={handleChange}
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Valid To <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="date"
                                                    name="validTo"
                                                    value={formData.validTo}
                                                    onChange={handleChange}
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Vendor, PO Number, Cost */}
                                    <Row>
                                        <Col md={12} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">Vendor</Form.Label>
                                                <Form.Select
                                                    name="vendorId"
                                                    value={formData.vendorId}
                                                    onChange={handleChange}
                                                    disabled={loadingVendors}
                                                >
                                                    <option value="">-- Select Vendor (Optional) --</option>
                                                    {vendors.map(vendor => (
                                                        <option key={vendor.id} value={vendor.id}>
                                                            {vendor.vendorName}
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                                {loadingVendors && (
                                                    <Form.Text className="text-muted">
                                                        <Spinner size="sm" className="me-2" animation="border" />
                                                        Loading vendors...
                                                    </Form.Text>
                                                )}
                                                {!loadingVendors && vendors.length === 0 && (
                                                    <Form.Text className="text-warning">
                                                        <i className="bi bi-exclamation-triangle me-1"></i>
                                                        No vendors available
                                                    </Form.Text>
                                                )}
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">PO Number</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="poNumber"
                                                    value={formData.poNumber}
                                                    onChange={handleChange}
                                                    placeholder="e.g., PO-2024-001"
                                                />
                                                <Form.Text className="text-muted">
                                                    Purchase order number
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} lg={4} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">Cost (₹)</Form.Label>
                                                <Form.Control
                                                    type="number"
                                                    name="cost"
                                                    value={formData.cost}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 50000"
                                                    step="0.01"
                                                    min="0"
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Description Field */}
                                    <Row>
                                        <Col md={12} className="mb-4">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Description / Notes
                                                    <span className="text-muted ms-2">(Optional)</span>
                                                </Form.Label>
                                                <Form.Control
                                                    as="textarea"
                                                    rows={4}
                                                    name="description"
                                                    value={formData.description}
                                                    onChange={handleChange}
                                                    placeholder="Add any additional notes about this license..."
                                                    maxLength={1000}
                                                />
                                                <Form.Text className="text-muted">
                                                    {formData.description.length}/1000 characters
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Action Buttons */}
                                    <div className="d-flex flex-column flex-sm-row justify-content-end gap-2 pt-3 border-top">
                                        <Button 
                                            variant="secondary"
                                            onClick={() => navigate('/licenses')}
                                            disabled={loading}
                                            className="order-2 order-sm-1"
                                        >
                                            <i className="bi bi-x-circle me-2"></i>
                                            Cancel
                                        </Button>
                                        <Button 
                                            variant="primary"
                                            type="submit"
                                            disabled={loading || loadingVendors}
                                            className="order-1 order-sm-2"
                                        >
                                            {loading ? (
                                                <>
                                                    <Spinner size="sm" className="me-2" animation="border" />
                                                    Saving...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    {isEditMode ? 'Update License' : 'Create License'}
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </Form>
                            </Card.Body>
                        </Card>

                        {/* Info Card */}
                        <Card className="shadow-sm border-0 mt-3">
                            <Card.Body className="bg-light">
                                <h6 className="mb-2">
                                    <i className="bi bi-info-circle me-2"></i>
                                    Important Information
                                </h6>
                                <ul className="mb-0 small">
                                    <li>License Key cannot be changed after creation</li>
                                    <li>Valid From date must be before Valid To date</li>
                                    <li>Max Usage defines how many devices/users can use this license simultaneously</li>
                                    <li>Vendor selection is optional but recommended for better tracking</li>
                                    <li>Use the description field to add important notes or special conditions</li>
                                    <li>Licenses will automatically show alerts when approaching expiry</li>
                                </ul>
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default LicenseForm;
