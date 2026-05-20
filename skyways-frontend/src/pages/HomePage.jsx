import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import FlightSearchForm from '../components/flights/FlightSearchForm'

const DESTINATIONS = [
  { city: 'Dubai',     code: 'DXB', image: '🏙️',  tag: 'Popular' },
  { city: 'Singapore', code: 'SIN', image: '🌴',  tag: 'Hot Deal' },
  { city: 'London',    code: 'LHR', image: '🎡',  tag: 'Trending' },
  { city: 'New York',  code: 'JFK', image: '🗽',  tag: 'Must Visit' },
  { city: 'Paris',     code: 'CDG', image: '🗼',  tag: 'Romantic' },
  { city: 'Tokyo',     code: 'NRT', image: '🌸',  tag: 'Adventure' },
]

const FEATURES = [
  { icon: '🔒', title: 'Secure Booking',      desc: 'SSL-encrypted payments with Stripe integration.' },
  { icon: '📧', title: 'Instant Confirmation', desc: 'Booking confirmation email delivered in seconds.' },
  { icon: '🔄', title: 'Easy Cancellation',   desc: 'Cancel anytime before departure with full refund.' },
  { icon: '✈️', title: 'Global Coverage',     desc: 'Access flights from 500+ airlines worldwide.' },
]

export default function HomePage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSearch = (params) => {
    setLoading(true)
    const query = new URLSearchParams(params).toString()
    navigate(`/flights?${query}`)
  }

  return (
    <div>
      {/* Hero */}
      <section className="bg-hero-pattern min-h-[520px] flex flex-col justify-center relative overflow-hidden">
        <div className="absolute inset-0 opacity-10"
          style={{ backgroundImage: 'radial-gradient(circle at 20% 80%, white 0, transparent 50%)' }} />
        <div className="relative max-w-7xl mx-auto px-4 py-16 text-white text-center">
          <div className="inline-flex items-center gap-2 bg-white/10 text-sky-200 text-sm px-4 py-1.5 rounded-full mb-6 backdrop-blur-sm border border-white/20">
            <span>✈</span> Book smarter, fly better
          </div>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold mb-4 tracking-tight">
            Your Journey Starts<br />
            <span className="text-sky-300">Right Here</span>
          </h1>
          <p className="text-lg text-blue-100 mb-12 max-w-xl mx-auto">
            Search hundreds of airlines, compare prices, and book the perfect flight — all in one place.
          </p>

          {/* Search form card */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-2xl max-w-4xl mx-auto text-left">
            <h2 className="text-gray-900 font-bold text-lg mb-5">Search Flights</h2>
            <FlightSearchForm onSearch={handleSearch} loading={loading} />
          </div>
        </div>
      </section>

      {/* Popular destinations */}
      <section className="max-w-7xl mx-auto px-4 py-16">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Popular Destinations</h2>
            <p className="text-gray-500 mt-1">Explore top routes at unbeatable prices</p>
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {DESTINATIONS.map(dest => (
            <button
              key={dest.code}
              onClick={() => navigate(`/flights?destination=${dest.code}`)}
              className="group relative rounded-2xl bg-gradient-to-br from-navy-50 to-sky-50 border border-gray-100 p-4 text-left hover:shadow-md hover:border-sky-200 transition-all"
            >
              <div className="text-4xl mb-3">{dest.image}</div>
              <p className="font-semibold text-gray-900">{dest.city}</p>
              <p className="text-xs text-gray-500">{dest.code}</p>
              <span className="absolute top-3 right-3 text-[10px] bg-sky-500 text-white px-1.5 py-0.5 rounded-full">
                {dest.tag}
              </span>
            </button>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="bg-navy-950 py-16">
        <div className="max-w-7xl mx-auto px-4">
          <h2 className="text-2xl font-bold text-white text-center mb-10">
            Why Fly with SkyWays?
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {FEATURES.map(f => (
              <div key={f.title} className="text-center p-6 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/8 transition-colors">
                <div className="text-4xl mb-4">{f.icon}</div>
                <h3 className="font-semibold text-white mb-2">{f.title}</h3>
                <p className="text-sm text-gray-400 leading-relaxed">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 px-4 text-center">
        <div className="max-w-2xl mx-auto">
          <h2 className="text-3xl font-bold text-gray-900 mb-4">Ready to Take Off?</h2>
          <p className="text-gray-500 mb-8">Join thousands of travellers who book with SkyWays every day.</p>
          <button onClick={() => navigate('/register')} className="btn-primary text-base px-10 py-4">
            Create Free Account
          </button>
        </div>
      </section>
    </div>
  )
}