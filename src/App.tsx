import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import AuthForm from './components/AuthForm';
import MainMenu from './components/MainMenu';
import Lobby from './components/Lobby';
import Game from './components/Game';
import { isLoggedIn } from './utils/auth';

function RequireAuth({ children }: { children: JSX.Element }) {
  const location = useLocation();
  if (!isLoggedIn()) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<AuthForm />} />
      <Route path="/menu" element={
        <RequireAuth>
          <MainMenu />
        </RequireAuth>
      } />
      <Route path="/lobby" element={
        <RequireAuth>
          <Lobby />
        </RequireAuth>
      } />
      <Route path="/game" element={
        <RequireAuth>
          <Game />
        </RequireAuth>
      } />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
} 