import { useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import AirportSearch from '../common/AirportSearch'

export default function FlightSearchForm({ onSearch, loading, compact = false }) {
  const [tripType, setTripType] = useState('ONE_WAY')

  const {
    control, register, handleSubmit, watch,
    formState: { errors },
  } = useForm({
    defaultValues: {
      origin: '', destination: '',
      date: new Date().toISOString().split('T')[0],
      returnDate: '',
      passengers: 1, classType: 'ECONOMY',
    },
  })

  const travelDate = watch('date')

  const gridCls = compact
    ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4'
    : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'

  const handleSubmit2 = handleSubmit((data) => onSearch({ ...data, tripType }))

  return (
    <form onSubmit={handleSubmit2}>
      {/* Trip type toggle */}
      <div className="flex gap-2 mb-5">
        {['ONE_WAY', 'ROUND_TRIP'].map(type => (
          <button
            key={type}
            type="button"
            onClick={() => setTripType(type)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-all ${
              tripType === type
                ? 'bg-sky-500 text-white border-sky-500 shadow-sm'
                : 'bg-white text-gray-600 border-gray-300 hover:border-sky-400'
            }`}
          >
            {type === 'ONE_WAY' ? '→ One Way' : '⇄ Round Trip'}
          </button>
        ))}
      </div>

      <div className={gridCls}>

        {/* From */}
        <div>
          <label className="label">From</label>
          <Controller
            name="origin"
            control={control}
            rules={{ required: 'Select departure airport' }}
            render={({ field }) => (
              <AirportSearch
                value={field.value}
                onChange={field.onChange}
                placeholder="City or airport code"
                error={!!errors.origin}
              />
            )}
          />
          {errors.origin && <p className="error-text">{errors.origin.message}</p>}
        </div>

        {/* To */}
        <div>
          <label className="label">To</label>
          <Controller
            name="destination"
            control={control}
            rules={{ required: 'Select destination airport' }}
            render={({ field }) => (
              <AirportSearch
                value={field.value}
                onChange={field.onChange}
                placeholder="City or airport code"
                error={!!errors.destination}
              />
            )}
          />
          {errors.destination && <p className="error-text">{errors.destination.message}</p>}
        </div>

        {/* Date */}
        <div>
          <label className="label">Date</label>
          <input
            type="date"
            className={`input ${errors.date ? 'border-red-400' : ''}`}
            min={new Date().toISOString().split('T')[0]}
            {...register('date', { required: 'Select travel date' })}
          />
          {errors.date && <p className="error-text">{errors.date.message}</p>}
        </div>

        {/* Return Date — only for round trip */}
        {tripType === 'ROUND_TRIP' && (
          <div>
            <label className="label">Return Date</label>
            <input
              type="date"
              className={`input ${errors.returnDate ? 'border-red-400' : ''}`}
              min={travelDate || new Date().toISOString().split('T')[0]}
              {...register('returnDate', { required: tripType === 'ROUND_TRIP' ? 'Select return date' : false })}
            />
            {errors.returnDate && <p className="error-text">{errors.returnDate.message}</p>}
          </div>
        )}

        {/* Passengers */}
        <div>
          <label className="label">Passengers</label>
          <select className="input" {...register('passengers')}>
            {[1,2,3,4,5,6,7,8,9].map(n => (
              <option key={n} value={n}>{n} {n === 1 ? 'Adult' : 'Adults'}</option>
            ))}
          </select>
        </div>

        {/* Class */}
        <div>
          <label className="label">Class</label>
          <select className="input" {...register('classType')}>
            <option value="ECONOMY">Economy</option>
            <option value="BUSINESS">Business</option>
            <option value="FIRST">First Class</option>
          </select>
        </div>
      </div>

      <div className="mt-6">
        <button type="submit" disabled={loading} className="btn-primary w-full sm:w-auto px-10 py-3">
          {loading
            ? <><span className="animate-spin inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full" /> Searching…</>
            : <>✈ &nbsp;Search Flights</>}
        </button>
      </div>
    </form>
  )
}