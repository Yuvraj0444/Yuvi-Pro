import api from './axios'

export const authApi = {
  register: (data) =>
    api.post('/api/v1/auth/register', data),

  login: (data) =>
    api.post('/api/v1/auth/login', data),

  getProfile: () =>
    api.get('/api/v1/users/profile'),

  updateProfile: (data) =>
    api.put('/api/v1/users/profile', data),
}