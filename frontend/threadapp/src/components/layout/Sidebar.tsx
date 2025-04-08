import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaPlus } from 'react-icons/fa';
import CreateThreadModal from '../threads/CreateThreadModal';

const Sidebar = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();

  const handleThreadCreated = (threadId: number) => {
    navigate(`/threads/${threadId}`);
  };

  return (
    <div className="fixed left-0 top-[60px] h-[calc(100vh-60px)] w-64 bg-white shadow-sm border-r border-gray-200">
      <div className="p-4">
        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center justify-center gap-2 w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors duration-200 shadow-sm hover:shadow-md"
        >
          <FaPlus className="text-lg text-white" />
          <span className="font-medium text-white">New Thread</span>
        </button>
      </div>

      <CreateThreadModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onThreadCreated={handleThreadCreated}
      />
    </div>
  );
};

export default Sidebar;