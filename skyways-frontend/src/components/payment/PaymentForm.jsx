import { useState } from 'react'
import { useForm } from 'react-hook-form'

function fmtCard(value) {
  return value.replace(/\D/g, '').replace(/(.{4})/g, '$1 ').trim().slice(0, 19)
}
function fmtExpiry(value) {
  return value.replace(/\D/g, '').replace(/^(\d{2})(\d)/, '$1/$2').slice(0, 5)
}

export default function PaymentForm({ amount, onSubmit, loading }) {
  const [cardNum, setCardNum] = useState('')
  const [expiry, setExpiry]   = useState('')
  const { register, handleSubmit, formState: { errors } } = useForm()

  const submit = (data) => onSubmit({ ...data, cardNumber: cardNum, expiry })

  return (
    <form onSubmit={handleSubmit(submit)} className="space-y-5">
      {/* Secure badge */}
      <div className="flex items-center gap-2 text-sm text-green-600 bg-green-50 px-4 py-2 rounded-xl">
        <span>🔒</span>
        <span>256-bit SSL encrypted payment</span>
      </div>

      {/* Card number */}
      <div>
        <label className="label">Card Number *</label>
        <input
          type="text"
          className="input font-mono tracking-widest"
          placeholder="1234 5678 9012 3456"
          value={cardNum}
          maxLength={19}
          onChange={e => setCardNum(fmtCard(e.target.value))}
        />
      </div>

      {/* Cardholder */}
      <div>
        <label className="label">Cardholder Name *</label>
        <input className="input" placeholder="Name as on card"
          {...register('cardholderName', { required: 'Required' })} />
        {errors.cardholderName && <p className="error-text">{errors.cardholderName.message}</p>}
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* Expiry */}
        <div>
          <label className="label">Expiry Date *</label>
          <input type="text" className="input" placeholder="MM/YY"
            value={expiry} maxLength={5}
            onChange={e => setExpiry(fmtExpiry(e.target.value))} />
        </div>
        {/* CVV */}
        <div>
          <label className="label">CVV *</label>
          <input type="password" className="input" placeholder="•••"
            maxLength={4}
            {...register('cvv', {
              required: 'Required',
              pattern: { value: /^\d{3,4}$/, message: 'Invalid CVV' },
            })} />
          {errors.cvv && <p className="error-text">{errors.cvv.message}</p>}
        </div>
      </div>

      {/* Amount summary */}
      <div className="bg-sky-50 rounded-xl px-4 py-3 flex justify-between items-center">
        <span className="text-sm text-gray-600">Amount to pay</span>
        <span className="text-xl font-bold text-sky-600">${amount}</span>
      </div>

      <button type="submit" disabled={loading} className="btn-primary w-full py-4 text-base">
        {loading ? 'Processing Payment…' : `💳  Pay $${amount}`}
      </button>

      <p className="text-center text-xs text-gray-400">
        By proceeding, you agree to our Terms of Service. Your card details are never stored.
      </p>
    </form>
  )
}