// src/components/audit/AuditLogList.jsx
import React, { useState, useEffect } from 'react';
import { 
    Container, Row, Col, Card, Table, Form, Button, 
    Badge, Spinner, InputGroup, Pagination, Tabs, Tab
} from 'react-bootstrap';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import auditLogService from '../../services/auditLogService';

const AuditLogList = () => {
    const [auditLogs, setAuditLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize] = useState(20);
    const [activeTab, setActiveTab] = useState('all');

    // Advanced Search States
    const [advancedFilters, setAdvancedFilters] = useState({
        entityType: '',
        action: '',
        username: '',
        startDate: '',
        endDate: ''
    });

    // Quick Search States
    const [licenseKeySearch, setLicenseKeySearch] = useState('');
    const [deviceIdSearch, setDeviceIdSearch] = useState('');

    const entityTypes = ['DEVICE', 'LICENSE', 'ASSIGNMENT', 'USER', 'VENDOR', 'SOFTWARE_VERSION', 'ALERT'];
    const actions = ['CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'ASSIGN', 'UNASSIGN', 'LOGIN', 'PASSWORD_CHANGE'];

    useEffect(() => {
        fetchAuditLogs();
    }, [currentPage, activeTab]);

    const fetchAuditLogs = async () => {
        setLoading(true);
        try {
            let data;
            
            if (activeTab === 'license-search' && licenseKeySearch) {
                data = await auditLogService.searchByLicenseKey(licenseKeySearch, currentPage, pageSize);
            } else if (activeTab === 'device-search' && deviceIdSearch) {
                data = await auditLogService.searchByDeviceId(deviceIdSearch, currentPage, pageSize);
            } else if (activeTab === 'advanced') {
                data = await auditLogService.advancedSearch(advancedFilters, currentPage, pageSize);
            } else {
                data = await auditLogService.getAllAuditLogs(currentPage, pageSize);
            }

            setAuditLogs(data.content);
            setTotalPages(data.totalPages);
        } catch (error) {
            console.error('Error fetching audit logs:', error);
            toast.error('Failed to fetch audit logs');
        } finally {
            setLoading(false);
        }
    };

    const handleLicenseKeySearch = (e) => {
        e.preventDefault();
        setCurrentPage(0);
        fetchAuditLogs();
    };

    const handleDeviceIdSearch = (e) => {
        e.preventDefault();
        setCurrentPage(0);
        fetchAuditLogs();
    };

    const handleAdvancedSearch = (e) => {
        e.preventDefault();
        setCurrentPage(0);
        fetchAuditLogs();
    };

    const handleClearFilters = () => {
        setAdvancedFilters({
            entityType: '',
            action: '',
            username: '',
            startDate: '',
            endDate: ''
        });
        setCurrentPage(0);
        setActiveTab('all');
    };

    const getActionBadge = (action) => {
        const colors = {
            CREATE: 'success',
            UPDATE: 'primary',
            DELETE: 'danger',
            ACTIVATE: 'success',
            DEACTIVATE: 'warning',
            ASSIGN: 'info',
            UNASSIGN: 'secondary',
            LOGIN: 'primary',
            PASSWORD_CHANGE: 'warning'
        };
        return <Badge bg={colors[action] || 'secondary'}>{action}</Badge>;
    };

    const getEntityTypeBadge = (entityType) => {
        const colors = {
            DEVICE: 'primary',
            LICENSE: 'warning',
            ASSIGNMENT: 'info',
            USER: 'success',
            VENDOR: 'secondary',
            SOFTWARE_VERSION: 'dark',
            ALERT: 'danger'
        };
        return <Badge bg={colors[entityType] || 'secondary'}>{entityType}</Badge>;
    };

    const parseDetails = (details) => {
        try {
            const parsed = JSON.parse(details);
            
            const info = {
                licenseKey: parsed.licenseKey || parsed.newLicenseKey || null,
                deviceId: parsed.deviceIdName || parsed.deviceId || null,
                status: parsed.status || null,
                failureReason: parsed.failureReason || null
            };

            return info;
        } catch (e) {
            return null;
        }
    };

    const renderDetails = (log) => {
        const info = parseDetails(log.details);
        
        if (!info) {
            return <small className="text-muted">No additional details</small>;
        }

        return (
            <div>
                {info.licenseKey && (
                    <div><small><strong>License:</strong> {info.licenseKey}</small></div>
                )}
                {info.deviceId && (
                    <div><small><strong>Device:</strong> {info.deviceId}</small></div>
                )}
                {info.status && (
                    <div>
                        <small>
                            <strong>Status:</strong>{' '}
                            <Badge bg={info.status === 'SUCCESS' ? 'success' : 'danger'}>
                                {info.status}
                            </Badge>
                        </small>
                    </div>
                )}
                {info.failureReason && (
                    <div><small className="text-danger"><strong>Reason:</strong> {info.failureReason}</small></div>
                )}
            </div>
        );
    };

    const renderLogsTable = () => {
        if (loading) {
            return (
                <div className="text-center py-5">
                    <Spinner animation="border" variant="primary" />
                    <p className="mt-3">Loading audit logs...</p>
                </div>
            );
        }

        if (auditLogs.length === 0) {
            return (
                <div className="text-center py-5 text-muted">
                    <i className="bi bi-inbox" style={{ fontSize: '3rem' }}></i>
                    <p className="mt-3">No audit logs found</p>
                </div>
            );
        }

        return (
            <>
                <div className="table-responsive">
                    <Table hover size="sm">
                        <thead className="table-light">
                            <tr>
                                <th>Timestamp</th>
                                <th>User</th>
                                <th>Entity Type</th>
                                <th>Action</th>
                                <th>Entity ID</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                            {auditLogs.map(log => (
                                <tr key={log.logId}>
                                    <td className="text-nowrap" style={{ fontSize: '0.85rem' }}>
                                        {new Date(log.timestamp).toLocaleString()}
                                    </td>
                                    <td className="fw-bold">{log.username}</td>
                                    <td>{getEntityTypeBadge(log.entityType)}</td>
                                    <td>{getActionBadge(log.action)}</td>
                                    <td className="text-break" style={{ maxWidth: '150px', fontSize: '0.85rem' }}>
                                        {log.entityId}
                                    </td>
                                    <td>{renderDetails(log)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="d-flex justify-content-center mt-3">
                        <Pagination>
                            <Pagination.First 
                                onClick={() => setCurrentPage(0)}
                                disabled={currentPage === 0}
                            />
                            <Pagination.Prev
                                onClick={() => setCurrentPage(currentPage - 1)}
                                disabled={currentPage === 0}
                            />

                            {[...Array(Math.min(5, totalPages))].map((_, idx) => {
                                const pageNum = currentPage < 3 ? idx : currentPage - 2 + idx;
                                if (pageNum >= totalPages) return null;
                                return (
                                    <Pagination.Item
                                        key={pageNum}
                                        active={pageNum === currentPage}
                                        onClick={() => setCurrentPage(pageNum)}
                                    >
                                        {pageNum + 1}
                                    </Pagination.Item>
                                );
                            })}

                            <Pagination.Next
                                onClick={() => setCurrentPage(currentPage + 1)}
                                disabled={currentPage === totalPages - 1}
                            />
                            <Pagination.Last
                                onClick={() => setCurrentPage(totalPages - 1)}
                                disabled={currentPage === totalPages - 1}
                            />
                        </Pagination>
                    </div>
                )}
            </>
        );
    };

    return (
        <>
            <NavigationBar />
            <div className="d-flex">
                <Sidebar />
                <div className="flex-grow-1 p-2 p-md-4" style={{ backgroundColor: '#f8f9fa', minHeight: '100vh' }}>
                    <Container fluid className="px-2 px-md-0">
                        {/* Header */}
                        <Row className="mb-3 mb-md-4">
                            <Col>
                                <h2 className="fw-bold">
                                    <i className="bi bi-journal-text me-2"></i>
                                    Audit Logs
                                </h2>
                                <p className="text-muted">Track all system activities and changes</p>
                            </Col>
                        </Row>

                        {/* Tabs */}
                        <Tabs 
                            activeKey={activeTab} 
                            onSelect={(k) => {
                                setActiveTab(k);
                                setCurrentPage(0);
                            }}
                            className="mb-3"
                        >
                            {/* All Logs Tab */}
                            <Tab eventKey="all" title="All Logs">
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        {renderLogsTable()}
                                    </Card.Body>
                                </Card>
                            </Tab>

                            {/* License Key Search Tab */}
                            <Tab eventKey="license-search" title="🔍 Search by License">
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <Form onSubmit={handleLicenseKeySearch} className="mb-4">
                                            <Row>
                                                <Col md={8} className="mb-3">
                                                    <Form.Group>
                                                        <Form.Label className="fw-bold">License Key</Form.Label>
                                                        <InputGroup>
                                                            <Form.Control
                                                                type="text"
                                                                placeholder="Enter license key (e.g., CISCO-IOS-XR-2024-001)"
                                                                value={licenseKeySearch}
                                                                onChange={(e) => setLicenseKeySearch(e.target.value)}
                                                            />
                                                            <Button 
                                                                variant="primary" 
                                                                type="submit"
                                                                disabled={!licenseKeySearch.trim()}
                                                            >
                                                                <i className="bi bi-search me-2"></i>
                                                                Search
                                                            </Button>
                                                        </InputGroup>
                                                    </Form.Group>
                                                </Col>
                                            </Row>
                                        </Form>
                                        {renderLogsTable()}
                                    </Card.Body>
                                </Card>
                            </Tab>

                            {/* Device ID Search Tab */}
                            <Tab eventKey="device-search" title="🔍 Search by Device">
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <Form onSubmit={handleDeviceIdSearch} className="mb-4">
                                            <Row>
                                                <Col md={8} className="mb-3">
                                                    <Form.Group>
                                                        <Form.Label className="fw-bold">Device ID</Form.Label>
                                                        <InputGroup>
                                                            <Form.Control
                                                                type="text"
                                                                placeholder="Enter device ID (e.g., RTR-BLR-001)"
                                                                value={deviceIdSearch}
                                                                onChange={(e) => setDeviceIdSearch(e.target.value)}
                                                            />
                                                            <Button 
                                                                variant="primary" 
                                                                type="submit"
                                                                disabled={!deviceIdSearch.trim()}
                                                            >
                                                                <i className="bi bi-search me-2"></i>
                                                                Search
                                                            </Button>
                                                        </InputGroup>
                                                    </Form.Group>
                                                </Col>
                                            </Row>
                                        </Form>
                                        {renderLogsTable()}
                                    </Card.Body>
                                </Card>
                            </Tab>

                            {/* Advanced Filter Tab */}
                            <Tab eventKey="advanced" title="⚙️ Advanced Filters">
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <Form onSubmit={handleAdvancedSearch} className="mb-4">
                                            <Row className="g-3">
                                                <Col md={6} lg={3}>
                                                    <Form.Group>
                                                        <Form.Label className="small fw-bold">Entity Type</Form.Label>
                                                        <Form.Select
                                                            size="sm"
                                                            value={advancedFilters.entityType}
                                                            onChange={(e) => setAdvancedFilters({
                                                                ...advancedFilters,
                                                                entityType: e.target.value
                                                            })}
                                                        >
                                                            <option value="">All Types</option>
                                                            {entityTypes.map(type => (
                                                                <option key={type} value={type}>{type}</option>
                                                            ))}
                                                        </Form.Select>
                                                    </Form.Group>
                                                </Col>

                                                <Col md={6} lg={3}>
                                                    <Form.Group>
                                                        <Form.Label className="small fw-bold">Action</Form.Label>
                                                        <Form.Select
                                                            size="sm"
                                                            value={advancedFilters.action}
                                                            onChange={(e) => setAdvancedFilters({
                                                                ...advancedFilters,
                                                                action: e.target.value
                                                            })}
                                                        >
                                                            <option value="">All Actions</option>
                                                            {actions.map(action => (
                                                                <option key={action} value={action}>{action}</option>
                                                            ))}
                                                        </Form.Select>
                                                    </Form.Group>
                                                </Col>

                                                <Col md={6} lg={3}>
                                                    <Form.Group>
                                                        <Form.Label className="small fw-bold">Username</Form.Label>
                                                        <Form.Control
                                                            type="text"
                                                            size="sm"
                                                            placeholder="Username"
                                                            value={advancedFilters.username}
                                                            onChange={(e) => setAdvancedFilters({
                                                                ...advancedFilters,
                                                                username: e.target.value
                                                            })}
                                                        />
                                                    </Form.Group>
                                                </Col>

                                                <Col md={6} lg={3}>
                                                    <Form.Group>
                                                        <Form.Label className="small fw-bold">Start Date</Form.Label>
                                                        <Form.Control
                                                            type="datetime-local"
                                                            size="sm"
                                                            value={advancedFilters.startDate}
                                                            onChange={(e) => setAdvancedFilters({
                                                                ...advancedFilters,
                                                                startDate: e.target.value
                                                            })}
                                                        />
                                                    </Form.Group>
                                                </Col>

                                                <Col md={6} lg={3}>
                                                    <Form.Group>
                                                        <Form.Label className="small fw-bold">End Date</Form.Label>
                                                        <Form.Control
                                                            type="datetime-local"
                                                            size="sm"
                                                            value={advancedFilters.endDate}
                                                            onChange={(e) => setAdvancedFilters({
                                                                ...advancedFilters,
                                                                endDate: e.target.value
                                                            })}
                                                        />
                                                    </Form.Group>
                                                </Col>
                                            </Row>

                                            <div className="d-flex gap-2 mt-3">
                                                <Button variant="primary" type="submit">
                                                    <i className="bi bi-search me-2"></i>
                                                    Apply Filters
                                                </Button>
                                                <Button variant="outline-secondary" onClick={handleClearFilters}>
                                                    <i className="bi bi-x-circle me-2"></i>
                                                    Clear
                                                </Button>
                                            </div>
                                        </Form>

                                        {renderLogsTable()}
                                    </Card.Body>
                                </Card>
                            </Tab>
                        </Tabs>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default AuditLogList;
