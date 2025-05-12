import React, { useEffect, useState } from 'react';
import { notificationService, NotificationDTO, NotificationType } from '../services/notificationService';
import { getCurrentUserId } from '../utils/authUtils';
import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import MainLayout from '../components/layout/MainLayout';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Toast } from 'primereact/toast';

// Number of notifications to show per page
const PAGE_SIZE = 20;

const NotificationsPage: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState<boolean>(true);
  const [currentPage, setCurrentPage] = useState<number>(0);
  
  const toast = React.useRef<Toast>(null);
  const userId = getCurrentUserId();

  // Load initial notifications
  useEffect(() => {
    if (!userId) return;
    
    const fetchInitialNotifications = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const result = await notificationService.getPaginatedNotifications(Number(userId), 0, PAGE_SIZE);
        setNotifications(result.notifications);
        setHasMore(result.hasMore);
        setCurrentPage(0);
      } catch (notifError) {
        setError('Failed to load notifications. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    
    fetchInitialNotifications();
  }, [userId]);
  
  const loadMoreNotifications = async () => {
    if (!userId || !hasMore || loadingMore) return;
    
    setLoadingMore(true);
    
    try {
      const nextPage = currentPage + 1;
      const result = await notificationService.getPaginatedNotifications(Number(userId), nextPage, PAGE_SIZE);
      
      if (result.notifications.length > 0) {
        setNotifications([...notifications, ...result.notifications]);
        setHasMore(result.hasMore);
        setCurrentPage(nextPage);
      } else {
        setHasMore(false);
      }
    } catch (error) {
      // Silent failure
    } finally {
      setLoadingMore(false);
    }
  };

  const handleMarkAllAsRead = async () => {
    if (!userId) return;
    
    try {
      await notificationService.markAllAsRead(Number(userId));
      setNotifications(notifications.map(n => ({ ...n, read: true })));
    } catch (error) {
      // Silent failure
    }
  };
  
  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications(notifications.map(n => 
        n.id === notificationId ? { ...n, read: true } : n
      ));
    } catch (error) {
      // Silent failure
    }
  };
  
  const handleDeleteNotification = async (notificationId: number) => {
    try {
      await notificationService.deleteNotification(notificationId);
      setNotifications(notifications.filter(n => n.id !== notificationId));
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
      default:
        return <><span className="text-gray-800">{notification.message || 'New notification'}</span></>;
    }
  };
  
  const renderNotifications = () => {
    if (loading) {
      return (
        <div className="p-8 flex justify-center items-center">
          <ProgressSpinner style={{ width: '50px', height: '50px' }} />
        </div>
      );
    }
    
    if (error) {
      return (
        <div className="p-8 text-center">
          <p className="text-red-500">{error}</p>
          <button 
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Retry
          </button>
        </div>
      );
    }
    
    if (notifications.length === 0) {
      return (
        <div className="p-8 text-center">
          <p className="text-gray-500">You don't have any notifications yet.</p>
        </div>
      );
    }
    
    // Add a "Mark all as read" button if there are unread notifications
    const hasUnreadNotifications = notifications.some(n => !n.read);
    
    return (
      <div>
        {hasUnreadNotifications && (
          <div className="flex justify-end mb-2 px-4">
            <Button 
              label="Mark all as read" 
              className="p-button-text p-button-sm" 
              onClick={handleMarkAllAsRead}
            />
          </div>
        )}
        
        <div className="divide-y">
          {notifications.map(notification => (
            <div 
              key={notification.id} 
              className={`p-4 hover:bg-gray-50 flex items-start ${notification.read ? 'opacity-75' : ''}`}
            >
              <div className="flex-grow">
                <div className="flex items-center gap-2">
                  <Link 
                    to={getNotificationLink(notification)}
                    onClick={() => !notification.read && handleMarkAsRead(notification.id)}
                    className="block mb-1 font-medium text-gray-800"
                  >
                    {getNotificationMessage(notification)}
                  </Link>
                  {!notification.read && (
                    <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2 py-0.5 rounded">
                      New
                    </span>
                  )}
                </div>
                <div className="text-sm text-gray-500">
                  {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
                </div>
              </div>
              <button 
                onClick={() => handleDeleteNotification(notification.id)}
                className="text-gray-400 hover:text-gray-600"
              >
                <i className="pi pi-trash"></i>
              </button>
            </div>
          ))}
        </div>
        
        {hasMore && (
          <div className="p-4 flex justify-center">
            <Button 
              label={loadingMore ? "Loading..." : "Load More"} 
              icon={loadingMore ? "pi pi-spin pi-spinner" : "pi pi-arrow-down"}
              onClick={loadMoreNotifications}
              disabled={loadingMore}
              className="p-button-outlined"
            />
          </div>
        )}
      </div>
    );
  };
  
  return (
    <MainLayout>
      <Toast ref={toast} position="top-right" />
      <div className="max-w-4xl mx-auto bg-white shadow-lg rounded-lg overflow-hidden">
        <h1 className="text-2xl font-bold mb-4 p-4 text-gray-800">Notifications</h1>
        
        <Card>
          {renderNotifications()}
        </Card>
      </div>
    </MainLayout>
  );
};

export default NotificationsPage; 