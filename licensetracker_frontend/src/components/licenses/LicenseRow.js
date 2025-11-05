import React from 'react';
import RoleBasedAccess from '../common/RoleBasedAccess';
import { Button, Badge, ProgressBar } from 'react-bootstrap';

const LicenseRow = ({ license, onDelete, navigate }) => {
  
  const getLicenseTypeBadge = (type) => {
    const colors = {
      PER_DEVICE: 'primary',
      PER_USER: 'info',
      ENTERPRISE: 'success',
      REGION: 'warning'
    };
    const labels = {
      PER_DEVICE: 'Per Device',
      PER_USER: 'Per User',
      ENTERPRISE: 'Enterprise',
      REGION: 'Region'
    };
    return <Badge bg={colors[type] || 'secondary'}>{labels[type] || type}</Badge>;
  };

  const getUsageVariant = (currentUsage, maxUsage) => {
    const percentage = (currentUsage / maxUsage) * 100;
    if (percentage >= 90) return 'danger';
    if (percentage >= 70) return 'warning';
    return 'success';
  };

  const isExpired = (validTo) => {
    return new Date(validTo) < new Date();
  };

  const isExpiringSoon = (validTo) => {
    const today = new Date();
    const expiryDate = new Date(validTo);
    const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 30 && daysUntilExpiry > 0;
  };

  return (
    <tr>
      {/* License Key */}
      <td>
        <code>{license.licenseKey}</code>
      </td>

      {/* Software Name */}
      <td>{license.softwareName}</td>

      {/* Type */}
      <td>{getLicenseTypeBadge(license.licenseType)}</td>

      {/* Vendor */}
      <td>
        {license.vendorName ? (
          <Badge bg="secondary" className="text-light">
            <i className="bi bi-building me-1"></i>
            {license.vendorName}
          </Badge>
        ) : (
          <span className="text-muted">-</span>
        )}
      </td>

      {/* Usage with Progress Bar */}
      <td>
        <div style={{ minWidth: '120px' }}>
          <div className="d-flex justify-content-between mb-1">
            <small>{license.currentUsage}/{license.maxUsage}</small>
            <small>{((license.currentUsage / license.maxUsage) * 100).toFixed(0)}%</small>
          </div>
          <ProgressBar 
            variant={getUsageVariant(license.currentUsage, license.maxUsage)}
            now={(license.currentUsage / license.maxUsage) * 100}
            style={{ height: '8px' }}
          />
        </div>
      </td>

      {/* Valid From */}
      <td>{new Date(license.validFrom).toLocaleDateString()}</td>

      {/* Valid To */}
      <td>
        {new Date(license.validTo).toLocaleDateString()}
        {isExpired(license.validTo) && (
          <Badge bg="danger" className="ms-2">Expired</Badge>
        )}
        {isExpiringSoon(license.validTo) && !isExpired(license.validTo) && (
          <Badge bg="warning" className="ms-2">Expiring Soon</Badge>
        )}
      </td>

      {/* Region */}
      <td>
        <Badge bg="info">{license.region}</Badge>
      </td>

      {/* Cost */}
      <td>
        {license.cost ? (
          <span className="fw-bold">₹{parseFloat(license.cost).toLocaleString()}</span>
        ) : (
          <span className="text-muted">-</span>
        )}
      </td>

      {/* Status */}
      <td>
        {license.active && !isExpired(license.validTo) ? (
          <Badge bg="success">
            <i className="bi bi-check-circle me-1"></i>
            Active
          </Badge>
        ) : isExpired(license.validTo) ? (
          <Badge bg="danger">
            <i className="bi bi-x-circle me-1"></i>
            Expired
          </Badge>
        ) : (
          <Badge bg="secondary">
            <i className="bi bi-dash-circle me-1"></i>
            Inactive
          </Badge>
        )}
      </td>

      {/* Actions */}
      <td>
      <RoleBasedAccess allowedRoles={['ADMIN', 'PROCUREMENT_OFFICER']}>
        <Button
          variant="outline-primary"
          size="sm"
          className="me-1 m-2"
          onClick={() => navigate(`/licenses/edit/${license.id}`)}
          title="Edit"
        >
          Edit&nbsp;
          <i className="bi bi-pencil"></i>
        </Button>
        <Button
          variant="outline-danger"
          size="sm"
          className='m-2'
          onClick={() => onDelete(license.id)}
          title="Delete"
        >
          Delete&nbsp;
          <i className="bi bi-trash"></i>
        </Button>
        </RoleBasedAccess>
        <Button
          variant="outline-info"
          size="sm"
          className="me-1 m-2"
          onClick={() => navigate(`/licenses/${license.id}`)}
          title="View Details"
        >
          View Details&nbsp;
          <i className="bi bi-eye"></i>
        </Button>
      </td>
    </tr>
  );
};

export default LicenseRow;
