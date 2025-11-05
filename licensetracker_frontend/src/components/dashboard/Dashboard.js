// src/components/dashboard/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import { toast } from 'sonner';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import AlertWidget from '../alerts/AlertWidget';
import DashboardStats from './DashboardStats';
import QuickStats from './QuickStats';
import authService from '../../services/authService';
import dashboardService from '../../services/dashboardService';

const Dashboard = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [user, setUser] = useState(null);
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const currentUser = authService.getCurrentUser();
        setUser(currentUser);

        // Check if password change is required
        if (currentUser?.passwordChangeRequired) {
            setShowPasswordModal(true);
        }

        // Fetch dashboard stats
        fetchDashboardStats();
    }, []);

    const fetchDashboardStats = async () => {
        setLoading(true);
        try {
            const data = await dashboardService.getDashboardStats();
            setStats(data);
        } catch (error) {
            console.error('Error fetching dashboard stats:', error);
            toast.error('Failed to load dashboard statistics');
        } finally {
            setLoading(false);
        }
    };

    const handlePasswordChanged = () => {
        setShowPasswordModal(false);
        const updatedUser = authService.getCurrentUser();
        setUser(updatedUser);
    };

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
                                    <i className="bi bi-speedometer2 me-2"></i>
                                    Dashboard
                                </h2>
                                {user && (
                                    <p className="text-muted">
                                        Welcome back, <strong>{user.fullName || user.username}</strong>!
                                        {user.region && (
                                            <span className="ms-2">
                                                <i className="bi bi-geo-alt"></i> {user.region}
                                            </span>
                                        )}
                                    </p>
                                )}
                            </Col>
                        </Row>

                        {/* Main Statistics Cards */}
                        <DashboardStats stats={stats} loading={loading} />

                        {/* Quick Stats Row */}
                        <QuickStats stats={stats} loading={loading} />

                        {/* Alert Widget and Activity */}
                        <Row className="mb-4">
                            <Col lg={8} className="mb-3">
                                <Card className="shadow-sm border-0 h-100">
                                    <Card.Header className="bg-white border-bottom">
                                        <h5 className="mb-0">
                                            <i className="bi bi-activity me-2"></i>
                                            System Overview
                                        </h5>
                                    </Card.Header>
                                    <Card.Body>
                                        {!loading && stats ? (
                                            <Row>
                                                <Col md={6} className="mb-3">
                                                    <div className="border-start border-primary border-3 ps-3">
                                                        <small className="text-muted">Device Status</small>
                                                        <div className="mt-2">
                                                            <span className="badge bg-success me-2">
                                                                {stats.activeDevices} Active
                                                            </span>
                                                            <span className="badge bg-secondary">
                                                                {stats.totalDevices - stats.activeDevices} Inactive
                                                            </span>
                                                        </div>
                                                    </div>
                                                </Col>
                                                <Col md={6} className="mb-3">
                                                    <div className="border-start border-success border-3 ps-3">
                                                        <small className="text-muted">License Status</small>
                                                        <div className="mt-2">
                                                            <span className="badge bg-success me-2">
                                                                {stats.activeLicenses} Active
                                                            </span>
                                                            <span className="badge bg-warning">
                                                                {stats.expiringLicenses} Expiring
                                                            </span>
                                                        </div>
                                                    </div>
                                                </Col>
                                                <Col md={6} className="mb-3">
                                                    <div className="border-start border-info border-3 ps-3">
                                                        <small className="text-muted">Assignments</small>
                                                        <div className="mt-2">
                                                            <div className="progress" style={{ height: '8px' }}>
                                                                <div
                                                                    className="progress-bar bg-info"
                                                                    style={{
                                                                        width: `${stats.totalAssignments > 0 
                                                                            ? (stats.activeAssignments / stats.totalAssignments) * 100 
                                                                            : 0}%`
                                                                    }}
                                                                ></div>
                                                            </div>
                                                            <small className="text-muted mt-1 d-block">
                                                                {stats.activeAssignments} of {stats.totalAssignments} active
                                                            </small>
                                                        </div>
                                                    </div>
                                                </Col>
                                                <Col md={6} className="mb-3">
                                                    <div className="border-start border-danger border-3 ps-3">
                                                        <small className="text-muted">Alerts</small>
                                                        <div className="mt-2">
                                                            <span className="badge bg-danger me-2">
                                                                {stats.criticalAlerts} Critical
                                                            </span>
                                                            <span className="badge bg-warning">
                                                                {stats.unacknowledgedAlerts - stats.criticalAlerts} Others
                                                            </span>
                                                        </div>
                                                    </div>
                                                </Col>
                                            </Row>
                                        ) : (
                                            <div className="text-center py-5">
                                                <div className="spinner-border text-primary" role="status">
                                                    <span className="visually-hidden">Loading...</span>
                                                </div>
                                            </div>
                                        )}
                                    </Card.Body>
                                </Card>
                            </Col>
                            <Col lg={4} className="mb-3">
                                <AlertWidget />
                            </Col>
                        </Row>
                    </Container>
                </div>
            </div>

            {/* Password Change Modal */}
            <ChangePasswordModal
                show={showPasswordModal}
                onHide={() => setShowPasswordModal(false)}
                onPasswordChanged={handlePasswordChanged}
                isForced={user?.passwordChangeRequired}
            />
        </>
    );
};

export default Dashboard;
