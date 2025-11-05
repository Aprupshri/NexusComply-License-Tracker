// src/components/vendors/VendorList.jsx
import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Badge, Spinner, Modal, Alert, Pagination } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import RoleBasedAccess from '../common/RoleBasedAccess';
import vendorService from '../../services/vendorService';

const VendorList = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [vendors, setVendors] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [searchTerm, setSearchTerm] = useState('');
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedVendor, setSelectedVendor] = useState(null);

    const navigate = useNavigate();

    useEffect(() => {
        fetchVendors();
    }, [currentPage, pageSize]);

    const fetchVendors = async () => {
        setLoading(true);
        try {
            const response = await vendorService.getAllVendors(currentPage, pageSize);
            setVendors(response.content);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error('Error fetching vendors:', error);
            toast.error('Failed to fetch vendors');
        } finally {
            setLoading(false);
        }
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
    };

    const handleDelete = (vendor) => {
        setSelectedVendor(vendor);
        setShowDeleteModal(true);
    };

    const confirmDelete = async () => {
        toast.promise(
            vendorService.deleteVendor(selectedVendor.id),
            {
                loading: 'Deleting vendor...',
                success: () => {
                    setShowDeleteModal(false);
                    setSelectedVendor(null);
                    fetchVendors();
                    return 'Vendor deleted successfully!';
                },
                error: (err) => err.response?.data?.message || 'Failed to delete vendor'
            }
        );
    };

    const filteredVendors = vendors.filter(vendor =>
        vendor.vendorName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (vendor.contactEmail && vendor.contactEmail.toLowerCase().includes(searchTerm.toLowerCase()))
    );

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
                                    Vendor Management
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD']}>
                                    <Button 
                                        variant="primary"
                                        onClick={() => navigate('/vendors/new')}
                                    >
                                        <i className="bi bi-plus-circle me-2"></i>
                                        Add Vendor
                                    </Button>
                                </RoleBasedAccess>
                            </Col>
                        </Row>

                        {/* Search and Filters */}
                        <Card className="shadow-sm border-0 mb-3">
                            <Card.Body>
                                <Row>
                                    <Col md={6}>
                                        <Form.Group>
                                            <Form.Control
                                                type="text"
                                                placeholder="Search vendors..."
                                                value={searchTerm}
                                                onChange={(e) => setSearchTerm(e.target.value)}
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Select
                                                value={pageSize}
                                                onChange={(e) => {
                                                    setPageSize(Number(e.target.value));
                                                    setCurrentPage(0);
                                                }}
                                            >
                                                <option value="10">10 per page</option>
                                                <option value="25">25 per page</option>
                                                <option value="50">50 per page</option>
                                                <option value="100">100 per page</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>

                        {/* Vendors Table */}
                        <Card className="shadow-sm border-0">
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading vendors...</p>
                                    </div>
                                ) : (
                                    <div class="overflow-auto" style={{width : "75vw"}}>

                                        <div className="mb-3">
                                            <small className="text-muted">
                                                Showing {filteredVendors.length} of {vendors.length} vendors
                                            </small>
                                        </div>
                                        <div className="table-responsive">
                                            <Table hover>
                                                <thead className="table-light">
                                                    <tr>
                                                        <th>Vendor Name</th>
                                                        <th>Contact Email</th>
                                                        <th>Contact Phone</th>
                                                        <th>Support Email</th>
                                                        <th>Actions</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {filteredVendors.length > 0 ? (
                                                        filteredVendors.map((vendor) => (
                                                            <tr key={vendor.id}>
                                                                <td>
                                                                    <i className="bi bi-building text-primary me-2"></i>
                                                                    <strong>{vendor.vendorName}</strong>
                                                                </td>
                                                                <td>
                                                                    {vendor.contactEmail ? (
                                                                        <a href={`mailto:${vendor.contactEmail}`}>
                                                                            {vendor.contactEmail}
                                                                        </a>
                                                                    ) : (
                                                                        <span className="text-muted">-</span>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    {vendor.contactPhone ? (
                                                                        <a href={`tel:${vendor.contactPhone}`}>
                                                                            {vendor.contactPhone}
                                                                        </a>
                                                                    ) : (
                                                                        <span className="text-muted">-</span>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    {vendor.supportEmail ? (
                                                                        <a href={`mailto:${vendor.supportEmail}`}>
                                                                            {vendor.supportEmail}
                                                                        </a>
                                                                    ) : (
                                                                        <span className="text-muted">-</span>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                    <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_LEAD']}>
                                                                        <Button
                                                                            variant="outline-primary"
                                                                            size="sm"
                                                                            className="me-1"
                                                                            onClick={() => navigate(`/vendors/edit/${vendor.id}`)}
                                                                            title="Edit"
                                                                        >
                                                                            <i className="bi bi-pencil"></i>
                                                                        </Button>
                                                                    </RoleBasedAccess>
                                                                    
                                                                    <RoleBasedAccess allowedRoles={['ADMIN']}>
                                                                        <Button
                                                                            variant="outline-danger"
                                                                            size="sm"
                                                                            onClick={() => handleDelete(vendor)}
                                                                            title="Delete"
                                                                        >
                                                                            <i className="bi bi-trash"></i>
                                                                        </Button>
                                                                    </RoleBasedAccess>
                                                                </td>
                                                            </tr>
                                                        ))
                                                    ) : (
                                                        <tr>
                                                            <td colSpan="5" className="text-center py-4">
                                                                <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                                                <p className="text-muted mt-2">No vendors found</p>
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
                    <Modal.Title>Delete Vendor</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert variant="danger">
                        <i className="bi bi-exclamation-triangle me-2"></i>
                        Are you sure you want to delete this vendor?
                    </Alert>
                    {selectedVendor && (
                        <div>
                            <p><strong>Vendor:</strong> {selectedVendor.vendorName}</p>
                            <p className="text-muted mb-0">This action cannot be undone.</p>
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

export default VendorList;
