import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import deviceService from '../../services/deviceService';

const DeviceForm = () => {
    const [formData, setFormData] = useState({
        deviceId: '',
        deviceType: 'ROUTER',
        model: '',
        ipAddress: '',
        location: '',
        region: 'BANGALORE',
        lifecycle: 'ACTIVE',
        softwareName: '',
        softwareVersion: '',
        purchasedDate: ''
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    
    const navigate = useNavigate();
    const { id } = useParams();
    const isEditMode = !!id;

    useEffect(() => {
        if (isEditMode) {
            fetchDevice();
        }
    }, [id]);

    const fetchDevice = async () => {
        setLoading(true);
        try {
            const device = await deviceService.getDeviceById(id);
            setFormData({
                deviceId: device.deviceId,
                deviceType: device.deviceType,
                model: device.model,
                ipAddress: device.ipAddress,
                location: device.location,
                region: device.region,
                lifecycle: device.lifecycle,
                softwareName: device.softwareName,
                softwareVersion: device.softwareVersion,
                purchasedDate: device.purchasedDate
            });
        } catch (error) {
            setError('Failed to fetch device details');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setSuccess('');

        try {
            if (isEditMode) {
                await deviceService.updateDevice(id, formData);
                setSuccess('Device updated successfully!');
            } else {
                await deviceService.createDevice(formData);
                setSuccess('Device created successfully!');
            }
            
            setTimeout(() => {
                navigate('/devices');
            }, 1500);
        } catch (error) {
            setError(error.response?.data?.message || 'Failed to save device');
        } finally {
            setLoading(false);
        }
    };

    const [showLifecycleWarning, setShowLifecycleWarning] = useState(false);

    // Add warning check when lifecycle changes
    const handleLifecycleChange = (e) => {
        const newLifecycle = e.target.value;
        
        // Show warning if changing to DECOMMISSIONED or OBSOLETE
        if ((newLifecycle === 'DECOMMISSIONED' || newLifecycle === 'OBSOLETE') && 
            formData.lifecycle !== newLifecycle) {
            setShowLifecycleWarning(true);
        } else {
            setShowLifecycleWarning(false);
        }
        
        setFormData({
            ...formData,
            lifecycle: newLifecycle
        });
    };


    const deviceTypes = [
        { value: 'ROUTER', label: 'Router', icon: 'router' },
        { value: 'SWITCH', label: 'Switch', icon: 'diagram-3' },
        { value: 'FIREWALL', label: 'Firewall', icon: 'shield-check' },
        { value: 'LOAD_BALANCER', label: 'Load Balancer', icon: 'distribute-horizontal' },
        { value: 'SERVER', label: 'Server', icon: 'server' }
    ];

    const regions = ['BANGALORE', 'CHENNAI', 'DELHI', 'MUMBAI', 'HYDERABAD', 'KOLKATA'];
    
    const lifecycles = [
        { value: 'ACTIVE', label: 'Active', variant: 'success' },
        { value: 'MAINTENANCE', label: 'Maintenance', variant: 'warning' },
        { value: 'OBSOLETE', label: 'Obsolete', variant: 'secondary' },
        { value: 'DECOMMISSIONED', label: 'Decommissioned', variant: 'danger' }
    ];

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
                                    {isEditMode ? 'Edit Device' : 'Add New Device'}
                                </h2>
                            </Col>
                            <Col className="text-end">
                                <Button 
                                    variant="outline-secondary"
                                    onClick={() => navigate('/devices')}
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                            </Col>
                        </Row>

                        {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}
                        {success && <Alert variant="success">{success}</Alert>}

                        <Card className="shadow-sm border-0">
                            <Card.Body className="p-4">
                                <Form onSubmit={handleSubmit}>
                                    <Row>
                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Device ID *</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="deviceId"
                                                    value={formData.deviceId}
                                                    onChange={handleChange}
                                                    placeholder="e.g., RTR-BLR-001"
                                                    required
                                                    disabled={isEditMode}
                                                />
                                                <Form.Text className="text-muted">
                                                    Unique identifier for the device (cannot be changed after creation)
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Device Type *</Form.Label>
                                                <Form.Select
                                                    name="deviceType"
                                                    value={formData.deviceType}
                                                    onChange={handleChange}
                                                    required
                                                >
                                                    {deviceTypes.map(type => (
                                                        <option key={type.value} value={type.value}>
                                                            {type.label}
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Row>
                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Model *</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="model"
                                                    value={formData.model}
                                                    onChange={handleChange}
                                                    placeholder="e.g., Cisco ASR 9000"
                                                    required
                                                />
                                            </Form.Group>
                                        </Col>

                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>IP Address</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="ipAddress"
                                                    value={formData.ipAddress}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 192.168.1.1"
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Row>
                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Location</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="location"
                                                    value={formData.location}
                                                    onChange={handleChange}
                                                    placeholder="e.g., Bangalore Data Center - Rack A5"
                                                />
                                            </Form.Group>
                                        </Col>

                                        <Col md={6}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Region *</Form.Label>
                                                <Form.Select
                                                    name="region"
                                                    value={formData.region}
                                                    onChange={handleChange}
                                                    required
                                                >
                                                    {regions.map(region => (
                                                        <option key={region} value={region}>{region}</option>
                                                    ))}
                                                </Form.Select>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Row>
                                        <Col md={4}>
                                        <Form.Group className="mb-3">
    <Form.Label>Lifecycle Status *</Form.Label>
    <Form.Select
        name="lifecycle"
        value={formData.lifecycle}
        onChange={handleLifecycleChange}
        required
    >
        {lifecycles.map(lifecycle => (
            <option key={lifecycle.value} value={lifecycle.value}>
                {lifecycle.label}
            </option>
        ))}
    </Form.Select>
</Form.Group>

{showLifecycleWarning && (
    <Alert variant="warning" className="mb-3">
        <i className="bi bi-exclamation-triangle me-2"></i>
        <strong>Warning:</strong> Changing device status to <strong>{formData.lifecycle}</strong> will 
        automatically revoke all active license assignments for this device.
    </Alert>
)}
                                        </Col>

                                        <Col md={4}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Software Name</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="softwareName"
                                                    value={formData.softwareName}
                                                    onChange={handleChange}
                                                    placeholder="e.g., IOS XR"
                                                />
                                            </Form.Group>
                                        </Col>

                                        <Col md={4}>
                                            <Form.Group className="mb-3">
                                                <Form.Label>Software Version</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="softwareVersion"
                                                    value={formData.softwareVersion}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 7.5.2"
                                                />
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Form.Group className="mb-4">
                                        <Form.Label>Purchased Date</Form.Label>
                                        <Form.Control
                                            type="date"
                                            name="purchasedDate"
                                            value={formData.purchasedDate}
                                            onChange={handleChange}
                                        />
                                    </Form.Group>
                                   

                                    <div className="d-flex justify-content-end gap-2">
                                        <Button 
                                            variant="secondary"
                                            onClick={() => navigate('/devices')}
                                            disabled={loading}
                                        >
                                            Cancel
                                        </Button>
                                        <Button 
                                            variant="primary"
                                            type="submit"
                                            disabled={loading}
                                        >
                                            {loading ? (
                                                <>
                                                    <Spinner size="sm" className="me-2" />
                                                    Saving...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    {isEditMode ? 'Update Device' : 'Create Device'}
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </Form>
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>
        </>
    );
};

export default DeviceForm;
