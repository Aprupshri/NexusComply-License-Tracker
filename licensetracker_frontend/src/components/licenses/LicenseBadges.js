import React from 'react';
import { Badge } from 'react-bootstrap';

export const getLicenseTypeBadge = (type) => {
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

export const isExpiringSoon = (validTo) => {
  const today = new Date();
  const expiryDate = new Date(validTo);
  const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
  return daysUntilExpiry <= 30 && daysUntilExpiry > 0;
};

export const isExpired = (validTo) => new Date(validTo) < new Date();
