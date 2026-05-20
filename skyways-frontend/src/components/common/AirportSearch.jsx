import { useState, useRef, useEffect } from 'react'
import { searchAirports } from '../../data/airports'

export default function AirportSearch({ value, onChange, placeholder, error }) {
  const [query, setQuery]         = useState(value || '')
  const [results, setResults]     = useState([])
  const [open, setOpen]           = useState(false)
  const [highlight, setHighlight] = useState(-1)
  const wrapRef = useRef(null)

  // Sync external value changes (e.g. form reset)
  useEffect(() => { if (!value) setQuery('') }, [value])

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleInput = (e) => {
    const q = e.target.value
    setQuery(q)
    onChange(null)           // clear selection while typing
    const hits = searchAirports(q)
    setResults(hits)
    setOpen(hits.length > 0)
    setHighlight(-1)
  }

  const select = (airport) => {
    setQuery(`${airport.code} — ${airport.city}`)
    onChange(airport.code)
    setOpen(false)
    setResults([])
  }

  const handleKey = (e) => {
    if (!open) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setHighlight(h => Math.min(h + 1, results.length - 1)) }
    if (e.key === 'ArrowUp')   { e.preventDefault(); setHighlight(h => Math.max(h - 1, 0)) }
    if (e.key === 'Enter' && highlight >= 0) { e.preventDefault(); select(results[highlight]) }
    if (e.key === 'Escape') setOpen(false)
  }

  return (
    <div ref={wrapRef} className="relative">
      <input
        type="text"
        autoComplete="off"
        value={query}
        onChange={handleInput}
        onKeyDown={handleKey}
        onFocus={() => query && results.length > 0 && setOpen(true)}
        placeholder={placeholder}
        className={`input ${error ? 'border-red-400 focus:ring-red-400' : ''}`}
      />

      {open && (
        <ul className="absolute z-50 mt-1 w-full bg-white rounded-xl border border-gray-200 shadow-xl overflow-hidden max-h-64 overflow-y-auto">
          {results.map((airport, i) => (
            <li
              key={airport.code}
              onMouseDown={() => select(airport)}
              className={`flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors
                ${i === highlight ? 'bg-blue-50' : 'hover:bg-gray-50'}`}
            >
              <span className="text-sm font-bold text-blue-600 w-10 shrink-0">{airport.code}</span>
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{airport.city}</p>
                <p className="text-xs text-gray-400 truncate">{airport.name} · {airport.country}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}