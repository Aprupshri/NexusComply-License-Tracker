import React, { useState, useEffect, useCallback } from 'react';
import { Container, Row, Col, Card, Table, Button, Pagination, Form, Badge, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import NavigationBar from '../common/Navbar';
import RoleBasedAccess from '../common/RoleBasedAccess';
import Sidebar from '../common/Sidebar';
import BulkUploadModel from './BulkUploadModel';
import deviceService from '../../services/deviceService';

const DeviceList = () => {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [searchTerm, setSearchTerm] = useState('');
    const [showBulkUpload, setShowBulkUpload] = useState(false);

    const navigate = useNavigate();

    const fetchDevices = useCallback(async () => {
        setLoading(true);
        try {
            const response = await deviceService.getAllDevices(currentPage, pageSize);
            setDevices(response.content);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Error fetching devices:', error);
        } finally {
            setLoading(false);
        }
    }, [currentPage, pageSize]);


    useEffect(() => {
        fetchDevices();
    }, [fetchDevices]);

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this device?')) {
            try {
                await deviceService.deleteDevice(id);
                fetchDevices();
            } catch (error) {
                console.error('Error deleting device:', error);
                alert('Failed to delete device');
            }
        }
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
    };

    const filteredDevices = devices.filter(device =>
        device.deviceId?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        device.model?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        device.location?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const getLifecycleBadge = (lifecycle) => {
        const colors = {
            ACTIVE: 'success',
            MAINTENANCE: 'warning',
            OBSOLETE: 'secondary',
            DECOMMISSIONED: 'danger'
        };
        return <Badge bg={colors[lifecycle] || 'secondary'}>{lifecycle}</Badge>;
    };

    const getDeviceTypeIcon = (type) => {
        const icons = {
            ROUTER: 'router',
            SWITCH: 'diagram-3',
            FIREWALL: 'shield-check',
            LOAD_BALANCER: 'distribute-horizontal',
            SERVER: 'server'
        };
        return icons[type] || 'hdd-network';
    };

    return (
        <>
            <NavigationBar />
            <div className="d-flex">
                <Sidebar />
                <div className="flex-grow-1 p-4" style={{ backgroundColor: '#f8f9fa' }}>
                    <Container fluid>
                        <Row className="mb-4">
                            <Col>
                                <h2 className="fw-bold">
                                    <i className="bi bi-hdd-network me-2"></i>
                                    Device Management
                                </h2>
                            </Col>
                        <Col xs="auto" className="text-end">
        {/* ADD ROLE-BASED ACCESS HERE */}
        <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
            <Button 
                variant="primary"
                onClick={() => navigate('/devices/new')}
            >
                <i className="bi bi-plus-circle me-2"></i>
                Add Device
            </Button>&nbsp;
            <Button
                                    variant="outline-success"
                                    className="me-2"
                                    onClick={() => setShowBulkUpload(true)}
                                >
                                    <i className="bi bi-upload me-2"></i>
                                    Bulk Upload
                                </Button>
        </RoleBasedAccess>
    </Col>
                             
                        </Row>

                        <Card className="shadow-sm border-0">
                            <Card.Header className="bg-white border-bottom">
                                <Row>
                                    <Col md={4}>
                                        <Form.Control
                                            type="text"
                                            placeholder="Search devices..."
                                            value={searchTerm}
                                            onChange={(e) => setSearchTerm(e.target.value)}
                                        />
                                    </Col>
                                    <Col md={2}>
                                        <Form.Select
                                            value={pageSize}
                                            onChange={(e) => {
                                                setPageSize(Number(e.target.value));
                                                setCurrentPage(0);
                                            }}
                                        >
                                            <option value="5">5 per page</option>
                                            <option value="10">10 per page</option>
                                            <option value="25">25 per page</option>
                                            <option value="50">50 per page</option>
                                        </Form.Select>
                                    </Col>
                                </Row>
                            </Card.Header>
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading devices...</p>
                                    </div>
                                ) : (
                                    <div class="overflow-auto" style={{width : "75vw"}}>
                                        <Table hover responsive>
                                            <thead className="table-light">
                                                <tr>
                                                    <th>Device ID</th>
                                                    <th>Type</th>
                                                    <th>Model</th>
                                                    <th>IP Address</th>
                                                    <th>Location</th>
                                                    <th>Region</th>
                                                    <th>Status</th>
                                                    <th>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {filteredDevices.length > 0 ? (
                                                    filteredDevices.map((device) => (
                                                        <tr key={device.id}>
                                                            <td className="fw-bold">{device.deviceId}</td>
                                                            <td>
                                                                <i className={`bi bi-${getDeviceTypeIcon(device.deviceType)} text-primary me-2`}></i>
                                                                {device.deviceType}
                                                            </td>
                                                            <td>{device.model}</td>
                                                            <td>
                                                                <code>{device.ipAddress}</code>
                                                            </td>
                                                            <td>{device.location}</td>
                                                            <td>
                                                                <Badge bg="info">{device.region}</Badge>
                                                            </td>
                                                            <td>{getLifecycleBadge(device.lifecycle)}</td>
                                                            <td>
                                                            <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'OPERATIONS_MANAGER']}>
                                                                <Button
                                                                    variant="outline-primary"
                                                                    size="sm"
                                                                    className="me-2"
                                                                    onClick={() => navigate(`/devices/edit/${device.id}`)}
                                                                >
                                                                    Edit&nbsp;
                                                                    <i className="bi bi-pencil"></i>
                                                                </Button>
                                                                </RoleBasedAccess>
                                                                <RoleBasedAccess allowedRoles={['ADMIN','NETWORK_ADMIN']}>
                                                                <Button
                                                                    variant="outline-danger"
                                                                    size="sm"
                                                                    onClick={() => handleDelete(device.id)}
                                                                >
                                                                    Delete&nbsp;
                                                                    <i className="bi bi-trash"></i>
                                                                </Button>
                                                                </RoleBasedAccess>
                                                                <Button
                                                                    variant="outline-info"
                                                                    size="sm"
                                                                    className="me-2 mt-2"
                                                                    onClick={() => navigate(`/devices/${device.id}`)}
                                                                >

                                                                    Assign&nbsp;
                                                                    <i className="bi bi-eye"></i>
                                                                </Button>

                                                            </td>
                                                        </tr>
                                                    ))
                                                ) : (
                                                    <tr>
                                                        <td colSpan="8" className="text-center py-4">
                                                            <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                                            <p className="text-muted mt-2">No devices found</p>
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </Table>

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
                                        <BulkUploadModel
                                            show={showBulkUpload}
                                            onHide={() => setShowBulkUpload(false)}
                                            onUploadComplete={() => {
                                                setShowBulkUpload(false);
                                                fetchDevices(); // Refresh device list
                                            }}
                                        />
                                    </div>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default DeviceList;
