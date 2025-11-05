// src/components/dashboard/QuickStats.jsx
import React from 'react';
import { Row, Col, Card, ProgressBar } from 'react-bootstrap';

const QuickStats = ({ stats, loading }) => {
    const getUtilizationColor = (percentage) => {
        if (percentage >= 90) return 'danger';
        if (percentage >= 75) return 'warning';
        return 'success';
    };

    return (
        <Row className="mb-4">
            <Col lg={4} className="mb-3">
                <Card className="shadow-sm border-0 h-100">
                    <Card.Body>
                        <h6 className="mb-3">
                            <i className="bi bi-graph-up text-primary me-2"></i>
                            License Utilization
                        </h6>
                        {!loading && stats ? (
                            <>
                                <div className="mb-3">
                                    <div className="d-flex justify-content-between mb-2">
                                        <span>Average Usage</span>
                                        <strong>{stats.averageLicenseUtilization}%</strong>
                                    </div>
                                    <ProgressBar 
                                        now={stats.averageLicenseUtilization} 
                                        variant={getUtilizationColor(stats.averageLicenseUtilization)}
                                    />
                                </div>
                                <small className="text-muted">
                                    {stats.activeLicenses} active licenses across all regions
                                </small>
                            </>
                        ) : (
                            <div className="text-center py-3">
                                <div className="spinner-border spinner-border-sm" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                            </div>
                        )}
                    </Card.Body>
                </Card>
            </Col>

            <Col lg={4} className="mb-3">
                <Card className="shadow-sm border-0 h-100">
                    <Card.Body>
                        <h6 className="mb-3">
                            <i className="bi bi-exclamation-triangle text-warning me-2"></i>
                            Compliance Status
                        </h6>
                        {!loading && stats ? (
                            <>
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <div>
                                        <div className="text-muted small">Devices without licenses</div>
                                        <h4 className="mb-0 text-danger">{stats.devicesWithoutLicenses}</h4>
                                    </div>
                                    <div className="text-end">
                                        <div className="text-muted small">Total devices</div>
                                        <h4 className="mb-0">{stats.totalDevices}</h4>
                                    </div>
                                </div>
                                {stats.devicesWithoutLicenses > 0 && (
                                    <small className="text-danger">
                                        <i className="bi bi-info-circle me-1"></i>
                                        Action required
                                    </small>
                                )}
                            </>
                        ) : (
                            <div className="text-center py-3">
                                <div className="spinner-border spinner-border-sm" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                            </div>
                        )}
                    </Card.Body>
                </Card>
            </Col>

            <Col lg={4} className="mb-3">
                <Card className="shadow-sm border-0 h-100">
                    <Card.Body>
                        <h6 className="mb-3">
                            <i className="bi bi-building text-success me-2"></i>
                            Vendor Overview
                        </h6>
                        {!loading && stats ? (
                            <>
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <div>
                                        <div className="text-muted small">Total Vendors</div>
                                        <h4 className="mb-0">{stats.totalVendors}</h4>
                                    </div>
                                    <div className="text-end">
                                        <div className="text-muted small">Active Licenses</div>
                                        <h4 className="mb-0">{stats.activeLicenses}</h4>
                                    </div>
                                </div>
                                <small className="text-muted">
                                    Managing {stats.totalLicenses} total licenses
                                </small>
                            </>
                        ) : (
                            <div className="text-center py-3">
                                <div className="spinner-border spinner-border-sm" role="status">
                                    <span className="visually-hidden">Loading...</span>
                                </div>
                            </div>
                        )}
                    </Card.Body>
                </Card>
            </Col>
        </Row>
    );
};

export default QuickStats;
