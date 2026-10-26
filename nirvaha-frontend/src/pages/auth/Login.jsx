import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Card } from '../../components/ui';
import { Shield, Lock, Mail, ArrowRight } from 'lucide-react';
import { useNavigate, Link, useLocation } from 'react-router-dom';

export const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const searchParams = new URLSearchParams(location.search);
  const redirectTarget = searchParams.get('redirect');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const DEMO_PRESETS = [
    { role: 'Citizen', email: 'citizen@puducherry.gov.in', password: 'Citizen123!' },
    { role: 'Officer', email: 'officer@puducherry.gov.in', password: 'Officer123!' },
    { role: 'Admin', email: 'admin@puducherry.gov.in', password: 'Admin123!' },
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const res = await login(email, password);
      if (res && res.user) {
        if (redirectTarget) {
          navigate(redirectTarget);
        } else if (res.user.role === 'CITIZEN') {
          navigate('/citizen/report');
        } else {
          navigate('/command-center');
        }
      }
    } catch (err) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-16">
      <Card className="space-y-6 border-[#D9DEDB] bg-white shadow-md">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-[#163A36] text-white rounded-xl flex items-center justify-center mx-auto border border-[#2F6F68]/30 shadow-sm">
            <Shield className="w-6 h-6 text-[#C87941]" />
          </div>
          <h2 className="text-xl font-bold text-[#18201F]">Nirvaha Account Login</h2>
          <p className="text-xs text-[#66716E]">Authenticated access for citizens, officers, and administrators</p>
        </div>

        <div className="bg-[#F5F7F6] border border-[#D9DEDB] p-2.5 rounded-lg space-y-1.5">
          <span className="block text-[11px] font-bold text-[#18201F] uppercase tracking-wider text-center">
            Demo Quick-Fill
          </span>
          <div className="grid grid-cols-3 gap-2">
            {DEMO_PRESETS.map((preset) => (
              <button
                key={preset.role}
                type="button"
                onClick={() => {
                  setEmail(preset.email);
                  setPassword(preset.password);
                  setError(null);
                }}
                className="py-1 px-2 bg-white hover:bg-[#EBF2F1] text-[#163A36] hover:text-[#2F6F68] font-semibold rounded border border-[#D9DEDB] text-xs transition text-center shadow-xs"
              >
                {preset.role}
              </button>
            ))}
          </div>
        </div>

        {error && (
          <div className="bg-[#B94A48]/10 border border-[#B94A48]/30 p-3 rounded-lg text-xs text-[#B94A48] font-semibold">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block font-bold text-[#18201F] uppercase tracking-wider mb-1">Email Address</label>
            <div className="input-with-icon">
              <input
                type="email"
                required
                placeholder="e.g. officer@puducherry.gov.in"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] text-xs focus:outline-none focus:border-[#163A36]"
              />
              <Mail className="w-4 h-4 text-[#66716E]" />
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="block font-bold text-[#18201F] uppercase tracking-wider">Password</label>
              <Link to="/forgot-password" className="text-[11px] text-[#2F6F68] hover:underline font-semibold">Forgot password?</Link>
            </div>
            <div className="input-with-icon">
              <input
                type="password"
                required
                placeholder="Enter password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] text-xs focus:outline-none focus:border-[#163A36]"
              />
              <Lock className="w-4 h-4 text-[#66716E]" />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-[#163A36] hover:bg-[#2F6F68] text-white font-bold rounded-lg shadow flex items-center justify-center space-x-2 transition"
          >
            <span>{loading ? 'Authenticating...' : 'Sign In'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <p className="border-t border-[#D9DEDB] pt-4 text-center text-xs text-[#66716E]">
          New to Nirvaha? <Link to="/signup" className="font-bold text-[#2F6F68] hover:underline">Create a citizen account</Link>
        </p>
      </Card>
    </div>
  );
};
