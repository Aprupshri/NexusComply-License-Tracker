// src/components/common/Sidebar.jsx
import React, { useState, useEffect } from 'react';
import { Nav, Badge, Offcanvas, Button } from 'react-bootstrap';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import alertService from '../../services/alertService';


const Sidebar = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [alertCount, setAlertCount] = useState(0);
    const [collapsed, setCollapsed] = useState(false);
    const [showMobileSidebar, setShowMobileSidebar] = useState(false);
    const { user } = useAuth();


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


    const menuItems = [
        { path: '/dashboard', icon: 'bi-speedometer2', label: 'Dashboard', roles: ['ADMIN','COMPLIANCE_OFFICER','COMPLIANCE_LEAD','PRODUCT_OWNER'] },
        { path: '/devices', icon: 'bi-hdd-network', label: 'Devices', roles: ['ADMIN','NETWORK_ADMIN','OPERATIONS_MANAGER'] },
        { path: '/licenses', icon: 'bi-key', label: 'Licenses', roles: ['ADMIN','NETWORK_ADMIN','PROCUREMENT_OFFICER'] },
        { path: '/vendors', icon: 'bi-building', label: 'Vendors', roles: ['ADMIN','PROCUREMENT_OFFICER','PROCURMENT_LEAD'] },
        { path: '/assignments', icon: 'bi-link-45deg', label: 'Assignments', roles: ['ADMIN','NETWORK_ADMIN'] },
        { path: '/software-versions', icon: 'bi-cpu', label: 'Software Versions', roles: ["ADMIN","OPERATIONS_MANAGER","NETWORK_ENGINEER"] },
        { path: '/alerts', icon: 'bi-bell', label: 'Alerts', badge: alertCount, roles: ["ADMIN", 'NETWORK_ADMIN', 'PROCUREMENT_OFFICER', 'COMPLIANCE_OFFICER', 'OPERATIONS_MANAGER'] },
        { path: '/audit-logs', icon: 'bi-journal-text', label: 'Audit Logs', roles: ['ADMIN', 'SECURITY_HEAD', 'IT_AUDITOR'] },
        { path: '/users', icon: 'bi-people', label: 'Users', roles: ['ADMIN','SECURITY_HEAD'] },
        { path: '/reports', icon: 'bi-file-earmark-bar-graph', label: 'Reports', roles: ['ADMIN','COMPLIANCE_OFFICER','PROCUREMENT_OFFICER','COMPLIANCE_LEAD','PROCUREMENT_LEAD','SECURITY_HEAD'] },
        // ✅ NEW: AI Assistant Menu Item
        { 
            path: '/ai-chat', 
            icon: 'bi-robot', 
            label: 'AI Assistant', 
            badge: 0,
            isBeta: true,
            roles: ['ADMIN', 'COMPLIANCE_OFFICER', 'IT_AUDITOR', 'COMPLIANCE_LEAD', 'PROCUREMENT_LEAD', 'PRODUCT_OWNER'] 
        },
    ];


    const visibleMenuItems = menuItems.filter(item => {
        if (!item.roles || item.roles.length === 0) {
            return true;
        }
        return item.roles.includes(user?.role);
    });


    const handleNavigation = (path) => {
        navigate(path);
        setShowMobileSidebar(false);
    };


    const SidebarContent = ({ isMobile = false }) => (
        <>

            {/* Menu Items */}
            <Nav className="flex-column p-2">
                {visibleMenuItems.map((item) => {
                    const isActive = location.pathname === item.path;
                    return (
                        <div
                            key={item.path}
                            onClick={() => handleNavigation(item.path)}
                            className={`nav-item-custom ${isActive ? 'active' : ''} ${collapsed ? 'collapsed' : ''}`}
                            style={{
                                cursor: 'pointer',
                                borderRadius: '10px',
                                marginBottom: '4px',
                                padding: collapsed ? '12px' : '12px 16px',
                                backgroundColor: isActive ? 'var(--bs-primary)' : 'transparent',
                                color: isActive ? 'white' : 'inherit',
                                transition: 'all 0.3s ease',
                                position: 'relative',
                                overflow: 'hidden'
                            }}
                            onMouseEnter={(e) => {
                                if (!isActive) {
                                    e.currentTarget.style.backgroundColor = 'rgba(13, 110, 253, 0.1)';
                                    e.currentTarget.style.transform = 'translateX(5px)';
                                }
                            }}
                            onMouseLeave={(e) => {
                                if (!isActive) {
                                    e.currentTarget.style.backgroundColor = 'transparent';
                                    e.currentTarget.style.transform = 'translateX(0)';
                                }
                            }}
                        >
                            <div className="d-flex align-items-center justify-content-between">
                                <div className="d-flex align-items-center">
                                    <i
                                        className={`bi ${item.icon}`}
                                        style={{
                                            fontSize: '1.3rem',
                                            marginRight: collapsed ? '0' : '16px',
                                            transition: 'all 0.3s ease'
                                        }}
                                    ></i>
                                    {!collapsed && (
                                        <span style={{ fontSize: '0.95rem', fontWeight: isActive ? '600' : '400' }}>
                                            {item.label}
                                        </span>
                                    )}
                                </div>
                                <div className="d-flex gap-1 align-items-center">
                                    {/* ✅ NEW: Beta Badge for AI Assistant */}
                                    {!collapsed && item.isBeta && (
                                        <Badge
                                            bg={isActive ? 'light' : 'info'}
                                            text={isActive ? 'info' : 'white'}
                                            pill
                                            style={{
                                                fontSize: '0.65rem',
                                                padding: '3px 6px',
                                            }}
                                        >
                                            BETA
                                        </Badge>
                                    )}
                                    {!collapsed && item.badge > 0 && (
                                        <Badge
                                            bg={isActive ? 'light' : 'danger'}
                                            text={isActive ? 'dark' : 'white'}
                                            pill
                                            style={{
                                                fontSize: '0.7rem',
                                                padding: '4px 8px',
                                                animation: 'pulse 2s infinite'
                                            }}
                                        >
                                            {item.badge > 99 ? '99+' : item.badge}
                                        </Badge>
                                    )}
                                </div>
                            </div>


                            {/* Active indicator */}
                            {isActive && (
                                <div
                                    style={{
                                        position: 'absolute',
                                        left: 0,
                                        top: '50%',
                                        transform: 'translateY(-50%)',
                                        width: '4px',
                                        height: '70%',
                                        backgroundColor: 'white',
                                        borderRadius: '0 4px 4px 0'
                                    }}
                                />
                            )}
                        </div>
                    );
                })}
            </Nav>


            <style>{`
                @keyframes pulse {
                    0%, 100% {
                        opacity: 1;
                    }
                    50% {
                        opacity: 0.7;
                    }
                }
            `}</style>
        </>
    );


    return (
        <>
            {/* Desktop Sidebar */}
            <div
                className="d-none d-lg-flex flex-column border-end position-relative"
                style={{
                    width: collapsed ? '80px' : '260px',
                    minHeight: '100vh',
                    backgroundColor: '#ffffff',
                    transition: 'width 0.3s ease',
                    boxShadow: '2px 0 10px rgba(0,0,0,0.05)'
                }}
            >
                {/* Collapse Toggle Button */}
                <Button
                    variant="link"
                    onClick={() => setCollapsed(!collapsed)}
                    className="position-absolute text-primary"
                    style={{
                        top: '10px',
                        right: collapsed ? '10px' : '-15px',
                        width: '30px',
                        height: '30px',
                        borderRadius: '50%',
                        backgroundColor: 'white',
                        border: '2px solid var(--bs-primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        zIndex: 1000,
                        padding: 0,
                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                        transition: 'all 0.3s ease'
                    }}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.transform = 'scale(1.1)';
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.transform = 'scale(1)';
                    }}
                >
                    <i className={`bi bi-chevron-${collapsed ? 'right' : 'left'}`}></i>
                </Button>


                <SidebarContent />
            </div>


            {/* Mobile Menu Button */}
            <Button
                variant="primary"
                className="d-lg-none position-fixed"
                style={{
                    bottom: '20px',
                    right: '20px',
                    width: '60px',
                    height: '60px',
                    borderRadius: '50%',
                    zIndex: 1040,
                    boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                }}
                onClick={() => setShowMobileSidebar(true)}
            >
                <i className="bi bi-list" style={{ fontSize: '1.8rem' }}></i>
            </Button>


            {/* Mobile Offcanvas Sidebar */}
            <Offcanvas
                show={showMobileSidebar}
                onHide={() => setShowMobileSidebar(false)}
                placement="start"
                className="d-lg-none"
                style={{ width: '280px' }}
            >
                <Offcanvas.Header closeButton className="border-bottom">
                    <Offcanvas.Title>
                        <i className="bi bi-grid-3x3-gap-fill me-2"></i>
                        Menu
                    </Offcanvas.Title>
                </Offcanvas.Header>
                <Offcanvas.Body className="p-0">
                    <SidebarContent isMobile={true} />
                </Offcanvas.Body>
            </Offcanvas>
        </>
    );
};


export default Sidebar;
