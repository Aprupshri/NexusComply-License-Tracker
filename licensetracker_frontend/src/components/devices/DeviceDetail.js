import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Badge, Spinner, Modal, Form, Alert, ProgressBar } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import RoleBasedAccess from '../common/RoleBasedAccess';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import deviceService from '../../services/deviceService';
import licenseService from '../../services/licenseService';
import assignmentService from '../../services/assignmentService';
import softwareVersionService from '../../services/softwareVersionService';

const DeviceDetail = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [device, setDevice] = useState(null);
    const [assignments, setAssignments] = useState([]);
    const [softwareVersions, setSoftwareVersions] = useState([]);
    const [availableLicenses, setAvailableLicenses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [showRevokeModal, setShowRevokeModal] = useState(false);
    const [showSoftwareModal, setShowSoftwareModal] = useState(false);
    const [selectedLicenseId, setSelectedLicenseId] = useState('');
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [selectedSoftwareVersion, setSelectedSoftwareVersion] = useState(null);
    const [revocationReason, setRevocationReason] = useState('');
    const [softwareFormData, setSoftwareFormData] = useState({
        softwareName: '',
        currentVersion: '',
        latestVersion: '',
        notes: '',
        updateUrl: '',
        releaseDate: ''
    });

    const navigate = useNavigate();
    const { id } = useParams();

    useEffect(() => {
        if (id) {
            loadAllData();
        }
    }, [id]);

    const loadAllData = async () => {
        setLoading(true);
        try {
            await Promise.all([
                fetchDeviceDetails(),
                fetchAssignments(),
                fetchAvailableLicenses(),
                fetchSoftwareVersions()
            ]);
        } catch (error) {
            console.error('Error loading data:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchDeviceDetails = async () => {
        try {
            const data = await deviceService.getDeviceById(id);
            setDevice(data);
        } catch (error) {
            console.error('Error fetching device details:', error);
            toast.error('Failed to fetch device details');
        }
    };

    const fetchAssignments = async () => {
        try {
            const data = await assignmentService.getActiveAssignmentsByDevice(id);
            setAssignments(data);
        } catch (error) {
            console.error('Error fetching assignments:', error);
        }
    };

    const fetchAvailableLicenses = async () => {
        try {
            const response = await licenseService.getAllLicenses(0, 100);
            const activeLicenses = response.content.filter(l => l.active);
            setAvailableLicenses(activeLicenses);
        } catch (error) {
            console.error('Error fetching licenses:', error);
        }
    };

    const fetchSoftwareVersions = async () => {
        try {
            const data = await softwareVersionService.getSoftwareVersionsByDevice(id);
            setSoftwareVersions(data);
        } catch (error) {
            console.error('Error fetching software versions:', error);
        }
    };

    const handleAssignLicense = async () => {
        if (!selectedLicenseId) {
            toast.error('Please select a license');
            return;
        }

        const user = JSON.parse(localStorage.getItem('user'));

        toast.promise(
            assignmentService.assignLicense(
                parseInt(id),
                parseInt(selectedLicenseId),
                user?.username || 'UNKNOWN'
            ),
            {
                loading: 'Assigning license...',
                success: () => {
                    setShowAssignModal(false);
                    setSelectedLicenseId('');
                    fetchAssignments();
                    return 'License assigned successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to assign license'
            }
        );
    };

    const handleUnassign = (assignment) => {
        setSelectedAssignment(assignment);
        setShowRevokeModal(true);
    };

    const confirmRevoke = async () => {
        if (!revocationReason.trim()) {
            toast.error('Please provide a revocation reason');
            return;
        }

        const user = JSON.parse(localStorage.getItem('user'));

        toast.promise(
            assignmentService.revokeAssignment(
                selectedAssignment.id,
                user?.username || 'UNKNOWN',
                revocationReason
            ),
            {
                loading: 'Revoking license...',
                success: () => {
                    setShowRevokeModal(false);
                    setRevocationReason('');
                    setSelectedAssignment(null);
                    fetchAssignments();
                    return 'License unassigned successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to unassign license'
            }
        );
    };

    const handleAddSoftwareVersion = () => {
        setSelectedSoftwareVersion(null);
        setSoftwareFormData({
            softwareName: device.softwareName || '',
            currentVersion: device.softwareVersion || '',
            latestVersion: '',
            notes: '',
            updateUrl: '',
            releaseDate: ''
        });
        setShowSoftwareModal(true);
    };

    const handleEditSoftwareVersion = (version) => {
        setSelectedSoftwareVersion(version);
        setSoftwareFormData({
            softwareName: version.softwareName,
            currentVersion: version.currentVersion,
            latestVersion: version.latestVersion || '',
            notes: version.notes || '',
            updateUrl: version.updateUrl || '',
            releaseDate: version.releaseDate || ''
        });
        setShowSoftwareModal(true);
    };

    const handleSoftwareFormChange = (e) => {
        setSoftwareFormData({
            ...softwareFormData,
            [e.target.name]: e.target.value
        });
    };

    const handleSaveSoftwareVersion = async () => {
        if (!softwareFormData.softwareName || !softwareFormData.currentVersion) {
            toast.error('Software name and current version are required');
            return;
        }

        const payload = {
            deviceId: parseInt(id),
            ...softwareFormData
        };

        const promise = selectedSoftwareVersion
            ? softwareVersionService.updateSoftwareVersion(selectedSoftwareVersion.id, payload)
            : softwareVersionService.createSoftwareVersion(payload);

        toast.promise(promise, {
            loading: selectedSoftwareVersion ? 'Updating...' : 'Creating...',
            success: () => {
                setShowSoftwareModal(false);
                fetchSoftwareVersions();
                return selectedSoftwareVersion
                    ? 'Software version updated successfully!'
                    : 'Software version created successfully!';
            },
            error: (err) => err.response?.data?.message || 'Failed to save software version'
        });
    };

    const handleCheckForUpdates = async (versionId) => {
        toast.promise(
            softwareVersionService.checkForUpdates(versionId),
            {
                loading: 'Checking for updates...',
                success: () => {
                    fetchSoftwareVersions();
                    return 'Update check completed';
                },
                error: 'Failed to check for updates'
            }
        );
    };

    const handleDeleteSoftwareVersion = async (versionId) => {
        if (window.confirm('Are you sure you want to delete this software version entry?')) {
            toast.promise(
                softwareVersionService.deleteSoftwareVersion(versionId),
                {
                    loading: 'Deleting...',
                    success: () => {
                        fetchSoftwareVersions();
                        return 'Software version deleted successfully!';
                    },
                    error: 'Failed to delete software version'
                }
            );
        }
    };

    const getLifecycleBadge = (lifecycle) => {
        const colors = {
            ACTIVE: 'success',
            MAINTENANCE: 'warning',
            OBSOLETE: 'secondary',
            DECOMMISSIONED: 'danger'
        };
        return <Badge bg={colors[lifecycle] || 'secondary'}>{lifecycle}</Badge>;
    };

    const getVersionStatusBadge = (status) => {
        const colors = {
            UP_TO_DATE: 'success',
            OUTDATED: 'warning',
            CRITICAL: 'danger',
            UNKNOWN: 'secondary'
        };
        const icons = {
            UP_TO_DATE: 'check-circle',
            OUTDATED: 'exclamation-triangle',
            CRITICAL: 'x-circle',
            UNKNOWN: 'question-circle'
        };
        return (
            <Badge bg={colors[status] || 'secondary'}>
                <i className={`bi bi-${icons[status]} me-1`}></i>
                {status.replace(/_/g, ' ')}
            </Badge>
        );
    };

    if (loading) {
        return (
            <>
                <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-4">
                        <div className="text-center py-5">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-3">Loading device details...</p>
                        </div>
                    </div>
                </div>
            </>
        );
    }

    if (!device) {
        return (
            <>
                <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-4">
                        <Alert variant="danger">Device not found</Alert>
                        <Button onClick={() => navigate('/devices')}>Back to Devices</Button>
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
                                    <i className="bi bi-hdd-network me-2"></i>
                                    Device Details
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <Button
                                    variant="outline-secondary"
                                    onClick={() => navigate('/devices')}
                                    className="me-2"
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                                <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
                                    <Button
                                        variant="outline-primary"
                                        onClick={() => navigate(`/devices/edit/${id}`)}
                                    >
                                        <i className="bi bi-pencil me-2"></i>
                                        Edit Device
                                    </Button>
                                </RoleBasedAccess>
                            </Col>
                        </Row>

                        {/* Device Information Card */}
                        <Card className="shadow-sm border-0 mb-4">
                            <Card.Header className="bg-primary text-white">
                                <h5 className="mb-0">
                                    <i className="bi bi-info-circle me-2"></i>
                                    Device Information
                                </h5>
                            </Card.Header>
                            <Card.Body>
                                <Row>
                                    <Col md={6}>
                                        <div className="mb-3">
                                            <strong>Device ID:</strong>
                                            <br />
                                            <span className="fs-5">{device.deviceId}</span>
                                        </div>
                                        <div className="mb-3">
                                            <strong>Type:</strong>
                                            <br />
                                            <Badge bg="primary" className="fs-6">{device.deviceType}</Badge>
                                        </div>
                                        <div className="mb-3">
                                            <strong>Model:</strong>
                                            <br />
                                            {device.model}
                                        </div>
                                        <div className="mb-3">
                                            <strong>IP Address:</strong>
                                            <br />
                                            <code className="fs-6">{device.ipAddress}</code>
                                        </div>
                                    </Col>
                                    <Col md={6}>
                                        <div className="mb-3">
                                            <strong>Location:</strong>
                                            <br />
                                            {device.location}
                                        </div>
                                        <div className="mb-3">
                                            <strong>Region:</strong>
                                            <br />
                                            <Badge bg="info" className="fs-6">{device.region}</Badge>
                                        </div>
                                        <div className="mb-3">
                                            <strong>Status:</strong>
                                            <br />
                                            {getLifecycleBadge(device.lifecycle)}
                                        </div>
                                        <div className="mb-3">
                                            <strong>Software:</strong>
                                            <br />
                                            {device.softwareName} {device.softwareVersion}
                                        </div>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>

                        {/* Software Versions Card */}
                        <Card className="shadow-sm border-0 mb-4">
                            <Card.Header className="bg-white border-bottom">
                                <Row>
                                    <Col>
                                        <h5 className="mb-0">
                                            <i className="bi bi-cpu me-2"></i>
                                            Software Versions ({softwareVersions.length})
                                        </h5>
                                    </Col>
                                    <Col xs="auto" className="text-end">
                                        <Button
                                            variant="primary"
                                            size="sm"
                                            onClick={handleAddSoftwareVersion}
                                        >
                                            <i className="bi bi-plus-circle me-2"></i>
                                            Add Version
                                        </Button>
                                    </Col>
                                </Row>
                            </Card.Header>
                            <Card.Body>
                                {softwareVersions.length > 0 ? (
                                    <Table hover responsive>
                                        <thead className="table-light">
                                            <tr>
                                                <th>Software Name</th>
                                                <th>Current Version</th>
                                                <th>Latest Version</th>
                                                <th>Status</th>
                                                <th>Last Checked</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {softwareVersions.map((version) => (
                                                <tr key={version.id}>
                                                    <td>
                                                        <strong>{version.softwareName}</strong>
                                                        {version.notes && (
                                                            <div>
                                                                <small className="text-muted">{version.notes}</small>
                                                            </div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <Badge bg="secondary">{version.currentVersion}</Badge>
                                                    </td>
                                                    <td>
                                                        {version.latestVersion ? (
                                                            <Badge bg="info">{version.latestVersion}</Badge>
                                                        ) : (
                                                            <span className="text-muted">-</span>
                                                        )}
                                                    </td>
                                                    <td>
                                                        {getVersionStatusBadge(version.status)}
                                                        {version.updateRecommended && (
                                                            <div className="mt-1">
                                                                <small className="text-warning">
                                                                    <i className="bi bi-exclamation-triangle me-1"></i>
                                                                    {version.updateMessage}
                                                                </small>
                                                            </div>
                                                        )}
                                                    </td>
                                                    <td>
                                                        {version.lastChecked ? (
                                                            <small>{new Date(version.lastChecked).toLocaleDateString()}</small>
                                                        ) : (
                                                            <span className="text-muted">-</span>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <Button
                                                            variant="outline-success"
                                                            size="sm"
                                                            className="me-1"
                                                            onClick={() => handleCheckForUpdates(version.id)}
                                                            title="Check for Updates"
                                                        >
                                                            <i className="bi bi-arrow-clockwise"></i>
                                                        </Button>
                                                        <Button
                                                            variant="outline-primary"
                                                            size="sm"
                                                            className="me-1"
                                                            onClick={() => handleEditSoftwareVersion(version)}
                                                            title="Edit"
                                                        >
                                                            <i className="bi bi-pencil"></i>
                                                        </Button>
                                                        {version.updateUrl && (
                                                            <Button
                                                                variant="outline-info"
                                                                size="sm"
                                                                className="me-1"
                                                                href={version.updateUrl}
                                                                target="_blank"
                                                                title="Download Update"
                                                            >
                                                                <i className="bi bi-download"></i>
                                                            </Button>
                                                        )}
                                                        <Button
                                                            variant="outline-danger"
                                                            size="sm"
                                                            onClick={() => handleDeleteSoftwareVersion(version.id)}
                                                            title="Delete"
                                                        >
                                                            <i className="bi bi-trash"></i>
                                                        </Button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </Table>
                                ) : (
                                    <div className="text-center py-4">
                                        <i className="bi bi-cpu text-muted" style={{ fontSize: '3rem' }}></i>
                                        <p className="text-muted mt-2">No software versions tracked</p>
                                        <p className="text-muted">Click "Add Version" to start tracking software versions</p>
                                    </div>
                                )}
                            </Card.Body>
                        </Card>

                        {/* Assigned Licenses Card */}
                        <Card className="shadow-sm border-0">
                            <Card.Header className="bg-white border-bottom">
                                <Row>
                                    <Col>
                                        <h5 className="mb-0">
                                            <i className="bi bi-key me-2"></i>
                                            Assigned Licenses ({assignments.length})
                                        </h5>
                                    </Col>
                                    <Col xs="auto" className="text-end">
                                        <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
                                            <Button
                                                variant="primary"
                                                size="sm"
                                                onClick={() => setShowAssignModal(true)}
                                            >
                                                <i className="bi bi-plus-circle me-2"></i>
                                                Assign License
                                            </Button>
                                        </RoleBasedAccess>
                                    </Col>
                                </Row>
                            </Card.Header>
                            <Card.Body>
                                {assignments.length > 0 ? (
                                    <Table hover responsive>
                                        <thead className="table-light">
                                            <tr>
                                                <th>License Key</th>
                                                <th>Software</th>
                                                <th>Assigned Date</th>
                                                <th>Assigned By</th>
                                                <th>Status</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {assignments.map((assignment) => (
                                                <tr key={assignment.id}>
                                                    <td><code>{assignment.licenseKey}</code></td>
                                                    <td>{assignment.softwareName}</td>
                                                    <td>
                                                        {new Date(assignment.assignedOn).toLocaleDateString()}
                                                        <br />
                                                        <small className="text-muted">
                                                            {new Date(assignment.assignedOn).toLocaleTimeString()}
                                                        </small>
                                                    </td>
                                                    <td>
                                                        <Badge bg="secondary">{assignment.assignedBy}</Badge>
                                                    </td>
                                                    <td>
                                                        <Badge bg="success">
                                                            <i className="bi bi-check-circle me-1"></i>
                                                            Active
                                                        </Badge>
                                                    </td>
                                                    <td>
                                                        <Button
                                                            variant="outline-info"
                                                            size="sm"
                                                            className="me-1"
                                                            onClick={() => navigate(`/licenses/${assignment.licenseId}`)}
                                                            title="View License"
                                                        >
                                                            <i className="bi bi-eye"></i>
                                                        </Button>
                                                        <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
                                                            <Button
                                                                variant="outline-danger"
                                                                size="sm"
                                                                onClick={() => handleUnassign(assignment)}
                                                            >
                                                                <i className="bi bi-x-circle me-1"></i>
                                                                Unassign
                                                            </Button>
                                                        </RoleBasedAccess>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </Table>
                                ) : (
                                    <div className="text-center py-4">
                                        <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                        <p className="text-muted mt-2">No licenses assigned to this device</p>
                                        <p className="text-muted">Click "Assign License" button to add a license</p>
                                    </div>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Assign License Modal */}
            <Modal show={showAssignModal} onHide={() => setShowAssignModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Assign License to Device</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form.Group>
                        <Form.Label>Select License</Form.Label>
                        <Form.Select
                            value={selectedLicenseId}
                            onChange={(e) => setSelectedLicenseId(e.target.value)}
                        >
                            <option value="">-- Select a License --</option>
                            {availableLicenses.map(license => (
                                <option
                                    key={license.id}
                                    value={license.id}
                                    disabled={license.currentUsage >= license.maxUsage}
                                >
                                    {license.licenseKey} - {license.softwareName}
                                    ({license.currentUsage}/{license.maxUsage} used)
                                    {license.currentUsage >= license.maxUsage && ' - FULL'}
                                </option>
                            ))}
                        </Form.Select>
                        <Form.Text className="text-muted">
                            Only active licenses with available capacity are shown
                        </Form.Text>
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowAssignModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleAssignLicense}
                        disabled={!selectedLicenseId}
                    >
                        <i className="bi bi-check-circle me-2"></i>
                        Assign License
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Revoke License Modal */}
            <Modal show={showRevokeModal} onHide={() => setShowRevokeModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Revoke License Assignment</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert variant="warning">
                        <i className="bi bi-exclamation-triangle me-2"></i>
                        You are about to revoke this license assignment.
                    </Alert>
                    {selectedAssignment && (
                        <div className="mb-3">
                            <p><strong>License:</strong> <code>{selectedAssignment.licenseKey}</code></p>
                            <p><strong>Software:</strong> {selectedAssignment.softwareName}</p>
                        </div>
                    )}
                    <Form.Group>
                        <Form.Label>Revocation Reason *</Form.Label>
                        <Form.Control
                            as="textarea"
                            rows={3}
                            value={revocationReason}
                            onChange={(e) => setRevocationReason(e.target.value)}
                            placeholder="Please provide a reason for revoking this license..."
                            required
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowRevokeModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="danger"
                        onClick={confirmRevoke}
                        disabled={!revocationReason.trim()}
                    >
                        <i className="bi bi-x-circle me-2"></i>
                        Revoke Assignment
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Software Version Modal */}
            <Modal show={showSoftwareModal} onHide={() => setShowSoftwareModal(false)} size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>
                        {selectedSoftwareVersion ? 'Edit Software Version' : 'Add Software Version'}
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form>
                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Software Name *</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="softwareName"
                                        value={softwareFormData.softwareName}
                                        onChange={handleSoftwareFormChange}
                                        placeholder="e.g., IOS XR, PAN-OS"
                                        required
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Current Version *</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="currentVersion"
                                        value={softwareFormData.currentVersion}
                                        onChange={handleSoftwareFormChange}
                                        placeholder="e.g., 9.1.0"
                                        required
                                    />
                                </Form.Group>
                            </Col>
                        </Row>

                        <Row>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Latest Version</Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="latestVersion"
                                        value={softwareFormData.latestVersion}
                                        onChange={handleSoftwareFormChange}
                                        placeholder="e.g., 10.2.0"
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={6}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Release Date</Form.Label>
                                    <Form.Control
                                        type="date"
                                        name="releaseDate"
                                        value={softwareFormData.releaseDate}
                                        onChange={handleSoftwareFormChange}
                                    />
                                </Form.Group>
                            </Col>
                        </Row>

                        <Form.Group className="mb-3">
                            <Form.Label>Update URL</Form.Label>
                            <Form.Control
                                type="url"
                                name="updateUrl"
                                value={softwareFormData.updateUrl}
                                onChange={handleSoftwareFormChange}
                                placeholder="https://download.example.com/update"
                            />
                            <Form.Text className="text-muted">
                                Download link for the software update
                            </Form.Text>
                        </Form.Group>

                        <Form.Group className="mb-3">
                            <Form.Label>Notes</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                name="notes"
                                value={softwareFormData.notes}
                                onChange={handleSoftwareFormChange}
                                placeholder="Additional notes about this version..."
                            />
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowSoftwareModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSaveSoftwareVersion}
                    >
                        <i className="bi bi-check-circle me-2"></i>
                        {selectedSoftwareVersion ? 'Update' : 'Add'} Version
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

export default DeviceDetail;
