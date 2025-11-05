// src/components/auditlogs/AuditLogDetailModal.jsx
import React from 'react';
import { Modal, Badge, Table, Card } from 'react-bootstrap';

const AuditLogDetailModal = ({ show, onHide, log }) => {
    if (!log) return null;

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleString();
    };

    const parseDetails = (details) => {
        try {
            return JSON.parse(details);
        } catch {
            return details;
        }
    };

    const getActionBadge = (action) => {
        const colors = {
            CREATE: 'success',
            UPDATE: 'info',
            DELETE: 'danger',
            ASSIGN: 'primary',
            UNASSIGN: 'warning',
            ACTIVATE: 'success',
            DEACTIVATE: 'secondary',
            LOGIN: 'info',
            LOGOUT: 'secondary',
            PASSWORD_CHANGE: 'warning',
            ACKNOWLEDGE: 'primary'
        };
        return <Badge bg={colors[action] || 'secondary'}>{action}</Badge>;
    };

    const details = log.details ? parseDetails(log.details) : null;

    return (
        <Modal show={show} onHide={onHide} size="lg">
            <Modal.Header closeButton>
                <Modal.Title>
                    <i className="bi bi-journal-text me-2"></i>
                    Audit Log Details
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <Card className="mb-3">
                    <Card.Header className="bg-light">
                        <strong>General Information</strong>
                    </Card.Header>
                    <Card.Body>
                        <Table borderless size="sm">
                            <tbody>
                                <tr>
                                    <td width="30%" className="text-muted">Log ID:</td>
                                    <td><code>{log.logId}</code></td>
                                </tr>
                                <tr>
                                    <td className="text-muted">Timestamp:</td>
                                    <td>
                                        <i className="bi bi-clock me-2"></i>
                                        {formatTimestamp(log.timestamp)}
                                    </td>
                                </tr>
                                <tr>
                                    <td className="text-muted">Action:</td>
                                    <td>{getActionBadge(log.action)}</td>
                                </tr>
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>

                <Card className="mb-3">
                    <Card.Header className="bg-light">
                        <strong>User Information</strong>
                    </Card.Header>
                    <Card.Body>
                        <Table borderless size="sm">
                            <tbody>
                                <tr>
                                    <td width="30%" className="text-muted">User ID:</td>
                                    <td>{log.userId}</td>
                                </tr>
                                <tr>
                                    <td className="text-muted">Username:</td>
                                    <td>
                                        <i className="bi bi-person-circle text-primary me-2"></i>
                                        <strong>{log.username}</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td className="text-muted">IP Address:</td>
                                    <td>
                                        <i className="bi bi-router me-2"></i>
                                        {log.ipAddress || 'N/A'}
                                    </td>
                                </tr>
                                {log.userAgent && (
                                    <tr>
                                        <td className="text-muted">User Agent:</td>
                                        <td>
                                            <small className="text-muted">{log.userAgent}</small>
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>

                <Card className="mb-3">
                    <Card.Header className="bg-light">
                        <strong>Entity Information</strong>
                    </Card.Header>
                    <Card.Body>
                        <Table borderless size="sm">
                            <tbody>
                                <tr>
                                    <td width="30%" className="text-muted">Entity Type:</td>
                                    <td>
                                        <Badge bg="info">{log.entityType}</Badge>
                                    </td>
                                </tr>
                                <tr>
                                    <td className="text-muted">Entity ID:</td>
                                    <td><code>{log.entityId || 'N/A'}</code></td>
                                </tr>
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>

                {details && (
                    <Card>
                        <Card.Header className="bg-light">
                            <strong>Details</strong>
                        </Card.Header>
                        <Card.Body>
                            {typeof details === 'object' ? (
                                <pre className="bg-light p-3 rounded" style={{ maxHeight: '300px', overflow: 'auto' }}>
                                    {JSON.stringify(details, null, 2)}
                                </pre>
                            ) : (
                                <p>{details}</p>
                            )}
                        </Card.Body>
                    </Card>
                )}
            </Modal.Body>
        </Modal>
    );
};

export default AuditLogDetailModal;
