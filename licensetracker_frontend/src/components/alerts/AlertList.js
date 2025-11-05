import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Badge, Spinner, Modal, ButtonGroup, OverlayTrigger, Tooltip } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import RoleBasedAccess from '../common/RoleBasedAccess';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import alertService from '../../services/alertService';

const AlertList = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [stats, setStats] = useState({
        total: 0,
        unacknowledged: 0,
        critical: 0,
        high: 0,
        medium: 0,
        low: 0
    });
    const [filters, setFilters] = useState({
        severity: '',
        alertType: '',
        region: '',
        acknowledged: 'false',
        days: '30' // NEW
    });

    const [showAcknowledgeModal, setShowAcknowledgeModal] = useState(false);
    const [selectedAlert, setSelectedAlert] = useState(null);
    const [generatingAlerts, setGeneratingAlerts] = useState(false);

    const navigate = useNavigate();

    const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
    const alertTypes = [
        'LICENSE_EXPIRING',
        'LICENSE_EXPIRED',
        'LICENSE_CAPACITY_WARNING',
        'LICENSE_CAPACITY_CRITICAL',
        'SOFTWARE_VERSION_OUTDATED',
        'SOFTWARE_VERSION_CRITICAL',
        'COMPLIANCE_VIOLATION'
    ];
    const regions = ['BANGALORE', 'CHENNAI', 'DELHI', 'MUMBAI', 'HYDERABAD', 'KOLKATA'];


    useEffect(() => {
        fetchAlerts();
        fetchStatistics();
    }, [currentPage, pageSize]);

    useEffect(() => {
        // Refresh alerts when filters change
        if (currentPage === 0) {
            fetchAlerts();
        } else {
            setCurrentPage(0);
        }
    }, [filters]);

    const fetchAlerts = async () => {
        setLoading(true);
        try {
            let data;
            if (filters.days) {
                data = await alertService.getAlertsExpiringInDays(parseInt(filters.days));
            } else {
                const response = await alertService.getAllAlerts(currentPage, pageSize);
                data = response.content;
            }
            setAlerts(data);
        } catch (error) {
            console.error('Error fetching alerts:', error);
            toast.error('Failed to fetch alerts');
        } finally {
            setLoading(false);
        }
    };
    const fetchStatistics = async () => {
        try {
            const data = await alertService.getStatistics();
            setStats(data);
        } catch (error) {
            console.error('Error fetching statistics:', error);
        }
    };

    const handleFilterChange = (e) => {
        setFilters({
            ...filters,
            [e.target.name]: e.target.value
        });
    };

    const resetFilters = () => {
        setFilters({
            severity: '',
            alertType: '',
            region: '',
            acknowledged: 'false'
        });
    };

    const handleAcknowledge = (alert) => {
        setSelectedAlert(alert);
        setShowAcknowledgeModal(true);
    };

    const confirmAcknowledge = async () => {
        const user = JSON.parse(localStorage.getItem('user'));

        toast.promise(
            alertService.acknowledgeAlert(selectedAlert.id, user?.username || 'SYSTEM'),
            {
                loading: 'Acknowledging alert...',
                success: () => {
                    setShowAcknowledgeModal(false);
                    setSelectedAlert(null);
                    fetchAlerts();
                    fetchStatistics();
                    return 'Alert acknowledged successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to acknowledge alert'
            }
        );
    };

    const handleAcknowledgeAll = async () => {
        if (!window.confirm('Are you sure you want to acknowledge all unacknowledged alerts?')) {
            return;
        }

        const user = JSON.parse(localStorage.getItem('user'));

        toast.promise(
            alertService.acknowledgeAllAlerts(user?.username || 'SYSTEM'),
            {
                loading: 'Acknowledging all alerts...',
                success: () => {
                    fetchAlerts();
                    fetchStatistics();
                    return 'All alerts acknowledged successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to acknowledge alerts'
            }
        );
    };

    const handleGenerateAlerts = async (type) => {
        setGeneratingAlerts(true);

        let promise;
        let message;

        switch (type) {
            case 'license-expiry':
                promise = alertService.generateLicenseExpiryAlerts();
                message = 'License expiry alerts generated!';
                break;
            case 'software-version':
                promise = alertService.generateSoftwareVersionAlerts();
                message = 'Software version alerts generated!';
                break;
            case 'capacity':
                promise = alertService.generateCapacityAlerts();
                message = 'License capacity alerts generated!';
                break;
            default:
                return;
        }

        toast.promise(promise, {
            loading: 'Generating alerts...',
            success: () => {
                setGeneratingAlerts(false);
                fetchAlerts();
                fetchStatistics();
                return message;
            },
            error: (err) => {
                setGeneratingAlerts(false);
                return err.response?.data?.message || 'Failed to generate alerts';
            }
        });
    };

    const getSeverityBadge = (severity) => {
        const colors = {
            CRITICAL: 'danger',
            HIGH: 'warning',
            MEDIUM: 'info',
            LOW: 'secondary'
        };
        const icons = {
            CRITICAL: '⚠️',
            HIGH: '🔴',
            MEDIUM: '🟡',
            LOW: '🔵'
        };
        return (
            <Badge bg={colors[severity] || 'secondary'}>
                {icons[severity]} {severity}
            </Badge>
        );
    };

    const getAlertTypeLabel = (type) => {
        return type.replace(/_/g, ' ');
    };

    const filteredAlerts = alerts.filter(alert => {
        const matchesSeverity = !filters.severity || alert.severity === filters.severity;
        const matchesType = !filters.alertType || alert.alertType === filters.alertType;
        const matchesRegion = !filters.region || alert.region === filters.region;
        const matchesAcknowledged = filters.acknowledged === '' ||
            alert.acknowledged.toString() === filters.acknowledged;

        return matchesSeverity && matchesType && matchesRegion && matchesAcknowledged;
    });

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
                                    <i className="bi bi-bell me-2"></i>
                                    Alerts & Notifications
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <ButtonGroup className="me-2">
                                    <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER']}>
                                        <OverlayTrigger
                                            placement="bottom"
                                            overlay={<Tooltip>Check License Expiry</Tooltip>}
                                        >
                                            <Button
                                                variant="outline-primary"
                                                size="sm"
                                                onClick={() => handleGenerateAlerts('license-expiry')}
                                                disabled={generatingAlerts}
                                            >
                                                <i className="bi bi-clock-history"></i>
                                            </Button>
                                        </OverlayTrigger>
                                    </RoleBasedAccess>
                                    <RoleBasedAccess allowedRoles={['ADMIN', 'OPERATIONS_MANAGER', 'NETWORK_ENGINEER']}>
                                        <OverlayTrigger
                                            placement="bottom"
                                            overlay={<Tooltip>Check Software Versions</Tooltip>}
                                        >
                                            <Button
                                                variant="outline-primary"
                                                size="sm"
                                                onClick={() => handleGenerateAlerts('software-version')}
                                                disabled={generatingAlerts}
                                            >
                                                <i className="bi bi-cpu"></i>
                                            </Button>
                                        </OverlayTrigger>
                                    </RoleBasedAccess>
                                    <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER']}>
                                        <OverlayTrigger
                                            placement="bottom"
                                            overlay={<Tooltip>Check License Capacity</Tooltip>}
                                        >
                                            <Button
                                                variant="outline-primary"
                                                size="sm"
                                                onClick={() => handleGenerateAlerts('capacity')}
                                                disabled={generatingAlerts}
                                            >
                                                <i className="bi bi-graph-up"></i>
                                            </Button>
                                        </OverlayTrigger>
                                    </RoleBasedAccess>
                                </ButtonGroup>
                                <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
                                    <Button
                                        variant="primary"
                                        size="sm"
                                        onClick={handleAcknowledgeAll}
                                        disabled={stats.unacknowledged === 0}
                                    >
                                        <i className="bi bi-check-all me-2"></i>
                                        Acknowledge All
                                    </Button>
                                </RoleBasedAccess>
                            </Col>
                        </Row>

                        {/* Statistics Cards */}
                        <Row className="mb-4">
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">Total</h6>
                                            <h3 className="mb-0">{stats.total}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">Unread</h6>
                                            <h3 className="mb-0 text-primary">{stats.unacknowledged}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0 border-start border-danger border-3">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">⚠️ Critical</h6>
                                            <h3 className="mb-0 text-danger">{stats.critical}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0 border-start border-warning border-3">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">🔴 High</h6>
                                            <h3 className="mb-0 text-warning">{stats.high}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0 border-start border-info border-3">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">🟡 Medium</h6>
                                            <h3 className="mb-0 text-info">{stats.medium}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={2}>
                                <Card className="shadow-sm border-0 border-start border-secondary border-3">
                                    <Card.Body>
                                        <div className="text-center">
                                            <h6 className="text-muted mb-2">🔵 Low</h6>
                                            <h3 className="mb-0 text-secondary">{stats.low}</h3>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                        </Row>

                        {/* Filters */}
                        <Card className="shadow-sm border-0 mb-3">
                            <Card.Header className="bg-white border-bottom">
                                <h5 className="mb-0">
                                    <i className="bi bi-funnel me-2"></i>
                                    Filters
                                </h5>
                            </Card.Header>
                            <Card.Body>
                                <Row>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Status</Form.Label>
                                            <Form.Select
                                                name="acknowledged"
                                                value={filters.acknowledged}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All</option>
                                                <option value="false">Unacknowledged</option>
                                                <option value="true">Acknowledged</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Severity</Form.Label>
                                            <Form.Select
                                                name="severity"
                                                value={filters.severity}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All Severities</option>
                                                {severities.map(severity => (
                                                    <option key={severity} value={severity}>
                                                        {severity}
                                                    </option>
                                                ))}
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={2}>
                                        <Form.Group>
                                            <Form.Label>Expiring In</Form.Label>
                                            <Form.Select
                                                name="days"
                                                value={filters.days}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All</option>
                                                <option value="7">7 days</option>
                                                <option value="15">15 days</option>
                                                <option value="30">30 days</option>
                                                <option value="60">60 days</option>
                                                <option value="90">90 days</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>

                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Alert Type</Form.Label>
                                            <Form.Select
                                                name="alertType"
                                                value={filters.alertType}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All Types</option>
                                                {alertTypes.map(type => (
                                                    <option key={type} value={type}>
                                                        {getAlertTypeLabel(type)}
                                                    </option>
                                                ))}
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={2}>
                                        <Form.Group>
                                            <Form.Label>Region</Form.Label>
                                            <Form.Select
                                                name="region"
                                                value={filters.region}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All Regions</option>
                                                {regions.map(region => (
                                                    <option key={region} value={region}>
                                                        {region}
                                                    </option>
                                                ))}
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={3} className="d-flex align-items-end">
                                        <Button
                                            variant="outline-secondary"
                                            onClick={resetFilters}
                                            className="w-100"
                                        >
                                            Reset
                                        </Button>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>

                        {/* Alerts Table */}
                        <Card className="shadow-sm border-0">
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading alerts...</p>
                                    </div>
                                ) : (
                                    <>
                                        <div className="mb-3">
                                            <small className="text-muted">
                                                Showing {filteredAlerts.length} of {alerts.length} alerts
                                            </small>
                                        </div>
                                        <div className="table-responsive">
                                            <Table hover>
                                                <thead className="table-light">
                                                    <tr>
                                                        <th>Severity</th>
                                                        <th>Type</th>
                                                        <th>Message</th>
                                                        <th>Region</th>
                                                        <th>Generated</th>
                                                        <th>Status</th>
                                                        <th>Actions</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {filteredAlerts.length > 0 ? (
                                                        filteredAlerts.map((alert) => (
                                                            <tr
                                                                key={alert.id}
                                                                className={!alert.acknowledged ? 'table-warning bg-opacity-10' : ''}
                                                            >
                                                                <td>{getSeverityBadge(alert.severity)}</td>
                                                                <td>
                                                                    <small>{getAlertTypeLabel(alert.alertType)}</small>
                                                                </td>
                                                                <td>
                                                                    <div style={{ maxWidth: '400px' }}>
                                                                        {alert.message}
                                                                    </div>
                                                                </td>
                                                                <td>
                                                                    {alert.region ? (
                                                                        <Badge bg="info">{alert.region}</Badge>
                                                                    ) : (
                                                                        <span className="text-muted">-</span>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    {new Date(alert.generatedAt).toLocaleDateString()}
                                                                    <br />
                                                                    <small className="text-muted">
                                                                        {new Date(alert.generatedAt).toLocaleTimeString()}
                                                                    </small>
                                                                </td>
                                                                <td>
                                                                    {alert.acknowledged ? (
                                                                        <div>
                                                                            <Badge bg="success">
                                                                                <i className="bi bi-check-circle me-1"></i>
                                                                                Acknowledged
                                                                            </Badge>
                                                                            <br />
                                                                            <small className="text-muted">
                                                                                by {alert.acknowledgedBy}
                                                                            </small>
                                                                        </div>
                                                                    ) : (
                                                                        <Badge bg="warning">
                                                                            <i className="bi bi-exclamation-circle me-1"></i>
                                                                            Unread
                                                                        </Badge>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    {!alert.acknowledged && (
                                                                        <Button
                                                                            variant="outline-success"
                                                                            size="sm"
                                                                            onClick={() => handleAcknowledge(alert)}
                                                                        >
                                                                            <i className="bi bi-check-circle me-1"></i>
                                                                            Acknowledge
                                                                        </Button>
                                                                    )}
                                                                </td>
                                                            </tr>
                                                        ))
                                                    ) : (
                                                        <tr>
                                                            <td colSpan="7" className="text-center py-4">
                                                                <i className="bi bi-bell-slash text-muted" style={{ fontSize: '3rem' }}></i>
                                                                <p className="text-muted mt-2">No alerts found</p>
                                                            </td>
                                                        </tr>
                                                    )}
                                                </tbody>
                                            </Table>
                                        </div>
                                    </>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Acknowledge Modal */}
            <Modal show={showAcknowledgeModal} onHide={() => setShowAcknowledgeModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Acknowledge Alert</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {selectedAlert && (
                        <div>
                            <div className="mb-3">
                                {getSeverityBadge(selectedAlert.severity)}
                            </div>
                            <p><strong>Type:</strong> {getAlertTypeLabel(selectedAlert.alertType)}</p>
                            <p><strong>Message:</strong> {selectedAlert.message}</p>
                            <p className="text-muted mb-0">
                                Are you sure you want to acknowledge this alert?
                            </p>
                        </div>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowAcknowledgeModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="success" onClick={confirmAcknowledge}>
                        <i className="bi bi-check-circle me-2"></i>
                        Acknowledge
                    </Button>
                </Modal.Footer>
            </Modal>

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

export default AlertList;