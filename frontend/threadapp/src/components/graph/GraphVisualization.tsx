import React, { useCallback, useEffect, useState, useRef, MouseEvent, useMemo } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Panel,
  NodeMouseHandler,
  ReactFlowInstance,
  Viewport,
  Handle,
  Position,
  NodeProps,
  ConnectionMode,
  OnConnectStart,
  OnConnectEnd,
  useReactFlow,
} from 'reactflow';
import { FaInfoCircle, FaCrosshairs, FaArrowsAlt, FaLink } from 'react-icons/fa';
import { connectNodes } from '../../utils/graphUtils';
import 'reactflow/dist/style.css';

// Define constants for default view
const ORIGIN_X = 0;
const ORIGIN_Y = 0;
const DEFAULT_ZOOM = 1.2;
const ORIGIN_NODE_ID = 'origin-node';

// Define interaction modes
enum InteractionMode {
  MOVE = 'move',
  CONNECT = 'connect'
}

// Define connection state
enum ConnectionState {
  NONE = 'none',
  SOURCE_SELECTED = 'source_selected',
  TARGET_HOVER = 'target_hover'
}

interface GraphNode {
  id: number;
  label: string;
  threadId: number;
  xPosition: number;
  yPosition: number;
  color: string;
  shape: string;
  size: number;
  version: number;
  detailsId?: number;
}

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

interface GraphVisualizationProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  onNodePositionChange?: (nodeId: number, x: number, y: number) => void;
  onNodeDelete?: (nodeId: number) => void;
  onNodeEdit?: (nodeId: number) => void;
  onNodeDetails?: (nodeId: number) => void;
  onConnectionChange?: () => void;
  onEdgeEdit?: (edgeId: number) => void;
}

// Define a custom node component without visible handles
const CustomNode = ({ data, id }: NodeProps) => {
  // Determine node type for styling
  const isCircle = data.style.borderRadius === '50%';
  
  return (
    <>
      <div 
        style={{
          ...data.style,
          border: data.isConnectionSource ? '2px solid #3498db' : 
                 data.isConnectionTarget ? '2px solid #2ecc71' : 
                 data.isBeingDragged ? '2px solid #f39c12' : 
                 data.style.border,
          boxShadow: data.isBeingDragged ? 
                     '0 8px 16px rgba(0, 0, 0, 0.5), inset 0 -5px 12px rgba(0, 0, 0, 0.2), inset 0 5px 12px rgba(255, 255, 255, 0.3)' : 
                     '0 4px 10px rgba(0, 0, 0, 0.4), inset 0 -3px 8px rgba(0, 0, 0, 0.2), inset 0 3px 8px rgba(255, 255, 255, 0.3)',
          transition: 'all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
          transform: data.isBeingDragged ? 'scale(1.08) translateY(-5px)' : 
                     data.isConnectionSource || data.isConnectionTarget ? 'scale(1.05)' : 'scale(1)',
          backgroundImage: isCircle ? 
                          `radial-gradient(circle at 30% 30%, ${adjustColor(data.style.background, 40)}, ${data.style.background} 70%, ${adjustColor(data.style.background, -30)} 100%)` :
                          `linear-gradient(135deg, ${adjustColor(data.style.background, 20)}, ${data.style.background} 60%, ${adjustColor(data.style.background, -20)} 100%)`,
        }}
        className={`node-container ${isCircle ? 'node-circle' : 'node-rect'}`}
      >
        {data.label}
        {isCircle && <div className="node-shine"></div>}
      </div>
      {/* Invisible handles for connections */}
      <Handle
        type="source"
        position={Position.Top}
        id="source"
        style={{ 
          opacity: 0,
          width: '100%',
          height: '100%',
          left: '0',
          top: '0',
          transform: 'none',
          background: 'none',
          border: 'none',
          zIndex: 5
        }}
        isConnectable={data.interactionMode === InteractionMode.CONNECT}
      />
      <Handle
        type="target"
        position={Position.Bottom}
        id="target"
        style={{ 
          opacity: 0,
          width: '100%',
          height: '100%',
          left: '0',
          top: '0',
          transform: 'none',
          background: 'none',
          border: 'none',
          zIndex: 4
        }}
        isConnectable={data.interactionMode === InteractionMode.CONNECT}
      />
    </>
  );
};

