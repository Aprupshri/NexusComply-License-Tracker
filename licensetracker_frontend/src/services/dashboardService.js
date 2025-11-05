// src/services/dashboardService.js
import api from './api';

const dashboardService = {
    getDashboardStats: async () => {
        const response = await api.get('/dashboard/stats');
        return response.data;
    },

    getDashboardStatsByRegion: async (region) => {
        const response = await api.get(`/dashboard/stats/region/${region}`);
        return response.data;
    }
};

export default dashboardService;
