import api from './api';

const licenseService = {
    getAllLicenses: async (page = 0, size = 10, sortBy = 'id') => {
        const response = await api.get(`/licenses?page=${page}&size=${size}&sortBy=${sortBy}`);
        return response.data;
    },

    getLicenseById: async (id) => {
        const response = await api.get(`/licenses/${id}`);
        return response.data;
    },

    getLicenseByKey: async (key) => {
        const response = await api.get(`/licenses/by-key/${key}`);
        return response.data;
    },

    createLicense: async (licenseData) => {
        const response = await api.post('/licenses', licenseData);
        return response.data;
    },

    updateLicense: async (id, licenseData) => {
        const response = await api.put(`/licenses/${id}`, licenseData);
        return response.data;
    },

    deleteLicense: async (id) => {
        const response = await api.delete(`/licenses/${id}`);
        return response.data;
    }
};

export default licenseService;
