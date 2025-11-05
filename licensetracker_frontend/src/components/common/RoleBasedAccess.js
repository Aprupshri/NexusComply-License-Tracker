// src/components/common/RoleBasedAccess.jsx
import React from 'react';
import authService from '../../services/authService';

const RoleBasedAccess = ({ allowedRoles, children, fallback = null }) => {
    const user = authService.getCurrentUser();
    
    if (!user) {
        return fallback;
    }
    
    // Check if user's role is in the allowed roles
    const hasAccess = allowedRoles.includes(user.role);
    
    return hasAccess ? children : fallback;
};

export default RoleBasedAccess;
