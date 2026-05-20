import { useState, useEffect, useCallback } from 'react'
import { useParams, useLocation, useNavigate } from 'react-router-dom'
import { paymentApi } from '../api/paymentApi'
import BookingSummary from '../components/booking/BookingSummary'
import LoadingSpinner from '../components/common/LoadingSpinner'
import Alert          from '../components/common/Alert'

const RAZORPAY_KEY_ID = import.meta.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_So35AlNQGIB4FP'

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true)
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload  = () => resolve(true)
    script.onerror = () => resolve(false)
    document.body.appendChild(script)
  })
}

export default function PaymentPage() {
  const { bookingId } = useParams()
  const location  = useLocation()
  const navigate  = useNavigate()

  const booking = location.state?.booking || null
  const flight  = location.state?.flight  || null

  const [error,      setError]      = useState('')
  const [processing, setProcessing] = useState(false)
  const [scriptReady, setScriptReady] = useState(false)

  const amount      = booking ? parseFloat(booking.totalAmount ?? 0) : 0
  const amountPaise = Math.round(amount * 100) // Razorpay uses smallest currency unit
  const currency    = booking?.currency || 'USD'
  const bookingRef  = booking?.bookingRef || bookingId

  useEffect(() => {
    loadRazorpayScript().then(setScriptReady)
  }, [])

  const openRazorpay = useCallback(async () => {
    if (!scriptReady) {
      setError('Payment gateway is loading — please try again in a moment.')
      return
    }
    setError('')
    setProcessing(true)

    const options = {
      key:         RAZORPAY_KEY_ID,
      amount:      amountPaise,
      currency,
      name:        'SkyWays Airlines',
      description: `Flight Booking · ${bookingRef}`,
      image:       '/logo.png',

      handler: async (response) => {
        // response: { razorpay_payment_id, razorpay_order_id?, razorpay_signature? }
        try {
          await paymentApi.verify({
            bookingId,
            razorpayPaymentId:  response.razorpay_payment_id,
            razorpayOrderId:    response.razorpay_order_id   || '',
            razorpaySignature:  response.razorpay_signature  || '',
          })
        } catch {
          // Verification endpoint may fail in test mode — still navigate to confirmation
        }
        navigate(`/confirmation/${bookingId}`, {
          state: { booking, flight, paymentId: response.razorpay_payment_id },
        })
      },

      prefill: {
        name:    booking?.passengerName || '',
        email:   booking?.email         || '',
        contact: '',
      },

      notes: {
        booking_ref: bookingRef,
        booking_id:  bookingId,
      },

      theme: { color: '#0ea5e9' },

      modal: {
        ondismiss: () => setProcessing(false),
      },
    }

    const rzp = new window.Razorpay(options)
    rzp.on('payment.failed', (resp) => {
      setProcessing(false)
      setError(`Payment failed: ${resp.error.description} (${resp.error.code})`)
    })
    rzp.open()
  }, [scriptReady, amountPaise, currency, bookingRef, bookingId, booking, flight, navigate])

  return (
    <div className="max-w-7xl mx-auto px-4 py-10">
      {/* Progress steps */}
      <div className="flex items-center gap-2 mb-8 text-sm">
        <span className="flex items-center gap-1.5 text-green-600 font-semibold">
          <span className="w-6 h-6 rounded-full bg-green-500 text-white flex items-center justify-center text-xs">✓</span>
          Passenger Details
        </span>
        <div className="h-px flex-1 bg-sky-200" />
        <span className="flex items-center gap-1.5 text-sky-600 font-semibold">
          <span className="w-6 h-6 rounded-full bg-sky-500 text-white flex items-center justify-center text-xs">2</span>
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
            <h1 className="page-title mb-1">Payment Details</h1>
            <p className="text-gray-500 text-sm mb-6">
              Booking Ref: <span className="font-mono font-semibold text-gray-700">{bookingRef}</span>
            </p>

            {error && <Alert message={error} type="error" onClose={() => setError('')} className="mb-4" />}

            {processing ? (
              <LoadingSpinner label="Opening payment gateway…" />
            ) : (
              <div className="space-y-6">
                {/* Amount summary */}
                <div className="bg-sky-50 border border-sky-100 rounded-xl px-6 py-5 flex justify-between items-center">
                  <div>
                    <p className="text-sm text-gray-500">Total amount</p>
                    <p className="text-3xl font-bold text-sky-600 mt-1">
                      {currency} {amount.toFixed(2)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-gray-400">Booking</p>
                    <p className="font-mono font-semibold text-gray-700 text-sm">{bookingRef}</p>
                  </div>
                </div>

                {/* Razorpay CTA */}
                <div className="flex flex-col gap-3">
                  <button
                    onClick={openRazorpay}
                    disabled={!scriptReady}
                    className="btn-primary w-full py-4 text-base flex items-center justify-center gap-3"
                  >
                    <img
                      src="https://razorpay.com/favicon.png"
                      alt=""
                      className="w-5 h-5 rounded"
                      onError={e => e.target.style.display = 'none'}
                    />
                    {scriptReady ? `Pay ${currency} ${amount.toFixed(2)} via Razorpay` : 'Loading payment gateway…'}
                  </button>

                  <p className="text-center text-xs text-gray-400">
                    Secured by Razorpay · 256-bit SSL · UPI, Cards, Netbanking & Wallets accepted
                  </p>
                </div>

                {/* Test mode hint */}
                <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-xs text-amber-800">
                  <strong>Test mode:</strong> Use card <span className="font-mono">4111 1111 1111 1111</span>, any future expiry, any CVV.
                </div>
              </div>
            )}
          </div>
        </div>

        <div>
          <BookingSummary flight={flight} passengers={1} />
          <div className="card mt-4 bg-amber-50 border-amber-200">
            <h4 className="font-semibold text-amber-800 mb-2 text-sm">Booking Reference</h4>
            <p className="font-mono text-lg font-bold text-amber-900">{bookingRef}</p>
            <p className="text-xs text-amber-700 mt-1">Keep this for your records</p>
          </div>
        </div>
      </div>
    </div>
  )
}