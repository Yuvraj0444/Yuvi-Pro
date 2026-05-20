import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function Navbar() {
  const { user, logout, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const navCls = ({ isActive }) =>
    `text-sm font-medium transition-colors ${isActive ? 'text-sky-400' : 'text-gray-300 hover:text-white'}`

  return (
    <nav className="bg-navy-950 border-b border-gray-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 shrink-0">
            <span className="text-2xl">✈</span>
            <span className="text-white font-bold text-xl tracking-tight">SkyWays</span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-8">
            <NavLink to="/"        end className={navCls}>Home</NavLink>
            <NavLink to="/flights"     className={navCls}>Search Flights</NavLink>
            {isAuthenticated && (
              <NavLink to="/bookings"  className={navCls}>My Bookings</NavLink>
            )}
          </div>

          {/* Auth actions */}
          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <NavLink to="/profile" className="flex items-center gap-2 text-sm text-gray-300 hover:text-white transition-colors">
                  <div className="w-8 h-8 rounded-full bg-sky-500 flex items-center justify-center text-white font-semibold text-xs">
                    {(user?.fullName || user?.email || 'U')[0].toUpperCase()}
                  </div>
                  <span className="hidden lg:block">{user?.fullName || user?.email}</span>
                </NavLink>
                <button onClick={handleLogout} className="btn-secondary text-xs px-4 py-2">
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login"    className="text-sm text-gray-300 hover:text-white transition-colors">Login</Link>
                <Link to="/register" className="btn-primary text-xs px-4 py-2">Sign Up</Link>
              </>
            )}
          </div>

          {/* Mobile hamburger */}
          <button
            className="md:hidden text-gray-300 hover:text-white p-2"
            onClick={() => setMenuOpen(v => !v)}
          >
            <div className="w-5 space-y-1">
              <div className={`h-0.5 bg-current transition-all ${menuOpen ? 'rotate-45 translate-y-1.5' : ''}`} />
              <div className={`h-0.5 bg-current transition-all ${menuOpen ? 'opacity-0' : ''}`} />
              <div className={`h-0.5 bg-current transition-all ${menuOpen ? '-rotate-45 -translate-y-1.5' : ''}`} />
            </div>
          </button>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="md:hidden border-t border-gray-800 py-4 space-y-3 animate-fade-in">
            <NavLink to="/"        end className={navCls} onClick={() => setMenuOpen(false)}>Home</NavLink>
            <NavLink to="/flights"     className={navCls} onClick={() => setMenuOpen(false)}>Search Flights</NavLink>
            {isAuthenticated && (
              <NavLink to="/bookings"  className={navCls} onClick={() => setMenuOpen(false)}>My Bookings</NavLink>
            )}
            <div className="pt-2 border-t border-gray-800 flex flex-col gap-2">
              {isAuthenticated ? (
                <>
                  <NavLink to="/profile" className={navCls} onClick={() => setMenuOpen(false)}>Profile</NavLink>
                  <button onClick={handleLogout} className="text-left text-sm text-red-400 hover:text-red-300">Logout</button>
                </>
              ) : (
                <>
                  <Link to="/login"    className="text-sm text-gray-300 hover:text-white" onClick={() => setMenuOpen(false)}>Login</Link>
                  <Link to="/register" className="text-sm text-sky-400 hover:text-sky-300" onClick={() => setMenuOpen(false)}>Sign Up</Link>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </nav>
  )
}