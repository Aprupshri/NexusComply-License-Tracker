import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Button, Form, Badge, Spinner, Modal, Pagination } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import RoleBasedAccess from '../common/RoleBasedAccess';
import userService from '../../services/userService';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [pageSize, setPageSize] = useState(10);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('');
    const [filterActive, setFilterActive] = useState('');
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [newRole, setNewRole] = useState('');
    const [stats, setStats] = useState({
        total: 0,
        active: 0,
        inactive: 0
    });

    const navigate = useNavigate();

    // Updated roles with descriptions
    const roles = [
        {
            value: 'ADMIN',
            label: 'Admin',
            description: 'Full system access - All modules',
            modules: 'All Modules'
        },
        {
            value: 'NETWORK_ADMIN',
            label: 'Network Admin',
            description: 'Device and license management',
            modules: 'Devices, Licenses (view), Assignments, Alerts'
        },
        {
            value: 'PROCUREMENT_OFFICER',
            label: 'Procurement Officer',
            description: 'License and vendor management',
            modules: 'Licenses, Vendors, Reports, Alerts'
        },
        {
            value: 'COMPLIANCE_OFFICER',
            label: 'Compliance Officer',
            description: 'Compliance monitoring',
            modules: 'Dashboard, Reports, Alerts, AI Assistant'
        },
        {
            value: 'IT_AUDITOR',
            label: 'IT Auditor',
            description: 'Audit and reporting',
            modules: 'Reports, Audit Logs, AI Assistant'
        },
        {
            value: 'OPERATIONS_MANAGER',
            label: 'Operations Manager',
            description: 'Device lifecycle management',
            modules: 'Devices lifecycle, Software versions, Alerts'
        },
        {
            value: 'NETWORK_ENGINEER',
            label: 'Network Engineer',
            description: 'Software updates and device info',
            modules: 'Software version update, Device info'
        },
        {
            value: 'SECURITY_HEAD',
            label: 'Security Head',
            description: 'Security and user management',
            modules: 'Audit Logs, Users, Reports'
        },
        {
            value: 'COMPLIANCE_LEAD',
            label: 'Compliance Lead',
            description: 'Compliance leadership',
            modules: 'Dashboard, Reports, AI Assistant'
        },
        {
            value: 'PROCUREMENT_LEAD',
            label: 'Procurement Lead',
            description: 'Vendor oversight',
            modules: 'Vendors, Reports, AI Assistant'
        },
        {
            value: 'PRODUCT_OWNER',
            label: 'Product Owner',
            description: 'Product oversight',
            modules: 'Dashboard, AI Assistant'
        }
    ];

    useEffect(() => {
        fetchUsers();
    }, [currentPage, pageSize]);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const response = await userService.getAllUsers(currentPage, pageSize);
            setUsers(response.content);
            setTotalPages(response.totalPages);
            calculateStats(response.content);
        } catch (error) {
            console.error('Error fetching users:', error);
            toast.error('Failed to fetch users');
        } finally {
            setLoading(false);
        }
    };

    const calculateStats = (data) => {
        const activeCount = data.filter(u => u.active).length;
        setStats({
            total: data.length,
            active: activeCount,
            inactive: data.length - activeCount
        });
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
    };

    const handleRoleChange = (user) => {
        setSelectedUser(user);
        setNewRole(user.role);
        setShowRoleModal(true);
    };

    const confirmRoleChange = async () => {
        if (!newRole) {
            toast.error('Please select a role');
            return;
        }

        const loadingToast = toast.loading('Updating role...');

        try {
            await userService.assignRole(selectedUser.id, newRole);
            toast.success(`Role updated successfully for ${selectedUser.username}`, {
                id: loadingToast
            });
            setShowRoleModal(false);
            setSelectedUser(null);
            setNewRole('');
            fetchUsers();
        } catch (error) {
            console.error('Error updating role:', error);
            toast.error(error.response?.data?.message || 'Failed to update role', {
                id: loadingToast
            });
        }
    };

    const handleToggleActive = async (user) => {
        const action = user.active ? 'deactivate' : 'activate';

        toast.promise(
            user.active
                ? userService.deactivateUser(user.id)
                : userService.activateUser(user.id),
            {
                loading: `${action === 'activate' ? 'Activating' : 'Deactivating'} user...`,
                success: () => {
                    fetchUsers();
                    return `User ${user.username} ${action}d successfully`;
                },
                error: (err) => err.response?.data?.message || `Failed to ${action} user`
            }
        );
    };

    const getRoleBadge = (role) => {
        const colors = {
            ADMIN: 'danger',
            NETWORK_ADMIN: 'primary',
            PROCUREMENT_OFFICER: 'success',
            COMPLIANCE_OFFICER: 'warning',
            IT_AUDITOR: 'info',
            OPERATIONS_MANAGER: 'secondary',
            NETWORK_ENGINEER: 'dark',
            SECURITY_HEAD: 'danger',
            COMPLIANCE_LEAD: 'warning',
            PROCUREMENT_LEAD: 'success',
            PRODUCT_OWNER: 'info'
        };
        return <Badge bg={colors[role] || 'secondary'}>{role.replace(/_/g, ' ')}</Badge>;
    };

    const filteredUsers = users.filter(user => {
        const matchesSearch =
            user.username?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            user.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            user.fullName?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesRole = !filterRole || user.role === filterRole;
        const matchesActive = filterActive === '' || user.active.toString() === filterActive;

        return matchesSearch && matchesRole && matchesActive;
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
                                    <i className="bi bi-people me-2"></i>
                                    User Management
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <RoleBasedAccess allowedRoles={['ADMIN']}>
                                    <Button
                                        variant="primary"
                                        onClick={() => navigate('/users/new')}
                                    >
                                        <i className="bi bi-person-plus me-2"></i>
                                        Add User
                                    </Button>
                                </RoleBasedAccess>
                            </Col>
                        </Row>

                        {/* Statistics Cards */}
                        <Row className="mb-4">
                            <Col md={4}>
                                <Card className="shadow-sm border-0">
                                    <Card.Body>
                                        <div className="d-flex justify-content-between align-items-center">
                                            <div>
                                                <p className="text-muted mb-1">Total Users</p>
                                                <h3 className="mb-0">{stats.total}</h3>
                                            </div>
                                            <div className="bg-primary bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-people text-primary" style={{ fontSize: '2rem' }}></i>
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
                                                <p className="text-muted mb-1">Active Users</p>
                                                <h3 className="mb-0 text-success">{stats.active}</h3>
                                            </div>
                                            <div className="bg-success bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-person-check text-success" style={{ fontSize: '2rem' }}></i>
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
                                                <p className="text-muted mb-1">Inactive Users</p>
                                                <h3 className="mb-0 text-danger">{stats.inactive}</h3>
                                            </div>
                                            <div className="bg-danger bg-opacity-10 p-3 rounded">
                                                <i className="bi bi-person-x text-danger" style={{ fontSize: '2rem' }}></i>
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
                                                placeholder="Username, email, or name..."
                                                value={searchTerm}
                                                onChange={(e) => setSearchTerm(e.target.value)}
                                            />
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Role</Form.Label>
                                            <Form.Select
                                                value={filterRole}
                                                onChange={(e) => setFilterRole(e.target.value)}
                                            >
                                                <option value="">All Roles</option>
                                                {roles.map(role => (
                                                    <option key={role.value} value={role.value}>
                                                        {role.label}
                                                    </option>
                                                ))}
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={3}>
                                        <Form.Group>
                                            <Form.Label>Status</Form.Label>
                                            <Form.Select
                                                value={filterActive}
                                                onChange={(e) => setFilterActive(e.target.value)}
                                            >
                                                <option value="">All Status</option>
                                                <option value="true">Active</option>
                                                <option value="false">Inactive</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                    <Col md={2}>
                                        <Form.Group>
                                            <Form.Label>Per Page</Form.Label>
                                            <Form.Select
                                                value={pageSize}
                                                onChange={(e) => {
                                                    setPageSize(Number(e.target.value));
                                                    setCurrentPage(0);
                                                }}
                                            >
                                                <option value="5">5</option>
                                                <option value="10">10</option>
                                                <option value="25">25</option>
                                                <option value="50">50</option>
                                            </Form.Select>
                                        </Form.Group>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>

                        {/* Users Table */}
                        <Card className="shadow-sm border-0">
                            <Card.Body>
                                {loading ? (
                                    <div className="text-center py-5">
                                        <Spinner animation="border" variant="primary" />
                                        <p className="mt-3">Loading users...</p>
                                    </div>
                                ) : (
                                    <>
                                        <div className="mb-3">
                                            <small className="text-muted">
                                                Showing {filteredUsers.length} of {users.length} users
                                            </small>
                                        </div>
                                        <div className="table-responsive">
                                            <Table hover>
                                                <thead className="table-light">
                                                    <tr>
                                                        <th>Username</th>
                                                        <th>Full Name</th>
                                                        <th>Email</th>
                                                        <th>Region</th>
                                                        <th>Role</th>
                                                        <th>Status</th>
                                                        <th>Actions</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {filteredUsers.length > 0 ? (
                                                        filteredUsers.map((user) => (
                                                            <tr key={user.id}>
                                                                <td>
                                                                    <i className="bi bi-person-circle text-primary me-2"></i>
                                                                    <strong>{user.username}</strong>
                                                                </td>
                                                                <td>{user.fullName || '-'}</td>
                                                                <td>{user.email}</td>
                                                                <td>
                                                                    <Badge bg="info">{user.region}</Badge>  {/* NEW */}
                                                                </td>
                                                                <td>{getRoleBadge(user.role)}</td>
                                                                <td>
                                                                    {user.active ? (
                                                                        <Badge bg="success">
                                                                            <i className="bi bi-check-circle me-1"></i>
                                                                            Active
                                                                        </Badge>
                                                                    ) : (
                                                                        <Badge bg="secondary">
                                                                            <i className="bi bi-x-circle me-1"></i>
                                                                            Inactive
                                                                        </Badge>
                                                                    )}
                                                                </td>
                                                                <td>
                                                                <RoleBasedAccess allowedRoles={['ADMIN']}>
                                                                    <Button
                                                                        variant="outline-primary"
                                                                        size="sm"
                                                                        className="me-1"
                                                                        onClick={() => handleRoleChange(user)}
                                                                        title="Change Role"
                                                                    >
                                                                        <i className="bi bi-shield-check"></i>
                                                                    </Button>
                                                                    <Button
                                                                        variant={user.active ? "outline-warning" : "outline-success"}
                                                                        size="sm"
                                                                        onClick={() => handleToggleActive(user)}
                                                                        title={user.active ? "Deactivate" : "Activate"}
                                                                    >
                                                                        <i className={`bi bi-${user.active ? 'x-circle' : 'check-circle'}`}></i>
                                                                    </Button>
                                                                </RoleBasedAccess>
                                                                </td>
                                                            </tr>
                                                        ))
                                                    ) : (
                                                        <tr>
                                                            <td colSpan="6" className="text-center py-4">
                                                                <i className="bi bi-inbox text-muted" style={{ fontSize: '3rem' }}></i>
                                                                <p className="text-muted mt-2">No users found</p>
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
                                    </>
                                )}
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

            {/* Change Role Modal */}
            <Modal show={showRoleModal} onHide={() => setShowRoleModal(false)} size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>Change User Role</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {selectedUser && (
                        <>
                            <div className="alert alert-info">
                                <strong>User:</strong> {selectedUser.username}<br />
                                <strong>Current Role:</strong> {getRoleBadge(selectedUser.role)}
                            </div>
                            <Form.Group className="mb-3">
                                <Form.Label>Select New Role *</Form.Label>
                                <Form.Select
                                    value={newRole}
                                    onChange={(e) => setNewRole(e.target.value)}
                                >
                                    {roles.map(role => (
                                        <option key={role.value} value={role.value}>
                                            {role.label}
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>

                            {/* Show module access for selected role */}
                            {newRole && (
                                <Card className="bg-light border-0">
                                    <Card.Body>
                                        <h6 className="mb-2">Module Access:</h6>
                                        <p className="mb-0">
                                            <strong>{roles.find(r => r.value === newRole)?.modules}</strong>
                                        </p>
                                        <small className="text-muted">
                                            {roles.find(r => r.value === newRole)?.description}
                                        </small>
                                    </Card.Body>
                                </Card>
                            )}
                        </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowRoleModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="primary" onClick={confirmRoleChange}>
                        <i className="bi bi-check-circle me-2"></i>
                        Update Role
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
};

export default UserList;
