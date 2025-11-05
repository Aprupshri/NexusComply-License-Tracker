// src/components/common/Navbar.jsx
import React, { useState, useEffect } from 'react';
import { Navbar, Container, Nav, NavDropdown, Badge, Offcanvas } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import logo from '../../utils/no-background_1-removebg-preview.png';
import authService from '../../services/authService';
import alertService from '../../services/alertService';
import RoleBasedAccess from './RoleBasedAccess';

const NavigationBar = ({ onShowPasswordModal }) => {
    const navigate = useNavigate();
    const user = authService.getCurrentUser();
    const [alertCount, setAlertCount] = useState(0);
    const [showMobileMenu, setShowMobileMenu] = useState(false);

    useEffect(() => {
        fetchAlertCount();
        const interval = setInterval(fetchAlertCount, 30000);
        return () => clearInterval(interval);
    }, []);

    const fetchAlertCount = async () => {
        try {
            const stats = await alertService.getStatistics();
            setAlertCount(stats.unacknowledged);
        } catch (error) {
            console.error('Error fetching alert count:', error);
        }
    };

    const handleLogout = () => {
        authService.logout();
        toast.success('Logged out successfully');
        navigate('/login');
    };

    const getRoleColor = (role) => {
        const colors = {
            ADMIN: 'danger',
            NETWORK_ADMIN: 'primary',
            PROCUREMENT_OFFICER: 'success',
            COMPLIANCE_OFFICER: 'info',
            OPERATIONS_MANAGER: 'warning',
            SECURITY_HEAD: 'dark',
            NETWORK_ENGINEER: 'secondary',
            IT_AUDITOR: 'info'
        };
        return colors[role] || 'secondary';
    };

    return (
        <>
            <Navbar
                bg="primary"
                variant="dark"
                expand="lg"
                className="shadow-sm sticky-top"
                style={{
                    transition: 'all 0.3s ease',
                    borderBottom: '3px solid rgba(255,255,255,0.1)'
                }}
            >
                <Container fluid className="px-3 px-md-4">
                    {/* Brand */}
                    <Navbar.Brand
                        onClick={() => navigate('/dashboard')}
                        style={{ cursor: 'pointer' }}
                        className="d-flex align-items-center"
                    >
                        <img
                            src={logo}
                            alt="logo"
                            style={{
                                width: '45px',
                                height: '55px',
                                transition: 'transform 0.3s ease'
                            }}
                            onMouseEnter={(e) => e.target.style.transform = 'scale(1.1)'}
                            onMouseLeave={(e) => e.target.style.transform = 'scale(1)'}
                        />
                        <span className="ms-2 fw-bold d-none d-sm-inline">Nexus Comply</span>
                    </Navbar.Brand>

                    {/* Mobile Toggle */}
                    <div className="d-flex align-items-center d-lg-none">
                        {/* Alert Bell - Mobile */}
                        <div
                            className="position-relative me-3"
                            onClick={() => navigate('/alerts')}
                            style={{ cursor: 'pointer' }}
                        >
                            <i className="bi bi-bell text-white" style={{ fontSize: '1.3rem' }}></i>
                            {alertCount > 0 && (
                                <Badge
                                    bg="danger"
                                    pill
                                    className="position-absolute top-0 start-100 translate-middle"
                                    style={{ fontSize: '0.65rem' }}
                                >
                                    {alertCount > 99 ? '99+' : alertCount}
                                </Badge>
                            )}
                        </div>

                        <Navbar.Toggle
                            aria-controls="basic-navbar-nav"
                            onClick={() => setShowMobileMenu(!showMobileMenu)}
                        />
                    </div>

                    {/* Desktop Menu */}
                    <Navbar.Collapse id="basic-navbar-nav" className="d-none d-lg-block">
                        <Nav className="ms-auto align-items-center">
                            {/* Notifications */}
                            <RoleBasedAccess allowedRoles={['ADMIN', 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER']}>
                                <Nav.Link
                                    onClick={() => navigate('/alerts')}
                                    className="position-relative me-3"
                                    style={{ transition: 'transform 0.2s' }}
                                    onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
                                    onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                                >
                                    <i className="bi bi-bell" style={{ fontSize: '1.3rem' }}></i>
                                    {alertCount > 0 && (
                                        <Badge
                                            bg="danger"
                                            pill
                                            className="position-absolute"
                                            style={{
                                                top: '-5px',
                                                right: '-10px',
                                                fontSize: '0.65rem'
                                            }}
                                        >
                                            {alertCount > 99 ? '99+' : alertCount}
                                        </Badge>
                                    )}
                                </Nav.Link>
                            </RoleBasedAccess>

                            {/* User Profile Dropdown */}
                            <NavDropdown
                                title={
                                    <span className="d-flex align-items-center">
                                        <div
                                            className="rounded-circle bg-white d-flex align-items-center justify-content-center me-2"
                                            style={{
                                                width: '35px',
                                                height: '35px',
                                                transition: 'transform 0.2s'
                                            }}
                                        >
                                            <i className="bi bi-person-fill text-primary" style={{ fontSize: '1.2rem' }}></i>
                                        </div>
                                        <span className="d-none d-xl-inline">{user?.fullName || user?.username}</span>
                                    </span>
                                }
                                id="user-dropdown"
                                align="end"
                                className="user-dropdown"
                            >
                                {/* User Info Header */}
                                <div className="px-3 py-2 border-bottom">
                                    <div className="fw-bold">{user?.fullName || user?.username}</div>
                                    <small className="text-muted">{user?.email}</small>
                                    <div className="mt-2">
                                        <Badge bg={getRoleColor(user?.role)} className="me-1">
                                            {user?.role?.replace(/_/g, ' ')}
                                        </Badge>
                                        {user?.region && (
                                            <Badge bg="info">
                                                <i className="bi bi-geo-alt"></i> {user?.region}
                                            </Badge>
                                        )}
                                    </div>
                                </div>

                                {/* Menu Items */}
                                <NavDropdown.Item onClick={() => navigate('/dashboard')}>
                                    <i className="bi bi-speedometer2 me-2"></i>
                                    Dashboard
                                </NavDropdown.Item>

                                <NavDropdown.Item onClick={onShowPasswordModal}>
                                    <i className="bi bi-key me-2"></i>
                                    Change Password
                                </NavDropdown.Item>

                                <NavDropdown.Divider />

                                <NavDropdown.Item
                                    onClick={handleLogout}
                                    className="text-danger"
                                >
                                    <i className="bi bi-box-arrow-right me-2"></i>
                                    Logout
                                </NavDropdown.Item>
                            </NavDropdown>
                        </Nav>
                    </Navbar.Collapse>
                </Container>
            </Navbar>

            {/* Mobile Offcanvas Menu */}
            <Offcanvas
                show={showMobileMenu}
                onHide={() => setShowMobileMenu(false)}
                placement="end"
                className="d-lg-none"
            >
                <Offcanvas.Header closeButton className="border-bottom">
                    <Offcanvas.Title>
                        <div className="d-flex align-items-center">
                            <div
                                className="rounded-circle bg-primary d-flex align-items-center justify-content-center me-2"
                                style={{ width: '40px', height: '40px' }}
                            >
                                <i className="bi bi-person-fill text-white" style={{ fontSize: '1.5rem' }}></i>
                            </div>
                            <div>
                                <div className="fw-bold">{user?.fullName || user?.username}</div>
                                <small className="text-muted">{user?.email}</small>
                            </div>
                        </div>
                    </Offcanvas.Title>
                </Offcanvas.Header>
                <Offcanvas.Body>
                    <div className="mb-3">
                        <Badge bg={getRoleColor(user?.role)} className="me-1">
                            {user?.role?.replace(/_/g, ' ')}
                        </Badge>
                        {user?.region && (
                            <Badge bg="info">
                                <i className="bi bi-geo-alt"></i> {user?.region}
                            </Badge>
                        )}
                    </div>

                    <Nav className="flex-column">
                        <Nav.Link
                            onClick={() => {
                                navigate('/dashboard');
                                setShowMobileMenu(false);
                            }}
                            className="py-3 border-bottom"
                        >
                            <i className="bi bi-speedometer2 me-2"></i>
                            Dashboard
                        </Nav.Link>

                        <Nav.Link
                            onClick={() => {
                                navigate('/alerts');
                                setShowMobileMenu(false);
                            }}
                            className="py-3 border-bottom position-relative"
                        >
                            <i className="bi bi-bell me-2"></i>
                            Alerts
                            {alertCount > 0 && (
                                <Badge bg="danger" pill className="ms-2">
                                    {alertCount}
                                </Badge>
                            )}
                        </Nav.Link>

                        <Nav.Link
                            onClick={() => {
                                onShowPasswordModal();
                                setShowMobileMenu(false);
                            }}
                            className="py-3 border-bottom"
                        >
                            <i className="bi bi-key me-2"></i>
                            Change Password
                        </Nav.Link>

                        <Nav.Link
                            onClick={() => {
                                handleLogout();
                                setShowMobileMenu(false);
                            }}
                            className="py-3 text-danger"
                        >
                            <i className="bi bi-box-arrow-right me-2"></i>
                            Logout
                        </Nav.Link>
                    </Nav>
                </Offcanvas.Body>
            </Offcanvas>

            <style jsx>{`
                .user-dropdown .dropdown-toggle::after {
                    margin-left: 0.5rem;
                }
                
                .user-dropdown .dropdown-menu {
                    min-width: 280px;
                    box-shadow: 0 4px 20px rgba(0,0,0,0.15);
                    border: none;
                    margin-top: 0.5rem;
                }

                .navbar-nav .nav-link:hover {
                    background-color: rgba(255,255,255,0.1);
                    border-radius: 8px;
                }
            `}</style>
        </>
    );
};

export default NavigationBar;
