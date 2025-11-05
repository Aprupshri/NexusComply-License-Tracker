// src/services/auditLogService.js
import api from './api';

const auditLogService = {
    /**
     * Get all audit logs
     */
    getAllAuditLogs: async (page = 0, size = 20) => {
        const response = await api.get(`/audit-logs?page=${page}&size=${size}`);
        return response.data;
    },

    /**
     * Get audit log by ID
     */
    getAuditLogById: async (id) => {
        const response = await api.get(`/audit-logs/${id}`);
        return response.data;
    },

    /**
     * Get audit logs by user
     */
    getAuditLogsByUser: async (userId, page = 0, size = 20) => {
        const response = await api.get(`/audit-logs/user/${userId}?page=${page}&size=${size}`);
        return response.data;
    },

    /**
     * Get audit logs by entity
     */
    getAuditLogsByEntity: async (entityType, entityId, page = 0, size = 20) => {
        const response = await api.get(
            `/audit-logs/entity/${entityType}/${entityId}?page=${page}&size=${size}`
        );
        return response.data;
    },

    /**
     * Filter audit logs
     */
    filterAuditLogs: async (filters, page = 0, size = 20) => {
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', size);
        
        if (filters.entityType) params.append('entityType', filters.entityType);
        if (filters.action) params.append('action', filters.action);
        if (filters.userId) params.append('userId', filters.userId);
        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);

        const response = await api.get(`/audit-logs/filter?${params}`);
        return response.data;
    },

    /**
     * Search audit logs
     */
    searchAuditLogs: async (searchTerm, page = 0, size = 20) => {
        const response = await api.get(
            `/audit-logs/search?searchTerm=${searchTerm}&page=${page}&size=${size}`
        );
        return response.data;
    },

    /**
     * NEW: Search by license key
     */
    searchByLicenseKey: async (licenseKey, page = 0, size = 20) => {
        const response = await api.get(
            `/audit-logs/search/license-key?licenseKey=${encodeURIComponent(licenseKey)}&page=${page}&size=${size}`
        );
        return response.data;
    },

    /**
     * NEW: Search by device ID
     */
    searchByDeviceId: async (deviceId, page = 0, size = 20) => {
        const response = await api.get(
            `/audit-logs/search/device-id?deviceId=${encodeURIComponent(deviceId)}&page=${page}&size=${size}`
        );
        return response.data;
    },

    /**
     * NEW: Advanced search with all filters
     */
    advancedSearch: async (filters, page = 0, size = 20) => {
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', size);
        
        if (filters.entityType) params.append('entityType', filters.entityType);
        if (filters.action) params.append('action', filters.action);
        if (filters.username) params.append('username', filters.username);
        if (filters.licenseKey) params.append('licenseKey', filters.licenseKey);
        if (filters.deviceId) params.append('deviceId', filters.deviceId);
        if (filters.startDate) params.append('startDate', filters.startDate);
        if (filters.endDate) params.append('endDate', filters.endDate);

        const response = await api.get(`/audit-logs/search/advanced?${params}`);
        return response.data;
    }
};

export default auditLogService;
