import AppLayout from "./components/AppLayout";
import ProtectedRoute from "./components/ProtectedRoute";

export default function App() {
  return (
    <ProtectedRoute>
      <AppLayout />
    </ProtectedRoute>
  );
}
