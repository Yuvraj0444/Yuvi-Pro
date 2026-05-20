import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { authApi }  from '../api/authApi'
import { useAuth }  from '../context/AuthContext'
import Alert         from '../components/common/Alert'
import LoadingSpinner from '../components/common/LoadingSpinner'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const [loading,  setLoading]  = useState(true)
  const [saving,   setSaving]   = useState(false)
  const [success,  setSuccess]  = useState('')
  const [error,    setError]    = useState('')
  const { register, handleSubmit, reset, formState: { errors } } = useForm()

  useEffect(() => {
    authApi.getProfile()
      .then(({ data }) => reset(data))
      .catch(() => reset(user || {}))
      .finally(() => setLoading(false))
  }, [reset, user])

  const onSubmit = async (data) => {
    setSaving(true)
    setError(''); setSuccess('')
    try {
      await authApi.updateProfile(data)
      setSuccess('Profile updated successfully.')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingSpinner label="Loading profile…" />

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="page-title mb-6">My Profile</h1>

      {success && <Alert message={success} type="success" onClose={() => setSuccess('')} />}
      {error   && <Alert message={error}   type="error"   onClose={() => setError('')}   />}

      <div className="card mt-4">
        {/* Avatar */}
        <div className="flex items-center gap-4 mb-8 pb-6 border-b border-gray-100">
          <div className="w-16 h-16 rounded-full bg-sky-500 flex items-center justify-center text-white font-bold text-2xl">
            {(user?.fullName || user?.email || 'U')[0].toUpperCase()}
          </div>
          <div>
            <p className="font-bold text-gray-900 text-lg">{user?.fullName || 'Traveller'}</p>
            <p className="text-gray-500 text-sm">{user?.email}</p>
            <span className="inline-block mt-1 text-xs bg-sky-100 text-sky-700 px-2 py-0.5 rounded-full font-medium">
              {user?.role || 'CUSTOMER'}
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Full Name</label>
            <input className="input" {...register('fullName', { required: 'Required' })} />
            {errors.fullName && <p className="error-text">{errors.fullName.message}</p>}
          </div>
          <div>
            <label className="label">Email (read-only)</label>
            <input className="input bg-gray-50 cursor-not-allowed" readOnly
              {...register('email')} />
          </div>
          <div>
            <label className="label">Phone</label>
            <input type="tel" className="input" placeholder="+1 234 567 8900"
              {...register('phone')} />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={saving} className="btn-primary">
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
            <button type="button" onClick={logout} className="btn-danger">
              Logout
            </button>
          </div>
        </form>
      </div>

      {/* Account security note */}
      <div className="card mt-4 bg-blue-50 border-blue-100">
        <h3 className="font-semibold text-blue-800 mb-2 text-sm">Account Security</h3>
        <p className="text-xs text-blue-600 leading-relaxed">
          Sensitive information such as passport details and payment data is encrypted with 3-DES
          and stored securely. Passwords are hashed with BCrypt. JWT tokens expire after session end.
        </p>
      </div>
    </div>
  )
}