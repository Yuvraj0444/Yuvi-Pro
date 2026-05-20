import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useAuth } from '../context/AuthContext'
import Alert from '../components/common/Alert'

export default function LoginPage() {
  const { login, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors } } = useForm()

  const fromLocation = location.state?.from

  const onSubmit = async (data) => {
    setError('')
    const result = await login(data.email, data.password)
    if (result.success) {
      navigate(
        { pathname: fromLocation?.pathname || '/', search: fromLocation?.search || '' },
        { state: fromLocation?.state, replace: true }
      )
    } else {
      setError(result.message)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-navy-950 via-navy-900 to-sky-900 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2">
            <span className="text-3xl">✈</span>
            <span className="text-white font-bold text-2xl">SkyWays</span>
          </Link>
          <p className="text-blue-200 mt-2 text-sm">Welcome back — sign in to your account</p>
        </div>

        <div className="card animate-slide-up">
          <h1 className="page-title text-center mb-6">Sign In</h1>

          <Alert message={error} type="error" onClose={() => setError('')} />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-4">
            <div>
              <label className="label">Email address</label>
              <input type="email" className="input" placeholder="you@example.com"
                {...register('email', {
                  required: 'Email is required',
                  pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Invalid email' },
                })} />
              {errors.email && <p className="error-text">{errors.email.message}</p>}
            </div>
            <div>
              <label className="label">Password</label>
              <input type="password" className="input" placeholder="••••••••"
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 6, message: 'Minimum 6 characters' },
                })} />
              {errors.password && <p className="error-text">{errors.password.message}</p>}
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
              {loading ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Don't have an account?{' '}
            <Link to="/register" className="text-sky-500 hover:text-sky-600 font-medium">
              Create one free
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}