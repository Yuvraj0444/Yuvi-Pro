const variants = {
  error:   'bg-red-50 border-red-200 text-red-700',
  success: 'bg-green-50 border-green-200 text-green-700',
  info:    'bg-sky-50 border-sky-200 text-sky-700',
  warning: 'bg-amber-50 border-amber-200 text-amber-700',
}

export default function Alert({ type = 'error', message, onClose }) {
  if (!message) return null
  return (
    <div className={`flex items-start gap-3 p-4 rounded-xl border ${variants[type]} animate-fade-in`}>
      <span className="text-sm flex-1">{message}</span>
      {onClose && (
        <button onClick={onClose} className="text-current opacity-60 hover:opacity-100 text-lg leading-none mt-0.5">
          ×
        </button>
      )}
    </div>
  )
}