/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: { sans: ['Inter', 'system-ui', 'sans-serif'] },
      colors: {
        navy: {
          50:  '#f0f4ff', 100: '#dce8ff', 200: '#b9d0fe',
          300: '#84adfc', 400: '#4a7df8', 500: '#1e56f3',
          600: '#0d38e9', 700: '#0a29cc', 800: '#0c24a6',
          900: '#0f2382', 950: '#0a1550',
        },
      },
      animation: {
        'fade-in': 'fadeIn 0.4s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
      },
      keyframes: {
        fadeIn:  { from: { opacity: 0 }, to: { opacity: 1 } },
        slideUp: { from: { opacity: 0, transform: 'translateY(12px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
      },
    },
  },
  plugins: [],
}