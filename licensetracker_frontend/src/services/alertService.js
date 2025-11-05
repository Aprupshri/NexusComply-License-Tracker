import api from './api';

const alertService = {
    getAllAlerts: async (page = 0, size = 10, sortBy = 'generatedAt') => {
        const response = await api.get(`/alerts?page=${page}&size=${size}&sortBy=${sortBy}`);
        return response.data;
    },

    // NEW: Get alerts expiring in X days
    getAlertsExpiringInDays: async (days = 30) => {
        const response = await api.get(`/alerts?days=${days}`);
        return response.data;
    },

    getAlertById: async (id) => {
        const response = await api.get(`/alerts/${id}`);
        return response.data;
    },

    getUnacknowledgedAlerts: async () => {
        const response = await api.get('/alerts/unacknowledged');
        return response.data;
    },

    getAlertsBySeverity: async (severity) => {
        const response = await api.get(`/alerts/by-severity/${severity}`);
        return response.data;
    },

    getAlertsByType: async (type) => {
        const response = await api.get(`/alerts/by-type/${type}`);
        return response.data;
    },

    getAlertsByRegion: async (region) => {
        const response = await api.get(`/alerts/by-region/${region}`);
        return response.data;
    },

    getStatistics: async () => {
        const response = await api.get('/alerts/statistics');
        return response.data;
    },

    acknowledgeAlert: async (id, acknowledgedBy) => {
        const response = await api.put(`/alerts/${id}/acknowledge`, { acknowledgedBy });
        return response.data;
    },

    acknowledgeAllAlerts: async (acknowledgedBy) => {
        const response = await api.put('/alerts/acknowledge-all', { acknowledgedBy });
        return response.data;
    },

    generateLicenseExpiryAlerts: async () => {
        const response = await api.post('/alerts/generate/license-expiry');
        return response.data;
    },

    generateSoftwareVersionAlerts: async () => {
        const response = await api.post('/alerts/generate/software-version');
        return response.data;
    },

    generateCapacityAlerts: async () => {
        const response = await api.post('/alerts/generate/capacity');
        return response.data;
    }
};

export default alertService;
