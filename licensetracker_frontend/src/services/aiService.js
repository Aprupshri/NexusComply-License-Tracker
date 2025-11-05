// src/services/aiService.js
import api from './api';

const aiService = {
    /**
     * Get AI response for a prompt using Gemini
     */
    getAIResponse: async (prompt) => {
        const response = await api.post('/ai/generate', {
            prompt: prompt,
            context: 'license-tracker'
        });
        return response.data;
    },

    /**
     * Get compliance summary
     */
    getComplianceSummary: async (days = 30, region = null) => {
        const params = new URLSearchParams();
        params.append('days', days);
        if (region) params.append('region', region);

        const response = await api.get(`/ai/compliance-summary?${params}`);
        return response.data;
    },

    /**
     * Get renewal forecast
     */
    getRenewalForecast: async (days = 90) => {
        const response = await api.get(`/ai/renewal-forecast?days=${days}`);
        return response.data;
    },

    /**
     * Get training checklist
     */
    getTrainingChecklist: async () => {
        const response = await api.get('/ai/training-checklist');
        return response.data;
    },

    /**
     * Get device lifecycle recommendations
     */
    getDeviceLifecycleRecommendations: async () => {
        const response = await api.get('/ai/device-lifecycle');
        return response.data;
    },

    /**
     * Get software update recommendations
     */
    getSoftwareUpdateRecommendations: async () => {
        const response = await api.get('/ai/software-updates');
        return response.data;
    }
};

export default aiService;
