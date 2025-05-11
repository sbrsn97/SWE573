import { Link } from 'react-router-dom';
import { FaThumbsUp, FaRegClock, FaUserPlus, FaTag } from 'react-icons/fa';
import Tag from '../tags/Tag';

interface ThreadCardProps {
  thread: {
    id: number;
    title: string;
    description: string | null;
    authorId: number;
    upvoteCount: number;
    downvoteCount: number;
    createdAt: string;
    tags: Array<{
      id: number;
      label: string;
      description: string;
      colorCodeString: string;
      wikidataEntityId: string;
    }>;
  };
}

const ThreadCard: React.FC<ThreadCardProps> = ({ thread }) => {
  // Format date to a readable string
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  // Truncate description to a specific length
  const truncateDescription = (text: string | null, maxLength: number = 150) => {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  return (
    <div className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow p-4">
      <Link to={`/threads/${thread.id}`} className="block">
        <h3 className="text-lg font-semibold text-blue-800 hover:text-blue-600 mb-2 transition-colors">
          {thread.title}
        </h3>
      </Link>
      
      {thread.description && (
        <p className="text-gray-600 text-sm mb-3">
          {truncateDescription(thread.description)}
        </p>
      )}
      
      <div className="flex flex-wrap gap-2 mb-2">
        {thread.tags.slice(0, 3).map(tag => (
          <Tag key={tag.id} tag={tag} />
        ))}
        {thread.tags.length > 3 && (
          <span className="inline-flex items-center bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded">
            <FaTag className="mr-1" size={10} />
            +{thread.tags.length - 3} more
          </span>
        )}
      </div>
      
      <div className="flex items-center justify-between text-xs text-gray-500 mt-2">
        <div className="flex items-center gap-2">
          <span className="flex items-center">
            <FaThumbsUp className="mr-1" />
            {thread.upvoteCount - thread.downvoteCount}
          </span>
          <span className="flex items-center">
            <FaRegClock className="mr-1" />
            {formatDate(thread.createdAt)}
          </span>
        </div>
        <Link 
          to={`/threads/${thread.id}`} 
          className="text-blue-600 hover:text-blue-800 font-medium"
        >
          Read more →
        </Link>
      </div>
    </div>
  );
};

export default ThreadCard; 