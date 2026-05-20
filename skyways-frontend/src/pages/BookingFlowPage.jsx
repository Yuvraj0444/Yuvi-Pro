import { useState } from 'react'
import { useParams, useLocation, useNavigate } from 'react-router-dom'
import { bookingApi } from '../api/bookingApi'
import { flightApi }  from '../api/flightApi'
import PassengerForm  from '../components/booking/PassengerForm'
import BookingSummary from '../components/booking/BookingSummary'
import LoadingSpinner from '../components/common/LoadingSpinner'
import Alert          from '../components/common/Alert'
import { useEffect }  from 'react'

export default function BookingFlowPage() {
  const { flightId } = useParams()
  const location  = useLocation()
  const navigate  = useNavigate()

  const [flight, setFlight]     = useState(location.state?.flight || null)
  const [loading, setLoading]   = useState(!flight)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError]       = useState('')

  useEffect(() => {
    if (!flight) {
      flightApi.getById(flightId)
        .then(({ data }) => setFlight(data))
        .catch(() => setError('Failed to load flight details.'))
        .finally(() => setLoading(false))
    }
  }, [flightId, flight])

  const passengers = Number(new URLSearchParams(location.search).get('passengers') || 1)

  const handlePassengerSubmit = async ({ passengers: paxList }) => {
    setSubmitting(true)
    setError('')
    try {
      const currency   = flight?.currency || 'INR'
      const basePrice  = parseFloat(flight?.fareClasses?.[0]?.basePrice ?? flight?.basePrice ?? 0)
      const totalAmount = (basePrice * paxList.length * 1.12).toFixed(2)

      const payload = { flightId, passengers: paxList, totalAmount, currency }
      const resp = await bookingApi.create(payload)
      // API returns { success, data: { bookingId, bookingRef, ... }, timestamp }
      const bookingData = resp.data?.data ?? resp.data
      navigate(`/payment/${bookingData.bookingId}`, {
        state: {
          booking: { ...bookingData, totalAmount, currency },
          flight,
        },
      })
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create booking. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading flight details…" />

  return (
    <div className="max-w-7xl mx-auto px-4 py-10">
      {/* Progress steps */}
      <div className="flex items-center gap-2 mb-8 text-sm">
        <span className="flex items-center gap-1.5 text-sky-600 font-semibold">
          <span className="w-6 h-6 rounded-full bg-sky-500 text-white flex items-center justify-center text-xs">1</span>
          Passenger Details
        </span>
        <div className="h-px flex-1 bg-gray-200" />
        <span className="flex items-center gap-1.5 text-gray-400">
          <span className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs">2</span>
          Payment
        </span>
        <div className="h-px flex-1 bg-gray-200" />
        <span className="flex items-center gap-1.5 text-gray-400">
          <span className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs">3</span>
          Confirmation
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <div className="card">
            <h1 className="page-title mb-6">Passenger Details</h1>
            {error && <Alert message={error} type="error" onClose={() => setError('')} className="mb-4" />}
            {submitting
              ? <LoadingSpinner label="Creating your booking…" />
              : <PassengerForm passengerCount={passengers} onSubmit={handlePassengerSubmit} />
            }
          </div>
        </div>
        <div>
          <BookingSummary flight={flight} passengers={passengers} />
        </div>
      </div>
    </div>
  )
}