const styles = {
  CONFIRMED:      'bg-green-100 text-green-700',
  INITIATED:      'bg-blue-100 text-blue-700',
  SEAT_RESERVED:  'bg-sky-100 text-sky-700',
  PAYMENT_PENDING:'bg-amber-100 text-amber-700',
  CANCELLED:      'bg-red-100 text-red-700',
  PENDING:        'bg-gray-100 text-gray-600',
  PROCESSING:     'bg-purple-100 text-purple-700',
  COMPLETED:      'bg-green-100 text-green-700',
  FAILED:         'bg-red-100 text-red-700',
  REFUNDED:       'bg-orange-100 text-orange-700',
  UP:             'bg-green-100 text-green-700',
  DOWN:           'bg-red-100 text-red-700',
}

export default function StatusBadge({ status }) {
  const cls = styles[status] || 'bg-gray-100 text-gray-600'
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cls}`}>
      {status?.replace(/_/g, ' ')}
    </span>
  )
}