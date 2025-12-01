/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{cljs,cljc}",
    "./resources/public/*.html"
  ],
  darkMode: 'class',
  theme: {
    container: {
      screens: {
        sm: '640px',
        md: '768px',
        lg: '1024px',
        xl: '1000px',
        '2xl': '1200px',
      },
      padding: '1rem',
    },
    extend: {
      colors: {
        primary: 'oklch(var(--color-primary) / <alpha-value>)',
        secondary: 'oklch(var(--color-secondary) / <alpha-value>)',
        accent: 'oklch(var(--color-accent) / <alpha-value>)',
        neutral: 'oklch(var(--color-neutral) / <alpha-value>)',
        error: 'oklch(var(--color-error) / <alpha-value>)',
        success: 'oklch(var(--color-success) / <alpha-value>)',
        warning: 'oklch(var(--color-warning) / <alpha-value>)',
      },
      opacity: {
        '15': '0.15',
        '35': '0.35',
        '65': '0.65',
        '85': '0.85',
      },
      spacing: {
        'form-group': '1.5rem',
        'form-label': '0.5rem',
        'form-error': '0.25rem',
      },
      fontSize: {
        'form-label': ['0.875rem', '1.25rem'],
        'form-input': ['1rem', '1.5rem'],
        'form-error': ['0.75rem', '1rem'],
      },
      borderRadius: {
        form: '0.375rem',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideDown: {
          '0%': { transform: 'translateY(-0.5rem)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        shake: {
          '0%, 100%': { transform: 'translateX(0)' },
          '25%': { transform: 'translateX(-0.25rem)' },
          '75%': { transform: 'translateX(0.25rem)' }
        }
      },
      animation: {
        fadeIn: 'fadeIn 0.2s ease-in-out',
        slideDown: 'slideDown 0.2s ease-in-out',
        shake: 'shake 0.5s ease-in-out'
      }
    },
  },
  // Plugins are now configured in the CSS file with @plugin directive for Tailwind v4
}
