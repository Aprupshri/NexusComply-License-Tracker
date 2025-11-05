// src/components/admin/UserForm.jsx
import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Spinner, Table } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import userService from '../../services/userService';


const UserForm = () => {
    const [formData, setFormData] = useState({
        username: '',
        password: '',
        confirmPassword: '',
        email: '',
        fullName: '',
        role: 'NETWORK_ENGINEER',
        region: 'BANGALORE'
    });

    const [loading, setLoading] = useState(false);
    
    const navigate = useNavigate();

    // Regions list
    const regions = [
        { value: 'BANGALORE', label: 'Bangalore', icon: 'geo-alt-fill', color: 'primary' },
        { value: 'CHENNAI', label: 'Chennai', icon: 'geo-alt-fill', color: 'success' },
        { value: 'DELHI', label: 'Delhi', icon: 'geo-alt-fill', color: 'danger' },
        { value: 'MUMBAI', label: 'Mumbai', icon: 'geo-alt-fill', color: 'warning' },
        { value: 'HYDERABAD', label: 'Hyderabad', icon: 'geo-alt-fill', color: 'info' },
        { value: 'KOLKATA', label: 'Kolkata', icon: 'geo-alt-fill', color: 'dark' },
    ];

    const roles = [
        { 
            value: 'ADMIN', 
            label: 'Admin', 
            description: 'Full system access',
            modules: 'All Modules',
            icon: 'shield-fill-check',
            color: 'danger'
        },
        { 
            value: 'NETWORK_ADMIN', 
            label: 'Network Admin', 
            description: 'Device and license management',
            modules: 'Devices, Licenses (view), Assignments, Alerts',
            icon: 'hdd-network',
            color: 'primary'
        },
        { 
            value: 'PROCUREMENT_OFFICER', 
            label: 'Procurement Officer', 
            description: 'License and vendor management',
            modules: 'Licenses, Vendors, Reports, Alerts',
            icon: 'cart-check',
            color: 'success'
        },
        { 
            value: 'COMPLIANCE_OFFICER', 
            label: 'Compliance Officer', 
            description: 'Compliance monitoring',
            modules: 'Dashboard, Reports, Alerts, AI Assistant',
            icon: 'clipboard-check',
            color: 'warning'
        },
        { 
            value: 'IT_AUDITOR', 
            label: 'IT Auditor', 
            description: 'Audit and reporting',
            modules: 'Reports, Audit Logs, AI Assistant',
            icon: 'search',
            color: 'info'
        },
        { 
            value: 'OPERATIONS_MANAGER', 
            label: 'Operations Manager', 
            description: 'Device lifecycle management',
            modules: 'Devices lifecycle, Software versions, Alerts',
            icon: 'gear',
            color: 'secondary'
        },
        { 
            value: 'NETWORK_ENGINEER', 
            label: 'Network Engineer', 
            description: 'Software updates and device info',
            modules: 'Software version update, Device info',
            icon: 'tools',
            color: 'dark'
        },
        { 
            value: 'SECURITY_HEAD', 
            label: 'Security Head', 
            description: 'Security and user management',
            modules: 'Audit Logs, Users, Reports',
            icon: 'shield-lock',
            color: 'danger'
        },
        { 
            value: 'COMPLIANCE_LEAD', 
            label: 'Compliance Lead', 
            description: 'Compliance leadership',
            modules: 'Dashboard, Reports, AI Assistant',
            icon: 'award',
            color: 'warning'
        },
        { 
            value: 'PROCUREMENT_LEAD', 
            label: 'Procurement Lead', 
            description: 'Vendor oversight',
            modules: 'Vendors, Reports, AI Assistant',
            icon: 'briefcase',
            color: 'success'
        },
        { 
            value: 'PRODUCT_OWNER', 
            label: 'Product Owner', 
            description: 'Product oversight',
            modules: 'Dashboard, AI Assistant',
            icon: 'person-badge',
            color: 'info'
        }
    ];

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const validateForm = () => {
        if (!formData.username.trim()) {
            toast.error('Username is required');
            return false;
        }

        if (!formData.email.trim()) {
            toast.error('Email is required');
            return false;
        }

        if (!formData.email.includes('@')) {
            toast.error('Please enter a valid email address');
            return false;
        }

        if (!formData.fullName.trim()) {
            toast.error('Full name is required');
            return false;
        }

        if (formData.password !== formData.confirmPassword) {
            toast.error('Passwords do not match');
            return false;
        }

        if (formData.password.length < 6) {
            toast.error('Password must be at least 6 characters long');
            return false;
        }

        if (!formData.role) {
            toast.error('Please select a role');
            return false;
        }

        if (!formData.region) {
            toast.error('Please select a region');
            return false;
        }

        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }

        setLoading(true);

        const payload = {
            username: formData.username.trim(),
            password: formData.password,
            email: formData.email.trim(),
            fullName: formData.fullName.trim(),
            role: formData.role,
            region: formData.region
        };

        toast.promise(
            userService.createUser(payload),
            {
                loading: 'Creating user...',
                success: () => {
                    setTimeout(() => {
                        navigate('/users');
                    }, 1000);
                    return `User ${formData.username} created successfully!`;
                },
                error: (err) => {
                    setLoading(false);
                    return err.response?.data?.message || 'Failed to create user';
                }
            }
        );
    };

    const selectedRole = roles.find(r => r.value === formData.role);
    const selectedRegion = regions.find(r => r.value === formData.region);

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
                                    <i className="bi bi-person-plus me-2"></i>
                                    Add New User
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <Button 
                                    variant="outline-secondary"
                                    onClick={() => navigate('/users')}
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                            </Col>
                        </Row>

                        <Card className="shadow-sm border-0">
                            <Card.Body className="p-4">
                                <Form onSubmit={handleSubmit}>
                                    {/* Username & Email */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Username <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="username"
                                                    value={formData.username}
                                                    onChange={handleChange}
                                                    placeholder="e.g., john.doe"
                                                    required
                                                />
                                                <Form.Text className="text-muted">
                                                    <i className="bi bi-info-circle me-1"></i>
                                                    Used for login, must be unique
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Email <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="email"
                                                    name="email"
                                                    value={formData.email}
                                                    onChange={handleChange}
                                                    placeholder="e.g., john.doe@company.com"
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Full Name */}
                                    <Row>
                                        <Col md={12} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Full Name <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="fullName"
                                                    value={formData.fullName}
                                                    onChange={handleChange}
                                                    placeholder="e.g., John Doe"
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Password */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Password <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="password"
                                                    name="password"
                                                    value={formData.password}
                                                    onChange={handleChange}
                                                    placeholder="Minimum 6 characters"
                                                    required
                                                    minLength={6}
                                                />
                                                <Form.Text className="text-muted">
                                                    <i className="bi bi-shield-check me-1"></i>
                                                    Must be at least 6 characters long
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Confirm Password <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Control
                                                    type="password"
                                                    name="confirmPassword"
                                                    value={formData.confirmPassword}
                                                    onChange={handleChange}
                                                    placeholder="Re-enter password"
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Role & Region */}
                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label className="fw-bold">
                                                    Role <span className="text-danger">*</span>
                                                </Form.Label>
                                                <Form.Select
                                                    name="role"
                                                    value={formData.role}
                                                    onChange={handleChange}
                                                    required
                                                >
                                                    <option value="">-- Select Role --</option>
                                                    {roles.map(role => (
                                                        <option key={role.value} value={role.value}>
                                                            {role.label} - {role.description}
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
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
                                                    <option value="">-- Select Region --</option>
                                                    {regions.map(region => (
                                                        <option key={region.value} value={region.value}>
                                                            {region.label}
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                                <Form.Text className="text-muted">
                                                    <i className="bi bi-geo-alt-fill me-1"></i>
                                                    Geographic region for this user
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Selected Role & Region Info */}
                                    <Row className="mb-4">
                                        {selectedRole && (
                                            <Col md={6} className="mb-3">
                                                <Card className="bg-light border-0">
                                                    <Card.Body>
                                                        <h6 className="mb-2">
                                                            <i className={`bi bi-${selectedRole.icon} text-${selectedRole.color} me-2`}></i>
                                                            {selectedRole.label}
                                                        </h6>
                                                        <p className="mb-1"><strong>Access:</strong> {selectedRole.modules}</p>
                                                        <p className="mb-0 text-muted"><small>{selectedRole.description}</small></p>
                                                    </Card.Body>
                                                </Card>
                                            </Col>
                                        )}

                                        {selectedRegion && (
                                            <Col md={6} className="mb-3">
                                                <Card className="bg-light border-0">
                                                    <Card.Body>
                                                        <h6 className="mb-2">
                                                            <i className={`bi bi-${selectedRegion.icon} text-${selectedRegion.color} me-2`}></i>
                                                            Region: {selectedRegion.label}
                                                        </h6>
                                                        <p className="mb-0 text-muted"><small>User will be assigned to {selectedRegion.label} region</small></p>
                                                    </Card.Body>
                                                </Card>
                                            </Col>
                                        )}
                                    </Row>

                                    {/* Action Buttons */}
                                    <div className="d-flex flex-column flex-sm-row justify-content-end gap-2 pt-3 border-top">
                                        <Button 
                                            variant="secondary"
                                            onClick={() => navigate('/users')}
                                            disabled={loading}
                                            className="order-2 order-sm-1"
                                        >
                                            <i className="bi bi-x-circle me-2"></i>
                                            Cancel
                                        </Button>
                                        <Button 
                                            variant="primary"
                                            type="submit"
                                            disabled={loading}
                                            className="order-1 order-sm-2"
                                        >
                                            {loading ? (
                                                <>
                                                    <Spinner size="sm" className="me-2" animation="border" />
                                                    Creating...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    Create User
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </Form>
                            </Card.Body>
                        </Card>

                        {/* Roles Reference Table */}
                        <Card className="shadow-sm border-0 mt-4">
                            <Card.Header className="bg-white border-bottom">
                                <h6 className="mb-0">
                                    <i className="bi bi-info-circle me-2"></i>
                                    Role Access Matrix
                                </h6>
                            </Card.Header>
                            <Card.Body>
                                <div className="table-responsive">
                                    <Table bordered hover size="sm">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Role</th>
                                                <th>Accessible Modules</th>
                                                <th>Description</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {roles.map(role => (
                                                <tr key={role.value}>
                                                    <td>
                                                        <i className={`bi bi-${role.icon} text-${role.color} me-2`}></i>
                                                        <strong>{role.label}</strong>
                                                    </td>
                                                    <td><small>{role.modules}</small></td>
                                                    <td><small className="text-muted">{role.description}</small></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </Table>
                                </div>
                            </Card.Body>
                        </Card>

                        {/* Regions Reference */}
                        <Card className="shadow-sm border-0 mt-4">
                            <Card.Header className="bg-white border-bottom">
                                <h6 className="mb-0">
                                    <i className="bi bi-geo-alt-fill me-2"></i>
                                    Available Regions
                                </h6>
                            </Card.Header>
                            <Card.Body>
                                <div className="row">
                                    {regions.map(region => (
                                        <div key={region.value} className="col-md-6 mb-2">
                                            <span className={`badge bg-${region.color} me-2`}>
                                                <i className={`bi bi-${region.icon}`}></i>
                                            </span>
                                            <strong>{region.label}</strong> ({region.value})
                                        </div>
                                    ))}
                                </div>
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default UserForm;
