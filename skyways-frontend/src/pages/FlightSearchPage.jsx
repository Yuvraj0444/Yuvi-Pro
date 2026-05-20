import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { flightApi } from '../api/flightApi'
import FlightSearchForm from '../components/flights/FlightSearchForm'
import FlightCard       from '../components/flights/FlightCard'
import LoadingSpinner   from '../components/common/LoadingSpinner'
import Alert            from '../components/common/Alert'

export default function FlightSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [flights, setFlights]   = useState([])
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')
  const [searched, setSearched] = useState(false)

  const currentParams = {
    origin:      searchParams.get('origin')      || '',
    destination: searchParams.get('destination') || '',
    date:        searchParams.get('date')        || new Date().toISOString().split('T')[0],
    passengers:  searchParams.get('passengers')  || 1,
    classType:   searchParams.get('classType')   || 'ECONOMY',
  }

  const doSearch = async (params) => {
    setLoading(true)
    setError('')
    setSearched(true)
    setSearchParams(params)
    try {
      const { data } = await flightApi.search(params)
      const flights = data.data ?? (Array.isArray(data) ? data : [])
      setFlights(flights)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to search flights. Please try again.')
      setFlights([])
    } finally {
      setLoading(false)
    }
  }

  // Auto-search when URL already has params (e.g., from homepage)
  useEffect(() => {
    if (currentParams.origin && currentParams.destination) {
      doSearch(currentParams)
    }
  }, []) // eslint-disable-line

  return (
    <div className="max-w-7xl mx-auto px-4 py-10">
      {/* Search form */}
      <div className="card mb-8">
        <h1 className="page-title mb-6">Search Flights</h1>
        <FlightSearchForm onSearch={doSearch} loading={loading} compact />
      </div>

      {/* Results */}
      {loading && <LoadingSpinner label="Searching available flights…" />}

      {error && <Alert message={error} type="error" onClose={() => setError('')} />}

      {!loading && searched && flights.length === 0 && !error && (
        <div className="text-center py-20">
          <div className="text-6xl mb-4">✈</div>
          <h3 className="text-xl font-semibold text-gray-700 mb-2">No flights found</h3>
          <p className="text-gray-500">Try different dates, airports, or check back later.</p>
        </div>
      )}

      {!loading && flights.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-4">
            <p className="text-gray-600 font-medium">
              {flights.length} flight{flights.length !== 1 ? 's' : ''} found
              {currentParams.origin && currentParams.destination &&
                ` · ${currentParams.origin} → ${currentParams.destination}`}
            </p>
          </div>
          <div className="space-y-4">
            {flights.map(f => (
              <FlightCard key={f.flightId || f.id} flight={f} />
            ))}
          </div>
        </div>
      )}

      {!searched && !loading && (
        <div className="text-center py-20 text-gray-400">
          <div className="text-6xl mb-4">🔍</div>
          <p className="text-lg">Enter your trip details above to search flights</p>
        </div>
      )}
    </div>
  )
}