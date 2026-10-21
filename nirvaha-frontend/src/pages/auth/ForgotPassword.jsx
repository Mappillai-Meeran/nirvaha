import React, { useState } from 'react';
import { authService } from '../../services';
import { Card } from '../../components/ui';
import { KeyRound, Mail, ArrowRight, CheckCircle2, AlertTriangle, ShieldCheck, ArrowLeft } from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';

export const ForgotPassword = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [tokenMode, setTokenMode] = useState(false);
  const [manualToken, setManualToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [serverMessage, setServerMessage] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleInitiateReset = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setServerMessage(null);
    try {
      const res = await authService.forgotPassword({ email });
      setServerMessage(res.message || 'If an account exists, password recovery has been initiated.');
    } catch (err) {
      setError(err.message || 'Failed to submit recovery request');
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteReset = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await authService.resetPassword({ token: manualToken.trim(), newPassword });
      setSuccessMsg('Password reset successfully! Redirecting to login...');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err.message || 'Failed to reset password. Please check your token.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-16">
      <Card className="space-y-6 border-[#D9DEDB] bg-white shadow-md">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 bg-[#163A36] text-white rounded-xl flex items-center justify-center mx-auto border border-[#2F6F68]/30 shadow-sm">
            <KeyRound className="w-6 h-6 text-[#C87941]" />
          </div>
          <h2 className="text-xl font-bold text-[#18201F]">Account Password Recovery</h2>
          <p className="text-xs text-[#66716E]">
            {tokenMode
              ? 'Enter your administrative recovery token and set a new password'
              : 'Submit your registered email address to request account recovery'}
          </p>
        </div>

        {/* Server message on forgot-password dispatch */}
        {serverMessage && (
          <div className="space-y-3">
            <div className="bg-[#163A36]/5 border border-[#163A36]/20 p-3.5 rounded-lg text-xs text-[#18201F] space-y-2">
              <div className="flex items-center space-x-2 font-semibold text-[#163A36]">
                <ShieldCheck className="w-4 h-4 text-[#2F6F68]" />
                <span>Recovery Request Processed</span>
              </div>
              <p className="text-[#404947] leading-relaxed">{serverMessage}</p>
            </div>
            <div className="bg-amber-50 border border-amber-200 p-3 rounded-lg text-xs text-amber-800 flex items-start space-x-2">
              <AlertTriangle className="w-4 h-4 text-amber-600 mt-0.5 shrink-0" />
              <span>
                <strong>Delivery Limitation Notice:</strong> Automated outbound email (SMTP) is not currently active on this instance. Recovery tokens are never exposed to the browser to safeguard municipal systems. Please contact your ward administrator for verified password resets.
              </span>
            </div>
          </div>
        )}

        {successMsg && (
          <div className="bg-[#3F7D5A]/10 border border-[#3F7D5A]/30 p-3 rounded-lg text-xs text-[#3F7D5A] flex items-center space-x-2 font-semibold">
            <CheckCircle2 className="w-4 h-4 text-[#3F7D5A]" />
            <span>{successMsg}</span>
          </div>
        )}

        {error && (
          <div className="bg-[#B94A48]/10 border border-[#B94A48]/30 p-3 rounded-lg text-xs text-[#B94A48] font-semibold">
            {error}
          </div>
        )}

        {!tokenMode ? (
          <form onSubmit={handleInitiateReset} className="space-y-4 text-xs">
            <div>
              <label className="block font-bold text-[#18201F] uppercase tracking-wider mb-1">Registered Email</label>
              <div className="relative">
                <input
                  type="email"
                  required
                  placeholder="e.g. resident@puducherry.gov.in"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] pl-9 text-xs focus:outline-none focus:border-[#163A36]"
                />
                <Mail className="w-4 h-4 text-[#66716E] absolute left-3 top-2.5" />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-[#163A36] hover:bg-[#2F6F68] text-white font-bold rounded-lg shadow flex items-center justify-center space-x-2 transition disabled:opacity-50"
            >
              <span>{loading ? 'Processing...' : 'Request Password Reset'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <div className="pt-2 border-t border-[#D9DEDB]/60 flex items-center justify-between text-xs text-[#66716E]">
              <button
                type="button"
                onClick={() => { setTokenMode(true); setError(null); }}
                className="text-[#2F6F68] hover:underline font-medium"
              >
                Have a recovery token?
              </button>
              <Link to="/login" className="flex items-center space-x-1 text-[#66716E] hover:text-[#18201F]">
                <ArrowLeft className="w-3 h-3" />
                <span>Back to Login</span>
              </Link>
            </div>
          </form>
        ) : (
          <form onSubmit={handleCompleteReset} className="space-y-4 text-xs">
            <div>
              <label className="block font-bold text-[#18201F] uppercase tracking-wider mb-1">Recovery Token</label>
              <input
                type="text"
                required
                placeholder="Paste your 64-character token"
                value={manualToken}
                onChange={(e) => setManualToken(e.target.value)}
                className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] font-mono text-xs focus:outline-none focus:border-[#163A36]"
              />
            </div>

            <div>
              <label className="block font-bold text-[#18201F] uppercase tracking-wider mb-1">New Account Password</label>
              <input
                type="password"
                required
                minLength={6}
                placeholder="At least 6 characters"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] text-xs focus:outline-none focus:border-[#3F7D5A]"
              />
            </div>

            <div>
              <label className="block font-bold text-[#18201F] uppercase tracking-wider mb-1">Confirm New Password</label>
              <input
                type="password"
                required
                minLength={6}
                placeholder="Re-enter new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="w-full bg-white border border-[#D9DEDB] rounded-lg px-3 py-2 text-[#18201F] text-xs focus:outline-none focus:border-[#3F7D5A]"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-[#3F7D5A] hover:bg-[#326447] text-white font-bold rounded-lg shadow flex items-center justify-center space-x-2 transition disabled:opacity-50"
            >
              <span>{loading ? 'Updating Password...' : 'Reset Account Password'}</span>
            </button>

            <div className="pt-2 border-t border-[#D9DEDB]/60 flex items-center justify-between text-xs text-[#66716E]">
              <button
                type="button"
                onClick={() => { setTokenMode(false); setError(null); }}
                className="text-[#2F6F68] hover:underline font-medium"
              >
                Request reset instructions
              </button>
              <Link to="/login" className="flex items-center space-x-1 text-[#66716E] hover:text-[#18201F]">
                <ArrowLeft className="w-3 h-3" />
                <span>Back to Login</span>
              </Link>
            </div>
          </form>
        )}
      </Card>
    </div>
  );
};
