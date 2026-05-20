import api from './axios'

export const paymentApi = {
  getOrder: (bookingId) =>
    api.get(`/api/v1/payments/orders/${bookingId}`),

  verify: (data) =>
    api.post('/api/v1/payments/verify', data),

  getStatus: (bookingId) =>
    api.get(`/api/v1/payments/${bookingId}`),
}