import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Badge, Spinner, Modal, Alert, Pagination } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import assignmentService from '../../services/assignmentService';

const AssignmentList = () => {
    const [assignments, setAssignments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({
        search: '',
        region: '',
        deviceType: '',
        showRevoked: false
    });
    const [showRevokeModal, setShowRevokeModal] = useState(false);
    const [selectedAssignment, setSelectedAssignment] = useState(null);
    const [revocationReason, setRevocationReason] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [stats, setStats] = useState({
        total: 0,
        active: 0,
        revoked: 0
    });
    
    const navigate = useNavigate();

    useEffect(() => {
        fetchAssignments();
    }, [filters.showRevoked]);

    const fetchAssignments = async () => {
        setLoading(true);
        try {
            const data = await assignmentService.getAllActiveAssignments();
            setAssignments(data);
            calculateStats(data);
        } catch (error) {
            console.error('Error fetching assignments:', error);
            setError('Failed to fetch assignments');
        } finally {
            setLoading(false);
        }
    };

    const calculateStats = (data) => {
        const activeCount = data.filter(a => a.active).length;
        setStats({
            total: data.length,
            active: activeCount,
            revoked: data.length - activeCount
        });
    };

    const handleFilterChange = (e) => {
        setFilters({
            ...filters,
            [e.target.name]: e.target.value
        });
    };

    const handleToggleRevoked = () => {
        setFilters({
            ...filters,
            showRevoked: !filters.showRevoked
        });
    };

    const resetFilters = () => {
        setFilters({
            search: '',
            region: '',
            deviceType: '',
            showRevoked: false
        });
    };

    const handleRevoke = (assignment) => {
        setSelectedAssignment(assignment);
        setShowRevokeModal(true);
    };

    const confirmRevoke = async () => {
        if (!revocationReason.trim()) {
            setError('Please provide a revocation reason');
            return;
        }

        try {
            const user = JSON.parse(localStorage.getItem('user'));
            await assignmentService.revokeAssignment(
                selectedAssignment.id,
                user?.username || 'UNKNOWN',
                revocationReason
            );
            
            setSuccess('Assignment revoked successfully!');
            setShowRevokeModal(false);
            setRevocationReason('');
            setSelectedAssignment(null);
            
            await fetchAssignments();
            
            setTimeout(() => setSuccess(''), 3000);
        } catch (error) {
            console.error('Revoke error:', error);
            setError(error.response?.data?.message || 'Failed to revoke assignment');
        }
    };

    const exportToCSV = () => {
        const csvContent = [
            ['Device ID', 'Device Type', 'License Key', 'Software', 'Assigned Date', 'Assigned By', 'Status'],
            ...filteredAssignments.map(a => [
                a.deviceIdName,
                a.deviceType,
                a.licenseKey,
                a.softwareName,
                new Date(a.assignedOn).toLocaleDateString(),
                a.assignedBy,
                a.active ? 'Active' : 'Revoked'
            ])
        ].map(row => row.join(',')).join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `license_assignments_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    };

    const filteredAssignments = assignments.filter(assignment => {
        const matchesSearch = 
            assignment.deviceIdName?.toLowerCase().includes(filters.search.toLowerCase()) ||
            assignment.licenseKey?.toLowerCase().includes(filters.search.toLowerCase()) ||
            assignment.softwareName?.toLowerCase().includes(filters.search.toLowerCase());
        
        const matchesRegion = !filters.region || assignment.deviceLocation?.includes(filters.region);
        const matchesDeviceType = !filters.deviceType || assignment.deviceType === filters.deviceType;
        
        return matchesSearch && matchesRegion && matchesDeviceType;
    });

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
                                    <i className="bi bi-link-45deg me-2"></i>
                                    License Assignments
                                </h2>
                            </Col>
                            <Col className="text-end">
                                <Button 
                                    variant="outline-success"
                                    onClick={exportToCSV}
                                    disabled={filteredAssignments.length === 0}
                                >
                                    <i className="bi bi-download me-2"></i>
                                    Export CSV
                                </Button>
                            </Col>
                        </Row>

                        {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}
                        {success && <Alert variant="success">{success}</Alert>}

                        {/* Statistics Cards */}
                        <Row className="mb-4">
                            <Col md={4}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Total Assignments</p>
                                                <h3 className="mb-0">{stats.total}</h3>
                                            </div>
                                            <div className="bg-primary bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-link-45deg text-primary" style={{ fontSize: '2rem' }}></i>
                                            </div>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={4}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Active Assignments</p>
                                                <h3 className="mb-0 text-success">{stats.active}</h3>
                                            </div>
                                            <div className="bg-success bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-check-circle text-success" style={{ fontSize: '2rem' }}></i>
                                            </div>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={4}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Revoked Assignments</p>
                                                <h3 className="mb-0 text-danger">{stats.revoked}</h3>
                                            </div>
                                            <div className="bg-danger bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-x-circle text-danger" style={{ fontSize: '2rem' }}></i>
                                            </div>
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
                                            <Form.Label>Search</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="search"
                                                placeholder="Device, License, Software..."
                                                value={filters.search}
                                                onChange={handleFilterChange}
                                            />
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
                                                <option value="BANGALORE">BANGALORE</option>
                                                <option value="CHENNAI">CHENNAI</option>
                                                <option value="DELHI">DELHI</option>
                                                <option value="MUMBAI">MUMBAI</option>
                                                <option value="HYDERABAD">HYDERABAD</option>
                                                <option value="KOLKATA">KOLKATA</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={2}>
                                        <Form.Group>
                                            <Form.Label>Device Type</Form.Label>
                                            <Form.Select
                                                name="deviceType"
                                                value={filters.deviceType}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All Types</option>
                                                <option value="ROUTER">Router</option>
                                                <option value="SWITCH">Switch</option>
                                                <option value="FIREWALL">Firewall</option>
                                                <option value="LOAD_BALANCER">Load Balancer</option>
                                                <option value="SERVER">Server</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={3} className="d-flex align-items-end">
                                        <Form.Check
                                            type="checkbox"
                                            label="Show Revoked Assignments"
                                            checked={filters.showRevoked}
                                            onChange={handleToggleRevoked}
                                        />
                                    </Col>
                                    <Col md={2} className="d-flex align-items-end">
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

                        {/* Assignments Table */}
                        <Card className="shadow-sm border-0">
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading assignments...</p>
                                    </div>
                                ) : (
                                    <div class="overflow-auto" style={{width : "75vw"}}>
                                        <div className="mb-3">
                                            <small className="text-muted">
                                                Showing {filteredAssignments.length} of {assignments.length} assignments
                                            </small>
                                        </div>
                                        <Table hover responsive>
                                            <thead className="table-light">
                                                <tr>
                                                    <th>Device ID</th>
                                                    <th>Device Type</th>
                                                    <th>Location</th>
                                                    <th>License Key</th>
                                                    <th>Software</th>
                                                    <th>Assigned Date</th>
                                                    <th>Assigned By</th>
                                                    <th>Status</th>
                                                    <th>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {filteredAssignments.length > 0 ? (
                                                    filteredAssignments.map((assignment) => (
                                                        <tr key={assignment.id}>
                                                            <td className="fw-bold">
                                                                <i className="bi bi-hdd-network text-primary me-2"></i>
                                                                {assignment.deviceIdName}
                                                            </td>
                                                            <td>{assignment.deviceType}</td>
                                                            <td>{assignment.deviceLocation}</td>
                                                            <td><code>{assignment.licenseKey}</code></td>
                                                            <td>{assignment.softwareName}</td>
                                                            <td>{new Date(assignment.assignedOn).toLocaleDateString()}</td>
                                                            <td>
                                                                <Badge bg="secondary">{assignment.assignedBy}</Badge>
                                                            </td>
                                                            <td>
                                                                {assignment.active ? (
                                                                    <Badge bg="success">Active</Badge>
                                                                ) : (
                                                                    <Badge bg="danger">Revoked</Badge>
                                                                )}
                                                            </td>
                                                            <td>
                                                                <Button
                                                                    variant="outline-info"
                                                                    size="sm"
                                                                    className="me-1"
                                                                    onClick={() => navigate(`/devices/${assignment.deviceId}`)}
                                                                    title="View Device"
                                                                >
                                                                    <i className="bi bi-hdd-network"></i>
                                                                </Button>
                                                                <Button
                                                                    variant="outline-primary"
                                                                    size="sm"
                                                                    className="me-1"
                                                                    onClick={() => navigate(`/licenses/${assignment.licenseId}`)}
                                                                    title="View License"
                                                                >
                                                                    <i className="bi bi-key"></i>
                                                                </Button>
                                                                {assignment.active && (
                                                                    <Button
                                                                        variant="outline-danger"
                                                                        size="sm"
                                                                        onClick={() => handleRevoke(assignment)}
                                                                        title="Revoke Assignment"
                                                                    >
                                                                        <i className="bi bi-x-circle"></i>
                                                                    </Button>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))
                                                ) : (
                                                    <tr>
                                                        <td colSpan="9" className="text-center py-4">
                                                            <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                                            <p className="text-muted mt-2">No assignments found</p>
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </Table>
                                    </div>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Revoke Modal */}
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
                            <p><strong>Device:</strong> {selectedAssignment.deviceIdName}</p>
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
                            placeholder="Please provide a reason for revoking this assignment..."
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
        </>
    );
};

export default AssignmentList;
