import React, { useState } from 'react';

export interface Tag {
  id: number;
  label: string;
  description: string;
  colorCodeString: string;
  wikidataEntityId: string;
}

interface TagProps {
  tag: Tag;
  onRemove?: () => void;
  className?: string;
}

const Tag: React.FC<TagProps> = ({ tag, onRemove, className = '' }) => {
  const [isHovering, setIsHovering] = useState(false);
  
  const getContrastColor = (hexColor: string): string => {
    const color = hexColor.replace('#', '');
    const r = parseInt(color.substr(0, 2), 16);
    const g = parseInt(color.substr(2, 2), 16);
    const b = parseInt(color.substr(4, 2), 16);
    
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.5 ? '#555555' : '#FFFFFF';
  };

  const tagColor = tag.colorCodeString || '#E5E7EB';
  const textColor = getContrastColor(tagColor);
  const borderColor = tagColor === '#FFFFFF' || tagColor === '#ffffff' ? '#D1D5DB' : 'rgba(0,0,0,0.15)';

  return (
    <span
      className={`inline-flex items-center px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 hover:translate-y-[-1px] ${className}`}
      style={{
        backgroundColor: tagColor,
        color: textColor,
        border: `1.5px solid ${borderColor}`,
        boxShadow: '0 2px 3px rgba(0, 0, 0, 0.08)'
      }}
      title={tag.description || tag.label}
    >
      {tag.label}
      {onRemove && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          onMouseEnter={() => setIsHovering(true)}
          onMouseLeave={() => setIsHovering(false)}
          className="ml-2 flex items-center justify-center text-base font-bold transition-all"
          style={{ 
            color: isHovering ? '#FF4040' : textColor,
            transform: isHovering ? 'scale(1.3)' : 'scale(1)'
          }}
          aria-label={`Remove ${tag.label} tag`}
        >
          ×
        </button>
      )}
    </span>
  );
};

export default Tag; 