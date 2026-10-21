import React, { useState, useEffect, useRef, useCallback } from 'react';
import { 
  Bell, 
  Check, 
  CheckCheck, 
  Clock, 
  AlertTriangle, 
  UserCheck, 
  CheckCircle2, 
  FileCheck, 
  RefreshCw, 
  X,
  ExternalLink 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { notificationService } from '../../services';

export const NotificationDropdown = () => {
  const { currentUser, ROLES } = useAuth();
  const navigate = useNavigate();

  const [isOpen, setIsOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const dropdownRef = useRef(null);

  const fetchUnreadCount = useCallback(async () => {
    if (!currentUser) return;
    try {
      const data = await notificationService.getUnreadCount();
      if (data && typeof data.unreadCount === 'number') {
        setUnreadCount(data.unreadCount);
      }
    } catch {
      // Gracefully ignore background polling errors to avoid disruption
    }
  }, [currentUser]);

  const fetchNotifications = useCallback(async () => {
    if (!currentUser) return;
    setLoading(true);
    setError(null);
    try {
      const data = await notificationService.getNotifications(0, 20);
      setNotifications(data?.content || []);
    } catch {
      setError('Unable to load notifications. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [currentUser]);

  // Initial and periodic unread count polling (every 30 seconds)
  useEffect(() => {
    if (!currentUser) return;
    const initialTimer = setTimeout(fetchUnreadCount, 0);
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => {
      clearTimeout(initialTimer);
      clearInterval(interval);
    };
  }, [currentUser, fetchUnreadCount]);

  // Load notification details when dropdown is opened
  useEffect(() => {
    if (isOpen) {
      const openTimer = setTimeout(() => {
        fetchNotifications();
        fetchUnreadCount();
      }, 0);
      return () => clearTimeout(openTimer);
    }
  }, [isOpen, fetchNotifications, fetchUnreadCount]);

  // Outside click & ESC listener to close dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleToggle = () => {
    setIsOpen((prev) => !prev);
  };

  const handleMarkAsRead = async (notification, event) => {
    event?.stopPropagation();
    if (notification.read) return;

    try {
      await notificationService.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === notification.id ? { ...n, read: true, readAt: new Date().toISOString() } : n
        )
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // If marking fails, keep original state
    }
  };

  const handleMarkAllAsRead = async () => {
    if (unreadCount === 0) return;
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, read: true, readAt: new Date().toISOString() }))
      );
      setUnreadCount(0);
    } catch {
      // Ignore failure
    }
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      await handleMarkAsRead(notification);
    }
    setIsOpen(false);

    // Context-sensitive routing
    if (notification.incidentId) {
      if (currentUser?.role === ROLES.CITIZEN) {
        navigate('/citizen/my-reports');
      } else {
        navigate('/command-center/queue');
      }
    }
  };

  const formatTimestamp = (isoDate) => {
    if (!isoDate) return '';
    try {
      const date = new Date(isoDate);
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMins / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString();
    } catch {
      return '';
    }
  };

  const getEventIcon = (eventType) => {
    switch (eventType) {
      case 'INCIDENT_ASSIGNED':
        return <UserCheck className="w-4 h-4 text-[#163A36]" />;
      case 'STATUS_CHANGED':
        return <Clock className="w-4 h-4 text-[#C58A32]" />;
      case 'RESOLUTION_SUBMITTED':
        return <CheckCircle2 className="w-4 h-4 text-[#3F7D5A]" />;
      case 'RESOLUTION_CONFIRMED':
        return <CheckCheck className="w-4 h-4 text-[#3F7D5A]" />;
      case 'RESOLUTION_DISPUTED':
        return <AlertTriangle className="w-4 h-4 text-[#B94A48]" />;
      case 'DISPUTE_REVIEWED':
        return <FileCheck className="w-4 h-4 text-[#163A36]" />;
      default:
        return <Bell className="w-4 h-4 text-[#66716E]" />;
    }
  };

  if (!currentUser) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Trigger Button */}
      <button
        type="button"
        onClick={handleToggle}
        aria-label={
          unreadCount > 0
            ? `${unreadCount} unread notifications`
            : 'Notifications'
        }
        aria-expanded={isOpen}
        className="relative p-2 text-[#596765] hover:text-[#163A36] hover:bg-[#f0f2ee] rounded-full transition focus:outline-none focus:ring-2 focus:ring-[#163A36]/30"
      >
        <Bell size={18} />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-[#B94A48] rounded-full ring-2 ring-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown Panel */}
      {isOpen && (
        <div 
          className="absolute right-0 mt-2 w-80 sm:w-96 max-w-[calc(100vw-24px)] bg-white border border-[#D9DEDB] rounded-lg shadow-xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-100"
          role="region"
          aria-label="Notification center"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-[#F5F4EF] border-b border-[#D9DEDB]">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-[#18201F]">
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#B94A48]/10 text-[#B94A48] border border-[#B94A48]/30">
                  {unreadCount} unread
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllAsRead}
                  className="text-[11px] font-semibold text-[#163A36] hover:underline flex items-center gap-1"
                >
                  <Check size={12} />
                  Mark all read
                </button>
              )}
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="text-[#66716E] hover:text-[#18201F] p-1 rounded"
                aria-label="Close notification menu"
              >
                <X size={14} />
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="max-h-96 overflow-y-auto divide-y divide-[#E9EDEB]">
            {loading ? (
              <div className="flex flex-col items-center justify-center py-8 text-[#66716E]">
                <RefreshCw className="w-5 h-5 animate-spin text-[#163A36] mb-2" />
                <span className="text-xs">Loading notifications...</span>
              </div>
            ) : error ? (
              <div className="p-4 text-center">
                <p className="text-xs text-[#B94A48] mb-2">{error}</p>
                <button
                  type="button"
                  onClick={fetchNotifications}
                  className="px-2.5 py-1 text-xs bg-[#163A36] text-white rounded font-medium hover:bg-[#123e3a] transition"
                >
                  Retry
                </button>
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-10 px-4 text-center text-[#66716E]">
                <Bell className="w-8 h-8 text-[#D9DEDB] mx-auto mb-2" />
                <p className="text-xs font-bold text-[#18201F]">No notifications yet</p>
                <p className="text-[11px] text-[#66716E] mt-1 max-w-[240px] mx-auto">
                  Lifecycle updates regarding assignments, resolutions, and reviews will appear here.
                </p>
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  onClick={() => handleNotificationClick(item)}
                  className={`p-3 text-left transition cursor-pointer flex gap-3 items-start ${
                    item.read
                      ? 'bg-white hover:bg-[#FAF9F6]'
                      : 'bg-[#F2F7F5] hover:bg-[#E8F1EE] border-l-4 border-l-[#163A36]'
                  }`}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleNotificationClick(item);
                    }
                  }}
                >
                  <div className="mt-0.5 p-1.5 rounded bg-white border border-[#D9DEDB] shadow-2xs shrink-0">
                    {getEventIcon(item.eventType)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <span className={`text-xs truncate ${item.read ? 'font-medium text-[#18201F]' : 'font-bold text-[#18201F]'}`}>
                        {item.title}
                      </span>
                      <span className="text-[10px] text-[#889290] shrink-0 font-mono">
                        {formatTimestamp(item.createdAt)}
                      </span>
                    </div>
                    <p className="text-[11px] text-[#596765] line-clamp-2 leading-relaxed">
                      {item.message}
                    </p>
                    {item.incidentId && (
                      <div className="mt-1.5 flex items-center justify-between">
                        <span className="inline-flex items-center text-[10px] font-semibold text-[#163A36] hover:underline">
                          Incident #{item.incidentId} <ExternalLink size={10} className="ml-1" />
                        </span>
                        {!item.read && (
                          <button
                            type="button"
                            onClick={(e) => handleMarkAsRead(item, e)}
                            className="text-[10px] text-[#66716E] hover:text-[#163A36] underline"
                          >
                            Mark as read
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="px-4 py-2 bg-[#F5F4EF] border-t border-[#D9DEDB] text-center">
              <span className="text-[10px] text-[#66716E]">
                Showing latest civic notifications • Nirvaha Secure In-App Hub
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
