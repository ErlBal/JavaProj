import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { api } from '../services/api';
import type { LoginRequest } from '../services/api';

export default function AuthForm() {
  const navigate = useNavigate();
  const [isLogin, setIsLogin] = useState(true);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState<LoginRequest>({
    username: '',
    password: ''
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = isLogin 
        ? await api.login(formData)
        : await api.register(formData);

      localStorage.setItem('jwtToken', response.token);
      localStorage.setItem('playerId', response.id.toString());
      navigate('/menu');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#232323',
    }}>
      <div style={{
        background: '#232323',
        border: '2px solid #a12a2a',
        borderRadius: 10,
        padding: '40px 32px',
        boxShadow: '0 4px 24px #111a',
        minWidth: 320,
        maxWidth: 360,
        width: '100%',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
      }}>
        <h2 style={{ color: '#a12a2a', fontWeight: 900, letterSpacing: 2, marginBottom: 24, fontSize: 28, textTransform: 'uppercase' }}>{isLogin ? 'Login' : 'Register'}</h2>
        {error && <div style={{ color: '#f4f4f4', background: '#a12a2a', borderRadius: 4, padding: '8px 12px', marginBottom: 16, fontWeight: 600 }}>{error}</div>}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, width: '100%' }}>
          <input
            type="text"
            name="username"
            placeholder="Username"
            value={formData.username}
            onChange={handleChange}
            required
            style={{ fontWeight: 600, letterSpacing: 1 }}
          />
          <input
            type="password"
            name="password"
            placeholder="Password"
            value={formData.password}
            onChange={handleChange}
            required
            style={{ fontWeight: 600, letterSpacing: 1 }}
          />
          <button type="submit" style={{ marginTop: 10 }}>{isLogin ? 'Login' : 'Register'}</button>
        </form>
        <button
          onClick={() => setIsLogin(!isLogin)}
          style={{
            marginTop: 18,
            background: 'none',
            border: 'none',
            color: '#a12a2a',
            fontWeight: 700,
            fontSize: 15,
            textDecoration: 'underline',
            cursor: 'pointer',
            boxShadow: 'none',
            padding: 0,
          }}
        >
          {isLogin ? 'Need an account? Register' : 'Already have an account? Login'}
        </button>
      </div>
    </div>
  );
} 