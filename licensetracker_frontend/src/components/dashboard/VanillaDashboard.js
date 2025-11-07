// src/components/dashboard/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import NavigationBar from '../common/Navbar';
import Sidebar from '../common/Sidebar';
import ChangePasswordModal from '../auth/ChangePasswordModal';
import authService from '../../services/authService';

const VanillaDashboard = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [user, setUser] = useState(null);

    useEffect(() => {
        const currentUser = authService.getCurrentUser();
        setUser(currentUser);

        // Check if password change is required
        if (currentUser?.passwordChangeRequired) {
            setShowPasswordModal(true);
        }
    }, []);

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
                <div className="flex-grow-1 p-4" style={{ 
                    backgroundColor: '#f8f9fa',
                    minHeight: '100vh'
                }}>
                    <Container fluid>
                        {/* Welcome Section */}
                        <Row>
                            <Col>
                                <div className="text-center py-5" style={{ marginTop: '10vh' }}>
                                    <div className="mb-4">
                                        <i className="bi bi-speedometer2" 
                                           style={{ fontSize: '4rem', color: '#0d6efd' }}>
                                        </i>
                                    </div>
                                    <h1 className="fw-bold mb-3" style={{ fontSize: '2.5rem' }}>
                                        Welcome Back{user?.fullName && `, ${user.fullName}`}!
                                    </h1>
                                    {user?.region && (
                                        <p className="text-muted fs-5">
                                            <i className="bi bi-geo-alt me-2"></i>
                                            {user.region}
                                        </p>
                                    )}
                                    <div className="mt-4">
                                        <p className="text-muted fs-6">
                                            {new Date().toLocaleDateString('en-US', { 
                                                weekday: 'long', 
                                                year: 'numeric', 
                                                month: 'long', 
                                                day: 'numeric' 
                                            })}
                                        </p>
                                    </div>
                                </div>
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

export default VanillaDashboard;
