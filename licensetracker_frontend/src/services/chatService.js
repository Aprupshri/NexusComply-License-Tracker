// src/services/chatService.js
import api from './api';

const chatService = {
    /**
     * Send message to AI assistant
     */
    sendMessage: async (message) => {
        const response = await api.post('/ai/chat/message', {
            message: message
        });
        return response.data;
    },

    /**
     * Clear chat history
     */
    clearChatHistory: async (chatId) => {
        const response = await api.delete(`/ai/chat/clear/${chatId}`);
        return response.data;
    },

    /**
     * Check AI assistant health
     */
    healthCheck: async () => {
        const response = await api.get('/ai/chat/health');
        return response.data;
    }
};

export default chatService;
