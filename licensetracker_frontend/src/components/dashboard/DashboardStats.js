// src/components/dashboard/DashboardStats.jsx
import React from 'react';
import { Row, Col } from 'react-bootstrap';
import StatsCard from './StatsCard';

const DashboardStats = ({ stats, loading }) => {
    return (
        <Row className="mb-4">
            <Col md={6} lg={3} className="mb-3">
                <StatsCard
                    title="Total Devices"
                    value={stats?.totalDevices || 0}
                    icon="bi-hdd-network"
                    bgColor="bg-primary bg-opacity-10"
                    textColor="text-primary"
                    loading={loading}
                    onClick="/devices"
                    subtitle={`${stats?.activeDevices || 0} active`}
                />
            </Col>
            
            <Col md={6} lg={3} className="mb-3">
                <StatsCard
                    title="Total Licenses"
                    value={stats?.totalLicenses || 0}
                    icon="bi-key"
                    bgColor="bg-success bg-opacity-10"
                    textColor="text-success"
                    loading={loading}
                    onClick="/licenses"
                    subtitle={`${stats?.expiringLicenses || 0} expiring soon`}
                />
            </Col>
            
            <Col md={6} lg={3} className="mb-3">
                <StatsCard
                    title="Active Assignments"
                    value={stats?.activeAssignments || 0}
                    icon="bi-link-45deg"
                    bgColor="bg-info bg-opacity-10"
                    textColor="text-info"
                    loading={loading}
                    onClick="/assignments"
                    subtitle={`of ${stats?.totalAssignments || 0} total`}
                />
            </Col>
            
            <Col md={6} lg={3} className="mb-3">
                <StatsCard
                    title="Unread Alerts"
                    value={stats?.unacknowledgedAlerts || 0}
                    icon="bi-bell"
                    bgColor="bg-warning bg-opacity-10"
                    textColor="text-warning"
                    loading={loading}
                    onClick="/alerts"
                    subtitle={`${stats?.criticalAlerts || 0} critical`}
                />
            </Col>
        </Row>
    );
};

export default DashboardStats;
