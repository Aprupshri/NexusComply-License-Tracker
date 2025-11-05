import api from './api';

const softwareVersionService = {
    getAllSoftwareVersions: async (page = 0, size = 10, sortBy = 'lastChecked') => {
        const response = await api.get(`/software-versions?page=${page}&size=${size}&sortBy=${sortBy}`);
        return response.data;
    },

    getSoftwareVersionById: async (id) => {
        const response = await api.get(`/software-versions/${id}`);
        return response.data;
    },

    getSoftwareVersionsByDevice: async (deviceId) => {
        const response = await api.get(`/software-versions/by-device/${deviceId}`);
        return response.data;
    },

    getSoftwareVersionsByStatus: async (status) => {
        const response = await api.get(`/software-versions/by-status/${status}`);
        return response.data;
    },

    getStatistics: async () => {
        const response = await api.get('/software-versions/statistics');
        return response.data;
    },

    createSoftwareVersion: async (data) => {
        const response = await api.post('/software-versions', data);
        return response.data;
    },

    updateSoftwareVersion: async (id, data) => {
        const response = await api.put(`/software-versions/${id}`, data);
        return response.data;
    },

    checkForUpdates: async (id) => {
        const response = await api.post(`/software-versions/${id}/check-updates`);
        return response.data;
    },

    deleteSoftwareVersion: async (id) => {
        const response = await api.delete(`/software-versions/${id}`);
        return response.data;
    }
};

export default softwareVersionService;
