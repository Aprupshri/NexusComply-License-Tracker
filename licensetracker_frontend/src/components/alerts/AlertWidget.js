import React, { useState, useEffect } from 'react';
import { Card, Badge, ListGroup, Button, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import alertService from '../../services/alertService';

const AlertWidget = () => {
    const [alerts, setAlerts] = useState([]);
    const [stats, setStats] = useState({
        total: 0,
        unacknowledged: 0,
        critical: 0,
        high: 0
    });
    const [loading, setLoading] = useState(true);
    
    const navigate = useNavigate();

    useEffect(() => {
        fetchAlerts();
        fetchStatistics();
    }, []);

    const fetchAlerts = async () => {
        try {
            const data = await alertService.getUnacknowledgedAlerts();
            // Get only first 5 alerts
            setAlerts(data.slice(0, 5));
        } catch (error) {
            console.error('Error fetching alerts:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchStatistics = async () => {
        try {
            const data = await alertService.getStatistics();
            setStats(data);
        } catch (error) {
            console.error('Error fetching statistics:', error);
        }
    };

    const handleAcknowledge = async (alertId) => {
        const user = JSON.parse(localStorage.getItem('user'));
        
        toast.promise(
            alertService.acknowledgeAlert(alertId, user?.username || 'SYSTEM'),
            {
                loading: 'Acknowledging...',
                success: () => {
                    fetchAlerts();
                    fetchStatistics();
                    return 'Alert acknowledged!';
                },
                error: 'Failed to acknowledge alert'
            }
        );
    };

    const getSeverityBadge = (severity) => {
        const colors = {
            CRITICAL: 'danger',
            HIGH: 'warning',
            MEDIUM: 'info',
            LOW: 'secondary'
        };
        const icons = {
            CRITICAL: '⚠️',
            HIGH: '🔴',
            MEDIUM: '🟡',
            LOW: '🔵'
        };
        return (
            <Badge bg={colors[severity] || 'secondary'} className="me-2">
                {icons[severity]} {severity}
            </Badge>
        );
    };

    return (
        <Card className="shadow-sm border-0 h-100">
            <Card.Header className="bg-white border-bottom d-flex justify-content-between align-items-center">
                <div>
                    <h5 className="mb-0">
                        <i className="bi bi-bell me-2"></i>
                        Recent Alerts
                    </h5>
                </div>
                <div>
                    {stats.unacknowledged > 0 && (
                        <Badge bg="danger" pill>
                            {stats.unacknowledged}
                        </Badge>
                    )}
                </div>
            </Card.Header>
            <Card.Body className="p-0">
                {loading ? (
                    <div className="text-center py-4">
                        <Spinner animation="border" size="sm" variant="primary" />
                    </div>
                ) : alerts.length > 0 ? (
                    <>
                        <ListGroup variant="flush">
                            {alerts.map((alert) => (
                                <ListGroup.Item key={alert.id} className="border-start border-3 border-danger">
                                    <div className="d-flex justify-content-between align-items-start">
                                        <div className="flex-grow-1">
                                            <div className="mb-1">
                                                {getSeverityBadge(alert.severity)}
                                                {alert.region && (
                                                    <Badge bg="info">{alert.region}</Badge>
                                                )}
                                            </div>
                                            <p className="mb-1" style={{ fontSize: '0.9rem' }}>
                                                {alert.message}
                                            </p>
                                            <small className="text-muted">
                                                {new Date(alert.generatedAt).toLocaleString()}
                                            </small>
                                        </div>
                                        <Button
                                            variant="outline-success"
                                            size="sm"
                                            onClick={() => handleAcknowledge(alert.id)}
                                        >
                                            <i className="bi bi-check"></i>
                                        </Button>
                                    </div>
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                        <div className="p-3 bg-light text-center">
                            <Button 
                                variant="link" 
                                size="sm"
                                onClick={() => navigate('/alerts')}
                            >
                                View All Alerts <i className="bi bi-arrow-right"></i>
                            </Button>
                        </div>
                    </>
                ) : (
                    <div className="text-center py-4">
                        <i className="bi bi-check-circle text-success" style={{ fontSize: '2rem' }}></i>
                        <p className="text-muted mt-2 mb-0">No unread alerts</p>
                    </div>
                )}
            </Card.Body>
        </Card>
    );
};

export default AlertWidget;
