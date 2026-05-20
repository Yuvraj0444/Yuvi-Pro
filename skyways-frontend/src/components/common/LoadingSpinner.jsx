export default function LoadingSpinner({ size = 'md', label = 'Loading…' }) {
  const sz = { sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12' }[size]
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12">
      <div className={`${sz} border-4 border-sky-100 border-t-sky-500 rounded-full animate-spin`} />
      {label && <p className="text-sm text-gray-500">{label}</p>}
    </div>
  )
}