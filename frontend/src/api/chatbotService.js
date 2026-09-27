import api from './axios';

export const chatbotService = {
  sendMessage(message, history = []) {
    return api.post('/chatbot/message', {
      message,
      history: history.slice(-10).map((item) => ({
        role: item.role,
        content: item.content,
      })),
    });
  },
};
