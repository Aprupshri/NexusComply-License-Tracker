// src/components/licenses/LicenseDetail.jsx
import React, { useState, useEffect } from 'react';
import { 
    Container, Row, Col, Card, Table, Button, Badge, Spinner, 
    ProgressBar, Alert, Modal 
} from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import licenseService from '../../services/licenseService';
import assignmentService from '../../services/assignmentService';

const LicenseDetail = () => {
    const [license, setLicense] = useState(null);
    const [assignments, setAssignments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    
    const navigate = useNavigate();
    const { id } = useParams();

    useEffect(() => {
        fetchLicenseDetails();
        fetchAssignments();
    }, [id]);

    const fetchLicenseDetails = async () => {
        setLoading(true);
        try {
            const data = await licenseService.getLicenseById(id);
            setLicense(data);
        } catch (error) {
            console.error('Error fetching license:', error);
            toast.error('Failed to fetch license details');
        } finally {
            setLoading(false);
        }
    };

    const fetchAssignments = async () => {
        try {
            const data = await assignmentService.getActiveAssignmentsByLicense(id);
            setAssignments(data);
        } catch (error) {
            console.error('Error fetching assignments:', error);
            toast.error('Failed to fetch assignments');
        }
    };

    const handleUnassign = async () => {
        if (!selectedAssignment) return;

        try {
            const user = JSON.parse(localStorage.getItem('user'));
            await assignmentService.revokeAssignment(
                selectedAssignment.id,
                user?.username || 'UNKNOWN',
                'Unassigned from license detail view'
            );
            
            toast.success('License unassigned successfully!');
            setShowDeleteModal(false);
            
            await fetchAssignments();
            await fetchLicenseDetails();
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to unassign license');
        }
    };

    const getUsagePercentage = () => {
        if (!license) return 0;
        return (license.currentUsage / license.maxUsage) * 100;
    };

    const getUsageVariant = () => {
        const percentage = getUsagePercentage();
        if (percentage >= 90) return 'danger';
        if (percentage >= 70) return 'warning';
        return 'success';
    };

    const isExpired = (validTo) => {
        return new Date(validTo) < new Date();
    };

    const isExpiringSoon = (validTo) => {
        const today = new Date();
        const expiryDate = new Date(validTo);
        const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
        return daysUntilExpiry <= 30 && daysUntilExpiry > 0;
    };

    const getDaysUntilExpiry = (validTo) => {
        const today = new Date();
        const expiryDate = new Date(validTo);
        return Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
    };

    const getLicenseTypeBadge = (type) => {
        const colors = {
            PER_DEVICE: 'primary',
            PER_USER: 'info',
            ENTERPRISE: 'success',
            REGION: 'warning'
        };
        const labels = {
            PER_DEVICE: 'Per Device',
            PER_USER: 'Per User',
            ENTERPRISE: 'Enterprise',
            REGION: 'Region'
        };
        return <Badge bg={colors[type] || 'secondary'}>{labels[type] || type}</Badge>;
    };

    if (loading) {
        return (
            <>
                <NavigationBar />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-2 p-md-4">
                        <div className="text-center py-5">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-3">Loading license details...</p>
                        </div>
                    </div>
                </div>
            </>
        );
    }

    if (!license) {
        return (
            <>
                <NavigationBar />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-2 p-md-4">
                        <Alert variant="danger">
                            <i className="bi bi-exclamation-circle me-2"></i>
                            License not found
                        </Alert>
                        <Button 
                            onClick={() => navigate('/licenses')}
                            variant="primary"
                        >
                            <i className="bi bi-arrow-left me-2"></i>
                            Back to Licenses
                        </Button>
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
                <div className="flex-grow-1 p-2 p-md-4" style={{ backgroundColor: '#f8f9fa', minHeight: '100vh' }}>
                    <Container fluid className="px-2 px-md-0">
                        {/* Header */}
                        <Row className="mb-3 mb-md-4">
                            <Col xs={12} className="mb-2 mb-md-0">
                                <h2 className="fw-bold mb-3 mb-md-0">
                                    <i className="bi bi-key me-2"></i>
                                    License Details
                                </h2>
                            </Col>
                            <Col xs={12} md="auto" className="d-flex flex-column flex-md-row gap-2">
                                <Button 
                                    variant="outline-secondary"
                                    size="sm"
                                    onClick={() => navigate('/licenses')}
                                    className="w-100 w-md-auto"
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back
                                </Button>
                                <Button 
                                    variant="outline-primary"
                                    size="sm"
                                    onClick={() => navigate(`/licenses/edit/${id}`)}
                                    className="w-100 w-md-auto"
                                >
                                    <i className="bi bi-pencil me-2"></i>
                                    Edit
                                </Button>
                            </Col>
                        </Row>

                        {/* Status Alerts */}
                        {isExpired(license.validTo) && (
                            <Alert variant="danger" className="mb-3">
                                <i className="bi bi-exclamation-triangle-fill me-2"></i>
                                <strong>License Expired:</strong> 
                                <span className="ms-2 d-block d-md-inline">
                                    This license expired on {new Date(license.validTo).toLocaleDateString()}
                                </span>
                            </Alert>
                        )}
                        {isExpiringSoon(license.validTo) && !isExpired(license.validTo) && (
                            <Alert variant="warning" className="mb-3">
                                <i className="bi bi-clock-history me-2"></i>
                                <strong>Expiring Soon:</strong>
                                <span className="ms-2 d-block d-md-inline">
                                    This license will expire in {getDaysUntilExpiry(license.validTo)} days
                                </span>
                            </Alert>
                        )}

                        {/* License Information Cards */}
                        <Row className="mb-3 mb-md-4 g-2 g-md-3">
                            {/* License Key */}
                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-key me-2"></i>
                                            License Key
                                        </h6>
                                        <p className="fw-bold mb-0 text-break">
                                            {license.licenseKey}
                                        </p>
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* Software Name */}
                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-box me-2"></i>
                                            Software Name
                                        </h6>
                                        <p className="fw-bold mb-0">
                                            {license.softwareName}
                                        </p>
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* License Type */}
                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-tag me-2"></i>
                                            License Type
                                        </h6>
                                        <div>
                                            {getLicenseTypeBadge(license.licenseType)}
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* Validity Period */}
                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-calendar me-2"></i>
                                            Valid From
                                        </h6>
                                        <p className="fw-bold mb-0">
                                            {new Date(license.validFrom).toLocaleDateString()}
                                        </p>
                                    </Card.Body>
                                </Card>
                            </Col>

                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-calendar me-2"></i>
                                            Valid To
                                        </h6>
                                        <p className="fw-bold mb-0">
                                            {new Date(license.validTo).toLocaleDateString()}
                                        </p>
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* Region */}
                            <Col xs={12} sm={6} lg={4}>
                                <Card className="h-100 shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="text-muted mb-2">
                                            <i className="bi bi-geo-alt-fill me-2"></i>
                                            Region
                                        </h6>
                                        <p className="fw-bold mb-0">
                                            {license.region}
                                        </p>
                                    </Card.Body>
                                </Card>
                            </Col>
                        </Row>

                        {/* Usage and Cost Information */}
                        <Row className="mb-3 mb-md-4 g-2 g-md-3">
                            {/* Usage */}
                            <Col xs={12} md={6} lg={4}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <h6 className="mb-3">
                                            <i className="bi bi-graph-up me-2"></i>
                                            Usage
                                        </h6>
                                        <p className="mb-2 d-flex justify-content-between">
                                            <span>{license.currentUsage} / {license.maxUsage}</span>
                                            <span className="text-muted">
                                                {getUsagePercentage().toFixed(1)}%
                                            </span>
                                        </p>
                                        <ProgressBar 
                                            now={getUsagePercentage()} 
                                            variant={getUsageVariant()}
                                        />
                                    </Card.Body>
                                </Card>
                            </Col>

                            {/* Cost */}
                            {license.cost && (
                                <Col xs={12} sm={6} md="auto" lg={4}>
                                    <Card className="shadow-sm border-0">
                                        <Card.Body>
                                            <h6 className="text-muted mb-2">
                                                <i className="bi bi-currency-rupee me-2"></i>
                                                Cost
                                            </h6>
                                            <p className="fw-bold mb-0">
                                                ₹{license.cost.toLocaleString('en-IN')}
                                            </p>
                                        </Card.Body>
                                    </Card>
                                </Col>
                            )}

                            {/* PO Number */}
                            {license.poNumber && (
                                <Col xs={12} sm={6} md="auto" lg={4}>
                                    <Card className="shadow-sm border-0">
                                        <Card.Body>
                                            <h6 className="text-muted mb-2">
                                                <i className="bi bi-receipt me-2"></i>
                                                PO Number
                                            </h6>
                                            <p className="fw-bold mb-0 text-break">
                                                {license.poNumber}
                                            </p>
                                        </Card.Body>
                                    </Card>
                                </Col>
                            )}
                        </Row>

                        {/* Vendor Information */}
                        {license.vendorName && (
                            <Row className="mb-3 mb-md-4">
                                <Col xs={12}>
                                    <Card className="shadow-sm border-0">
                                        <Card.Header className="bg-white border-bottom">
                                            <h6 className="mb-0">
                                                <i className="bi bi-building me-2"></i>
                                                Vendor Information
                                            </h6>
                                        </Card.Header>
                                        <Card.Body>
                                            <Row className="g-2 g-md-3">
                                                <Col xs={12} sm={6}>
                                                    <div>
                                                        <p className="text-muted mb-1">Vendor Name</p>
                                                        <p className="fw-bold">{license.vendorName}</p>
                                                    </div>
                                                </Col>
                                                {license.vendorContactEmail && (
                                                    <Col xs={12} sm={6}>
                                                        <div>
                                                            <p className="text-muted mb-1">Contact Email</p>
                                                            <p className="fw-bold text-break">
                                                                {license.vendorContactEmail}
                                                            </p>
                                                        </div>
                                                    </Col>
                                                )}
                                                {license.vendorContactPhone && (
                                                    <Col xs={12} sm={6}>
                                                        <div>
                                                            <p className="text-muted mb-1">Contact Phone</p>
                                                            <p className="fw-bold">{license.vendorContactPhone}</p>
                                                        </div>
                                                    </Col>
                                                )}
                                            </Row>
                                        </Card.Body>
                                    </Card>
                                </Col>
                            </Row>
                        )}

                        {/* Description */}
                        {license.description && (
                            <Row className="mb-3 mb-md-4">
                                <Col xs={12}>
                                    <Card className="shadow-sm border-0">
                                        <Card.Header className="bg-white border-bottom">
                                            <h6 className="mb-0">
                                                <i className="bi bi-file-text me-2"></i>
                                                Description
                                            </h6>
                                        </Card.Header>
                                        <Card.Body>
                                            <p className="mb-0">{license.description}</p>
                                        </Card.Body>
                                    </Card>
                                </Col>
                            </Row>
                        )}

                        {/* Active Assignments */}
                        <Row className="mb-3 mb-md-4">
                            <Col xs={12}>
                                <Card className="shadow-sm border-0">
                                    <Card.Header className="bg-white border-bottom">
                                        <h6 className="mb-0">
                                            <i className="bi bi-link-45deg me-2"></i>
                                            Active Assignments ({assignments.length})
                                        </h6>
                                    </Card.Header>
                                    <Card.Body>
                                        {assignments.length === 0 ? (
                                            <p className="text-muted mb-0">No active assignments for this license</p>
                                        ) : (
                                            <div className="table-responsive">
                                                <Table hover size="sm" className="mb-0">
                                                    <thead className="table-light">
                                                        <tr>
                                                            <th>Device ID</th>
                                                            <th className="d-none d-md-table-cell">Device Type</th>
                                                            <th className="d-none d-md-table-cell">Location</th>
                                                            <th className="text-center">Action</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {assignments.map(assignment => (
                                                            <tr key={assignment.id}>
                                                                <td className="fw-bold text-break">
                                                                    {assignment.deviceIdName}
                                                                </td>
                                                                <td className="d-none d-md-table-cell">
                                                                    <Badge bg="secondary">
                                                                        {assignment.deviceType}
                                                                    </Badge>
                                                                </td>
                                                                <td className="d-none d-md-table-cell">
                                                                    {assignment.deviceLocation}
                                                                </td>
                                                                <td className="text-center">
                                                                    <Button
                                                                        variant="outline-danger"
                                                                        size="sm"
                                                                        onClick={() => {
                                                                            setSelectedAssignment(assignment);
                                                                            setShowDeleteModal(true);
                                                                        }}
                                                                    >
                                                                        <i className="bi bi-trash"></i>
                                                                    </Button>
                                                                </td>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </Table>
                                            </div>
                                        )}
                                    </Card.Body>
                                </Card>
                            </Col>
                        </Row>
                    </Container>
                </div>
            </div>

            {/* Delete Confirmation Modal */}
            <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>
                        <i className="bi bi-exclamation-triangle me-2 text-warning"></i>
                        Confirm Unassignment
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Are you sure you want to unassign this license from <strong>{selectedAssignment?.deviceIdName}</strong>?</p>
                    <p className="text-muted mb-0">This action cannot be undone.</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="danger" onClick={handleUnassign}>
                        <i className="bi bi-trash me-2"></i>
                        Unassign
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
};

export default LicenseDetail;
