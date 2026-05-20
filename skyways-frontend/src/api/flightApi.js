import api from './axios'

export const flightApi = {
  search: ({ origin, destination, date, passengers = 1, classType = 'ECONOMY' }) =>
    api.get('/api/v1/flights/search', {
      params: { origin, destination, departureDate: date, passengers, cabinClass: classType },
    }),

  getById: (flightId) =>
    api.get(`/api/v1/flights/${flightId}`),

  getSeats: (flightId) =>
    api.get(`/api/v1/flights/${flightId}/seats`),
}