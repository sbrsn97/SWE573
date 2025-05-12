import React, { useState, useEffect, useRef } from 'react';
import { FaBell } from 'react-icons/fa';
import { notificationService, NotificationDTO, NotificationType } from '../../services/notificationService';
import { getCurrentUserId } from '../../utils/authUtils';
import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';

interface NotificationBellProps {
  className?: string;
}

const NotificationBell: React.FC<NotificationBellProps> = ({ className = '' }) => {
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const userId = getCurrentUserId();

  useEffect(() => {
    if (!userId) return;

    // Fetch unread count
    const fetchUnreadCount = async () => {
      try {
        const count = await notificationService.getUnreadCount(Number(userId));
        setUnreadCount(count);
      } catch (error) {
        // Silent failure to prevent UI issues
      }
    };

    fetchUnreadCount();

    // Poll for updates every 30 seconds
    const interval = setInterval(fetchUnreadCount, 30000);

    return () => clearInterval(interval);
  }, [userId]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleBellClick = async () => {
    if (!userId) return;

    if (!isOpen) {
      // Fetch notifications when opening the dropdown
      try {
        // Get only unread notifications for the bell dropdown
        const notifs = await notificationService.getUnreadNotifications(Number(userId));
        setNotifications(notifs);
      } catch (error) {
        // Silent failure
      }
    }

    setIsOpen(!isOpen);
  };

  const handleMarkAllAsRead = async () => {
    if (!userId) return;

    try {
      await notificationService.markAllAsRead(Number(userId));
      setUnreadCount(0);
      setNotifications([]);
    } catch (error) {
      // Silent failure
    }
  };

  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(notifications.filter(n => n.id !== notificationId));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      // Silent failure
    }
  };

  const getNotificationLink = (notification: NotificationDTO) => {
    switch (notification.type) {
      case NotificationType.NEW_COMMENT_ON_THREAD:
      case NotificationType.NEW_COMMENT_ON_FOLLOWED_THREAD:
      case NotificationType.THREAD_UPVOTE:
      case NotificationType.THREAD_DOWNVOTE:
      case NotificationType.THREAD_UPDATED:
      case NotificationType.NEW_THREAD_FOLLOWED:
        return notification.threadId ? `/threads/${notification.threadId}` : '#';
      case NotificationType.COMMENT_REPLY:
      case NotificationType.COMMENT_UPVOTE:
      case NotificationType.COMMENT_DOWNVOTE:
        return notification.threadId ? `/threads/${notification.threadId}` : '#';
      case NotificationType.USER_FOLLOWED:
      case NotificationType.USER_UNFOLLOWED:
        return notification.actionUserId ? `/users/${notification.actionUserId}` : '#';
      default:
        return '#';
    }
  };

  // Format notification message based on type
  const getNotificationMessage = (notification: NotificationDTO) => {
    const username = notification.actionUsername || 'Someone';
    
    switch (notification.type) {
      case NotificationType.THREAD_UPVOTE:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">upvoted your thread</span></>;
      case NotificationType.THREAD_DOWNVOTE:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">downvoted your thread</span></>;
      case NotificationType.COMMENT_UPVOTE:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">upvoted your comment</span></>;
      case NotificationType.COMMENT_DOWNVOTE:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">downvoted your comment</span></>;
      case NotificationType.NEW_COMMENT_ON_THREAD:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">commented on your thread</span></>;
      case NotificationType.NEW_COMMENT_ON_FOLLOWED_THREAD:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">commented on a thread you follow</span></>;
      case NotificationType.COMMENT_REPLY:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">replied to your comment</span></>;
      case NotificationType.USER_FOLLOWED:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">started following you</span></>;
      case NotificationType.USER_UNFOLLOWED:
        return <><span className="font-bold text-gray-800">{username}</span> <span className="text-gray-800">unfollowed you</span></>;
      case NotificationType.THREAD_UPDATED:
        return <><span className="text-gray-800">A thread you follow was updated</span></>;
      case NotificationType.NEW_THREAD_FOLLOWED:
        return <><span className="text-gray-800">You started following a thread</span></>;
      // You can add more cases as needed
      default:
        return <><span className="text-gray-800">{notification.message || 'New notification'}</span></>;
    }
  };

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      <button
        className="relative p-2 rounded-full hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        onClick={handleBellClick}
        aria-expanded={isOpen}
      >
        <FaBell className="h-6 w-6 text-gray-600" />
        {unreadCount > 0 && (
          <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-red-100 transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg overflow-hidden z-50">
          <div className="py-2">
            <div className="px-4 py-2 text-sm font-medium text-gray-700 border-b flex justify-between items-center">
              <span>Notifications</span>
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllAsRead}
                  className="text-xs text-indigo-600 hover:text-indigo-900"
                >
                  Mark all as read
                </button>
              )}
            </div>
            <div className="max-h-96 overflow-y-auto">
              {notifications.length > 0 ? (
                notifications.map((notification) => (
                  <div key={notification.id} className="px-4 py-3 hover:bg-gray-50 border-b">
                    <Link 
                      to={getNotificationLink(notification)}
                      onClick={() => handleMarkAsRead(notification.id)}
                      className="block"
                    >
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium text-gray-900">
                          {getNotificationMessage(notification)}
                        </p>
                      </div>
                      <p className="text-xs text-gray-500 mt-1">
                        {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
                      </p>
                    </Link>
                  </div>
                ))
              ) : (
                <div className="px-4 py-6 text-sm text-gray-500 text-center">
                  No unread notifications
                </div>
              )}
            </div>
            <div className="px-4 py-2 border-t">
              <Link
                to="/notifications"
                className="block text-center text-sm font-medium text-indigo-600 hover:text-indigo-500"
                onClick={() => setIsOpen(false)}
              >
                View all notifications
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell; 