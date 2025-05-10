import React, { useState } from 'react';
import { FaTimes, FaTrash } from 'react-icons/fa';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';
import { HexColorPicker } from 'react-colorful';

interface GraphEdge {
  id: number;
  sourceNodeId: number;
  targetNodeId: number;
  label: string;
  type: string;
  weight: number;
  color: string;
  threadId: number;
}

interface EdgeDetailsProps {
  edge: GraphEdge;
  onClose: () => void;
  onUpdate: () => void;
  onDelete?: (edgeId: number) => void;
}

const EdgeDetails: React.FC<EdgeDetailsProps> = ({ edge, onClose, onUpdate, onDelete }) => {
  const [label, setLabel] = useState(edge.label || '');
  const [type, setType] = useState(edge.type || 'default');
  const [weight, setWeight] = useState(edge.weight || 1);
  const [color, setColor] = useState(edge.color || '#555555');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.update(edge.id),
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            label,
            type,
            weight,
            color
          })
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }

      onUpdate();
      onClose();
    } catch (err) {
      console.error('Error updating edge:', err);
      setError('Failed to update edge');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    // Confirm deletion
    if (!window.confirm('Are you sure you want to delete this connection?')) {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const response = await fetchWithAuth(
        API_ENDPOINTS.graph.edges.delete(edge.id),
        {
          method: 'DELETE'
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }

      if (onDelete) {
        onDelete(edge.id);
      } else {
        onUpdate();
      }
      onClose();
    } catch (err) {
      console.error('Error deleting edge:', err);
      setError('Failed to delete connection');
      setIsDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white/95 backdrop-blur-sm rounded-lg p-6 max-w-md w-full shadow-xl">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-semibold">Edit Connection</h3>
          <button 
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700"
          >
            <FaTimes />
          </button>
        </div>

        <div className="flex justify-between items-center mb-4">
          <div className="text-sm text-gray-600">
            Connection ID: {edge.id}
          </div>
          <button
            onClick={handleDelete}
            disabled={isDeleting}
            className="flex items-center gap-1 text-red-500 hover:text-red-700 px-3 py-1 rounded-md hover:bg-red-50 transition-colors"
          >
            <FaTrash size={14} />
            <span>{isDeleting ? 'Deleting...' : 'Delete'}</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Label (optional)
            </label>
            <input
              type="text"
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g., 'depends on', 'related to'"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="default">Default</option>
              <option value="straight">Straight</option>
              <option value="step">Step</option>
              <option value="smoothstep">Smooth Step</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Weight: {weight}
            </label>
            <input
              type="range"
              min="1"
              max="10"
              value={weight}
              onChange={(e) => setWeight(parseInt(e.target.value))}
              className="w-full accent-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Edge Color
            </label>
            <div className="flex flex-col md:flex-row gap-4">
              <div className="w-full md:w-1/2">
                <HexColorPicker 
                  color={color} 
                  onChange={setColor} 
                  style={{ width: '100%', height: '150px' }}
                />
              </div>
              <div className="flex-1 flex flex-col">
                <input
                  type="text"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md mb-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <div className="h-12 w-full rounded-md mt-2 border border-gray-200" style={{ backgroundColor: color }}></div>
              </div>
            </div>
          </div>

          {error && (
            <div className="text-red-500 text-sm text-center">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition-colors"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EdgeDetails; 