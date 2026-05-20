import api from './axios'

export const bookingApi = {
  create: (data) =>
    api.post('/api/v1/bookings', data),

  getById: (bookingId) =>
    api.get(`/api/v1/bookings/${bookingId}`),

  getMyBookings: () =>
    api.get('/api/v1/bookings/user'),

  cancel: (bookingId) =>
    api.put(`/api/v1/bookings/${bookingId}/cancel`),
}