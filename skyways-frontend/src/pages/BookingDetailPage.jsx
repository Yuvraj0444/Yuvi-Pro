import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { bookingApi } from '../api/bookingApi'
import { paymentApi } from '../api/paymentApi'
import StatusBadge    from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import Alert          from '../components/common/Alert'
import { format, parseISO } from 'date-fns'

function fmtDate(iso) {
  try { return format(parseISO(iso), 'dd MMM yyyy, HH:mm') } catch { return iso || '—' }
}

export default function BookingDetailPage() {
  const { bookingId } = useParams()
  const navigate = useNavigate()
  const [booking,  setBooking]  = useState(null)
  const [payment,  setPayment]  = useState(null)
  const [loading,  setLoading]  = useState(true)
  const [error,    setError]    = useState('')
  const [cancelling, setCancelling] = useState(false)

  useEffect(() => {
    Promise.all([
      bookingApi.getById(bookingId),
      paymentApi.getByBooking(bookingId).catch(() => ({ data: null })),
    ]).then(([bRes, pRes]) => {
      setBooking(bRes.data)
      setPayment(pRes.data)
    }).catch(err => setError(err.response?.data?.message || 'Failed to load booking.'))
      .finally(() => setLoading(false))
  }, [bookingId])

  const handleCancel = async () => {
    if (!window.confirm('Cancel this booking? This action cannot be undone.')) return
    setCancelling(true)
    try {
      await bookingApi.cancel(bookingId)
      setBooking(b => ({ ...b, status: 'CANCELLED' }))
    } catch (err) {
      setError(err.response?.data?.message || 'Cancellation failed.')
    } finally {
      setCancelling(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading booking details…" />

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <button onClick={() => navigate('/bookings')} className="text-sm text-sky-500 hover:text-sky-600 mb-6 flex items-center gap-1">
        ← Back to My Bookings
      </button>

      {error && <Alert message={error} type="error" onClose={() => setError('')} />}

      {booking && (
        <>
          {/* Header */}
          <div className="card mb-4">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs text-gray-400 mb-1">Booking Reference</p>
                <p className="font-mono text-2xl font-bold text-sky-600">{booking.bookingRef}</p>
              </div>
              <StatusBadge status={booking.status} />
            </div>
            <div className="grid grid-cols-2 gap-4 mt-4 text-sm">
              <div>
                <p className="text-gray-400">Created</p>
                <p className="font-medium">{fmtDate(booking.createdAt)}</p>
              </div>
              <div>
                <p className="text-gray-400">Total Amount</p>
                <p className="font-bold text-navy-900 text-base">${parseFloat(booking.totalAmount).toFixed(2)} {booking.currency}</p>
              </div>
            </div>
          </div>

          {/* Passengers */}
          {booking.passengers?.length > 0 && (
            <div className="card mb-4">
              <h2 className="font-bold text-gray-900 mb-4">Passengers ({booking.passengers.length})</h2>
              <div className="space-y-3">
                {booking.passengers.map((p, i) => (
                  <div key={i} className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl">
                    <div className="w-9 h-9 rounded-full bg-sky-100 flex items-center justify-center text-sky-700 font-bold text-sm">
                      {i + 1}
                    </div>
                    <div>
                      <p className="font-semibold text-sm">{p.firstName} {p.lastName}</p>
                      <p className="text-xs text-gray-400">Passport: {p.passportNo} · {p.nationality}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Payment info */}
          {payment && (
            <div className="card mb-4">
              <h2 className="font-bold text-gray-900 mb-4">Payment</h2>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div><p className="text-gray-400">Status</p><StatusBadge status={payment.status} /></div>
                <div><p className="text-gray-400">Amount</p><p className="font-bold">${parseFloat(payment.amount).toFixed(2)}</p></div>
                {payment.stripeChargeId && (
                  <div className="col-span-2">
                    <p className="text-gray-400">Charge ID</p>
                    <p className="font-mono text-xs">{payment.stripeChargeId}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Actions */}
          {!['CANCELLED'].includes(booking.status) && (
            <div className="flex gap-3">
              <button onClick={handleCancel} disabled={cancelling} className="btn-danger">
                {cancelling ? 'Cancelling…' : 'Cancel Booking'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}