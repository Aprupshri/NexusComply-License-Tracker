import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Badge, Spinner, Modal, Alert, Pagination } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import RoleBasedAccess from '../common/RoleBasedAccess';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import softwareVersionService from '../../services/softwareVersionService';

const SoftwareVersionList = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [versions, setVersions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [stats, setStats] = useState({
        total: 0,
        upToDate: 0,
        outdated: 0,
        critical: 0,
        unknown: 0
    });
    const [filters, setFilters] = useState({
        search: '',
        status: ''
    });
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedVersion, setSelectedVersion] = useState(null);

    const navigate = useNavigate();

    useEffect(() => {
        fetchVersions();
        fetchStatistics();
    }, [currentPage, pageSize]);

    const fetchVersions = async () => {
        setLoading(true);
        try {
            const response = await softwareVersionService.getAllSoftwareVersions(currentPage, pageSize);
            setVersions(response.content);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Error fetching software versions:', error);
            toast.error('Failed to fetch software versions');
        } finally {
            setLoading(false);
        }
    };

    const fetchStatistics = async () => {
        try {
            const data = await softwareVersionService.getStatistics();
            setStats(data);
        } catch (error) {
            console.error('Error fetching statistics:', error);
        }
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
    };

    const handleFilterChange = (e) => {
        setFilters({
            ...filters,
            [e.target.name]: e.target.value
        });
    };

    const resetFilters = () => {
        setFilters({
            search: '',
            status: ''
        });
    };

    const handleCheckForUpdates = async (versionId) => {
        toast.promise(
            softwareVersionService.checkForUpdates(versionId),
            {
                loading: 'Checking for updates...',
                success: () => {
                    fetchVersions();
                    fetchStatistics();
                    return 'Update check completed';
                },
                error: 'Failed to check for updates'
            }
        );
    };

    const handleDelete = (version) => {
        setSelectedVersion(version);
        setShowDeleteModal(true);
    };

    const confirmDelete = async () => {
        toast.promise(
            softwareVersionService.deleteSoftwareVersion(selectedVersion.id),
            {
                loading: 'Deleting software version...',
                success: () => {
                    setShowDeleteModal(false);
                    setSelectedVersion(null);
                    fetchVersions();
                    fetchStatistics();
                    return 'Software version deleted successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to delete software version'
            }
        );
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

    const filteredVersions = versions.filter(version => {
        const matchesSearch =
            version.softwareName?.toLowerCase().includes(filters.search.toLowerCase()) ||
            version.deviceIdName?.toLowerCase().includes(filters.search.toLowerCase()) ||
            version.currentVersion?.toLowerCase().includes(filters.search.toLowerCase());

        const matchesStatus = !filters.status || version.status === filters.status;

        return matchesSearch && matchesStatus;
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
                                    <i className="bi bi-cpu me-2"></i>
                                    Software Version Management
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <RoleBasedAccess allowedRoles={['ADMIN', 'OPERATIONS_MANAGER', 'NETWORK_ENGINEER']}>
                                    <Button
                                        variant="primary"
                                        onClick={() => navigate('/software-versions/new')}
                                    >
                                        <i className="bi bi-plus-circle me-2"></i>
                                        Add Version
                                    </Button>
                                </RoleBasedAccess>
                            </Col>
                        </Row>

                        {/* Statistics Cards */}
                        <Row className="mb-4">
                            <Col md={6} lg={3}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Total Versions</p>
                                                <h3 className="mb-0">{stats.total}</h3>
                                            </div>
                                            <div className="bg-primary bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-cpu text-primary" style={{ fontSize: '2rem' }}></i>
                                            </div>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={3}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Up to Date</p>
                                                <h3 className="mb-0 text-success">{stats.upToDate}</h3>
                                            </div>
                                            <div className="bg-success bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-check-circle text-success" style={{ fontSize: '2rem' }}></i>
                                            </div>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={3}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Outdated</p>
                                                <h3 className="mb-0 text-warning">{stats.outdated}</h3>
                                            </div>
                                            <div className="bg-warning bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-exclamation-triangle text-warning" style={{ fontSize: '2rem' }}></i>
                                            </div>
                                        </div>
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col md={6} lg={3}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Critical</p>
                                                <h3 className="mb-0 text-danger">{stats.critical}</h3>
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
                                    <Col md={4}>
                                        <Form.Group>
                                            <Form.Label>Search</Form.Label>
                                            <Form.Control
                                                type="text"
                                                name="search"
                                                placeholder="Software name, device, version..."
                                                value={filters.search}
                                                onChange={handleFilterChange}
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Status</Form.Label>
                                            <Form.Select
                                                name="status"
                                                value={filters.status}
                                                onChange={handleFilterChange}
                                            >
                                                <option value="">All Status</option>
                                                <option value="UP_TO_DATE">Up to Date</option>
                                                <option value="OUTDATED">Outdated</option>
                                                <option value="CRITICAL">Critical</option>
                                                <option value="UNKNOWN">Unknown</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Per Page</Form.Label>
                                            <Form.Select
                                                value={pageSize}
                                                onChange={(e) => {
                                                    setPageSize(Number(e.target.value));
                                                    setCurrentPage(0);
                                                }}
                                            >
                                                <option value="10">10</option>
                                                <option value="25">25</option>
                                                <option value="50">50</option>
                                                <option value="100">100</option>
                                            </Form.Select>
                                        </Form.Group>
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

                        {/* Versions Table */}
                        <Card className="shadow-sm border-0">
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading software versions...</p>
                                    </div>
                                ) : (
                                    <div class="overflow-auto" style={{width : "75vw"}}>
                                        <div className="mb-3">
                                            <small className="text-muted">
                                                Showing {filteredVersions.length} of {versions.length} versions
                                            </small>
                                        </div>
                                        <div className="table-responsive">
                                            <Table hover>
                                                <thead className="table-light">
                                                    <tr>
                                                        <th>Device</th>
                                                        <th>Software Name</th>
                                                        <th>Current Version</th>
                                                        <th>Latest Version</th>
                                                        <th>Status</th>
                                                        <th>Last Checked</th>
                                                        <th>Actions</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {filteredVersions.length > 0 ? (
                                                        filteredVersions.map((version) => (
                                                            <tr key={version.id}>
                                                                <td>
                                                                    <i className="bi bi-hdd-network text-primary me-2"></i>
                                                                    <strong>{version.deviceIdName}</strong>
                                                                    <br />
                                                                    <small className="text-muted">{version.deviceType}</small>
                                                                </td>
                                                                <td>
                                                                    <strong>{version.softwareName}</strong>
                                                                    {version.notes && (
                                                                        <div>
                                                                            <small className="text-muted">{version.notes}</small>
                                                                        </div>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    <Badge bg="secondary" className="fs-6">
                                                                        {version.currentVersion}
                                                                    </Badge>
                                                                </td>
                                                                <td>
                                                                    {version.latestVersion ? (
                                                                        <Badge bg="info" className="fs-6">
                                                                            {version.latestVersion}
                                                                        </Badge>
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
                                                                                Update Available
                                                                            </small>
                                                                        </div>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    {version.lastChecked ? (
                                                                        <>
                                                                            {new Date(version.lastChecked).toLocaleDateString()}
                                                                            <br />
                                                                            <small className="text-muted">
                                                                                {new Date(version.lastChecked).toLocaleTimeString()}
                                                                            </small>
                                                                        </>
                                                                    ) : (
                                                                        <span className="text-muted">Never</span>
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
                                                                        variant="outline-info"
                                                                        size="sm"
                                                                        className="me-1"
                                                                        onClick={() => navigate(`/devices/${version.deviceId}`)}
                                                                        title="View Device"
                                                                    >
                                                                        <i className="bi bi-hdd-network"></i>
                                                                    </Button>
                                                                    <RoleBasedAccess allowedRoles={['ADMIN', 'OPERATIONS_MANAGER', 'NETWORK_ENGINEER']}>
                                                                        <Button
                                                                            variant="outline-primary"
                                                                            size="sm"
                                                                            className="me-1"
                                                                            onClick={() => navigate(`/software-versions/edit/${version.id}`)}
                                                                        >
                                                                            <i className="bi bi-pencil"></i>
                                                                        </Button>
                                                                    </RoleBasedAccess>
                                                                    {version.updateUrl && (
                                                                        <Button
                                                                            variant="outline-warning"
                                                                            size="sm"
                                                                            className="me-1"
                                                                            href={version.updateUrl}
                                                                            target="_blank"
                                                                            title="Download Update"
                                                                        >
                                                                            <i className="bi bi-download"></i>
                                                                        </Button>
                                                                    )}
                                                                    <RoleBasedAccess allowedRoles={['ADMIN', 'OPERATIONS_MANAGER']}>
                                                                        <Button
                                                                            variant="outline-danger"
                                                                            size="sm"
                                                                            onClick={() => handleDelete(version)}
                                                                        >
                                                                            <i className="bi bi-trash"></i>
                                                                        </Button>
                                                                    </RoleBasedAccess>
                                                                </td>
                                                            </tr>
                                                        ))
                                                    ) : (
                                                        <tr>
                                                            <td colSpan="7" className="text-center py-4">
                                                                <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                                                <p className="text-muted mt-2">No software versions found</p>
                                                            </td>
                                                        </tr>
                                                    )}
                                                </tbody>
                                            </Table>
                                        </div>

                                        {totalPages > 1 && (
                                            <div className="d-flex justify-content-center mt-4">
                                                <Pagination>
                                                    <Pagination.First
                                                        onClick={() => handlePageChange(0)}
                                                        disabled={currentPage === 0}
                                                    />
                                                    <Pagination.Prev
                                                        onClick={() => handlePageChange(currentPage - 1)}
                                                        disabled={currentPage === 0}
                                                    />

                                                    {[...Array(totalPages)].map((_, index) => (
                                                        <Pagination.Item
                                                            key={index}
                                                            active={index === currentPage}
                                                            onClick={() => handlePageChange(index)}
                                                        >
                                                            {index + 1}
                                                        </Pagination.Item>
                                                    ))}

                                                    <Pagination.Next
                                                        onClick={() => handlePageChange(currentPage + 1)}
                                                        disabled={currentPage === totalPages - 1}
                                                    />
                                                    <Pagination.Last
                                                        onClick={() => handlePageChange(totalPages - 1)}
                                                        disabled={currentPage === totalPages - 1}
                                                    />
                                                </Pagination>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Delete Confirmation Modal */}
            <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)}>
                <Modal.Header closeButton>
                    <Modal.Title>Delete Software Version</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert variant="danger">
                        <i className="bi bi-exclamation-triangle me-2"></i>
                        Are you sure you want to delete this software version entry?
                    </Alert>
                    {selectedVersion && (
                        <div>
                            <p><strong>Device:</strong> {selectedVersion.deviceIdName}</p>
                            <p><strong>Software:</strong> {selectedVersion.softwareName}</p>
                            <p><strong>Version:</strong> {selectedVersion.currentVersion}</p>
                        </div>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="danger" onClick={confirmDelete}>
                        <i className="bi bi-trash me-2"></i>
                        Delete
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

export default SoftwareVersionList;
