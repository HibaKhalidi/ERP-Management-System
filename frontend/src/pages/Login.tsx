import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  if (user) return <Navigate to="/" replace />;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      nav('/');
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <form className="card login" onSubmit={submit}>
      <h2>ERP — Connexion</h2>
      {error && <div className="error">{error}</div>}
      <div className="field"><label>Utilisateur</label><input value={username} onChange={(e) => setUsername(e.target.value)} required /></div>
      <div className="field"><label>Mot de passe</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
      <button type="submit" style={{ width: '100%' }}>Se connecter</button>
      <p className="muted" style={{ fontSize: 12 }}>Démo : admin / admin123</p>
    </form>
  );
}
