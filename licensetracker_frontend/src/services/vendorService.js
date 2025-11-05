// src/services/vendorService.js
import api from './api';

const vendorService = {
    getAllVendors: async (page = 0, size = 10, sortBy = 'vendorName') => {
        const response = await api.get(`/vendors?page=${page}&size=${size}&sortBy=${sortBy}`);
        return response.data;
    },

    getAllVendorsList: async () => {
        const response = await api.get('/vendors/list');
        return response.data;
    },

    getVendorById: async (id) => {
        const response = await api.get(`/vendors/${id}`);
        return response.data;
    },

    createVendor: async (vendorData) => {
        const response = await api.post('/vendors', vendorData);
        return response.data;
    },

    updateVendor: async (id, vendorData) => {
        const response = await api.put(`/vendors/${id}`, vendorData);
        return response.data;
    },

    deleteVendor: async (id) => {
        const response = await api.delete(`/vendors/${id}`);
        return response.data;
    },

    checkVendorNameExists: async (vendorName) => {
        const response = await api.get(`/vendors/exists?vendorName=${vendorName}`);
        return response.data.exists;
    }
};

export default vendorService;
