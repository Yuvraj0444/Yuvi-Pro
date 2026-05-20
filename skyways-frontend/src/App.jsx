import { Routes, Route } from 'react-router-dom'
import Navbar          from './components/layout/Navbar'
import Footer          from './components/layout/Footer'
import ProtectedRoute  from './components/common/ProtectedRoute'

import HomePage               from './pages/HomePage'
import LoginPage              from './pages/LoginPage'
import RegisterPage           from './pages/RegisterPage'
import FlightSearchPage       from './pages/FlightSearchPage'
import BookingFlowPage        from './pages/BookingFlowPage'
import PaymentPage            from './pages/PaymentPage'
import BookingConfirmationPage from './pages/BookingConfirmationPage'
import SeatSelectionPage      from './pages/SeatSelectionPage'
import MyBookingsPage         from './pages/MyBookingsPage'
import BookingDetailPage      from './pages/BookingDetailPage'
import ProfilePage            from './pages/ProfilePage'

export default function App() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/"            element={<HomePage />} />
          <Route path="/login"       element={<LoginPage />} />
          <Route path="/register"    element={<RegisterPage />} />
          <Route path="/flights"     element={<FlightSearchPage />} />

          {/* Protected routes — require JWT */}
          <Route element={<ProtectedRoute />}>
            <Route path="/seat-select/:flightId"  element={<SeatSelectionPage />} />
            <Route path="/book/:flightId"          element={<BookingFlowPage />} />
            <Route path="/payment/:bookingId"      element={<PaymentPage />} />
            <Route path="/confirmation/:bookingId" element={<BookingConfirmationPage />} />
            <Route path="/bookings"                element={<MyBookingsPage />} />
            <Route path="/bookings/:bookingId"     element={<BookingDetailPage />} />
            <Route path="/profile"                 element={<ProfilePage />} />
          </Route>
        </Routes>
      </main>
      <Footer />
    </div>
  )
}