import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import softwareVersionService from '../../services/softwareVersionService';
import deviceService from '../../services/deviceService';

const SoftwareVersionForm = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [formData, setFormData] = useState({
        deviceId: '',
        softwareName: '',
        currentVersion: '',
        latestVersion: '',
        notes: '',
        updateUrl: '',
        releaseDate: ''
    });
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(false);
    const [loadingDevices, setLoadingDevices] = useState(false);
    
    const navigate = useNavigate();
    const { id } = useParams();
    const isEditMode = !!id;

    useEffect(() => {
        fetchDevices();
        if (isEditMode) {
            fetchVersion();
        }
    }, [id]);

    const fetchDevices = async () => {
        setLoadingDevices(true);
        try {
            const response = await deviceService.getAllDevices(0, 1000);
            setDevices(response.content);
        } catch (error) {
            console.error('Error fetching devices:', error);
            toast.error('Failed to fetch devices');
        } finally {
            setLoadingDevices(false);
        }
    };

    const fetchVersion = async () => {
        setLoading(true);
        try {
            const data = await softwareVersionService.getSoftwareVersionById(id);
            setFormData({
                deviceId: data.deviceId,
                softwareName: data.softwareName,
                currentVersion: data.currentVersion,
                latestVersion: data.latestVersion || '',
                notes: data.notes || '',
                updateUrl: data.updateUrl || '',
                releaseDate: data.releaseDate || ''
            });
        } catch (error) {
            console.error('Error fetching version:', error);
            toast.error('Failed to fetch software version details');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({
            ...formData,
            [name]: value
        });

        if (name === 'deviceId' && value) {
            const selectedDevice = devices.find(d => d.id === parseInt(value));
            if (selectedDevice && selectedDevice.softwareName && !formData.softwareName) {
                setFormData(prev => ({
                    ...prev,
                    deviceId: value,
                    softwareName: selectedDevice.softwareName,
                    currentVersion: selectedDevice.softwareVersion || prev.currentVersion
                }));
            }
        }
    };

    const validateForm = () => {
        if (!formData.deviceId) {
            toast.error('Please select a device');
            return false;
        }
        if (!formData.softwareName.trim()) {
            toast.error('Software name is required');
            return false;
        }
        if (!formData.currentVersion.trim()) {
            toast.error('Current version is required');
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
            ...formData,
            deviceId: parseInt(formData.deviceId)
        };

        const promise = isEditMode
            ? softwareVersionService.updateSoftwareVersion(id, payload)
            : softwareVersionService.createSoftwareVersion(payload);

        toast.promise(promise, {
            loading: isEditMode ? 'Updating...' : 'Creating...',
            success: () => {
                setTimeout(() => {
                    navigate('/software-versions');
                }, 1000);
                return isEditMode 
                    ? 'Software version updated successfully!'
                    : 'Software version created successfully!';
            },
            error: (err) => {
                setLoading(false);
                return err.response?.data?.message || `Failed to ${isEditMode ? 'update' : 'create'} software version`;
            }
        });
    };

    if (loading && isEditMode) {
        return (
            <>
                <NavigationBar onShowPasswordModal={() => setShowPasswordModal(true)} />
                <div className="d-flex">
                    <Sidebar />
                    <div className="flex-grow-1 p-4">
                        <div className="text-center py-5">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-3">Loading software version details...</p>
                        </div>
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
                                    <i className="bi bi-cpu me-2"></i>
                                    {isEditMode ? 'Edit Software Version' : 'Add Software Version'}
                                </h2>
                            </Col>
                            <Col xs="auto" className="text-end">
                                <Button 
                                    variant="outline-secondary"
                                    onClick={() => navigate('/software-versions')}
                                >
                                    <i className="bi bi-arrow-left me-2"></i>
                                    Back to List
                                </Button>
                            </Col>
                        </Row>

                        <Card className="shadow-sm border-0">
                            <Card.Body className="p-4">
                                <Form onSubmit={handleSubmit}>
                                    <Row>
                                        <Col md={12} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Device *</Form.Label>
                                                <Form.Select
                                                    name="deviceId"
                                                    value={formData.deviceId}
                                                    onChange={handleChange}
                                                    required
                                                    disabled={isEditMode || loadingDevices}
                                                >
                                                    <option value="">-- Select Device --</option>
                                                    {devices.map(device => (
                                                        <option key={device.id} value={device.id}>
                                                            {device.deviceId} - {device.deviceType} ({device.location})
                                                        </option>
                                                    ))}
                                                </Form.Select>
                                                {isEditMode && (
                                                    <Form.Text className="text-muted">
                                                        Device cannot be changed in edit mode
                                                    </Form.Text>
                                                )}
                                                {loadingDevices && (
                                                    <Form.Text className="text-muted">
                                                        <Spinner size="sm" className="me-2" />
                                                        Loading devices...
                                                    </Form.Text>
                                                )}
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Software Name *</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="softwareName"
                                                    value={formData.softwareName}
                                                    onChange={handleChange}
                                                    placeholder="e.g., IOS XR, PAN-OS, Junos"
                                                    required
                                                />
                                                <Form.Text className="text-muted">
                                                    Name of the software/firmware
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Current Version *</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="currentVersion"
                                                    value={formData.currentVersion}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 9.1.0, 10.2.3"
                                                    required
                                                />
                                                <Form.Text className="text-muted">
                                                    Currently installed version
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <Row>
                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Latest Available Version</Form.Label>
                                                <Form.Control
                                                    type="text"
                                                    name="latestVersion"
                                                    value={formData.latestVersion}
                                                    onChange={handleChange}
                                                    placeholder="e.g., 10.2.0"
                                                />
                                                <Form.Text className="text-muted">
                                                    Latest version available from vendor
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>

                                        <Col md={6} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Release Date</Form.Label>
                                                <Form.Control
                                                    type="date"
                                                    name="releaseDate"
                                                    value={formData.releaseDate}
                                                    onChange={handleChange}
                                                />
                                                <Form.Text className="text-muted">
                                                    Release date of the latest version
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Update URL */}
                                    <Row>
                                        <Col md={12} className="mb-3">
                                            <Form.Group>
                                                <Form.Label>Update/Download URL</Form.Label>
                                                <Form.Control
                                                    type="url"
                                                    name="updateUrl"
                                                    value={formData.updateUrl}
                                                    onChange={handleChange}
                                                    placeholder="https://download.vendor.com/updates/..."
                                                />
                                                <Form.Text className="text-muted">
                                                    Link to download the update
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    {/* Notes */}
                                    <Row>
                                        <Col md={12} className="mb-4">
                                            <Form.Group>
                                                <Form.Label>Notes</Form.Label>
                                                <Form.Control
                                                    as="textarea"
                                                    rows={4}
                                                    name="notes"
                                                    value={formData.notes}
                                                    onChange={handleChange}
                                                    placeholder="Add any additional notes about this version..."
                                                    maxLength={1000}
                                                />
                                                <Form.Text className="text-muted">
                                                    {formData.notes.length}/1000 characters
                                                </Form.Text>
                                            </Form.Group>
                                        </Col>
                                    </Row>

                                    <div className="d-flex flex-column flex-sm-row justify-content-end gap-2">
                                        <Button 
                                            variant="secondary"
                                            onClick={() => navigate('/software-versions')}
                                            disabled={loading}
                                            className="order-2 order-sm-1"
                                        >
                                            Cancel
                                        </Button>
                                        <Button 
                                            variant="primary"
                                            type="submit"
                                            disabled={loading || loadingDevices}
                                            className="order-1 order-sm-2"
                                        >
                                            {loading ? (
                                                <>
                                                    <Spinner size="sm" className="me-2" />
                                                    Saving...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    {isEditMode ? 'Update Version' : 'Add Version'}
                                                </>
                                            )}
                                        </Button>
                                    </div>
                                </Form>
                            </Card.Body>
                        </Card>

                        <Card className="shadow-sm border-0 mt-4">
                            <Card.Body className="bg-light">
                                <h6 className="mb-2">
                                    <i className="bi bi-info-circle me-2"></i>
                                    Important Information
                                </h6>
                                <ul className="mb-0 small">
                                    <li>Software version tracking helps identify outdated systems</li>
                                    <li>System automatically compares current vs latest version</li>
                                    <li>Critical updates are flagged when version is 2+ major versions behind</li>
                                    <li>Use "Check for Updates" to manually refresh version status</li>
                                    <li>Provide update URL for easy access to downloads</li>
                                </ul>
                            </Card.Body>
                        </Card>
                    </Container>
                </div>
            </div>

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

export default SoftwareVersionForm;
