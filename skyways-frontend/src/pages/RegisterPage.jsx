import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useAuth } from '../context/AuthContext'
import Alert from '../components/common/Alert'

export default function RegisterPage() {
  const { register: registerUser, loading } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const { register, handleSubmit, watch, formState: { errors } } = useForm()
  const password = watch('password')

  const onSubmit = async (data) => {
    setError('')
    const { confirmPassword, ...payload } = data
    const result = await registerUser(payload)
    if (result.success) navigate('/')
    else setError(result.message)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-navy-950 via-navy-900 to-sky-900 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2">
            <span className="text-3xl">✈</span>
            <span className="text-white font-bold text-2xl">SkyWays</span>
          </Link>
          <p className="text-blue-200 mt-2 text-sm">Create your free account and start flying</p>
        </div>

        <div className="card animate-slide-up">
          <h1 className="page-title text-center mb-6">Create Account</h1>

          <Alert message={error} type="error" onClose={() => setError('')} />

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-4">
            <div>
              <label className="label">Full Name</label>
              <input className="input" placeholder="John Doe"
                {...register('fullName', { required: 'Full name is required' })} />
              {errors.fullName && <p className="error-text">{errors.fullName.message}</p>}
            </div>
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
              <label className="label">Phone (optional)</label>
              <input type="tel" className="input" placeholder="+1 234 567 8900"
                {...register('phone')} />
            </div>
            <div>
              <label className="label">Password</label>
              <input type="password" className="input" placeholder="Minimum 8 characters"
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 8, message: 'Minimum 8 characters' },
                  pattern: {
                    value: /^(?=.*[A-Z])(?=.*\d)/,
                    message: 'Must contain at least 1 uppercase letter and 1 number',
                  },
                })} />
              {errors.password && <p className="error-text">{errors.password.message}</p>}
            </div>
            <div>
              <label className="label">Confirm Password</label>
              <input type="password" className="input" placeholder="Repeat your password"
                {...register('confirmPassword', {
                  required: 'Please confirm your password',
                  validate: v => v === password || 'Passwords do not match',
                })} />
              {errors.confirmPassword && <p className="error-text">{errors.confirmPassword.message}</p>}
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full mt-2">
              {loading ? 'Creating account…' : 'Create Account'}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-sky-500 hover:text-sky-600 font-medium">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}