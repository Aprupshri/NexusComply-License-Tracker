import api from './api';

const assignmentService = {
    assignLicense: async (deviceId, licenseId, assignedBy) => {
        const response = await api.post('/assignments', {
            deviceId,
            licenseId,
            assignedBy
        });
        return response.data;
    },

    revokeAssignment: async (assignmentId, revokedBy, revocationReason) => {
        const response = await api.post(`/assignments/${assignmentId}/revoke`, {
            revokedBy,
            revocationReason
        });
        return response.data;
    },

    getActiveAssignmentsByDevice: async (deviceId) => {
        const response = await api.get(`/assignments/by-device/${deviceId}`);
        return response.data;
    },

    getAllAssignmentsByDevice: async (deviceId) => {
        const response = await api.get(`/assignments/by-device/${deviceId}/all`);
        return response.data;
    },

    getActiveAssignmentsByLicense: async (licenseId) => {
        const response = await api.get(`/assignments/by-license/${licenseId}`);
        return response.data;
    },

    getAllAssignmentsByLicense: async (licenseId) => {
        const response = await api.get(`/assignments/by-license/${licenseId}/all`);
        return response.data;
    },

    getAllActiveAssignments: async () => {
        const response = await api.get('/assignments');
        return response.data;
    },

    getAssignmentById: async (id) => {
        const response = await api.get(`/assignments/${id}`);
        return response.data;
    }
};

export default assignmentService;
