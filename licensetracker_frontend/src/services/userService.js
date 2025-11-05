// src/services/userService.js
import api from './api';

const userService = {
    /**
     * Get all users with pagination
     */
    getAllUsers: async (page = 0, size = 10, sortBy = 'id') => {
        const response = await api.get(
            `/users?page=${page}&size=${size}&sortBy=${sortBy}`
        );
        return response.data;
    },

    /**
     * Get user by ID
     */
    getUserById: async (id) => {
        const response = await api.get(`/users/${id}`);
        return response.data;
    },

    /**
     * Create new user
     */
    createUser: async (userData) => {
        const response = await api.post('/users', {
            username: userData.username,
            email: userData.email,
            password: userData.password,
            fullName: userData.fullName,
            role: userData.role,
            region: userData.region
        });
        return response.data;
    },

    /**
     * Assign role to user
     */
    assignRole: async (userId, role) => {
        const response = await api.put(`/users/${userId}/assign-role`, { role });
        return response.data;
    },

    /**
     * Activate user
     */
    activateUser: async (userId) => {
        const response = await api.put(`/users/${userId}/activate`);
        return response.data;
    },

    /**
     * Deactivate user
     */
    deactivateUser: async (userId) => {
        const response = await api.put(`/users/${userId}/deactivate`);
        return response.data;
    },

    /**
     * Update user
     */
    updateUser: async (userId, userData) => {
        const response = await api.put(`/users/${userId}`, userData);
        return response.data;
    },

    /**
     * Get users by region
     */
    getUsersByRegion: async (region, page = 0, size = 10) => {
        const response = await api.get(
            `/users/region/${region}?page=${page}&size=${size}`
        );
        return response.data;
    }
};

export default userService;
