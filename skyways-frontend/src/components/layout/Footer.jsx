import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="bg-navy-950 text-gray-400 mt-auto">
      <div className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-2xl">✈</span>
              <span className="text-white font-bold text-lg">SkyWays</span>
            </div>
            <p className="text-sm leading-relaxed">
              Your trusted partner for seamless air travel. Connecting cities, connecting lives.
            </p>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Quick Links</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/"         className="hover:text-sky-400 transition-colors">Home</Link></li>
              <li><Link to="/flights"  className="hover:text-sky-400 transition-colors">Search Flights</Link></li>
              <li><Link to="/bookings" className="hover:text-sky-400 transition-colors">My Bookings</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Services</h4>
            <ul className="space-y-2 text-sm">
              <li><span>Flight Booking</span></li>
              <li><span>Seat Selection</span></li>
              <li><span>Online Check-in</span></li>
              <li><span>Baggage Info</span></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-semibold mb-4">Contact</h4>
            <ul className="space-y-2 text-sm">
              <li>support@skyways.com</li>
              <li>+1 (800) SKY-WAYS</li>
              <li>24/7 Customer Support</li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-10 pt-6 text-center text-xs">
          © {new Date().getFullYear()} SkyWays Airlines. All rights reserved.
        </div>
      </div>
    </footer>
  )
}