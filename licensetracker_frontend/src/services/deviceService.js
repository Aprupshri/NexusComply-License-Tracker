import api from './api';

const deviceService = {
    getAllDevices: async (page = 0, size = 10, sortBy = 'id') => {
        const response = await api.get(`/devices?page=${page}&size=${size}&sortBy=${sortBy}`);
        return response.data;
    },

    getDeviceById: async (id) => {
        const response = await api.get(`/devices/${id}`);
        return response.data;
    },

    getDeviceByDeviceId: async (deviceId) => {
        const response = await api.get(`/devices/by-device-id/${deviceId}`);
        return response.data;
    },

    createDevice: async (deviceData) => {
        const response = await api.post('/devices', deviceData);
        return response.data;
    },

    updateDevice: async (id, deviceData) => {
        const response = await api.put(`/devices/${id}`, deviceData);
        return response.data;
    },

    deleteDevice: async (id) => {
        const response = await api.delete(`/devices/${id}`);
        return response.data;
    },

    // Bulk upload methods
    bulkUpload: async (file) => {
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await api.post('/devices/bulk-upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        return response.data;
    },

    downloadTemplate: async () => {
        const response = await api.get('/devices/template', {
            responseType: 'blob'
        });
        return response.data;
    }
};

export default deviceService;
