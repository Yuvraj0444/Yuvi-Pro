import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { bookingApi } from '../api/bookingApi'
import StatusBadge    from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import Alert          from '../components/common/Alert'
import { format, parseISO } from 'date-fns'

function fmtDate(iso) {
  try { return format(parseISO(iso), 'dd MMM yyyy') } catch { return iso }
}

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [cancelling, setCancelling] = useState(null)

  const load = () => {
    setLoading(true)
    bookingApi.getMyBookings()
      .then(({ data }) => setBookings(Array.isArray(data) ? data : data.content || []))
      .catch(err => setError(err.response?.data?.message || 'Failed to load bookings.'))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const handleCancel = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return
    setCancelling(bookingId)
    try {
      await bookingApi.cancel(bookingId)
      setBookings(b => b.map(bk => bk.bookingId === bookingId ? { ...bk, status: 'CANCELLED' } : bk))
    } catch (err) {
      setError(err.response?.data?.message || 'Cancellation failed.')
    } finally {
      setCancelling(null)
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="page-title">My Bookings</h1>
          <p className="text-gray-500 text-sm mt-0.5">{bookings.length} total booking(s)</p>
        </div>
        <Link to="/flights" className="btn-primary text-sm px-5 py-2.5">
          + New Booking
        </Link>
      </div>

      {error && <Alert message={error} type="error" onClose={() => setError('')} />}
      {loading && <LoadingSpinner label="Loading your bookings…" />}

      {!loading && bookings.length === 0 && !error && (
        <div className="text-center py-20 card">
          <div className="text-6xl mb-4">🎫</div>
          <h3 className="text-xl font-semibold text-gray-700 mb-2">No bookings yet</h3>
          <p className="text-gray-500 mb-6">Search flights and book your first trip with SkyWays.</p>
          <Link to="/flights" className="btn-primary inline-flex">Search Flights</Link>
        </div>
      )}

      {!loading && bookings.length > 0 && (
        <div className="space-y-4">
          {bookings.map(b => (
            <div key={b.bookingId} className="card hover:shadow-md transition-shadow animate-fade-in">
              <div className="flex flex-col sm:flex-row sm:items-center gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="font-mono font-bold text-sky-600">{b.bookingRef}</span>
                    <StatusBadge status={b.status} />
                  </div>
                  <p className="text-gray-500 text-sm">
                    Booked on {fmtDate(b.createdAt)} &nbsp;·&nbsp;
                    {b.passengers?.length || 1} passenger(s) &nbsp;·&nbsp;
                    <span className="font-semibold text-gray-700">${parseFloat(b.totalAmount).toFixed(2)}</span>
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Link to={`/bookings/${b.bookingId}`} className="btn-secondary text-xs px-4 py-2">
                    Details
                  </Link>
                  {!['CANCELLED', 'CONFIRMED'].includes(b.status) && (
                    <button
                      onClick={() => handleCancel(b.bookingId)}
                      disabled={cancelling === b.bookingId}
                      className="btn-danger text-xs px-4 py-2"
                    >
                      {cancelling === b.bookingId ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}