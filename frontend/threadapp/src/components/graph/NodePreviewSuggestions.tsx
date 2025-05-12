import React, { useState, useEffect } from 'react';
import { FaSpinner, FaExternalLinkAlt, FaPlus } from 'react-icons/fa';
import { API_ENDPOINTS } from '../../config/config';
import { fetchWithAuth } from '../../utils/authUtils';

interface WikidataEntity {
  id: string;
  label: string;
  description: string;
  url?: string;
}

interface WikidataProperty {
  id: string;
  label: string;
  description: string;
}

interface NodePreview {
  keywords: string[];
  suggestedEntities: WikidataEntity[];
  suggestedProperties: WikidataProperty[];
}

interface NodePreviewSuggestionsProps {
  label: string;
  description: string;
  onEntitySelect?: (entity: WikidataEntity) => void;
  onPropertySelect?: (property: WikidataProperty) => void;
}

const NodePreviewSuggestions: React.FC<NodePreviewSuggestionsProps> = ({
  label,
  description,
  onEntitySelect,
  onPropertySelect
}) => {
  const [preview, setPreview] = useState<NodePreview | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Only fetch preview if we have content to analyze
    if (!label.trim() && !description.trim()) {
      setPreview(null);
      return;
    }

    const fetchPreview = async () => {
      setLoading(true);
      setError(null);

      try {
        // Use POST method instead of GET
        const response = await fetchWithAuth(
          API_ENDPOINTS.graph.preview.generate,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({
              label: label || '',
              description: description || ''
            })
          }
        );

        if (!response.ok) {
          throw new Error(`Failed to fetch preview: ${response.status}`);
        }

        const data = await response.json();
        
        // Sort entities to prioritize those that match the label exactly or closely
        if (data.data?.suggestedEntities?.length > 0 && label) {
          const lowerLabel = label.toLowerCase().trim();
          data.data.suggestedEntities.sort((a, b) => {
            const aLower = a.label.toLowerCase();
            const bLower = b.label.toLowerCase();
            
            // Exact match gets top priority
            if (aLower === lowerLabel && bLower !== lowerLabel) return -1;
            if (bLower === lowerLabel && aLower !== lowerLabel) return 1;
            
            // Starts with gets second priority
            if (aLower.startsWith(lowerLabel) && !bLower.startsWith(lowerLabel)) return -1;
            if (bLower.startsWith(lowerLabel) && !aLower.startsWith(lowerLabel)) return 1;
            
            // Contains gets third priority
            if (aLower.includes(lowerLabel) && !bLower.includes(lowerLabel)) return -1;
            if (bLower.includes(lowerLabel) && !aLower.includes(lowerLabel)) return 1;
            
            // Default to alphabetical
            return aLower.localeCompare(bLower);
          });
        }
        
        setPreview(data.data);
      } catch (err) {
        console.error('Error fetching node preview:', err);
        setError('Failed to load suggestions');
      } finally {
        setLoading(false);
      }
    };

    // Debounce the preview request to avoid too many API calls
    const debounceTimer = setTimeout(() => {
      fetchPreview();
    }, 500);

    return () => clearTimeout(debounceTimer);
  }, [label, description]);

  if (loading) {
    return (
      <div className="my-4 flex justify-center">
        <FaSpinner className="animate-spin text-blue-500 text-xl" />
      </div>
    );
  }

  if (error) {
    return <div className="text-red-500 text-sm my-2">{error}</div>;
  }

  if (!preview || 
      (!preview.keywords.length && 
       !preview.suggestedEntities.length && 
       !preview.suggestedProperties.length)) {
    return null;
  }

  return (
    <div className="mt-4 border rounded-lg p-3 bg-gray-50">
      <h3 className="text-md font-semibold mb-2">Suggestions</h3>

      {preview.keywords.length > 0 && (
        <div className="mb-3">
          <h4 className="text-sm font-medium text-gray-700 mb-1">Keywords</h4>
          <div className="flex flex-wrap gap-1">
            {preview.keywords.map((keyword, index) => (
              <span 
                key={index}
                className="bg-gray-200 text-gray-800 text-xs px-2 py-1 rounded"
              >
                {keyword}
              </span>
            ))}
          </div>
        </div>
      )}

      {preview.suggestedEntities.length > 0 && (
        <div className="mb-3">
          <h4 className="text-sm font-medium text-gray-700 mb-1">Suggested Entities</h4>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {preview.suggestedEntities.map((entity) => (
              <div 
                key={entity.id}
                className="border rounded p-2 bg-white hover:bg-blue-50 flex justify-between items-center"
              >
                <div>
                  <div className="font-medium">{entity.label}</div>
                  <div className="text-xs text-gray-600 truncate max-w-xs">
                    {entity.description}
                  </div>
                  <div className="text-xs text-gray-500">{entity.id}</div>
                </div>
                <div className="flex space-x-1">
                  <button 
                    className="text-blue-500 hover:text-blue-700 p-1"
                    onClick={() => onEntitySelect && onEntitySelect(entity)}
                    title="Use this entity"
                  >
                    <FaPlus />
                  </button>
                  <a 
                    href={`https://www.wikidata.org/wiki/${entity.id}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-gray-500 hover:text-gray-700 p-1"
                    title="View on Wikidata"
                  >
                    <FaExternalLinkAlt />
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {preview.suggestedProperties.length > 0 && (
        <div>
          <h4 className="text-sm font-medium text-gray-700 mb-1">Suggested Properties</h4>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {preview.suggestedProperties.map((property) => (
              <div 
                key={property.id}
                className="border rounded p-2 bg-white hover:bg-blue-50 flex justify-between items-center"
              >
                <div>
                  <div className="font-medium">{property.label} ({property.id})</div>
                  <div className="text-xs text-gray-600 truncate max-w-xs">
                    {property.description}
                  </div>
                </div>
                <div className="flex space-x-1">
                  <button 
                    className="text-blue-500 hover:text-blue-700 p-1"
                    onClick={() => onPropertySelect && onPropertySelect(property)}
                    title="Use this property"
                  >
                    <FaPlus />
                  </button>
                  <a 
                    href={`https://www.wikidata.org/wiki/Property:${property.id}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-gray-500 hover:text-gray-700 p-1"
                    title="View on Wikidata"
                  >
                    <FaExternalLinkAlt />
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default NodePreviewSuggestions; 