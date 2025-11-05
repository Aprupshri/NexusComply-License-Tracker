import api from './api';

const authService = {
    login: async (username, password) => {
        const response = await api.post('/auth/login', {
            username,
            password
        });
        
        if (response.data.token) {
            localStorage.setItem('token', response.data.token);
            localStorage.setItem('user', JSON.stringify({
                username: response.data.username,
                email: response.data.email,
                role: response.data.role,
                region: response.data.region, 
                fullName: response.data.fullName,
                passwordChangeRequired: response.data.passwordChangeRequired
            }));
        }
        
        return response.data;
    },

    logout: () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },

    getCurrentUser: () => {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },

    isAuthenticated: () => {
        return !!localStorage.getItem('token');
    },

    changePassword: async (currentPassword, newPassword, confirmPassword) => {
        const response = await api.post('/auth/change-password', {
            currentPassword,
            newPassword,
            confirmPassword
        });
        return response.data;
    },

    updateUserInStorage: (userData) => {
        const currentUser = authService.getCurrentUser();
        const updatedUser = { ...currentUser, ...userData };
        localStorage.setItem('user', JSON.stringify(updatedUser));
    },

    // NEW: Forgot password methods
    forgotPassword: async (email) => {
        const response = await api.post('/auth/forgot-password', { email });
        return response.data;
    },

    validateResetToken: async (token) => {
        const response = await api.get(`/auth/validate-reset-token/${token}`);
        return response.data;
    },

    resetPassword: async (token, newPassword, confirmPassword) => {
        const response = await api.post('/auth/reset-password', {
            token,
            newPassword,
            confirmPassword
        });
        return response.data;
    }
};

export default authService;
