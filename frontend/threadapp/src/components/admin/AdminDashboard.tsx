import React from 'react';
import { Link } from 'react-router-dom';
import MainLayout from '../layout/MainLayout';
import { FaList, FaTag, FaUserShield, FaCommentSlash } from 'react-icons/fa';

const AdminDashboard: React.FC = () => {
  const adminTools = [
    {
      id: 'profanity',
      title: 'Profanity Management',
      description: 'Manage the profanity filter, add or remove words from the blocklist.',
      icon: <FaCommentSlash size={24} />,
      path: '/admin/profanity',
      color: 'bg-red-100 text-red-600'
    },
    {
      id: 'tags',
      title: 'Tag Management',
      description: 'Create, delete, and manage tags used across the platform.',
      icon: <FaTag size={24} />,
      path: '/admin/tags',
      color: 'bg-blue-100 text-blue-600'
    },
    // Add more admin tools here as they're implemented
  ];

  return (
    <MainLayout>
      <div className="container mx-auto px-4 py-8">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold">Admin Dashboard</h1>
          <div className="flex items-center">
            <FaUserShield className="text-gray-600 mr-2" size={20} />
            <span className="text-gray-600">Admin Controls</span>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {adminTools.map((tool) => (
            <Link
              key={tool.id}
              to={tool.path}
              className="block bg-white rounded-xl shadow-md hover:shadow-lg transition-shadow duration-200"
            >
              <div className="p-6">
                <div className={`inline-flex items-center justify-center p-3 rounded-full ${tool.color} mb-4`}>
                  {tool.icon}
                </div>
                <h2 className="text-xl font-semibold mb-2">{tool.title}</h2>
                <p className="text-gray-600">{tool.description}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </MainLayout>
  );
};

export default AdminDashboard; 