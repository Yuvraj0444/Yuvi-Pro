import { useNavigate, useSearchParams } from 'react-router-dom'
import { format, parseISO } from 'date-fns'

const CURRENCY_SYMBOL = { INR: '₹', USD: '$', EUR: '€', GBP: '£', AED: 'د.إ', SGD: 'S$', AUD: 'A$' }

function fmtTime(iso) {
  try { return format(parseISO(iso), 'HH:mm') } catch { return iso }
}
function fmtDate(iso) {
  try { return format(parseISO(iso), 'dd MMM') } catch { return '' }
}
function fmtDuration(mins) {
  if (!mins) return '--'
  return `${Math.floor(mins / 60)}h ${mins % 60}m`
}

export default function FlightCard({ flight }) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const passengers = searchParams.get('passengers') || 1

  const handleBook = () => {
    navigate(`/seat-select/${flight.flightId}?passengers=${passengers}`, { state: { flight } })
  }

  const price    = flight.basePrice ?? flight.fareClasses?.[0]?.basePrice
  const currency = flight.currency ?? 'USD'
  const symbol   = CURRENCY_SYMBOL[currency] ?? currency + ' '
  const isDirect = !flight.stops || flight.stops === 0

  return (
    <div className="card hover:shadow-lg transition-all duration-200">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">

        {/* Airline */}
        <div className="flex-shrink-0 text-center w-20">
          <div className="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center mx-auto mb-1">
            <span className="text-xl">✈</span>
          </div>
          <p className="text-xs font-semibold text-gray-600 leading-tight">{flight.airlineName || flight.airlineIata}</p>
          <p className="text-xs text-gray-400">{isDirect ? flight.flightNumber : 'Multi-carrier'}</p>
        </div>

        {/* Route */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 sm:gap-4">

            {/* Departure */}
            <div className="text-center min-w-[60px]">
              <p className="text-2xl font-bold text-gray-900 tabular-nums">{fmtTime(flight.departureTime)}</p>
              <p className="text-sm font-bold text-blue-600">{flight.originIata}</p>
              <p className="text-xs text-gray-400 truncate max-w-[80px]">{flight.originCity}</p>
            </div>

            {/* Duration line */}
            <div className="flex-1 flex flex-col items-center">
              <p className="text-xs text-gray-500 mb-1 font-medium">{fmtDuration(flight.durationMinutes)}</p>
              <div className="w-full flex items-center gap-1">
                <div className="h-px flex-1 bg-gray-300" />
                {!isDirect && (
                  <div className="w-2 h-2 rounded-full bg-orange-400 border-2 border-white ring-1 ring-orange-400" />
                )}
                <span className="text-gray-400 text-xs">✈</span>
                <div className="h-px flex-1 bg-gray-300" />
              </div>
              {isDirect
                ? <p className="text-xs text-emerald-600 mt-1 font-medium">Non-stop</p>
                : <p className="text-xs text-orange-500 mt-1 font-medium">1 Stop · via {flight.connectionVia}</p>
              }
            </div>

            {/* Arrival */}
            <div className="text-center min-w-[60px]">
              <p className="text-2xl font-bold text-gray-900 tabular-nums">{fmtTime(flight.arrivalTime)}</p>
              <p className="text-sm font-bold text-blue-600">{flight.destinationIata}</p>
              <p className="text-xs text-gray-400 truncate max-w-[80px]">{flight.destinationCity}</p>
            </div>
          </div>

          {/* Tags */}
          <div className="flex flex-wrap items-center gap-2 mt-3">
            <span className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded-full font-medium">
              {flight.cabinClass}
            </span>
            <span className="text-xs bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-full font-medium">
              {flight.availableSeats} seats left
            </span>
            {!isDirect && (
              <span className="text-xs bg-orange-50 text-orange-600 px-2 py-0.5 rounded-full font-medium">
                {fmtDuration(flight.layoverMinutes)} layover in {flight.connectionCity || flight.connectionVia}
              </span>
            )}
            <span className="text-xs text-gray-400">{fmtDate(flight.departureTime)}</span>
          </div>
        </div>

        {/* Price + CTA */}
        <div className="flex-shrink-0 text-right sm:pl-4 sm:border-l sm:border-gray-100">
          <p className="text-xs text-gray-400 mb-0.5">from</p>
          <p className="text-3xl font-extrabold text-gray-900">
            {symbol}{price != null ? Number(price).toLocaleString('en-IN') : '---'}
          </p>
          <p className="text-xs text-gray-400 mb-3">per person</p>
          <button onClick={handleBook} className="btn-primary text-sm px-5 py-2.5">
            Book Now
          </button>
        </div>
      </div>
    </div>
  )
}