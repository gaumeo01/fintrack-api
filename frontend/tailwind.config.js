/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#17211b",
        mint: "#2f8f6b",
        coral: "#d75f4d",
        amber: "#c88a20",
        paper: "#f7f8f4",
      },
    },
  },
  plugins: [],
};
