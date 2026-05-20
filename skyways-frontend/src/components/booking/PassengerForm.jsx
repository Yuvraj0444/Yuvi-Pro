import { useForm, useFieldArray } from 'react-hook-form'

const NATIONALITIES = [
  'Indian', 'American', 'British', 'Australian', 'Canadian',
  'German', 'French', 'Singaporean', 'UAE National', 'Other',
]

function PassengerFields({ index, register, errors }) {
  const e = errors?.passengers?.[index] || {}
  return (
    <div className="card mb-4">
      <h4 className="font-semibold text-gray-700 mb-4">Passenger {index + 1}</h4>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="label">First Name *</label>
          <input className="input" {...register(`passengers.${index}.firstName`, { required: 'Required' })} />
          {e.firstName && <p className="error-text">{e.firstName.message}</p>}
        </div>
        <div>
          <label className="label">Last Name *</label>
          <input className="input" {...register(`passengers.${index}.lastName`, { required: 'Required' })} />
          {e.lastName && <p className="error-text">{e.lastName.message}</p>}
        </div>
        <div>
          <label className="label">Passport Number *</label>
          <input className="input" placeholder="A1234567"
            {...register(`passengers.${index}.passportNo`, {
              required: 'Required',
              pattern: { value: /^[A-Z0-9]{6,9}$/, message: 'Invalid passport number' },
            })} />
          {e.passportNo && <p className="error-text">{e.passportNo.message}</p>}
        </div>
        <div>
          <label className="label">Passport Expiry *</label>
          <input type="date" className="input"
            min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
            {...register(`passengers.${index}.passportExpiry`, { required: 'Required' })} />
          {e.passportExpiry && <p className="error-text">{e.passportExpiry.message}</p>}
        </div>
        <div>
          <label className="label">Date of Birth *</label>
          <input type="date" className="input"
            max={new Date(Date.now() - 2 * 365 * 86400000).toISOString().split('T')[0]}
            {...register(`passengers.${index}.dateOfBirth`, { required: 'Required' })} />
          {e.dateOfBirth && <p className="error-text">{e.dateOfBirth.message}</p>}
        </div>
        <div>
          <label className="label">Nationality *</label>
          <select className="input" {...register(`passengers.${index}.nationality`, { required: 'Required' })}>
            <option value="">Select nationality</option>
            {NATIONALITIES.map(n => <option key={n} value={n}>{n}</option>)}
          </select>
          {e.nationality && <p className="error-text">{e.nationality.message}</p>}
        </div>
        <div>
          <label className="label">Email *</label>
          <input type="email" className="input" placeholder="passenger@email.com"
            {...register(`passengers.${index}.email`, {
              required: 'Required',
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Invalid email' },
            })} />
          {e.email && <p className="error-text">{e.email.message}</p>}
        </div>
        <div>
          <label className="label">Phone</label>
          <input type="tel" className="input" placeholder="+1 234 567 8900"
            {...register(`passengers.${index}.phone`)} />
        </div>
      </div>
    </div>
  )
}

export default function PassengerForm({ passengerCount = 1, onSubmit }) {
  const defaultPassenger = {
    firstName: '', lastName: '', passportNo: '', passportExpiry: '',
    dateOfBirth: '', nationality: '', email: '', phone: '',
  }
  const { register, handleSubmit, control, formState: { errors } } = useForm({
    defaultValues: {
      passengers: Array.from({ length: passengerCount }, () => ({ ...defaultPassenger })),
    },
  })
  const { fields } = useFieldArray({ control, name: 'passengers' })

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {fields.map((field, i) => (
        <PassengerFields key={field.id} index={i} register={register} errors={errors} />
      ))}
      <button type="submit" className="btn-primary w-full mt-2">
        Continue to Payment →
      </button>
    </form>
  )
}