// Helper function to lighten or darken colors
const adjustColor = (color: string, amount: number): string => {
  // Handle hex colors
  if (color.startsWith('#')) {
    let hex = color.slice(1);
    
    // Convert 3-digit hex to 6-digit
    if (hex.length === 3) {
      hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
    }
    
    // Convert to RGB
    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);
    
    // Adjust and clamp values
    const newR = Math.min(255, Math.max(0, r + amount));
    const newG = Math.min(255, Math.max(0, g + amount));
    const newB = Math.min(255, Math.max(0, b + amount));
    
    // Convert back to hex
    return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
  }
  
  // For non-hex colors (rgb, etc.), just return the original
  return color;
};

const GraphVisualization: React.FC<GraphVisualizationProps> = ({
  nodes,
  edges,
  onNodePositionChange,
  onNodeDelete,
  onNodeEdit,
  onNodeDetails,
  onConnectionChange,
  onEdgeEdit,
}) => {
  const flowInstanceRef = useRef<ReactFlowInstance | null>(null);
  const [showHelp, setShowHelp] = useState(false);
  // Add interaction mode state
  const [interactionMode, setInteractionMode] = useState<InteractionMode>(InteractionMode.MOVE);
  // Add connection state tracking
  const [connectionState, setConnectionState] = useState<ConnectionState>(ConnectionState.NONE);
  const [sourceNodeId, setSourceNodeId] = useState<string | null>(null);
  const [draggedNodeId, setDraggedNodeId] = useState<string | null>(null);
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);
  // Track connection drag state
  const [connectionDragging, setConnectionDragging] = useState(false);
  const [connectionSourceId, setConnectionSourceId] = useState<string | null>(null);
  const [connectionTargetId, setConnectionTargetId] = useState<string | null>(null);
  // Add click delay tracking
  const [clickStartTime, setClickStartTime] = useState<number | null>(null);
  const [clickStartPosition, setClickStartPosition] = useState<{ x: number; y: number } | null>(null);
  const CLICK_DELAY = 200; // milliseconds
  const CLICK_DISTANCE_THRESHOLD = 5; // pixels

  // Define default viewport with origin and fixed zoom
  const defaultViewport: Viewport = {
    x: ORIGIN_X,
    y: ORIGIN_Y,
    zoom: DEFAULT_ZOOM
  };

  // Convert backend nodes to ReactFlow nodes
  const initialNodes: Node[] = nodes.map((node: GraphNode) => ({
    id: node.id.toString(),
    data: { 
      label: node.label,
      nodeData: node, // Store the full node data for reference
      style: {
        background: node.color,
        width: node.size,
        height: node.size,
        borderRadius: node.shape === 'circle' ? '50%' : '0%',
        border: '1px solid #ccc',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#fff',
        fontWeight: 'bold',
        fontSize: `${Math.max(10, Math.min(16, node.size / 3))}px`, // Calculate font size based on node size
        wordBreak: 'break-word',
        textAlign: 'center',
        padding: '2px',
        overflow: 'hidden',
      },
      interactionMode: interactionMode,
      isConnectionSource: false,
      isConnectionTarget: false,
      isBeingDragged: false
    },
    position: { x: node.xPosition || 0, y: node.yPosition || 0 }, // Ensure we have non-null values
    // Add a custom node type for custom rendering
    type: 'customNode',
  }));

  // Add origin node if no nodes exist
  if (initialNodes.length === 0) {
    initialNodes.push({
      id: ORIGIN_NODE_ID,
      data: { 
        label: 'Origin',
        style: {
          background: '#ff9900',
          width: 40,
          height: 40,
          borderRadius: '50%',
          border: '1px dashed #333',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          fontWeight: 'bold',
          fontSize: '10px',
          opacity: 0.5,
        },
        interactionMode: interactionMode,
        isConnectionSource: false,
        isConnectionTarget: false,
        isBeingDragged: false
      },
      position: { x: 0, y: 0 },
      type: 'customNode',
    });
  }

  // Convert backend edges to ReactFlow edges
  const initialEdges: Edge[] = edges.map((edge: GraphEdge) => ({
    id: edge.id.toString(),
    source: edge.sourceNodeId.toString(),
    target: edge.targetNodeId.toString(),
    label: edge.label,
    type: edge.type,
    style: { stroke: edge.color || '#555' },
    animated: false,
  }));

  const [reactFlowNodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [reactFlowEdges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // Update the internal nodes when props change
  useEffect(() => {
    const updatedNodes = [...initialNodes];
    
    // Add origin node if no nodes exist
    if (nodes.length === 0 && !updatedNodes.some(node => node.id === ORIGIN_NODE_ID)) {
      updatedNodes.push({
        id: ORIGIN_NODE_ID,
        data: { 
          label: 'Origin',
          style: {
            background: '#ff9900',
            width: 40,
            height: 40,
            borderRadius: '50%',
            border: '1px dashed #333',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontWeight: 'bold',
            fontSize: '10px',
            opacity: 0.5,
          },
          interactionMode: interactionMode,
          isConnectionSource: false,
          isConnectionTarget: false,
          isBeingDragged: false
        },
        position: { x: 0, y: 0 },
        type: 'customNode',
      });
    } else if (nodes.length > 0) {
      // Remove origin node if there are real nodes
      const originNodeIndex = updatedNodes.findIndex(node => node.id === ORIGIN_NODE_ID);
      if (originNodeIndex !== -1) {
        updatedNodes.splice(originNodeIndex, 1);
      }
    }
    
    // Update font size for all nodes based on node size
    updatedNodes.forEach(node => {
      if (node.id !== ORIGIN_NODE_ID) {
        const nodeData = (node.data as any).nodeData;
        if (nodeData && nodeData.size) {
          node.data.style = {
            ...node.data.style,
            fontSize: `${Math.min(8, Math.min(12, nodeData.size / 5))}px`,
          };
        }
      }
    });
    
    setNodes(updatedNodes);
  }, [nodes, setNodes]);

  // Update interaction mode for all nodes when it changes
  useEffect(() => {
    setNodes((nds) => 
      nds.map((node) => ({
        ...node,
        data: {
          ...node.data,
          interactionMode: interactionMode
        }
      }))
    );
    
    // Reset connection state when changing modes
    setConnectionState(ConnectionState.NONE);
    setSourceNodeId(null);
    setHoveredNodeId(null);
  }, [interactionMode, setNodes]);

  // Update connection highlights when connection state changes
  useEffect(() => {
    setNodes(nodes => 
      nodes.map(node => ({
        ...node,
        data: {
          ...node.data,
          isConnectionSource: node.id === sourceNodeId || node.id === connectionSourceId,
          isConnectionTarget: (node.id === hoveredNodeId && node.id !== sourceNodeId) || 
                             (node.id === connectionTargetId && node.id !== connectionSourceId),
          isBeingDragged: node.id === draggedNodeId
        }
      }))
    );
  }, [sourceNodeId, hoveredNodeId, draggedNodeId, connectionSourceId, connectionTargetId, setNodes]);

  // Update the internal edges when props change
  useEffect(() => {
    setEdges(initialEdges);
  }, [edges, setEdges]);

  const onConnect = useCallback(
    async (params: Connection) => {
      try {
        // First update the UI
        setEdges((eds) => addEdge(params, eds));
        
        // Then persist to database
        if (params.source && params.target) {
          const response = await connectNodes(
            parseInt(params.source),
            parseInt(params.target),
            { 
              type: 'default',
              weight: 1,
              color: '#555555'
            }
          );
          
          if (!response.success) {
            // If the API call fails, revert the UI change
            setEdges((eds) => eds.filter(edge => 
              edge.id !== `${params.source}-${params.target}`
            ));
            throw new Error(response.message || 'Failed to create connection');
          }
          
          // Notify parent component about the connection change
          if (onConnectionChange) {
            onConnectionChange();
          }
        }
      } catch (error) {
        console.error('Error creating connection:', error);
        // You might want to show an error message to the user here
      }
    },
    [setEdges, onConnectionChange]
  );

  // Handle node drag start
  const onNodeDragStart: NodeMouseHandler = useCallback(
    (event, node) => {
      setDraggedNodeId(node.id);
      setClickStartTime(Date.now());
      setClickStartPosition({ x: event.clientX, y: event.clientY });
    },
    []
  );

  // Only trigger the position change callback after drag completes
  const onNodeDragStop = useCallback(
    (event: React.MouseEvent, node: Node) => {
      // Don't save position changes for origin node
      if (node.id === ORIGIN_NODE_ID) return;
      
      const endTime = Date.now();
      const endPosition = { x: event.clientX, y: event.clientY };
      
      // Check if this was a click or a drag
      const isClick = clickStartTime && 
                     endTime - clickStartTime < CLICK_DELAY && 
                     clickStartPosition && 
                     Math.abs(endPosition.x - clickStartPosition.x) < CLICK_DISTANCE_THRESHOLD &&
                     Math.abs(endPosition.y - clickStartPosition.y) < CLICK_DISTANCE_THRESHOLD;
      
      if (isClick && interactionMode === InteractionMode.MOVE && onNodeDetails) {
        // If it was a click and we're in move mode, show node details
        onNodeDetails(parseInt(node.id));
      } else if (onNodePositionChange && node.position.x !== null && node.position.y !== null) {
        // If it was a drag, update position
        onNodePositionChange(
          parseInt(node.id),
          node.position.x,
          node.position.y
        );
      }
      
      // Reset states
      setDraggedNodeId(null);
      setClickStartTime(null);
      setClickStartPosition(null);
    },
    [onNodePositionChange, onNodeDetails, interactionMode, clickStartTime, clickStartPosition]
  );

  // Enhanced node click handler for connection mode
  const onNodeClick: NodeMouseHandler = useCallback(
    async (event, node) => {
      // Don't handle click for origin node
      if (node.id === ORIGIN_NODE_ID) return;
      
      if (interactionMode === InteractionMode.CONNECT) {
        // Handle connection logic
        if (connectionState === ConnectionState.NONE) {
          // Select source node
          setConnectionState(ConnectionState.SOURCE_SELECTED);
          setSourceNodeId(node.id);
        } else if (connectionState === ConnectionState.SOURCE_SELECTED) {
          // If we have a source node and click on a different node, create connection
          if (sourceNodeId && sourceNodeId !== node.id) {
            try {
              // Create the connection in the database
              const response = await connectNodes(
                parseInt(sourceNodeId),
                parseInt(node.id),
                { 
                  type: 'default',
                  weight: 1,
                  color: '#555555'
                }
              );
              
              if (response.success) {
                // Add the connection to the UI
                const newConnection: Connection = {
                  source: sourceNodeId,
                  target: node.id,
                  sourceHandle: 'source',
                  targetHandle: 'target'
                };
                
                setEdges((eds) => addEdge(newConnection, eds));
                
                // Notify parent component about the connection change
                if (onConnectionChange) {
                  onConnectionChange();
                }
              } else {
                throw new Error(response.message || 'Failed to create connection');
              }
            } catch (error) {
              console.error('Error creating connection:', error);
              // You might want to show an error message to the user here
            }
          }
          
          // Reset connection state
          setConnectionState(ConnectionState.NONE);
          setSourceNodeId(null);
          setHoveredNodeId(null);
        }
      }
    },
    [interactionMode, connectionState, sourceNodeId, setEdges, onConnectionChange]
  );
  
  // Handle connection drag start
  const onConnectStart: OnConnectStart = useCallback((event, { nodeId, handleId }) => {
    setConnectionDragging(true);
    setConnectionSourceId(nodeId);
  }, []);

  // Handle connection drag end
  const onConnectEnd: OnConnectEnd = useCallback((event) => {
    setConnectionDragging(false);
    setConnectionSourceId(null);
    setConnectionTargetId(null);
  }, []);

  // Enhanced node mouse enter handler for highlighting potential targets
  const onNodeMouseEnter: NodeMouseHandler = useCallback(
    (event, node) => {
      // When in connect mode with source selected
      if (interactionMode === InteractionMode.CONNECT && 
          connectionState === ConnectionState.SOURCE_SELECTED && 
          sourceNodeId !== node.id) {
        setHoveredNodeId(node.id);
      }
      
      // When dragging a connection
      if (connectionDragging && connectionSourceId !== node.id) {
        setConnectionTargetId(node.id);
      }
    },
    [interactionMode, connectionState, sourceNodeId, connectionDragging, connectionSourceId]
  );

  // Enhanced node mouse leave handler for highlighting
  const onNodeMouseLeave: NodeMouseHandler = useCallback(
    (event, node) => {
      if (node.id === hoveredNodeId) {
        setHoveredNodeId(null);
      }
      
      if (node.id === connectionTargetId) {
        setConnectionTargetId(null);
      }
    },
    [hoveredNodeId, connectionTargetId]
  );

  // Double click handler for node editing
  const onNodeDoubleClick: NodeMouseHandler = useCallback(
    (event, node) => {
      // Don't handle double click for origin node
      if (node.id === ORIGIN_NODE_ID) return;
      
      if (onNodeEdit) {
        onNodeEdit(parseInt(node.id));
      }
    },
    [onNodeEdit]
  );

  // Toggle mode between move and connect
  const toggleInteractionMode = useCallback(() => {
    setInteractionMode(mode => 
      mode === InteractionMode.MOVE ? InteractionMode.CONNECT : InteractionMode.MOVE
    );
  }, []);

  // Clear connection state on background click
  const onPaneClick = useCallback(() => {
    setConnectionState(ConnectionState.NONE);
    setSourceNodeId(null);
    setHoveredNodeId(null);
  }, []);

  // Save instance when flow is initialized
  const onInit = useCallback((instance: ReactFlowInstance) => {
    flowInstanceRef.current = instance;
  }, []);

  // Center view to origin
  const centerView = useCallback(() => {
    if (flowInstanceRef.current) {
      flowInstanceRef.current.setViewport({
        x: ORIGIN_X,
        y: ORIGIN_Y,
        zoom: DEFAULT_ZOOM,
      }, { duration: 800 });
    }
  }, []);

  // Add edge double click handler
  const onEdgeDoubleClick = useCallback(
    (event: React.MouseEvent, edge: Edge) => {
      if (onEdgeEdit) {
        onEdgeEdit(parseInt(edge.id));
      }
    },
    [onEdgeEdit]
  );

  const toggleHelp = () => {
    setShowHelp(prev => !prev);
  };

  // Memoize nodeTypes to prevent recreation on each render
  const nodeTypes = useMemo(() => ({ customNode: CustomNode }), []);

  return (
    <div style={{ width: '100%', height: '600px', position: 'relative' }}>
      <style>
        {`
          .react-flow__edge-path {
            stroke-width: 2;
            transition: stroke 0.3s, stroke-width 0.3s;
          }

          .react-flow__edge:hover .react-flow__edge-path {
            stroke-width: 3;
            stroke: #3498db;
          }

          @keyframes pulse {
            0% { box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4), inset 0 -3px 8px rgba(0, 0, 0, 0.2), inset 0 3px 8px rgba(255, 255, 255, 0.3); }
            50% { box-shadow: 0 6px 15px rgba(0, 0, 0, 0.45), inset 0 -4px 10px rgba(0, 0, 0, 0.25), inset 0 4px 10px rgba(255, 255, 255, 0.35); }
            100% { box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4), inset 0 -3px 8px rgba(0, 0, 0, 0.2), inset 0 3px 8px rgba(255, 255, 255, 0.3); }
          }
          
          @keyframes shimmer {
            0% { opacity: 0.5; transform: translate(20%, 10%) scale(1); }
            50% { opacity: 0.7; transform: translate(15%, 5%) scale(1.1); }
            100% { opacity: 0.5; transform: translate(20%, 10%) scale(1); }
          }

          .node-container {
            position: relative;
            z-index: 1;
            overflow: hidden;
          }

          .node-container:hover {
            animation: pulse 2s infinite;
            z-index: 10;
          }

          .node-circle {
            filter: drop-shadow(0 2px 5px rgba(0, 0, 0, 0.2));
          }
          
          .node-rect {
            filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.2));
          }

          .node-shine {
            position: absolute;
            top: 0;
            left: 0;
            width: 60%;
            height: 60%;
            border-radius: 50%;
            background: radial-gradient(circle at center, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0.5) 30%, rgba(255, 255, 255, 0) 70%);
            pointer-events: none;
            transform: translate(20%, 10%);
            opacity: 0.5;
            animation: shimmer 4s infinite ease-in-out;
          }

          .node-container:hover .node-shine {
            animation: shimmer 2s infinite ease-in-out;
          }

          .react-flow__node.selected .node-container {
            box-shadow: 0 6px 16px rgba(52, 152, 219, 0.5), inset 0 -3px 8px rgba(0, 0, 0, 0.2), inset 0 3px 8px rgba(255, 255, 255, 0.3) !important;
            border: 2px solid #3498db !important;
            transform: scale(1.05);
          }
          
          /* Animation for edges */
          @keyframes dash {
            from {
              stroke-dashoffset: 20;
            }
            to {
              stroke-dashoffset: 0;
            }
          }
          
          .react-flow__edge.selected .react-flow__edge-path {
            stroke-dasharray: 5;
            animation: dash 1s linear infinite;
            stroke: #3498db;
            stroke-width: 3;
          }

          .react-flow__edge:hover .react-flow__edge-path {
            stroke-width: 3;
            stroke: #3498db;
            cursor: pointer;
          }
        `}
      </style>
      <ReactFlow
        nodes={reactFlowNodes}
        edges={reactFlowEdges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onConnectStart={onConnectStart}
        onConnectEnd={onConnectEnd}
        onNodeDragStart={onNodeDragStart}
        onNodeDragStop={onNodeDragStop}
        onNodeDoubleClick={onNodeDoubleClick}
        onNodeClick={onNodeClick}
        onNodeMouseEnter={onNodeMouseEnter}
        onNodeMouseLeave={onNodeMouseLeave}
        onEdgeDoubleClick={onEdgeDoubleClick}
        onPaneClick={onPaneClick}
        onInit={onInit}
        defaultViewport={defaultViewport}
        nodesDraggable={interactionMode === InteractionMode.MOVE}
        nodesConnectable={interactionMode === InteractionMode.CONNECT}
        elementsSelectable={true}
        nodeTypes={nodeTypes}
        connectionMode={ConnectionMode.Loose}
      >
        <Background />
        <Controls />
        <Panel position="top-right">
          <div className="flex gap-2">
            {/* Mode toggle button */}
            <button 
              onClick={toggleInteractionMode}
              className={`bg-white p-2 rounded-full shadow transition-colors ${
                interactionMode === InteractionMode.MOVE ? 'bg-blue-100' : 'bg-green-100'
              }`}
              title={`Current mode: ${interactionMode === InteractionMode.MOVE ? 'Move' : 'Connect'}`}
            >
              {interactionMode === InteractionMode.MOVE ? (
                <FaArrowsAlt size={18} className="text-blue-500" />
              ) : (
                <FaLink size={18} className="text-green-500" />
              )}
            </button>
            
            {/* Help button */}
            <button 
              onClick={toggleHelp}
              className="bg-white p-2 rounded-full shadow hover:bg-gray-100 transition-colors"
              title="Help"
            >
              <FaInfoCircle size={18} className="text-blue-500" />
            </button>
          </div>
          
          {showHelp && (
            <div className="absolute top-10 right-0 bg-white p-3 rounded shadow-md w-64 z-50">
              <button 
                className="mb-3 w-full px-3 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 flex items-center justify-center gap-2 text-sm opacity-80 hover:opacity-100 transition-opacity"
                onClick={() => {
                  centerView();
                  setShowHelp(false);
                }}
              >
                <FaCrosshairs size={14} />
                <span>Center View</span>
              </button>
              
              <div className="text-sm space-y-2 text-gray-600">
                <p>• Click mode button to toggle between:</p>
                <p>  - Move: Drag nodes to reposition</p>
                <p>  - Connect: Click source node, then target node</p>
                <p>• Click for node details</p>
                <p>• Use mouse wheel to zoom</p>
              </div>
            </div>
          )}
        </Panel>
      </ReactFlow>
    </div>
  );
};

export default GraphVisualization; 