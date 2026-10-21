import React, { useState } from 'react';
import { ArrowRight, CheckCircle2, Lock, Mail, UserRound } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { authService } from '../../services';
import { useAuth } from '../../context/AuthContext';

export const SignUp = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    if (form.password.length < 8) {
      setError('Use at least 8 characters for your password.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('The two passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await authService.register({ name: form.name.trim(), email: form.email.trim(), password: form.password });
      await login(form.email.trim(), form.password);
      navigate('/citizen/report');
    } catch (requestError) {
      setError(requestError.message || 'We could not create your account. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-12">
      <section className="bg-[#fffefb] border border-[#d8ddda] rounded-lg p-6 shadow-sm space-y-6">
        <div className="space-y-2">
          <span className="eyebrow">Citizen account</span>
          <h1 className="text-2xl font-bold text-[#1e2b2d]">Create your Nirvaha account</h1>
          <p className="text-sm text-[#66716e]">Use one account to submit issues, follow updates, and confirm whether work was completed.</p>
        </div>

        <div className="flex items-start gap-2 rounded-md bg-[#eef5f1] p-3 text-xs text-[#396154]">
          <CheckCircle2 size={16} className="shrink-0" />
          <span>Public registration creates a citizen account. Municipal officer access is issued by the administration.</span>
        </div>

        {error && <div className="rounded-md border border-[#d89a90] bg-[#fff1ef] p-3 text-sm text-[#943827]">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <label className="block"><span className="field-label">Your name</span><span className="input-with-icon"><UserRound size={16} /><input className="field-control" required autoComplete="name" value={form.name} onChange={update('name')} placeholder="Your full name" /></span></label>
          <label className="block"><span className="field-label">Email address</span><span className="input-with-icon"><Mail size={16} /><input className="field-control" required type="email" autoComplete="email" value={form.email} onChange={update('email')} placeholder="name@example.com" /></span></label>
          <label className="block"><span className="field-label">Password</span><span className="input-with-icon"><Lock size={16} /><input className="field-control" required type="password" minLength="8" autoComplete="new-password" value={form.password} onChange={update('password')} placeholder="At least 8 characters" /></span></label>
          <label className="block"><span className="field-label">Confirm password</span><span className="input-with-icon"><Lock size={16} /><input className="field-control" required type="password" minLength="8" autoComplete="new-password" value={form.confirmPassword} onChange={update('confirmPassword')} placeholder="Repeat your password" /></span></label>
          <button className="primary-action mt-1" type="submit" disabled={loading}>{loading ? 'Creating account...' : 'Create citizen account'}<ArrowRight size={17} /></button>
        </form>

        <p className="border-t border-[#d8ddda] pt-4 text-center text-sm text-[#66716e]">Already have an account? <Link className="font-bold text-[#174f4a] hover:underline" to="/login">Sign in</Link></p>
      </section>
    </div>
  );
};
