import { format, parseISO } from 'date-fns'

function fmtTime(iso) {
  try { return format(parseISO(iso), 'HH:mm, dd MMM yyyy') } catch { return iso }
}

function currencySymbol(code) {
  return { INR: '₹', USD: '$', EUR: '€', GBP: '£' }[code] ?? (code + ' ')
}

export default function BookingSummary({ flight, passengers = 1, className = '' }) {
  if (!flight) return null
  const price    = parseFloat(flight.fareClasses?.[0]?.basePrice ?? flight.basePrice ?? 0)
  const baseFare = (price * passengers).toFixed(2)
  const taxes    = (price * passengers * 0.12).toFixed(2)
  const total    = (price * passengers * 1.12).toFixed(2)
  const sym      = currencySymbol(flight.currency)

  return (
    <div className={`card ${className}`}>
      <h3 className="font-bold text-gray-900 mb-4 text-lg">Booking Summary</h3>

      <div className="space-y-3 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-500">Flight</span>
          <span className="font-medium">{flight.flightNumber}</span>
        </div>
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
        <div className="flex justify-between">
          <span className="text-gray-500">Passengers</span>
          <span className="font-medium">{passengers}</span>
        </div>
      </div>

      <div className="border-t border-gray-100 mt-4 pt-4 space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-500">Base fare × {passengers}</span>
          <span>{sym}{Number(baseFare).toLocaleString()}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-500">Taxes & fees</span>
          <span>{sym}{Number(taxes).toLocaleString()}</span>
        </div>
        <div className="flex justify-between font-bold text-base border-t border-gray-100 pt-2 mt-2">
          <span>Total</span>
          <span className="text-sky-600">{sym}{Number(total).toLocaleString()}</span>
        </div>
      </div>
    </div>
  )
}