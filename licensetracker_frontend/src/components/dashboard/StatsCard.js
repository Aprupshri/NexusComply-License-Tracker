// src/components/dashboard/StatsCard.jsx
import React from 'react';
import { Card, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';

const StatsCard = ({ 
    title, 
    value, 
    icon, 
    bgColor, 
    textColor, 
    loading = false,
    onClick = null,
    subtitle = null,
    trend = null
}) => {
    const navigate = useNavigate();

    const handleClick = () => {
        if (onClick) {
            if (typeof onClick === 'string') {
                navigate(onClick);
            } else {
                onClick();
            }
        }
    };

    return (
        <Card 
            className={`shadow-sm border-0 ${onClick ? 'cursor-pointer hover-card' : ''}`}
            onClick={handleClick}
            style={{ 
                cursor: onClick ? 'pointer' : 'default',
                transition: 'transform 0.2s, box-shadow 0.2s'
            }}
            onMouseEnter={(e) => {
                if (onClick) {
                    e.currentTarget.style.transform = 'translateY(-5px)';
                    e.currentTarget.style.boxShadow = '0 4px 20px rgba(0,0,0,0.1)';
                }
            }}
            onMouseLeave={(e) => {
                if (onClick) {
                    e.currentTarget.style.transform = 'translateY(0)';
                    e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
                }
            }}
        >
            <Card.Body>
                <div className="d-flex justify-content-between align-items-center">
                    <div className="flex-grow-1">
                        <p className="text-muted mb-1 small">{title}</p>
                        {loading ? (
                            <Spinner animation="border" size="sm" />
                        ) : (
                            <>
                                <h3 className="mb-0 fw-bold">{value}</h3>
                                {subtitle && (
                                    <small className="text-muted">{subtitle}</small>
                                )}
                                {trend && (
                                    <div className="mt-2">
                                        <small className={`badge bg-${trend.type === 'up' ? 'success' : 'danger'}`}>
                                            <i className={`bi bi-arrow-${trend.type}`}></i> {trend.value}
                                        </small>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                    <div className={`${bgColor} p-3 rounded`}>
                        <i className={`bi ${icon} ${textColor}`} style={{ fontSize: '2rem' }}></i>
                    </div>
                </div>
            </Card.Body>
        </Card>
    );
};

export default StatsCard;
