import api from './api';

const reportService = {
    getLicenseReport: async (vendor = '', software = '', region = '', status = '') => {
        const params = new URLSearchParams();
        if (vendor) params.append('vendor', vendor);
        if (software) params.append('software', software);
        if (region) params.append('region', region);
        if (status) params.append('status', status);
        
        const response = await api.get(`/reports/licenses?${params.toString()}`);
        return response.data;
    },

    getDeviceReport: async (deviceType = '', region = '', lifecycle = '') => {
        const params = new URLSearchParams();
        if (deviceType) params.append('deviceType', deviceType);
        if (region) params.append('region', region);
        if (lifecycle) params.append('lifecycle', lifecycle);
        
        const response = await api.get(`/reports/devices?${params.toString()}`);
        return response.data;
    },

    getAssignmentReport: async (region = '', active = null) => {
        const params = new URLSearchParams();
        if (region) params.append('region', region);
        if (active !== null) params.append('active', active);
        
        const response = await api.get(`/reports/assignments?${params.toString()}`);
        return response.data;
    },

    getComplianceReport: async () => {
        const response = await api.get('/reports/compliance');
        return response.data;
    },

    getComplianceReportByRegion: async (region) => {
        const response = await api.get(`/reports/compliance/${region}`);
        return response.data;
    }
};

export default reportService;
