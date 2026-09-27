import { Navigate } from 'react-router-dom';
import { useStoredUser } from '../utils/authStore';

const PrivateRoute = ({ children }) => {
  const user = useStoredUser();

  // Si pas d'utilisateur ou pas de token, retour au login
  return user && user.token ? children : <Navigate to="/login" />;
};

export default PrivateRoute;
