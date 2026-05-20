import { useEffect, useState } from 'react'
import { useParams, useLocation, Link } from 'react-router-dom'
import { bookingApi } from '../api/bookingApi'
import StatusBadge    from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import { format, parseISO } from 'date-fns'

function fmtTime(iso) {
  try { return format(parseISO(iso), 'HH:mm, dd MMM yyyy') } catch { return iso }
}

export default function BookingConfirmationPage() {
  const { bookingId } = useParams()
  const location = useLocation()
  const [booking, setBooking] = useState(location.state?.booking || null)
  const [flight,  setFlight]  = useState(location.state?.flight  || null)
  const [loading, setLoading] = useState(!booking)

  useEffect(() => {
    if (!booking) {
      bookingApi.getById(bookingId)
        .then(({ data }) => setBooking(data))
        .finally(() => setLoading(false))
    }
  }, [bookingId, booking])

  if (loading) return <LoadingSpinner label="Loading confirmation…" />

  return (
    <div className="max-w-2xl mx-auto px-4 py-16 text-center">
      {/* Success animation */}
      <div className="w-24 h-24 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-6 text-5xl">
        ✅
      </div>
      <h1 className="text-3xl font-bold text-gray-900 mb-2">Booking Confirmed!</h1>
      <p className="text-gray-500 mb-8">
        Your booking is confirmed. A confirmation email has been sent to your registered address.
      </p>

      {/* Booking ref card */}
      <div className="card mb-6 text-left">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-bold text-gray-900 text-lg">Booking Details</h2>
          <StatusBadge status={booking?.status || 'CONFIRMED'} />
        </div>
        <div className="space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-500">Booking Reference</span>
            <span className="font-mono font-bold text-sky-600 text-base">
              {booking?.bookingRef || bookingId}
            </span>
          </div>
          {flight && (
            <>
              <div className="flex justify-between">
                <span className="text-gray-500">Route</span>
                <span className="font-medium">{flight.originIata} → {flight.destinationIata}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Departure</span>
                <span className="font-medium">{fmtTime(flight.departureTime)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Arrival</span>
                <span className="font-medium">{fmtTime(flight.arrivalTime)}</span>
              </div>
            </>
          )}
          <div className="flex justify-between border-t border-gray-100 pt-3 mt-3">
            <span className="text-gray-500">Total Paid</span>
            <span className="font-bold text-navy-900">
              {booking?.currency === 'INR' ? '₹' : '$'}
              {parseFloat(booking?.totalAmount || 0).toLocaleString()}
            </span>
          </div>
          {booking?.passengers?.length > 0 && (
            <div className="flex justify-between">
              <span className="text-gray-500">Passengers</span>
              <span className="font-medium">{booking.passengers.length} passenger(s)</span>
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        <Link to={`/bookings/${bookingId}`} className="btn-primary">
          View Full Details
        </Link>
        <Link to="/bookings" className="btn-secondary">
          All My Bookings
        </Link>
        <Link to="/" className="btn-secondary">
          Back to Home
        </Link>
      </div>

      <div className="mt-8 p-4 bg-sky-50 rounded-xl text-sm text-sky-700 border border-sky-100">
        <p>✉️ A confirmation email with your ticket details has been dispatched via our notification service.</p>
      </div>
    </div>
  )
}