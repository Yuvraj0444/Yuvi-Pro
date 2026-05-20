import { useState, useMemo } from 'react'
import { useParams, useLocation, useNavigate } from 'react-router-dom'
import BookingSummary from '../components/booking/BookingSummary'

// Deterministic pseudo-random based on string seed — used to mark seats as occupied
function seededHash(seed, item) {
  let h = 5381
  const s = String(seed) + String(item)
  for (let i = 0; i < s.length; i++) {
    h = ((h << 5) + h) ^ s.charCodeAt(i)
    h = h >>> 0
  }
  return h / 0xffffffff
}

const CABIN_CONFIG = {
  FIRST:          { rows: 4,  startRow: 1,  leftCols: ['A'],      rightCols: ['C'],         label: 'First Class' },
  BUSINESS:       { rows: 10, startRow: 5,  leftCols: ['A', 'C'], rightCols: ['D', 'F'],    label: 'Business Class' },
  PREMIUMECONOMY: { rows: 8,  startRow: 15, leftCols: ['A', 'B', 'C'], rightCols: ['D', 'E', 'F'], label: 'Premium Economy' },
  ECONOMY:        { rows: 30, startRow: 20, leftCols: ['A', 'B', 'C'], rightCols: ['D', 'E', 'F'], label: 'Economy' },
}

function getCabinKey(cabinClass) {
  const c = (cabinClass || 'ECONOMY').toUpperCase().replace(/[_\s]/g, '')
  if (c.startsWith('FIRST'))    return 'FIRST'
  if (c.startsWith('BUSINESS')) return 'BUSINESS'
  if (c.startsWith('PREMIUM'))  return 'PREMIUMECONOMY'
  return 'ECONOMY'
}

function buildSeatMap(cabinClass, flightId, availableSeats) {
  const key = getCabinKey(cabinClass)
  const cfg = CABIN_CONFIG[key]
  const { rows, startRow, leftCols, rightCols } = cfg
  const allLetters = [...leftCols, ...rightCols]

  const allSeats = []
  for (let r = 0; r < rows; r++) {
    for (const col of allLetters) {
      allSeats.push(`${startRow + r}${col}`)
    }
  }

  const totalSeats = allSeats.length
  const safeAvailable = Math.min(availableSeats ?? totalSeats, totalSeats)
  const takenCount = Math.max(0, totalSeats - safeAvailable)

  // Sort by seeded hash to get a deterministic but shuffled order, then take first N as occupied
  const sorted = [...allSeats].sort((a, b) => seededHash(flightId, a) - seededHash(flightId, b))
  const taken = new Set(sorted.slice(0, takenCount))

  return { allSeats, taken, cfg }
}

function SeatButton({ id, status, onClick }) {
  const base = 'flex-1 h-7 rounded-md text-xs font-semibold transition-all duration-100 border'
  const styles = {
    available: `${base} bg-emerald-50 border-emerald-300 text-emerald-600 hover:bg-emerald-200 cursor-pointer`,
    selected:  `${base} bg-sky-500 border-sky-600 text-white cursor-pointer shadow-md scale-105`,
    occupied:  `${base} bg-gray-200 border-gray-300 text-gray-400 cursor-not-allowed`,
  }
  return (
    <button
      onClick={onClick}
      disabled={status === 'occupied'}
      title={id}
      className={styles[status]}
    >
      {status === 'selected' ? '✓' : ''}
    </button>
  )
}

export default function SeatSelectionPage() {
  const { flightId }   = useParams()
  const location       = useLocation()
  const navigate       = useNavigate()

  const flight         = location.state?.flight || null
  const passengerCount = Number(new URLSearchParams(location.search).get('passengers') || 1)

  const [selectedSeats, setSelectedSeats] = useState([])

  const { allSeats, taken, cfg } = useMemo(
    () => buildSeatMap(flight?.cabinClass, flight?.flightId, flight?.availableSeats),
    [flight]
  )
  const { leftCols, rightCols, startRow, rows, label } = cfg

  const seatStatus = (id) => {
    if (taken.has(id))              return 'occupied'
    if (selectedSeats.includes(id)) return 'selected'
    return 'available'
  }

  const toggleSeat = (id) => {
    if (taken.has(id)) return
    setSelectedSeats(prev => {
      if (prev.includes(id)) return prev.filter(s => s !== id)
      if (prev.length >= passengerCount) return [...prev.slice(1), id]
      return [...prev, id]
    })
  }

  const handleConfirm = () => {
    navigate(`/book/${flightId}?passengers=${passengerCount}`, {
      state: { flight, selectedSeats },
    })
  }

  if (!flight) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center">
        <p className="text-gray-500 text-lg">Flight details not found.</p>
        <button onClick={() => navigate('/flights')} className="btn-primary mt-4">Search Again</button>
      </div>
    )
  }

  const ready = selectedSeats.length === passengerCount

  return (
    <div className="max-w-7xl mx-auto px-4 py-10">
      {/* 4-step progress bar */}
      <div className="flex items-center gap-2 mb-8 text-sm">
        <span className="flex items-center gap-1.5 text-sky-600 font-semibold">
          <span className="w-6 h-6 rounded-full bg-sky-500 text-white flex items-center justify-center text-xs">1</span>
          Seat Selection
        </span>
        <div className="h-px flex-1 bg-gray-200" />
        <span className="flex items-center gap-1.5 text-gray-400">
          <span className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs">2</span>
          Passenger Details
        </span>
        <div className="h-px flex-1 bg-gray-200" />
        <span className="flex items-center gap-1.5 text-gray-400">
          <span className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs">3</span>
          Payment
        </span>
        <div className="h-px flex-1 bg-gray-200" />
        <span className="flex items-center gap-1.5 text-gray-400">
          <span className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center text-xs">4</span>
          Confirmation
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Seat map panel */}
        <div className="lg:col-span-2">
          <div className="card">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h1 className="page-title">Select Your Seats</h1>
                <p className="text-sm text-gray-500 mt-0.5">
                  {label} · Choose {passengerCount} seat{passengerCount > 1 ? 's' : ''}
                </p>
              </div>
              <span className={`text-sm font-semibold ${ready ? 'text-emerald-600' : 'text-sky-600'}`}>
                {selectedSeats.length} / {passengerCount} selected
              </span>
            </div>

            {/* Legend */}
            <div className="flex items-center gap-5 mb-5 text-xs text-gray-600">
              <div className="flex items-center gap-1.5">
                <div className="w-6 h-6 rounded-md bg-emerald-50 border border-emerald-300" />
                <span>Available</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="w-6 h-6 rounded-md bg-sky-500 border border-sky-600" />
                <span>Selected</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="w-6 h-6 rounded-md bg-gray-200 border border-gray-300" />
                <span>Occupied</span>
              </div>
            </div>

            {/* Nose label */}
            <div className="flex justify-center mb-4">
              <span className="text-xs text-gray-400 bg-gray-50 border border-gray-200 px-4 py-1 rounded-full">
                ✈ Aircraft Front
              </span>
            </div>

            {/* Seat grid */}
            <div className="overflow-x-auto">
              <div className="min-w-[280px] max-w-sm mx-auto select-none">
                {/* Column headers */}
                <div className="flex items-center mb-2">
                  <div className="w-9 shrink-0" />
                  <div className="flex-1 flex gap-1">
                    {leftCols.map(c => (
                      <div key={c} className="flex-1 text-center text-xs text-gray-400 font-bold">{c}</div>
                    ))}
                  </div>
                  <div className="w-5 shrink-0" />
                  <div className="flex-1 flex gap-1">
                    {rightCols.map(c => (
                      <div key={c} className="flex-1 text-center text-xs text-gray-400 font-bold">{c}</div>
                    ))}
                  </div>
                </div>

                {Array.from({ length: rows }, (_, i) => {
                  const rowNum = startRow + i
                  return (
                    <div key={rowNum} className="flex items-center mb-1.5">
                      <div className="w-9 shrink-0 text-center text-xs text-gray-400">{rowNum}</div>
                      <div className="flex-1 flex gap-1">
                        {leftCols.map(col => {
                          const id = `${rowNum}${col}`
                          return <SeatButton key={id} id={id} status={seatStatus(id)} onClick={() => toggleSeat(id)} />
                        })}
                      </div>
                      <div className="w-5 shrink-0" />
                      <div className="flex-1 flex gap-1">
                        {rightCols.map(col => {
                          const id = `${rowNum}${col}`
                          return <SeatButton key={id} id={id} status={seatStatus(id)} onClick={() => toggleSeat(id)} />
                        })}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Bottom CTA */}
            <div className="mt-6">
              {selectedSeats.length === 0 ? (
                <div className="p-4 bg-gray-50 border border-gray-200 rounded-xl text-center text-sm text-gray-500">
                  Click a green seat to select it
                </div>
              ) : (
                <div className="p-4 bg-sky-50 border border-sky-100 rounded-xl flex items-center justify-between gap-4">
                  <div>
                    <p className="text-xs text-sky-700 font-semibold uppercase tracking-wide mb-1">Selected</p>
                    <p className="text-lg font-bold text-sky-800">{selectedSeats.join(', ')}</p>
                  </div>
                  <button
                    onClick={handleConfirm}
                    disabled={!ready}
                    className="btn-primary px-6 py-2.5 shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {ready ? 'Continue →' : `Select ${passengerCount - selectedSeats.length} more`}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          <BookingSummary flight={flight} passengers={passengerCount} />
          {selectedSeats.length > 0 && (
            <div className="card">
              <h4 className="text-sm font-semibold text-gray-700 mb-3">Your Seats</h4>
              <div className="flex flex-wrap gap-2">
                {selectedSeats.map((s, i) => (
                  <div key={s} className="flex items-center gap-1.5 bg-sky-50 border border-sky-200 rounded-lg px-3 py-1.5">
                    <span className="text-xs text-sky-500">Pax {i + 1}</span>
                    <span className="font-bold text-sky-700">{s}</span>
                  </div>
                ))}
              </div>
              {passengerCount > selectedSeats.length && (
                <p className="text-xs text-gray-400 mt-2">
                  {passengerCount - selectedSeats.length} more seat{passengerCount - selectedSeats.length > 1 ? 's' : ''} needed
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}